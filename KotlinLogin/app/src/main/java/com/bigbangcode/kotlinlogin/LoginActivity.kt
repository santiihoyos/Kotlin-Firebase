package com.bigbangcode.kotlinlogin

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*


class LoginActivity : AppCompatActivity(), FacebookCallback<LoginResult>, GoogleApiClient.OnConnectionFailedListener {

    val RC_SIGN_IN = 123

    //region Members
    private val mButtonEnter by lazy { findViewById(R.id.Button_Enter_LoginActivity) as Button }
    private val mEditTextUser by lazy { findViewById(R.id.EditText_User_LoginActivity) as EditText }
    private val mEditTextPass by lazy { findViewById(R.id.EditText_Password_LoginActivity) as EditText }
    private val mLoginButtonFaceBook by lazy { findViewById(R.id.LoginButton_LoginFacebook_LoginActivity) as LoginButton }
    private val mButtonEnterGoogleSigIn by lazy { findViewById(R.id.Button_GoogleSigIn_LoginActivity) }
    private var mProgressDialog: ProgressDialog? = null

    private var mAuth: FirebaseAuth? = null
    private val mCallbackManager: CallbackManager = CallbackManager.Factory.create()
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = FirebaseAuth.AuthStateListener { firebaseAuth ->

        val user = firebaseAuth.currentUser

        if (user != null) {
            // User is signed in
            Log.d("INFO", "onAuthStateChanged:signed_in:" + user.uid)
        } else {
            // User is signed out
            Log.d("INFO", "onAuthStateChanged:signed_out");
        }

    }

    //endregion

    /**
     * On Create
     * @param savedInstanceState bundle of view saved
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mButtonEnter.setOnClickListener({ click_mButtonEnter(it as Button) })
        mButtonEnterGoogleSigIn.setOnClickListener({ click_mButtonEnterGoogleSigIn() })
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.setCanceledOnTouchOutside(false)
        mProgressDialog?.isIndeterminate = true
        mProgressDialog?.setMessage(resources.getText(R.string.information_cancel_progressDialog_LoginActivity))
        mProgressDialog?.setOnCancelListener { onCancel() }

        FirebaseApp.initializeApp(this)
        mAuth = FirebaseAuth.getInstance()

        mLoginButtonFaceBook.setReadPermissions("email")
        LoginManager.getInstance().registerCallback(mCallbackManager, this)

        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.id_cliente_oAuth))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this).enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    /**
     * Event Click on button mButtonEnter
     * @param button: Button
     */
    private fun click_mButtonEnter(button: Button) {

        val user = mEditTextUser.text.toString()
        val pass = mEditTextPass.text.toString()

        mProgressDialog?.show()

        mAuth?.signInWithEmailAndPassword(user, pass)
                ?.addOnCompleteListener(this, { onComplete(it) })

        button.isEnabled = false
    }

    /**
     * Click en loggear con Google Sign-In
     */
    private fun click_mButtonEnterGoogleSigIn() {

        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Listener de finalizacion de login en firebase en general,
     * vengan de Facebook, correo y pass o  Google Sig-In y Twitter
     * @param task login task
     */
    private fun onComplete(task: Task<AuthResult>) {

        if (task.isSuccessful) {
            Toast.makeText(this, resources.getText(R.string.authenticationOk_LoginActivity), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, resources.getText(R.string.authenticationFail_LoginActivity), Toast.LENGTH_SHORT).show()
        }

        mProgressDialog?.hide()
        mButtonEnter.isEnabled = true
    }

    /**
     * Cancel FaceBook an orProgress
     */
    override fun onCancel() {
        Toast.makeText(this, "Cancelado login con facebook", Toast.LENGTH_SHORT).show()
    }

    /**
     * onSuccess of Facebook login
     */
    override fun onSuccess(p0: LoginResult?) {
        val credential = FacebookAuthProvider.getCredential(p0?.accessToken!!.token)
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(this, { onComplete(it) })
    }

    /**
     * Error on FaceBookLogin
     */
    override fun onError(p0: FacebookException?) {
        Toast.makeText(this, "Autenticación con facebook fallida!", Toast.LENGTH_SHORT).show()
    }

    /**
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {

                val account = result.signInAccount
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                mAuth?.signInWithCredential(credential)?.addOnCompleteListener({ onComplete(it) })

            } else {
                Toast.makeText(this, "Error login con Google", Toast.LENGTH_SHORT).show()
            }

        } else {
            mCallbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    public override fun onStart() {
        super.onStart()
        mAuth?.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth?.removeAuthStateListener(mAuthListener!!)
        }
    }
}