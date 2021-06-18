package com.misha.takson

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley


class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        swipeRefreshLayout.setOnRefreshListener {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            myUpdateOperation()
        }
        findViewById<ListView>(R.id.listView).setOnItemClickListener { parent, view, position, id ->
            var item = parent.getItemAtPosition(position) as Poezdka;
            val i = Intent(this, PoezdkaActivity::class.java)
            i.putExtra("Poezdka", item.getId())
            startActivity(i)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_buttons, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_current -> {
                val i = Intent(this, PoezdkaActivity::class.java)
                i.putExtra("Current", "true")
                startActivity(i)
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    override fun onStart() {
        super.onStart()
        myUpdateOperation()
    }

    fun myUpdateOperation() {
        if (swipeRefreshLayout.isRefreshing == false) swipeRefreshLayout.isRefreshing = true;

        val sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "none");
        val urlString = sharedPreferences.getString("url", "none");

        val queue = Volley.newRequestQueue(this)
        val url = urlString + "/voditeli/poezdki"

        // Request a string response from the provided URL.
        val stringRequest = object: JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                var poezdki = response.getJSONArray("poezdki");
                var newPoezdki = arrayOfNulls<Poezdka>(poezdki.length())
                for (i in 0 until poezdki.length()) {
                    val item = poezdki.getJSONObject(i)
                    var destination = item.getString("from_place") + "\n" + item.getString("to_place")
                    newPoezdki[i] = Poezdka(item.getInt("id"), destination)
                    // Your code here
                }
                val adapter: ArrayAdapter<Poezdka> = ArrayAdapter<Poezdka>(
                    this,
                    R.layout.item_view,
                    R.id.itemTextView,
                    newPoezdki
                )
                findViewById<ListView>(R.id.listView).adapter = adapter;
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
                    swipeRefreshLayout.isRefreshing = false;
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