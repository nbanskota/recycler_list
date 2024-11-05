package com.recyclerlist

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.recyclerlist.components.setBorder
import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveChannelType
import com.recyclerlist.model.LiveShowMetadata
import com.recyclerlist.model.RenderItem
import com.recyclerlist.utils.Debounce
import com.recyclerlist.utils.animateViewOnPress
import com.recyclerlist.utils.formatTimeRange
import kotlinx.coroutines.MainScope


class RecyclerListAdapter(
  private var items: List<RenderItem<LiveChannelTile>>,
  private val actionListener: ItemActionListener<LiveChannelTile>,
) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val TAG = "RecyclerListAdapter"
  private val debounce = Debounce(MainScope())
  private var viewRefs = mutableMapOf<Int, View>()
  private var indexMap = mutableMapOf<Int, Int>()  //
  private var focusedIndex: Int? = null

  init {
    setHasStableIds(true)
  }

  companion object {
    const val TYPE_GROUP_HEADER = 0
    const val TYPE_ITEM = 1
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if (viewType == TYPE_GROUP_HEADER) {
      val view =
        LayoutInflater.from(parent.context).inflate(R.layout.layout_header, parent, false)
      GroupHeaderViewHolder(view)
    } else {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
      ItemViewHolder(view)
    }
  }

  override fun getItemCount() = items.sumOf { it.items.size + 1 }

  fun getItemIndexMap(): MutableMap<Int, Int> {
    return indexMap
  }

  private fun createItemViewIndexMap() {
    var indexMap = mutableMapOf<Int, Int>() //item index and viewIndex
    var headerCounter = 0
    for (i in 0 until viewRefs.size) {
      if (getItemViewType(i) == TYPE_GROUP_HEADER) {
        headerCounter++
        continue
      }
      indexMap[i - headerCounter] = i
    }
    this.indexMap = indexMap.toSortedMap()
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (holder is GroupHeaderViewHolder) {
      holder.bind(items[getGroupPosition(position)], position)
    } else if (holder is ItemViewHolder) {
      holder.bind(getItem(position), actionListener, position)
      debounce.withDelay(50L) {
        createItemViewIndexMap()
      }
    }
  }

//  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
//    super.onBindViewHolder(holder, position, payloads)
//    if (holder is ItemViewHolder && payloads.isNotEmpty()) {
//      val isFocused = payloads[0] as Boolean
//      holder.updateRow(isFocused)
//    }
//  }


  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    Log.d(TAG, "onDetachedFromRecyclerView: ")
  }

  private fun getGroupPosition(position: Int): Int {
    var count = 0
    for ((index, group) in items.withIndex()) {
      if (position == count) return index
      count += group.items.size + 1
    }
    throw IllegalStateException("Invalid position")
  }

  fun getItem(position: Int): LiveChannelTile {
    var count = 0
    for (group in items) {
      if (position <= count + group.items.size) {
        return group.items[position - count - 1]
      }
      count += group.items.size + 1
    }
    throw IllegalStateException("Invalid position")
  }


  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  fun updateData(items: List<RenderItem<LiveChannelTile>>) {
    this.items = items
    this.notifyItemRangeChanged(0, items.count())

  }

  private fun isGroupHeader(position: Int): Boolean {
    var count = 0
    for (group in items) {
      if (position == count) return true
      count += group.items.size + 1
    }
    return false
  }

  override fun getItemViewType(position: Int): Int {
    return if (isGroupHeader(position)) TYPE_GROUP_HEADER else TYPE_ITEM
  }

  fun getViewRefs(): MutableMap<Int, Int> {
    return indexMap
  }

  inner class ItemViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {
    //Here we define how individual item looks like. Need to make this generic where view style can be passes from JS side
    private val childContainer: LinearLayout = itemView.findViewById(R.id.childContainer)
    private val titleTextView: TextView = itemView.findViewById(R.id.title)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
    private val tagTextView: TextView = itemView.findViewById(R.id.tag)
    private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    private val imageContainer: LinearLayout = itemView.findViewById(R.id.imageContainer)
    private val logoImage: ImageView = itemView.findViewById(R.id.logo_image);
    private val previewImage: ImageView = itemView.findViewById(R.id.preview_image);
    private val defaultBackgroundColor =
      AppCompatResources.getDrawable(itemView.context, R.drawable.livetv_item_gradient)

    private var lastPressTime: Long = 0
    private val pressInterval = 1000

    private fun getLiveShow(liveChannelTile: LiveChannelTile): LiveShowMetadata? {

      val item = when (liveChannelTile.type) {
        LiveChannelType.ON_NOW.value -> liveChannelTile.liveShow
        LiveChannelType.ON_NEXT.value -> liveChannelTile.nextShow
        LiveChannelType.ON_LATER.value -> liveChannelTile.laterShow
        else -> {
          null
        }
      }
      return item
    }

    fun setProgress(progress: Int) {
      progressBar.visibility = View.VISIBLE
      progressBar.progress = progress
    }


    private fun updateRow(isFocused: Boolean, position: Int) {
      val itemIndex = indexMap.entries.find { it.value == position }?.key ?: return

      val upperIndex = (itemIndex - 3).coerceAtLeast(0)
      val lowerIndex = (itemIndex + 3).coerceAtMost(itemCount - 1)
      val selectedItemRowIndex = itemIndex.div(3)
      for (i in upperIndex..lowerIndex) {
        val view = viewRefs[indexMap[i]]?.findViewById<TextView>(R.id.tag)
        if(i.div(3) == selectedItemRowIndex){
          view?.visibility = if (isFocused) View.VISIBLE else View.INVISIBLE
        }
      }

    }

    fun bind(
      liveChannelTile: LiveChannelTile,
      actionListener: ItemActionListener<LiveChannelTile>,
      position: Int
    ) {
      itemView.alpha = 0f
      itemView.animate()
        .alpha(1f)
        .setDuration(300)
        .start()
      itemView.tag = "itemView-${position}"
      itemView.id = 1000 + position

      viewRefs[position] = itemView
      val data = getLiveShow(liveChannelTile)
      val selectedViewBackground = setBorder(
        strokeWidth = Color.parseColor(liveChannelTile.color),
        borderColor = 2,
        radius = 4F,
        backgroundColor = Color.parseColor("#151515")
      )
      val unselectedViewBackground = defaultBackgroundColor

      if (liveChannelTile.type != LiveChannelType.ON_NOW.value) {
        imageContainer.visibility = View.GONE
        logoImage.visibility = View.GONE
      } else {
        imageContainer.visibility = View.VISIBLE
        logoImage.visibility = View.VISIBLE
        try {
          Glide.with(logoImage.context)
            .load(liveChannelTile.logoUrl + "?image-profile=livetv_channel_logo")
            .into(logoImage)
          Glide.with(previewImage.context)
            .load(data?.landscapeImageUrl + "?image-profile=live_now_card")
            .into(previewImage)
        } catch (exception: Exception) {
          //The null pointer exception might occur when image is being loaded but the view is already unmounted from the screen
        }
      }
      titleTextView.text = data?.title ?: ""
      descriptionTextView.text = formatTimeRange(data?.startTime, data?.endTime)
      tagTextView.text = liveChannelTile.type
      tagTextView.setBackgroundColor(Color.parseColor(liveChannelTile.color))

      itemView.setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastPressTime) > pressInterval) {
          lastPressTime = currentTime
          animateViewOnPress(itemView)
          actionListener.onItemClicked(liveChannelTile, position)
        }
      }

      itemView.setOnFocusChangeListener { view, isFocused ->
        if(view.isShown) actionListener.onItemFocusChanged(view, position, isFocused)
        if (isFocused) {
          logoImage.background = selectedViewBackground
          childContainer.background = selectedViewBackground
          focusedIndex = position
        } else {
          logoImage.background = unselectedViewBackground
          childContainer.background = unselectedViewBackground
        }

        updateRow(isFocused, position)
      }
    }
  }

  inner class GroupHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleTextView: TextView = itemView.findViewById(R.id.header_text)

    fun <T> bind(group: RenderItem<T>, position: Int) {
      itemView.id = 1000 + position
      viewRefs[position] = itemView
      if (group.title == null) titleTextView.visibility = View.GONE else titleTextView.visibility =
        View.VISIBLE
      titleTextView.text = group.title
    }
  }
}

