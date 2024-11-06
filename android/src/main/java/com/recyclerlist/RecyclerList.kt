package com.recyclerlist

import CustomLayoutManager
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.infer.annotation.Verify
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.ThemedReactContext
import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveChannelType
import com.recyclerlist.model.RenderItem
import com.recyclerlist.utils.Debounce
import com.recyclerlist.utils.Emitter
import com.recyclerlist.utils.IntervalRunner
import com.recyclerlist.utils.jsonToObject
import com.recyclerlist.utils.toJson
import com.recyclerlist.utils.updateStartAndEndTimes
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

class RecyclerList(private val context: ThemedReactContext) : RecyclerView(context), ItemActionListener<LiveChannelTile> {
  private val TAG = "RecyclerList"
  private var columnCount: Int = 1
  private var totalSpanCount: Int = 1
  private var orientation: Int = VERTICAL
  private var recyclerListAdapter: RecyclerListAdapter? = null
  private lateinit var layoutManager: CustomLayoutManager
  private var isGridLayout: Boolean = false
  private var items = listOf<RenderItem<LiveChannelTile>>()
  private val debounce = Debounce(MainScope())
  private val emitter = Emitter(context)
  private var intervalRunner: IntervalRunner? = null

  init {
    this.id = generateViewId()

  }

  override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener?) {
    Log.d(TAG, "addOnAttachStateChangeListener: ")
    super.addOnAttachStateChangeListener(listener)
  }

  fun setData(data: ReadableArray) {
    val items = mutableListOf<RenderItem<LiveChannelTile>>()
    for (i in 0 until data.size()) {
      val jsonString = data.getMap(i).toJson()
      val objects = jsonToObject<RenderItem<LiveChannelTile>>(jsonString)
      items.add(objects)
    }
    updateStartAndEndTimes(items)
    this.items = items
    this.layoutManager = CustomLayoutManager(context, totalSpanCount, orientation, false)
    this.layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

      override fun getSpanSize(position: Int): Int {
        return when (recyclerListAdapter?.getItemViewType(position)) {
          RecyclerListAdapter.TYPE_GROUP_HEADER -> {
            totalSpanCount
          }

          RecyclerListAdapter.TYPE_ITEM -> {
            val item = recyclerListAdapter?.getItem(position)
            if (item?.type == LiveChannelType.ON_NOW.value) 2 else 1
          }

          else -> 1
        }
      }

      override fun isSpanIndexCacheEnabled(): Boolean {
        return false
      }
    }
    setLayoutManager(this.layoutManager)
    this.recyclerListAdapter = RecyclerListAdapter(items, this)
    this.recyclerListAdapter?.setColumnCount(this.columnCount)
    this.setAdapter(recyclerListAdapter)
    updateProgress()
    intervalRunner?.start()

  }

  fun setColumnCount(columns: Int, orientation: Int = VERTICAL, spans: ArrayList<Int> = arrayListOf()) {
    this.columnCount = columns
    this.totalSpanCount = spans.sumOf { it }
    this.isGridLayout = columns != 1
    this.orientation = orientation
  }

  override fun onItemClicked(item: LiveChannelTile, index: Int) {
    val event = Arguments.createMap().apply {
      putInt("index", index)
      putString("item", item.toJson())
    }
    emitter.emit("onItemPress", event)
  }


  override fun onItemFocusChanged(view: View, position: Int, isFocused: Boolean) {
    if (!isFocused) return
    setFocusMap(view, position)
    this.layoutManager.smoothScrollToPositionWithDelay(this, null, position)
    debounce.withDelay(100L) {
      val event = Arguments.createMap().apply {
        putInt("index", position)
        putInt("viewId", (view.id))
      }
      emitter.emit("onFocusChange", event)
    }
  }

  private fun setFocusMap(itemView: View, position: Int) {
    val indexMap = this.recyclerListAdapter?.getItemIndexMap()
    val currentID = itemView.id
    val itemIndex = indexMap?.entries?.find { it.value == position }?.key ?: return

    val upIndex = (itemIndex - this.columnCount)
    val downIndex = (itemIndex + this.columnCount).coerceAtMost(indexMap.size - 1)
    val leftIndex = (itemIndex - 1).coerceAtLeast(0)
    val rightIndex = (itemIndex + 1).coerceAtMost(indexMap.size - 1)

    val up = if (upIndex >= 0) findViewByPosition(indexMap[upIndex])?.id ?: currentID else currentID
    val down = if (downIndex < indexMap.size - 1) findViewByPosition(indexMap[downIndex])?.id ?: currentID else currentID
    val left = if (leftIndex >= 0 && leftIndex % this.columnCount < this.columnCount - 1) findViewByPosition(indexMap[leftIndex])?.id ?: currentID else currentID
    val right = if (rightIndex < indexMap.size && rightIndex % this.columnCount > 0) findViewByPosition(indexMap[rightIndex])?.id ?: currentID else currentID

    itemView.nextFocusUpId = up
    itemView.nextFocusDownId = down
    itemView.nextFocusLeftId = left
    itemView.nextFocusRightId = right

  }

  override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    Log.d(TAG, "onFocusChanged: $gainFocus $direction $previouslyFocusedRect")
  }

  private fun findViewByPosition(position: Int?): View? {
    return position?.let { this.findViewHolderForAdapterPosition(it)?.itemView }
  }

  private fun getVisibleViews(): List<View> {
    val visibleViews = mutableListOf<View>()
    val firstVisiblePosition = this.layoutManager.findFirstVisibleItemPosition()
    val lastVisiblePosition = this.layoutManager.findLastVisibleItemPosition()
    Log.d(TAG, "getVisibleViews: firstVisiblePosition: $firstVisiblePosition lastVisiblePosition : ${lastVisiblePosition}")
    for (i in firstVisiblePosition..lastVisiblePosition) {
      findViewByPosition(i)?.let {
        visibleViews.add(it)
      }

    }
    return visibleViews
  }


  private fun onProgressComplete(position: Int) {
  }


  private fun updateProgress() {
    intervalRunner = IntervalRunner(5000L) {
      val visibleItems = getVisibleViews()
      visibleItems.forEach {
        if (it.isShown) {
          val position = layoutManager.getPosition(it)
          if (recyclerListAdapter?.getItemViewType(position) != RecyclerListAdapter.TYPE_GROUP_HEADER) {
            val item = recyclerListAdapter?.getItem(position)
            getLivePlayerProgress(
              item?.liveShow?.startTime ?: 0,
              item?.liveShow?.endTime ?: 0,
              onProgressComplete = ::onProgressComplete,
              onProgress = { progress ->
                val progressBar = it.findViewById<ProgressBar>(R.id.progress_bar)
                progressBar.progress = (progress * 100).toInt()
              },
              position
            )

          }
        }
      }
    }
  }

  private fun getLivePlayerProgress(
    startTime: Long,
    endTime: Long,
    onProgressComplete: (Int) -> Unit,
    onProgress :(Double) -> Unit,
    position: Int
  ) {

    val currentTime = System.currentTimeMillis() / 1000
    val totalDuration = endTime - startTime
    val elapsed = currentTime - startTime

    val progress = elapsed.toDouble() / totalDuration.toDouble()
    if (currentTime >= endTime) {
      onProgress(1.0)
      onProgressComplete(position)
    }else {
      onProgress(progress)
    }
  }


  override fun onScrolled(dx: Int, dy: Int) {
    super.onScrolled(dx, dy * 10)
  }

//  private var lastKeyDownTime: Long = 0
//  private val debounceInterval = 80L
//
//  override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
//    if (event?.keyCode == KeyEvent.KEYCODE_DPAD_DOWN || event?.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//      val currentTime = System.currentTimeMillis()
//      if ((currentTime - lastKeyDownTime) >= debounceInterval) {
//        lastKeyDownTime = currentTime
//        return false
//      } else {
//        return true
//      }
//    }
//    return false
//  }

}


