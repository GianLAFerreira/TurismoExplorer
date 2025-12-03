package com.example.turismoexplorer.popular

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.turismoexplorer.R
import com.example.turismoexplorer.domain.Place

class PlacesAdapter(
    private val isFavorite: (String) -> Boolean = { false },
    private val onFavoriteClick: (Place) -> Unit = {}
) : ListAdapter<Place, PlaceViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(v, isFavorite, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Place>() {
            override fun areItemsTheSame(oldItem: Place, newItem: Place) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Place, newItem: Place) = oldItem == newItem
        }
    }
}

class PlaceViewHolder(
    itemView: View,
    private val isFavorite: (String) -> Boolean,
    private val onFavoriteClick: (Place) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val name: TextView = itemView.findViewById(R.id.placeName)
    private val address: TextView = itemView.findViewById(R.id.placeAddress)
    private val rating: TextView = itemView.findViewById(R.id.placeRating)
    private val favBtn: ImageButton = itemView.findViewById(R.id.favButton)

    fun bind(p: Place) {
        name.text = p.name
        address.text = p.address
        rating.text = if (p.rating != null) "Nota: ${"%.1f".format(p.rating)}" else "Sem avaliação"

        updateFavIcon(isFavorite(p.id))
        favBtn.setOnClickListener {
            onFavoriteClick(p)
            // UI otimista (opcional, será corrigida quando a lista atualizar)
            updateFavIcon(!isFavorite(p.id))
        }
    }

    private fun updateFavIcon(favorited: Boolean) {
        favBtn.setImageResource(
            if (favorited) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }
}
