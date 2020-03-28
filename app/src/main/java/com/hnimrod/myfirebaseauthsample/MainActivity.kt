package com.hnimrod.myfirebaseauthsample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val statusTextView by lazy { findViewById<TextView>(R.id.status) }
    private val nameTextView by lazy { findViewById<TextView>(R.id.name) }
    private val mailTextView by lazy { findViewById<TextView>(R.id.mail) }
    private val uidTextView by lazy { findViewById<TextView>(R.id.uid) }
    private val signInButton by lazy { findViewById<SignInButton>(R.id.button_signin) }
    private val signOutButton by lazy { findViewById<Button>(R.id.button_signout) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progress) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, options)
        auth = FirebaseAuth.getInstance()

        signInButton.setOnClickListener { signIn() }
        signOutButton.setOnClickListener { signOut() }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java) ?: return
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google sign in failed: $e", Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        statusTextView.text =
            getString(if (user != null) R.string.status_signed_in else R.string.status_signed_out)
        nameTextView.text = user?.displayName
        mailTextView.text = user?.email
        uidTextView.text = user?.uid

        signInButton.isEnabled = user == null
        signOutButton.isEnabled = user != null
    }

    private fun signIn() {
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        progressBar.isVisible = true
        auth.signInWithCredential(GoogleAuthProvider.getCredential(acct.idToken, null))
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    updateUI(auth.currentUser)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
                progressBar.isVisible = false
            }
    }

    companion object {
        private const val TAG = "MyFirebaseAuth"
        private const val RC_SIGN_IN = 1234
    }
}
