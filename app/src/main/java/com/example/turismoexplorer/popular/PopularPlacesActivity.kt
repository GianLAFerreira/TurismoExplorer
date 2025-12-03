// Kotlin
package com.example.turismoexplorer.popular

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turismoexplorer.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.View
import android.widget.ProgressBar



class PopularPlacesActivity : ComponentActivity() {

    private val viewModel: PopularPlacesViewModel by viewModels()
    private val adapter = PlacesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_places)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { finish() }

        val cityInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.cityInput)
        val loadButton = findViewById<Button>(R.id.loadButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val recycler = findViewById<RecyclerView>(R.id.placesRecycler)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadButton.setOnClickListener {
            val city = cityInput.text?.toString()?.trim().orEmpty()
            if (city.isNotEmpty()) viewModel.load(city)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is PopularPlacesViewModel.UiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                        }
                        is PopularPlacesViewModel.UiState.Success -> {
                            progressBar.visibility = View.GONE
                            adapter.submitList(state.data)
                        }
                        is PopularPlacesViewModel.UiState.Empty -> {
                            progressBar.visibility = View.GONE
                            adapter.submitList(emptyList())
                            Toast.makeText(this@PopularPlacesActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is PopularPlacesViewModel.UiState.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@PopularPlacesActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}

