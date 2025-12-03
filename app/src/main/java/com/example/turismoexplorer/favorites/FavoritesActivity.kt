package com.example.turismoexplorer.favorites


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turismoexplorer.R
import com.example.turismoexplorer.data.favorites.FavoritesRepository
import com.example.turismoexplorer.domain.Place
import com.example.turismoexplorer.popular.PlacesAdapter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
class FavoritesActivity : ComponentActivity() {

    private lateinit var favoritesRepo: FavoritesRepository
    private lateinit var adapter: PlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_places) // Reutiliza o layout com toolbar

        // Botão de voltar na toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener { finish() }

        favoritesRepo = FavoritesRepository(this)
        adapter = PlacesAdapter(
            isFavorite = { true },
            onFavoriteClick = { place -> remove(place) }
        )

        val recycler = findViewById<RecyclerView>(R.id.placesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val cityInput = findViewById<EditText>(R.id.cityInput)
        val loadButton = findViewById<Button>(R.id.loadButton)

        // Esconde o botão de buscar (aqui é uma lista filtrável por texto)
        loadButton.visibility = View.GONE
        cityInput.hint = "Buscar favoritos por nome ou cidade"


        // Flow de busca com debounce
        val queryFlow = MutableStateFlow("")
        cityInput.addTextChangedListener { text ->
            queryFlow.value = text?.toString().orEmpty()
        }

        lifecycleScope.launch {
            queryFlow
                .debounce(300)
                .flatMapLatest { q ->
                    if (q.isBlank()) favoritesRepo.favorites()
                    else favoritesRepo.searchFavorites(q)
                }
                .collectLatest { places ->
                    adapter.submitList(places)
                }
        }

        // Carrega lista completa inicialmente
        lifecycleScope.launch {
            favoritesRepo.favorites().collectLatest { places ->
                if (queryFlow.value.isBlank()) {
                    adapter.submitList(places)
                }
            }
        }
    }

    private fun remove(place: Place) {
        lifecycleScope.launch {
            favoritesRepo.removeById(place.id)
            Toast.makeText(this@FavoritesActivity, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
        }
    }
}
