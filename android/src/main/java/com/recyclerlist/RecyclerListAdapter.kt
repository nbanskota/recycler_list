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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.react.bridge.UiThreadUtil
import com.recyclerlist.components.setBorder
import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveChannelType
import com.recyclerlist.model.LiveShowMetadata
import com.recyclerlist.model.RenderItem
import com.recyclerlist.utils.Debounce
import com.recyclerlist.utils.ViewAnimator
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
  private var indexMap = mutableMapOf<Int, Int>()
  private var focusedIndex: Int? = null
  private var columnCount: Int = 1

  private val viewAnimator = ViewAnimator()

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

  fun setColumnCount(count: Int) {
    this.columnCount = count
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

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
    if (payloads.isNotEmpty()) {
      // Handle partial update with payload
      val payload = payloads[0]
      if (payload is LiveChannelTile) {
        if (holder is ItemViewHolder) {
          holder.updateData(payload)
        }
      }
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }

  }


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

  fun setItem(position: Int, item: LiveChannelTile) {
    Log.d(TAG, "setItem: ${item.liveShow?.title}")
    var count = 0
    for (group in items) {
      if (position <= count + group.items.size) {
        group.items[position - count - 1] = item
        break
      }
      count += group.items.size + 1
    }
  }

  override fun getItemId(position: Int): Long {
    return (1000 + position).toLong()
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

    private fun updateRow(isFocused: Boolean, position: Int) {
      val itemIndex = indexMap.entries.find { it.value == position }?.key ?: return
      val upperIndex = (itemIndex - this@RecyclerListAdapter.columnCount - 1).coerceAtLeast(0)
      val lowerIndex = (itemIndex + this@RecyclerListAdapter.columnCount - 1).coerceAtMost(itemCount - 1)
      val selectedItemRowIndex = itemIndex.div(this@RecyclerListAdapter.columnCount)
      for (i in upperIndex..lowerIndex) {
        val view = viewRefs[indexMap[i]]?.findViewById<TextView>(R.id.tag)
        if (i.div(this@RecyclerListAdapter.columnCount) == selectedItemRowIndex) view?.visibility = if (isFocused) View.VISIBLE else View.INVISIBLE
      }
    }

    fun updateData(liveChannelTile: LiveChannelTile) {
      val data = getLiveShow(liveChannelTile)
      titleTextView.text = data?.title ?: "Updating soon"
      descriptionTextView.text = formatTimeRange(data?.startTime, data?.endTime) ?: "Updating soon..."
      tagTextView.text = liveChannelTile.type
      progressBar.progress = ((System.currentTimeMillis() / 1000 - (data?.startTime ?: 0L)).toDouble() /
        ((data?.endTime ?: 1L) - (data?.startTime ?: 0L)).toDouble()).coerceIn(0.0, 1.0).times(100).toInt()
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
        progressBar.visibility = View.GONE
      } else {
        imageContainer.visibility = View.VISIBLE
        logoImage.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
      }
      tagTextView.setBackgroundColor(Color.parseColor(liveChannelTile.color))
      updateData(liveChannelTile)

      itemView.setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastPressTime) > pressInterval) {
          lastPressTime = currentTime
          viewAnimator.animateScale(it, 1F, 0.99F, 1F, 0.99F, 50L) {
            viewAnimator.animateScale(it, 0.99F, 1F, 0.99F, 1F, 50L) {
              actionListener.onItemClicked(liveChannelTile, position)
            }
          }
        }
      }

      itemView.setOnFocusChangeListener { view, isFocused ->
        if (view.isShown) actionListener.onItemFocusChanged(view, position, isFocused)

        if (isFocused) {
          viewAnimator.animateScale(view, 1F, 1.01F, 1F, 1.01F, 300L)
          logoImage.background = selectedViewBackground
          childContainer.background = selectedViewBackground
          focusedIndex = position
        } else {
          viewAnimator.animateScale(view, 1.01F, 1F, 1.01F, 1F, 300L)
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
      if (group.title == null) titleTextView.visibility = View.GONE else titleTextView.visibility = View.VISIBLE
      titleTextView.text = group.title
    }
  }

  private class LiveChannelTileCallback : DiffUtil.ItemCallback<LiveChannelTile>(){
    override fun areItemsTheSame(oldItem: LiveChannelTile, newItem: LiveChannelTile): Boolean {
      return oldItem.liveShow?.startTime == newItem.liveShow?.startTime
    }

    override fun areContentsTheSame(oldItem: LiveChannelTile, newItem: LiveChannelTile): Boolean {
      return oldItem == newItem
    }
  }
}

