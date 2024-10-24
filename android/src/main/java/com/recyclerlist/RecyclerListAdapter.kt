package com.recyclerlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


data class Item(val title: String, val description: String)

class RecyclerListAdapter(private var items: List<Item>, private val onFocusChanged: (Int) -> Unit) :
  RecyclerView.Adapter<RecyclerListAdapter.CustomViewHolder>() {


  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
    return CustomViewHolder(view)
  }

  override fun getItemCount(): Int {
    return items.count()
  }

  override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
    val item = items[position]
   holder.titleTextView.text = item.title
    holder.descriptionTextView.text = item.description
    if(position % 3  == 1 || position % 3  == 2){
      holder.imageContainer.visibility  = View.GONE
    }else{
      holder.imageContainer.visibility  = View.VISIBLE
    }
    holder.containerView.setOnFocusChangeListener{_, hasFocus ->
      if(hasFocus){
        onFocusChanged(position)
        holder.containerView.setBackgroundColor(Color.GREEN)
      }else{
        holder.containerView.setBackgroundColor(Color.TRANSPARENT)
      }

    }
  }

  fun updateData(items: List<Item>){
    this.items = items
    this.notifyDataSetChanged()

  }


  class CustomViewHolder(private val itemView: View): RecyclerView.ViewHolder(itemView) {
    //Here we define how individual item looks like. Need to make this generic where view style can be passes from JS side
    val containerView : LinearLayout = itemView.findViewById(R.id.rootContainer);
    val childContainer : LinearLayout = itemView.findViewById(R.id.childContainer)
    val titleTextView: TextView = itemView.findViewById(R.id.title)
    val descriptionTextView: TextView = itemView.findViewById(R.id.description)
    val imageContainer : LinearLayout = itemView.findViewById(R.id.imageContainer)

  }

}
