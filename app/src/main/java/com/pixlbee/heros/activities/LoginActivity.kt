package com.pixlbee.heros.activities

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pixlbee.heros.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailEt: EditText
    private lateinit var passwordEt: EditText
    private lateinit var emailEtContainer: TextInputLayout
    private lateinit var passwordEtContainer: TextInputLayout
    private lateinit var loginBtn: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = resources.getColor(R.color.status_bar_color)

        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        actionBar?.hide()

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
                emailEtContainer.error = resources.getString(R.string.error_field_empty)
            } else {
                emailEtContainer.isErrorEnabled = false
            }
            if(TextUtils.isEmpty(password)) {
                passwordEtContainer.isErrorEnabled = true
                passwordEtContainer.error =resources.getString(R.string.error_field_empty)
            }else {
                passwordEtContainer.isErrorEnabled = false
            }

            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                email = email + "@thw.thw"
                auth = Firebase.auth
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, resources.getString(R.string.login_successful), Toast.LENGTH_LONG).show()

                            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString("current_user", email.replaceAfter("@", "").replace("@", ""))
                            editor.commit()

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            emailEtContainer.isErrorEnabled = true
                            emailEtContainer.error = resources.getString(R.string.error_check_username)
                            passwordEtContainer.isErrorEnabled = true
                            passwordEtContainer.error = resources.getString(R.string.error_check_password)
                            Toast.makeText(this, resources.getString(R.string.error_login_failed), Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}