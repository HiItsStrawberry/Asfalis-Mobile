package com.example.asfalis_mobile.services

import android.content.Context
import android.content.SharedPreferences

class PrefService(context : Context) {
    // Shared preference mode
    private val PRIVATE_MODE: Int = 0

    // Shared filename
    private val PREF_NAME = "ASFALIS_SHARED_PREFERENCE"
    private val IS_VALID = "IS_VALID"
    private val IS_LOGIN = "IS_LOGIN"
    private val USER_ID = "USER_ID"
    private val USERNAME = "USERNAME"
    private val EMAIL_ADDRESS = "EMAIL_ADDRESS"
    private val AUTH_TOKEN = "AUTH_TOKEN"

    // Create a local storage for mobile
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

    // Create an editor for editing the local storage
    private val editor: SharedPreferences.Editor = pref.edit()

    // Add all values into the local storage
    fun createSession(isLogin: Boolean, userId: Int, username: String, email: String, token: String) {
        editor.putBoolean(IS_LOGIN, isLogin)
        editor.putInt(USER_ID, userId)
        editor.putString(USERNAME, username)
        editor.putString(EMAIL_ADDRESS, email)
        editor.putString(AUTH_TOKEN, token)
        editor.commit()
    }

    // Get userId from storage
    fun getUserId() : Int {
        return pref.getInt(USER_ID, 0)
    }

    // Get username from storage
    fun getUsername() : String? {
        return pref.getString(USERNAME, "")
    }

    // Get login jwt token from storage
    fun getAuthToken() : String? {
        return pref.getString(AUTH_TOKEN, "")
    }

    // Get login state from storage
    fun getIsLogin(): Boolean {
        return pref.getBoolean(IS_LOGIN, false)
    }

    // Set new token value in storage
    fun setAuthToken(token: String)  {
        editor.putString(AUTH_TOKEN, token)
        editor.commit()
    }

    // Set new login state in storage
    fun setIsLogin(isLogin: Boolean) {
        editor.putBoolean(IS_LOGIN, isLogin)
        editor.commit()
    }

    // Clear all values from storage
    fun clearSession() {
        editor.clear()
        editor.commit()
    }
}