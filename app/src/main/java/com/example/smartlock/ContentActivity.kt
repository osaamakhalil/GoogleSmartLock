package com.example.smartlock

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.Button
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.CredentialsClient
import com.google.android.gms.auth.api.credentials.CredentialsOptions

class ContentActivity : AppCompatActivity() {

    lateinit var mSignInButton: Button
    lateinit var mCredentialsClient: CredentialsClient




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

//        //To force the display the Smart Lock for Passwords save dialog
//       // Targeting Android O and above
//        val options = CredentialsOptions.Builder()
//            .forceEnableSaveDialog()
//            .build()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mCredentialsClient = Credentials.getClient(this, options)
//            val autofillManager = getSystemService(AutofillManager::class.java)
//            autofillManager.commit()
//            window
//                .decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
//        } else {
//            mCredentialsClient = Credentials.getClient(this)
//
//        }
//        mSignInButton.setOnClickListener {
//            mCredentialsClient.disableAutoSignIn()
//            val intent = Intent(this,SmartLock::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//            startActivity(intent)
//            finish()
//
//        }

    }
}