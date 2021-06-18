package com.misha.takson

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class PoezdkaActivity : AppCompatActivity() {
    private var current = false;
    private var poezdkaId = 0;
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout;
    private lateinit var buttonStart: Button;
    private lateinit var buttonEnd: Button;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poezdka)

        swipeRefreshLayout = findViewById(R.id.swiperefreshPoezdka)
        swipeRefreshLayout.setOnRefreshListener {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            update()
        }

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener { onButtonStartClick() }
        buttonEnd = findViewById(R.id.buttonEnd);
        buttonEnd.setOnClickListener { onButtonEndClick() }

        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        val current = intent.getStringExtra("Current")
        if (!current.isNullOrEmpty()) {
            this.current = true;
        } else {
            poezdkaId = intent.getIntExtra("Poezdka", 0);
        }
    }

    override fun onStart() {
        super.onStart()

        update();
    }

    private fun update() {
        if (!swipeRefreshLayout.isRefreshing) swipeRefreshLayout.isRefreshing = true;
        if (current) {
            val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
            val token = sharedPreferences.getString("token", "none");
            val urlString = sharedPreferences.getString("url", "none");

            val queue = Volley.newRequestQueue(this)
            val url = urlString + "/voditeli/currentPoezdka"

            // Request a string response from the provided URL.
            val stringRequest = object: JsonObjectRequest(
                Request.Method.POST, url, null,
                { response ->
                    var poezdka = response.getJSONObject("poezdka");
                    var text = "От: " + poezdka.getString("from_place") + "\n" +
                            "Куда: " + poezdka.getString("to_place") + "\n" +
                            "КМ: " + poezdka.getString("km") + "\n" +
                            "Цена: " + poezdka.getString("price") + "\n" +
                            "Номер пасажира: " + poezdka.getString("phone") + "\n" +
                            "Статус: " + poezdka.getString("status");
                    findViewById<TextView>(R.id.infoView).text = text;
                    buttonStart.visibility = GONE;
                    buttonEnd.visibility = VISIBLE;
                    swipeRefreshLayout.isRefreshing = false;
                },
                { error ->
                    if (error.networkResponse.statusCode == 401) {
                        val toast = Toast.makeText(applicationContext, "Закончилась сессия, пожалуйста, перезайдите.", Toast.LENGTH_LONG)
                        toast.show()

                        val i = Intent(this, MainActivity::class.java)
                        // set the new task and clear flags
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(i)
                    } else if (error.networkResponse.statusCode == 400) {
                        val toast = Toast.makeText(applicationContext, "У вас пока ещё нет поездки, начните новую.", Toast.LENGTH_LONG)
                        toast.show()

                        finish();
                    } else {
                        val toast = Toast.makeText(applicationContext, "Произошла неизвестная ошибка, попробуйте ещё раз.", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    if (token.isNullOrEmpty()) headers["X-Access-Token"] = "none"
                    else headers["X-Access-Token"] = token
                    return headers
                }
            }
            queue.add(stringRequest)
        } else {
            val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
            val token = sharedPreferences.getString("token", "none");
            val urlString = sharedPreferences.getString("url", "none");

            val queue = Volley.newRequestQueue(this)
            val url = urlString + "/voditeli/poezdka"

            // Request a string response from the provided URL.
            val stringRequest = object: JsonObjectRequest(
                Request.Method.POST, url, null,
                { response ->
                    var poezdka = response.getJSONObject("poezdka");
                    var text = "От: " + poezdka.getString("from_place") + "\n" +
                            "Куда: " + poezdka.getString("to_place") + "\n" +
                            "КМ: " + poezdka.getString("km") + "\n" +
                            "Цена: " + poezdka.getString("price") + "\n" +
                            "Номер пасажира: " + poezdka.getString("phone") + "\n" +
                            "Статус: " + poezdka.getString("status");
                    findViewById<TextView>(R.id.infoView).text = text;
                    buttonStart.visibility = VISIBLE;
                    buttonEnd.visibility = GONE;
                    swipeRefreshLayout.isRefreshing = false;
                },
                { error ->
                    if (error.networkResponse.statusCode == 401) {
                        val toast = Toast.makeText(applicationContext, "Закончилась сессия, пожалуйста, перезайдите.", Toast.LENGTH_LONG)
                        toast.show()

                        val i = Intent(this, MainActivity::class.java)
                        // set the new task and clear flags
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(i)
                    } else {
                        val toast = Toast.makeText(applicationContext, "Произошла неизвестная ошибка, попробуйте ещё раз.", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    if (token.isNullOrEmpty()) headers["X-Access-Token"] = "none"
                    else headers["X-Access-Token"] = token
                    return headers
                }
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8";
                }

                override fun getBody(): ByteArray {
                    val jsonBody = JSONObject()
                    jsonBody.put("id", poezdkaId)
                    return jsonBody.toString().toByteArray()
                }
            }
            queue.add(stringRequest)
        }
    }

    private fun onButtonStartClick() {
        val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "none");
        val urlString = sharedPreferences.getString("url", "none");

        val queue = Volley.newRequestQueue(this)
        val url = urlString + "/voditeli/startPoezdka"

        // Request a string response from the provided URL.
        val stringRequest = object: StringRequest(
            Request.Method.POST, url,
            {
                current = true;
                update();
            },
            { error ->
                if (error.networkResponse.statusCode == 401) {
                    val toast = Toast.makeText(applicationContext, "Закончилась сессия, пожалуйста, перезайдите.", Toast.LENGTH_LONG)
                    toast.show()

                    val i = Intent(this, MainActivity::class.java)
                    // set the new task and clear flags
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                } else {
                    val toast = Toast.makeText(applicationContext, "Произошла неизвестная ошибка, попробуйте ещё раз.", Toast.LENGTH_LONG)
                    toast.show()
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (token.isNullOrEmpty()) headers["X-Access-Token"] = "none"
                else headers["X-Access-Token"] = token
                return headers
            }
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8";
            }

            override fun getBody(): ByteArray {
                val jsonBody = JSONObject()
                jsonBody.put("id", poezdkaId)
                return jsonBody.toString().toByteArray()
            }
        }
        queue.add(stringRequest)
    }

    private fun onButtonEndClick() {
        val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "none");
        val urlString = sharedPreferences.getString("url", "none");

        val queue = Volley.newRequestQueue(this)
        val url = urlString + "/voditeli/endPoezdka"

        // Request a string response from the provided URL.
        val stringRequest = object: StringRequest(
            Request.Method.POST, url,
            {
                val toast = Toast.makeText(applicationContext, "Поездка успешно завершена!", Toast.LENGTH_LONG)
                toast.show()
                finish()
            },
            { error ->
                if (error.networkResponse.statusCode == 401) {
                    val toast = Toast.makeText(applicationContext, "Закончилась сессия, пожалуйста, перезайдите.", Toast.LENGTH_LONG)
                    toast.show()

                    val i = Intent(this, MainActivity::class.java)
                    // set the new task and clear flags
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                } else {
                    val toast = Toast.makeText(applicationContext, "Произошла неизвестная ошибка, попробуйте ещё раз.", Toast.LENGTH_LONG)
                    toast.show()
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (token.isNullOrEmpty()) headers["X-Access-Token"] = "none"
                else headers["X-Access-Token"] = token
                return headers
            }
        }
        queue.add(stringRequest)
    }
}