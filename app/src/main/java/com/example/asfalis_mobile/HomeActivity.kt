package com.example.asfalis_mobile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.asfalis_mobile.databinding.ActivityHomeBinding
import com.example.asfalis_mobile.networks.RetrofitInstance
import com.example.asfalis_mobile.services.PrefService
import com.example.asfalis_mobile.services.UserService


class HomeActivity : AppCompatActivity() {

    private lateinit var userApi: UserService
    private lateinit var prefService: PrefService
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the page title
        supportActionBar?.title = "Home"

        prefService = PrefService(this)
        userApi = RetrofitInstance.getRetrofitInstance().create(UserService::class.java)


        // Set the listener for buttons
        binding.apply {

            // Set the text to logged in username
            tvUsername.text = prefService.getUsername()

            // Redirect user to QR Scanner page when clicked
            btnQRScanner.setOnClickListener {
                val intent = Intent(applicationContext, QRScannerActivity::class.java)
                startActivity(intent)
                finish()
            }

            // Redirect user to login page when clicked
            btnSignout.setOnClickListener {
                displayLoadingScreen(true)
                redirectLogin()
            }
        }
    }


    // Redirect user to login page when the app is restarted
    override fun onRestart() {
        super.onRestart()
        redirectLogin()
    }


    // Redirect user to login page
    private fun redirectLogin() {
        prefService.setIsLogin(false)
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    // Show the loading icon when function is processing
    private fun displayLoadingScreen(isLoading: Boolean) {
        binding.apply {
            val items = mutableListOf(tvWelcomeHome, tvUsername, btnQRScanner, btnSignout)

            if (isLoading) {
                for (item in items) {
                    item.visibility = View.GONE
                }
            } else {
                for (item in items) {
                    item.visibility = View.VISIBLE
                }
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