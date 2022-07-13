package com.thw.inventory_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailEt: EditText
    private lateinit var passwordEt: EditText
    private lateinit var emailEtContainer: TextInputLayout
    private lateinit var passwordEtContainer: TextInputLayout
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        getSupportActionBar()?.hide()
        getActionBar()?.hide()

        emailEt = findViewById(R.id.email_edt_text)
        passwordEt = findViewById(R.id.pass_edt_text)

        emailEtContainer = findViewById(R.id.email_edt_text_container)
        passwordEtContainer = findViewById(R.id.pass_edt_text_container)

        loginBtn = findViewById(R.id.login_btn)

        //User Read:
        //Name: thwobernburg
        //PW: thw-her0s-2022

        //User Edit
        //Name: thwobernburg_admin
        //PW: thw-her0s-1307

        loginBtn.setOnClickListener {
            var email: String = emailEt.text.toString()
            var password: String = passwordEt.text.toString()

            if(TextUtils.isEmpty(email)) {
                emailEtContainer.isErrorEnabled = true
                emailEtContainer.error = "Feld darf nicht leer sein"
            } else {
                emailEtContainer.isErrorEnabled = false
            }
            if(TextUtils.isEmpty(password)) {
                passwordEtContainer.isErrorEnabled = true
                passwordEtContainer.error = "Feld darf nicht leer sein"
            }else {
                passwordEtContainer.isErrorEnabled = false
            }

            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                email = email + "@thw.thw"
                Log.e("Error", "Trying login")
                auth = Firebase.auth
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.e("Error", "Login success")
                            Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            emailEtContainer.isErrorEnabled = true
                            emailEtContainer.error = "Benutzernamen überprüfen"
                            passwordEtContainer.isErrorEnabled = true
                            passwordEtContainer.error = "Passwort überprüfen"
                            Toast.makeText(this, "Login Fehlgeschlagen", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}