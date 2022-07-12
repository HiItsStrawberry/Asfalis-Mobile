package com.example.asfalis_mobile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.asfalis_mobile.Helpers.createToast
import com.example.asfalis_mobile.Helpers.getMapValue
import com.example.asfalis_mobile.Helpers.printErrorMessage
import com.example.asfalis_mobile.Helpers.validateInput
import com.example.asfalis_mobile.databinding.ActivityLoginBinding
import com.example.asfalis_mobile.models.JwtDTO
import com.example.asfalis_mobile.models.LoginPersonalDTO
import com.example.asfalis_mobile.networks.RetrofitInstance
import com.example.asfalis_mobile.services.PrefService
import com.example.asfalis_mobile.services.UserService
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var userApi: UserService
    private lateinit var prefService: PrefService
    private lateinit var binding : ActivityLoginBinding


    private var isAvailable: Boolean = false
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize all object
        prefService = PrefService(this)
        executor = ContextCompat.getMainExecutor(this)
        userApi = RetrofitInstance.getRetrofitInstance().create(UserService::class.java)

        val biometricManager = BiometricManager.from(this)


        // Check if the current device is available for fingerprint login
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL or BIOMETRIC_WEAK)) {

            // fingerprint is available
            BiometricManager.BIOMETRIC_SUCCESS -> {
                isAvailable = true
                binding.ivFingerprint.visibility = View.VISIBLE
            }

            // fingerprint is available but none is registered
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED  -> {
                this.createToast(getString(R.string.bio_not_registered),1)
            }

            // fingerprint is not available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                binding.ivFingerprint.visibility = View.GONE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE  ->
                binding.ivFingerprint.visibility = View.GONE
        }


        // Authentication for fingerprint input
        biometricPrompt = BiometricPrompt(this, executor, object: BiometricPrompt.AuthenticationCallback() {

            // Error
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                applicationContext.createToast(errString.toString(), 2)
            }

            // Invalid fingerprint
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                applicationContext.createToast(getString(R.string.bio_invalid), 2)
            }

            // Valid fingerprint
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                prefService.setIsLogin(true)
                resetLoginSession(prefService.getAuthToken()!!)
            }
        })


        initialize() // Run the initialize function when then app is started


        // Set the listener for buttons
        binding.apply {

            // Run the login function when clicked
            btnLogin.setOnClickListener{
                loginPersonal()
            }

            // Show the prompt for fingerprint login when clicked
            ivFingerprint.setOnClickListener {
                showFingerprintPrompt(prefService.getAuthToken()!!)
            }
        }
    }


    // A method that runs when resuming the app/activity
    override fun onResume() {
        super.onResume()
        initialize()
    }

    // A method to shows prompt for fingerprint login
    private fun showFingerprintPrompt(token: String) {

        // Tells users fingerprint is not available for first time login
        if (token.isNullOrEmpty()) {
            this.createToast(getString(R.string.first_time), 1)
            return
        }

        // Else show prompt for fingerprint login
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.bio_title))
            .setSubtitle(getString(R.string.bio_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    // A method that runs when the app/activity is started
    private fun initialize() {

        // Set login state to false while opening the app
        prefService.setIsLogin(false)

        // Get the login jwt token from storage
        val token = prefService.getAuthToken()

        // Validate and refresh token
        if (!resetLoginSession(token!!)) return

        // Return the login page if token is not valid
        val username = prefService.getUsername()
        if (!username.isNullOrEmpty()) {
            binding.etName.setText(username)
            showFingerprintPrompt(token)
        }

        // Set loading state to false
        displayLoadingScreen(false)
    }


    // A method to validate and refresh the login session
    private fun resetLoginSession(token: String): Boolean {
        // Set loading state to true
        displayLoadingScreen(true)

        // Return to login page if login token is null
        if (token.isNullOrEmpty()) {
            redirectLogin("")
            return false
        }

        // Bind the existing token into JwtDTO object
        val jwtModel = JwtDTO(token)

        // Get the current login state
        val isLogin = prefService.getIsLogin()

        // Call api to validate the login jwt token
        userApi.validateToken(jwtModel).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {

                // Return error message and redirect to login page if not valid
                if (!response.isSuccessful) {
                    biometricPrompt.cancelAuthentication()
                    redirectLogin(response.errorBody()?.string()!!)
                    return
                }

                // Refresh the login jwt token if valid
                if (isLogin) {
                    val success = response.body()?.string();
                    val refreshedToken = JSONObject(success).getString("token")
                    prefService.setAuthToken(refreshedToken)
                    redirectHome()
                    return
                }
            }

            // Print error messages if api call is failed
            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                applicationContext.printErrorMessage(t)
                displayLoadingScreen(false)
            }
        })
        return true
    }


    // A method to login
    private fun loginPersonal() {
        val username = binding.etName
        val password = binding.etPassword

        // Create a new list that holds inputs
        val values = mutableListOf(username, password)

        // Map all edit text and its value, then validate it
        val inputs = values.getMapValue().validateInput()

        // Check if all inputs are there
        if (inputs.count() != values.count()) return

        // Bind information into LoginPersonalDTO object
        val loginPersonal = LoginPersonalDTO(
            Name = inputs[username].toString(),
            Password = inputs[password].toString()
        )

        // Set the loading state to true
        displayLoadingScreen(true)

        // Call api to validate user personal login
        userApi.login(loginPersonal).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {

                // Return error message if the api call is not successful
                if (!response.isSuccessful) {
                    val error = response.errorBody()?.string()
                    applicationContext.createToast(error!!,1)
                    displayLoadingScreen(false)
                    return
                }

                // Check if the response body is empty
                val success = response.body()?.string() ?: return

                // Get the values from JSON
                val successObj = JSONObject(success)

                // Create a new storage will all required values
                prefService.createSession(
                    true,
                    successObj.getInt("userId"),
                    successObj.getString("username"),
                    successObj.getString("email"),
                    successObj.getString("token")
                )

                // Redirect to home if login valid
                redirectHome()
            }

            // Print error messages if api call is failed
            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                applicationContext.printErrorMessage(t)
                displayLoadingScreen(false)
            }
        })
    }


    // Redirect user to home page
    private fun redirectLogin(error: String) {

        // Clear storage and input if there are changes in database
        // It happens when new changes made in database and not reflected in here
        if (error.isNotEmpty()) {
            prefService.clearSession()
            binding.etName.setText("")
            this.createToast(error, 1)
        }

        // Set loading state to false
        displayLoadingScreen(false)
    }


    // Redirect user to home page
    private fun redirectHome() {
        this.createToast(getString(R.string.logged_in),1)
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Finish/end all activities in current page
    }


    // Show the loading icon when function is processing
    private fun displayLoadingScreen(isLoading: Boolean) {
        binding.apply {

            // Check if the user mobile has fingerprint icon available
            val items = if (isAvailable) {
                mutableListOf(tvWelcome, etName, etPassword, btnLogin, ivFingerprint)
            } else {
                mutableListOf(tvWelcome, etName, etPassword, btnLogin)
            }

            // Loop and show/hide all components based on the loading state
            if (isLoading) {
                for (item in items) {
                    item.visibility = View.GONE
                }
                pbLoading.visibility = View.VISIBLE
            } else {
                for (item in items) {
                    item.visibility = View.VISIBLE
                }
                pbLoading.visibility = View.GONE
            }
        }
    }


    // Prompt for application exit
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_title))
            .setMessage(getString(R.string.exit_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
