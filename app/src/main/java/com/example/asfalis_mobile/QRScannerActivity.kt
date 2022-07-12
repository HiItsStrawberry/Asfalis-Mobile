package com.example.asfalis_mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.asfalis_mobile.Helpers.createToast
import com.example.asfalis_mobile.Helpers.printErrorMessage
import com.example.asfalis_mobile.databinding.ActivityQrscannerBinding
import com.example.asfalis_mobile.networks.RetrofitInstance
import com.example.asfalis_mobile.services.PrefService
import com.example.asfalis_mobile.services.UserService
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QRScannerActivity : AppCompatActivity() {

    private lateinit var userApi: UserService
    private lateinit var prefService: PrefService
    private lateinit var codeScanner : CodeScanner
    private lateinit var binding: ActivityQrscannerBinding
    private val  CAMERA_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the page title and back button
        supportActionBar?.title = "QR Code Scanner"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefService = PrefService(this)
        userApi = RetrofitInstance.getRetrofitInstance().create(UserService::class.java)


        // Ask for permission of camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            askCameraPermission()
        } else {
            // Proceed to scanner if permission is granted
            codeScanner = CodeScanner(this, binding.scannerView)
            codeScannerSetup()
        }
    }


    // A method to decrypt the QR Code
    private fun decryptQRCode(cipherText: String) {

        // Default values (error state, dialog title, userId, token)
        var error = false
        var title = "QR Code Scanner"
        val userId = prefService.getUserId()
        val token = prefService.getAuthToken()

        // Redirect to login page if token is null
        if (token.isNullOrEmpty()) redirectLogin()

        // Set the token to Bearer token (mainly for JWT authorization)
        val bearerToken = "Bearer $token"

        // Call api to decrypt the code
        userApi.getCodeFromQR(userId, cipherText, bearerToken).enqueue(object :
            Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {

                // Change default value if the api call is failed
                val responseValue = if (!response.isSuccessful) {
                    title = "Error"
                    error = true
                    "Sorry, you are not authorized to perform this activity"
                } else {
                    // Else get the decrypted code sent from api
                    response.body()?.string()!!
                }

                // Show the response result in a dialog
                showDialog(title, responseValue, error)
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                applicationContext.printErrorMessage(t)
            }
        })
    }


    // QR Code Scanner configuration
    private fun codeScannerSetup() {

        // Camera configuration
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        // Run the decrypt function when scanned successfully
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                decryptQRCode(it.text)
            }
        }

        // Return error message if something wrong with camera
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            runOnUiThread {
                this.createToast("Camera initialization error: ${it.message}", 1)
            }
        }

        // Set the listener for buttons
        binding.scannerView.setOnClickListener {
            // Start/continue scan when clicked
            codeScanner.startPreview()
        }
    }


    // A method to ask permission for camera with a code
    // The code can be random, as long it matches
    private fun askCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE)
    }


    // A method that auto validate the permission of camera
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.createToast(getString(R.string.camera_granted), 1)
            } else {
                this.createToast(getString(R.string.camera_denied), 1)
            }
        }
    }


    // Continue to scan while the app is resumed
    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }


    // Reset the QR Scanner while the app is paused
    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }


    // Redirect user to login page when the app is restarted
    override fun onRestart() {
        super.onRestart()
        redirectLogin()
    }


    // A method to create and show a custom dialog
    private fun showDialog(title: String, value: String, error: Boolean) {

        // Build a new dialog
        val builder = AlertDialog.Builder(this)

        // Dialog title
        builder.setTitle(title)

        // Dialog message
        builder.setMessage("Scan result: $value")

        // Dialog icon for failed and success response from api
        val icon = if (error) {
            android.R.drawable.ic_lock_idle_lock
        } else {
            android.R.drawable.ic_dialog_info
        }

        // Set the icon to the dialog
        builder.setIcon(icon)

        //performing positive action
        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.cancel()
        }

        // Create the AlertDialog
        val alert = builder.create()

        alert.show()
    }


    // Redirect user to login page
    private fun redirectLogin() {
        prefService.setIsLogin(false)
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    // Redirect back to home page
    override fun onBackPressed() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
        return
    }


    // A method for making the page's back button works
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}