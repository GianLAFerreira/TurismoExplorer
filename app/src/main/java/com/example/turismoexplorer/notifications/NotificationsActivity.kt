package com.example.turismoexplorer.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turismoexplorer.R
import com.example.turismoexplorer.data.local.NotificationEntity
import com.example.turismoexplorer.data.notifications.NotificationsRepository
import com.example.turismoexplorer.map.PlacesMapActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsActivity : ComponentActivity() {

    private lateinit var repo: NotificationsRepository
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        repo = NotificationsRepository(this)
        adapter = NotificationsAdapter { n -> openOnMap(n) }

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        val clearButton = findViewById<MaterialButton>(R.id.clearButton)
        val recycler = findViewById<RecyclerView>(R.id.notificationsRecycler)

        toolbar.setNavigationOnClickListener { finish() }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        clearButton.setOnClickListener {
            lifecycleScope.launch { repo.clearAll() }
        }

        lifecycleScope.launch {
            repo.all().collectLatest { list ->
                adapter.submitList(list)
                clearButton.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun openOnMap(n: NotificationEntity) {
        val intent = Intent(this, PlacesMapActivity::class.java).apply {
            putExtra("target_lat", n.lat ?: Double.NaN)
            putExtra("target_lng", n.lng ?: Double.NaN)
            putExtra("target_name", n.title)
        }
        startActivity(intent)
    }
}