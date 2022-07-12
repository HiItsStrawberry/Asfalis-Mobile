package com.example.asfalis_mobile

import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.Toast

object Helpers {

    // Map all edit text and its value into a map array
    fun List<EditText>.getMapValue(): MutableMap<EditText, String> {

        // Create a new empty map (dictionary in c#)
        val mapValues: MutableMap<EditText, String> = mutableMapOf()

        for (item in this)
        {
            mapValues[item] = item.text.toString().trim()
        }

        return mapValues
    }


    // A method to validate all values from edit text from an array
    fun Map<EditText, String?>.validateInput(): MutableMap<EditText, String> {
        val values : MutableMap<EditText, String> = mutableMapOf()

        // Loop all edit text and its value
        for ((editText, value) in this)
        {
            // Check if the value is null
            if (value.isNullOrEmpty())
            {
                // Return error message if null
                editText.error = "The field is required"
                editText.requestFocus()
                return values
            }
            values[editText] = value
        }
        return values
    }


    // Print all stack traces and error message for debugging
    fun Context.printErrorMessage(t: Throwable) {
        this.createToast("There was an error connecting to the system", 1)
        Log.e("Response Error", t.message.toString())
        t.printStackTrace()
    }


    // Create a new long and short toast with the message given
    fun Context.createToast(message: String, option: Int) {
        if (option == 1)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        else
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}