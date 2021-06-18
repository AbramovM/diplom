package com.misha.takson

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("url", "none");
        if (url != "none" && !url.isNullOrEmpty()) {
            findViewById<EditText>(R.id.editTextTextPersonName).setText(url);
        } else {
            findViewById<EditText>(R.id.editTextTextPersonName).setText("https://takson.serverless.social");
        }
        findViewById<EditText>(R.id.editTextTextPersonName).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("url", s.toString()).apply();
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    fun textChange() {

    }

    override fun onStart() {
        super.onStart()

        val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "none");
        val urlString = sharedPreferences.getString("url", "none");
        if (token != "none" && !token.isNullOrEmpty()) {
            val queue = Volley.newRequestQueue(this)
            val url = urlString + "/voditeli/checkToken"

            // Request a string response from the provided URL.
            val stringRequest = object: StringRequest(
                Request.Method.POST, url,
                {
                    val i = Intent(this, MainActivity::class.java)
                    // set the new task and clear flags
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                },
                {
                    with (sharedPreferences.edit()) {
                        putString("token", "none")
                        apply()
                    }
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["X-Access-Token"] = token
                    return headers
                }
            }
            findViewById<ProgressBar>(R.id.progress).visibility = VISIBLE
            // Add the request to the RequestQueue.
            queue.add(stringRequest)
        }
    }

    fun onLoginButtonClick(view: View) {
        val queue = Volley.newRequestQueue(this)
        val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
        val urlString = sharedPreferences.getString("url", "none");
        val url = urlString + "/voditeli/login"

        // Request a string response from the provided URL.
        val stringRequest = object: StringRequest(
            Request.Method.POST, url,
            { response ->
                val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
                with (sharedPreferences.edit()) {
                    putString("token", response)
                    apply()
                }
                val i = Intent(this, MainActivity::class.java)
                // set the new task and clear flags
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
            },
            { error ->
                val toast = Toast.makeText(applicationContext, "Неправильный логин или пароль.", Toast.LENGTH_LONG)
                toast.show()
                findViewById<ProgressBar>(R.id.progress).visibility = GONE
            }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8";
            }

            override fun getBody(): ByteArray {
                val jsonBody = JSONObject()
                jsonBody.put("login", findViewById<EditText>(R.id.et_login).text.toString())
                jsonBody.put("password", findViewById<EditText>(R.id.et_password).text.toString())
                return jsonBody.toString().toByteArray()
            }
        }

        findViewById<ProgressBar>(R.id.progress).visibility = VISIBLE
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}