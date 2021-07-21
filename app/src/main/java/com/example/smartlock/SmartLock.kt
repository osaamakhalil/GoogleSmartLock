package com.example.smartlock

import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.credentials.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.OnCompleteListener


class SmartLock : AppCompatActivity() {

    private val TAG: String = SmartLock::class.java.simpleName
    private val KEY_IS_RESOLVING = "is_resolving"

    private val IS_REQUESTING: String = "is_requesting"

    private val RC_CREDENTIALS_READ = 2
    private val RC_CREDENTIALS_SAVE = 3

    private var mIsResolving = false
    private var mIsRequesting = false

    lateinit var mCredentialsClient: CredentialsClient

    lateinit var mUserNumEditText: EditText
    lateinit var mPasswordEditText: EditText
    lateinit var mSignInButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUserNumEditText = findViewById(R.id.editTextPhoneNum)
        mPasswordEditText = findViewById(R.id.editTextTextPassword)
        mSignInButton = findViewById(R.id.SignInButton)

        mSignInButton.setOnClickListener {

            val userNum: String = mUserNumEditText.text.toString()
            val password: String = mPasswordEditText.text.toString()

            if (userNum.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }

            val credential: Credential = Credential.Builder(userNum)
                .setPassword(password) // Important: only store passwords in this field.
                // Android autofill uses this value to complete
                // sign-in forms, so repurposing this field will
                // likely cause errors.
                .build()
            saveCredential(credential)
        }

        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING)
            mIsRequesting = savedInstanceState.getBoolean(IS_REQUESTING)
        }
        //To force the display the Smart Lock for Passwords save dialog
        //Targeting Android O and above
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mCredentialsClient = Credentials.getClient(this, options)
            val autofillManager = getSystemService(AutofillManager::class.java)
            autofillManager.commit()
            window
                .decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        } else {
            mCredentialsClient = Credentials.getClient(this)

        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_RESOLVING, mIsResolving)
        outState.putBoolean(IS_REQUESTING, mIsRequesting)
    }

    override fun onStart() {
        super.onStart()
        if (!mIsResolving) {
            requestCredentials()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult:$requestCode:$resultCode:$data")

        if (requestCode == RC_CREDENTIALS_READ) {
            if (resultCode === RESULT_OK) {
                //to get the users credentials and use them to sign in
                val credential: Credential? = data!!.getParcelableExtra(Credential.EXTRA_KEY)
                if (credential != null) {
                    onCredentialRetrieved(credential)
                } else {
                    Log.e(TAG, "Credential Read: NOT OK")
                    Toast.makeText(this, "Credential Read Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (requestCode == RC_CREDENTIALS_SAVE) {
            if (resultCode === RESULT_OK) {
                Log.d(TAG, "SAVE: OK")
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "Credential save failed.")
            }
            goToContent()
        }
        mIsResolving = false
    }

    private fun requestCredentials() {
        mIsRequesting = true

        val mCredentialRequest: CredentialRequest.Builder = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
        // showProgress();
        mCredentialsClient.request(mCredentialRequest.build()).addOnCompleteListener(
            OnCompleteListener { task ->
                //hideProgress();
                mIsRequesting = false
                if (task.isSuccessful) {
                    // See "Handle successful credential requests"
                    task.result?.credential?.let { onCredentialRetrieved(it) }
                    return@OnCompleteListener
                }
                val e = task.exception
                if (e is ResolvableApiException) {
                    // This is most likely the case where the user has multiple saved
                    // credentials and needs to pick one. This requires showing UI to
                    // resolve the read request.
                    val rae: ResolvableApiException = e
                    resolveResult(rae, RC_CREDENTIALS_READ)
                    return@OnCompleteListener
                } else {
                    Log.w(TAG, "request: not handling exception", e)
                }
            })
    }

    private fun onCredentialRetrieved(credential: Credential) {

        val accountType: String? = credential.accountType
        if (accountType == null) {
            // Sign the user in with information from the Credential.
            signInWithPassword(credential.id, credential.password)
        } else {
            Log.d(TAG, "Retrieved credential invalid, so delete retrieved credential.")
            Toast.makeText(
                this,
                "Retrieved credentials are invalid, so will be deleted.",
                Toast.LENGTH_LONG
            ).show()
            deleteCredential(credential)
            requestCredentials()
        }
    }

    /************************************************************/
    private fun signInWithPassword(id: String, password: String?) {
        // use the user's ID and password from the Credential object to complete your app's sign-in process.
        mUserNumEditText.setText(id)
        mPasswordEditText.setText(password)

    }
    /************************************************************/

    private fun resolveResult(rae: ResolvableApiException, requestCode: Int) {
        if (mIsResolving) {
            Log.w(TAG, "resolveResult: already resolving.")
            return
        }
        Log.d(TAG, "Resolving: $rae")
        mIsResolving = try {
            //to pormpt the user to select a saved account
            rae.startResolutionForResult(this, requestCode)
            true
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to send Credentials intent.", e)
            //hideProgress()
            false
        }
    }


    //whenSignInButtonClicked
    private fun saveCredential(credential: Credential) {

        //showProgress()

        mCredentialsClient.save(credential).addOnCompleteListener(
            OnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "SAVE: OK")
                    showToast("save:SUCCESS")
                    //hideProgress()
                    goToContent()
                    return@OnCompleteListener
                }
                val e: Exception? = task.exception
                if (e is ResolvableApiException) {
                    // Try to resolve the save request. This will prompt the user if
                    // the credential is new.
                    val rae = e as ResolvableApiException
                    try {
                        resolveResult(rae, RC_CREDENTIALS_SAVE)
                    } catch (exception: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to send resolution.", exception)
                        showToast("save failed")
                    }
                } else {

                    Log.w(TAG, "save:FAILURE", e)
                    showToast("Unexpected error, see logs for detals")
                    //hideProgress()
                }
            })
    }

    private fun deleteCredential(mCurrentCredential: Credential) {
        mCredentialsClient.delete(mCurrentCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Credential successfully deleted.")
            } else {
                Log.d(TAG, "Credential not deleted successfully.")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun goToContent() {
        startActivity(Intent(this, ContentActivity::class.java))
        finish()
    }
}
