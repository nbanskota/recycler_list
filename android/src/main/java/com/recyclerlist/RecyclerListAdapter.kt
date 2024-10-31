package com.recyclerlist

import android.graphics.Color
import android.util.LayoutDirection
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
import com.recyclerlist.model.Item
import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveChannelType
import com.recyclerlist.model.LiveShowMetadata
import com.recyclerlist.model.RenderItem
import com.recyclerlist.utils.Debouncer
import com.recyclerlist.utils.animateViewOnPress
import com.recyclerlist.utils.formatTimeRange
import kotlinx.coroutines.MainScope


class RecyclerListAdapter(
  private var items: List<RenderItem<LiveChannelTile>>,
  private val actionListener: ItemActionListener<LiveChannelTile>,
) :
  RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val TAG = "RecyclerListAdapter"
  private val debounce = Debouncer(MainScope())
  private var viewRefs = mutableMapOf<Int, View>()
  private var indexMap = mutableMapOf<Int, Int>()
  private var focusedIndex: Int? = null
  private val observer = object : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
      super.onChanged()
      //  setFocusMap()
    }
  }

  init {
    setHasStableIds(true)
    this.registerAdapterDataObserver(observer)
  }

  fun deinit() {
    this.unregisterAdapterDataObserver(observer)
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

  private fun setFocusMap() {

    val numOfColumn = 3
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

//    Log.d(TAG, "someFunction: ${this.indexMap }")
//
//    for ((itemIndex, viewIndex) in this.indexMap ) {
//
//      val currentView = viewRefs[viewIndex] ?: continue
//      val currentID = currentView.id
//
//      val upIndex = (itemIndex - numOfColumn)
//      val downIndex = (itemIndex + numOfColumn).coerceAtMost(this.indexMap .size - 1)
//      val leftIndex = (itemIndex - 1).coerceAtLeast(0)
//      val rightIndex = (itemIndex + 1).coerceAtMost(this.indexMap .size - 1)
//
//      val up = if (upIndex >= 0) viewRefs[this.indexMap [upIndex]]?.id ?: currentID else currentID
//      val down = if (downIndex < this.indexMap .size) viewRefs[this.indexMap [downIndex]]?.id ?: currentID else currentID
//      val left = if (leftIndex >= 0 && leftIndex % numOfColumn < numOfColumn - 1) viewRefs[this.indexMap [leftIndex]]?.id ?: currentID else currentID
//      val right = if (rightIndex < viewRefs.size && rightIndex % numOfColumn > 0) viewRefs[this.indexMap [rightIndex]]?.id ?: currentID else currentID
//
//      currentView.nextFocusUpId = up
//      currentView.nextFocusDownId = down
//      currentView.nextFocusLeftId = left
//      currentView.nextFocusRightId = right
//    }
  }


  override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
    super.onViewRecycled(holder)
    Log.d("niteshb", "onViewRecycled: ")

    focusedIndex?.let { index ->
      if (index == holder.bindingAdapterPosition) {
        setFocusMap()
      }
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (holder is GroupHeaderViewHolder) {
      holder.bind(items[getGroupPosition(position)], position)
    } else if (holder is ItemViewHolder) {
      holder.bind(getItem(position), actionListener, position)
      debounce.debounce(100L) {
        setFocusMap()
      }

    }

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

    fun bind(
      liveChannelTile: LiveChannelTile,
      actionListener: ItemActionListener<LiveChannelTile>,
      position: Int
    ) {
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
        actionListener.onItemFocused(view, position, isFocused)
        if (isFocused) {
          logoImage.animate().scaleX(1.01f).scaleY(1.01f).setDuration(100).start()
          childContainer.animate().scaleX(1.01f).scaleY(1.01f).setDuration(100).start()
          logoImage.background = selectedViewBackground
          childContainer.background = selectedViewBackground
          focusedIndex = position
//
          val numOfColumn = 3
            val currentID = view.id
            val itemIndex = indexMap.entries.find { it.value == position }?.key ?: return@setOnFocusChangeListener   //key is itemIndex and value is viewIndex in recycler view

            val upIndex = (itemIndex - numOfColumn)
            val downIndex = (itemIndex + numOfColumn).coerceAtMost(indexMap.size - 1)
            val leftIndex = (itemIndex - 1).coerceAtLeast(0)
            val rightIndex = (itemIndex + 1).coerceAtMost(indexMap.size - 1)

            val up = if (upIndex >= 0) viewRefs[indexMap[upIndex]]?.id ?: currentID else currentID
            val down = if (downIndex < indexMap.size) viewRefs[indexMap[downIndex]]?.id ?: currentID else currentID
            val left = if (leftIndex >= 0 && leftIndex % numOfColumn < numOfColumn - 1) viewRefs[indexMap[leftIndex]]?.id ?: currentID else currentID
            val right = if (rightIndex < viewRefs.size && rightIndex % numOfColumn > 0) viewRefs[indexMap[rightIndex]]?.id ?: currentID else currentID

            view.nextFocusUpId = up
            view.nextFocusDownId = down
            view.nextFocusLeftId = left
            view.nextFocusRightId = right



        } else {
          logoImage.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
          childContainer.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
          logoImage.background = unselectedViewBackground
          childContainer.background = unselectedViewBackground
        }
      }
    }
  }

  fun headerCount(start: Int, end: Int): Int {
    if(start < 0 ) return  0
    var count = 0
    for (i in start until end) {
        if (getItemViewType(i) == TYPE_GROUP_HEADER) {
          count++
        }
    }
    return count
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

