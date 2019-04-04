package com.example.testapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import  com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener{
            createAndLoginEmail()
        }
    }

    //이메일 회원가입 및 로그인 메소드
    fun createAndLoginEmail() {

        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                } else if (task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this,
                        task.exception?.message, Toast.LENGTH_SHORT).show()
                } else {
                    signinEmail()
                }
            }

    }

    //로그인 메소드
    fun signinEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    moveMainPage(auth?.currentUser)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }

    }

    fun moveMainPage(user: FirebaseUser?) {

        // User is signed in
        if (user != null) {
            startActivity(Intent(this, FirstMainActivity::class.java))
            finish()
        }
    }
}

