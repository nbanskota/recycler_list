package com.recyclerlist

import CustomLayoutManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.UiThreadUtil
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

class RecyclerList(private val context: ThemedReactContext) : RelativeLayout(context), ItemActionListener<LiveChannelTile> {
  private val TAG = "RecyclerList"
  private var columnCount: Int = 1
  private var totalSpanCount: Int = 1
  private var orientation: Int = VERTICAL
  private var recyclerListAdapter: RecyclerListAdapter? = null
  private var layoutManager: CustomLayoutManager? = null
  private var isGridLayout: Boolean = false
  private var items = listOf<RenderItem<LiveChannelTile>>()
  private val debounce = Debounce(MainScope())
  private val emitter = Emitter(context)
  private var intervalRunner: IntervalRunner? = null
  private var focusFinder: IntervalRunner? = null
  private var focusedView: View? = null
  private var recyclerView: RecyclerView? = null
  private var sudoView: View? = null
  private var regainFocusPosition = 1

  private val sudoFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
    if (v is RecyclerView) {
      Log.d(TAG, "RecyclerView Focused: $hasFocus")
      sudoView?.clearFocus()
    } else if (v is LinearLayout) {
      Log.d(TAG, "RecyclerView Focused: $hasFocus")
      recyclerView?.clearFocus()
    }


  }

  init {
    this.id = generateViewId()
    val rootView = View.inflate(context, R.layout.layout_recycler_view, this)
    recyclerView = rootView.findViewById(R.id.recycler_view)
    sudoView = rootView.findViewById(R.id.sudo_view)

    this.sudoView?.onFocusChangeListener = sudoFocusChangeListener
    this.recyclerView?.onFocusChangeListener = sudoFocusChangeListener
    focusFinderTimer()
  }

  fun setData(data: ReadableArray) {
    val items = mutableListOf<RenderItem<LiveChannelTile>>()
    for (i in 0 until data.size()) {
      val jsonString = data.getMap(i).toJson()
      val objects = jsonToObject<RenderItem<LiveChannelTile>>(jsonString)
      items.add(objects)
    }
   // updateStartAndEndTimes(items) //only for mocking
    this.items = items
    this.layoutManager = CustomLayoutManager(context, totalSpanCount, orientation, false)
    this.layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

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
    this.recyclerView?.setLayoutManager(this.layoutManager)
    this.recyclerListAdapter = RecyclerListAdapter(items, this)
    this.recyclerListAdapter?.setColumnCount(this.columnCount)
    this.recyclerView?.setAdapter(recyclerListAdapter)
    updateProgress()
    intervalRunner?.start()


  }

  override fun requestChildFocus(child: View?, focused: View?) {
    super.requestChildFocus(child, focused)
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
    this.focusedView = view
    setFocusMap(view, position)
    this.recyclerView?.let { this.layoutManager?.smoothScrollToPositionWithDelay(it, null, position) }
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
    val down = if (downIndex < indexMap.size) findViewByPosition(indexMap[downIndex])?.id ?: currentID else currentID
    val left = if (leftIndex >= 0 && leftIndex % this.columnCount < this.columnCount - 1) findViewByPosition(indexMap[leftIndex])?.id ?: currentID else currentID
    val right = if (rightIndex < indexMap.size && rightIndex % this.columnCount > 0) findViewByPosition(indexMap[rightIndex])?.id ?: currentID else currentID

    itemView.nextFocusUpId = up
    itemView.nextFocusDownId = down
    itemView.nextFocusLeftId = left
    itemView.nextFocusRightId = right
    regainFocusPosition = position
  }

  private fun findViewByPosition(position: Int?): View? {
    return position?.let { this.recyclerView?.findViewHolderForAdapterPosition(it)?.itemView }
  }

  private fun getVisibleViews(): List<View> {
    val visibleViews = mutableListOf<View>()
    val firstVisiblePosition = this.layoutManager?.findFirstVisibleItemPosition() ?: 0
    val lastVisiblePosition = this.layoutManager?.findLastVisibleItemPosition() ?: 0
    for (i in firstVisiblePosition..lastVisiblePosition) {
      findViewByPosition(i)?.let {
        visibleViews.add(it)
      }

    }
    return visibleViews
  }


  private fun onProgressComplete(position: Int) {
    //Update the next item
    if (recyclerListAdapter?.getItemViewType(position) == RecyclerListAdapter.TYPE_GROUP_HEADER) return
    val item = recyclerListAdapter?.getItem(position)
    updateLiveChannelTile(item)?.let {
      if (position == 1) {
        recyclerListAdapter?.setItem(position, it)
        UiThreadUtil.runOnUiThread {
          recyclerListAdapter?.notifyItemChanged(position, item)
        }
      }
    }
  }


  private fun updateProgress() {
    intervalRunner = IntervalRunner(2000L) {
      val visibleItems = getVisibleViews()
      visibleItems.forEach {
        if (it.isShown) {
          val position = layoutManager?.getPosition(it) ?: return@IntervalRunner
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

  private fun focusFinderTimer() {
    focusFinder = IntervalRunner(50L) {
      val views = getVisibleViews()
      if (views.isNotEmpty()) {
        views[regainFocusPosition].post{
          views[regainFocusPosition].requestFocus()
        }

        focusFinder?.stop()
      }
    }
    focusFinder?.start()
  }

  private fun getLivePlayerProgress(
    startTime: Long,
    endTime: Long,
    onProgressComplete: (Int) -> Unit,
    onProgress: (Double) -> Unit,
    position: Int
  ) {

    val currentTime = System.currentTimeMillis() / 1000
    val totalDuration = endTime - startTime
    val elapsed = currentTime - startTime

    val progress = elapsed.toDouble() / totalDuration.toDouble()
    if (currentTime >= endTime) {
      onProgress(1.0)
      onProgressComplete(position)
    } else {
      onProgress(progress)
    }
  }

  private fun isExitingFocusUp(): Boolean {
    val currentID = focusedView?.id ?: return false
    val nextView = focusedView?.nextFocusUpId?.let { findViewById<View>(it) }
    return nextView == null || currentID == nextView.id
  }

  private fun isExitingFocusDown(): Boolean {
    val currentID = focusedView?.id ?: return false
    val nextView = focusedView?.nextFocusDownId?.let { findViewById<View>(it) }
    return nextView == null || currentID == nextView.id
  }

  private fun isExitingFocusLeft(): Boolean {
    val currentID = focusedView?.id ?: return false
    val nextView = focusedView?.nextFocusLeftId?.let { findViewById<View>(it) }
    return nextView == null || currentID == nextView.id
  }

  private fun isExitingFocusRight(): Boolean {
    val currentID = focusedView?.id ?: return false
    val nextView = focusedView?.nextFocusRightId?.let { findViewById<View>(it) }
    return nextView == null || currentID == nextView.id
  }

  private fun sudoFocusView(direction: Int) {

    when (direction) {
      View.FOCUS_DOWN -> {
        sudoView?.nextFocusUpId = focusedView?.id ?: recyclerView?.id ?: return
      }

      View.FOCUS_UP -> {
        sudoView?.nextFocusDownId = focusedView?.id ?: recyclerView?.id ?: return
      }

      View.FOCUS_LEFT -> {
        sudoView?.nextFocusRightId = focusedView?.id ?: recyclerView?.id ?: return
      }

      View.FOCUS_RIGHT -> {
        sudoView?.nextFocusLeftId = focusedView?.id ?: recyclerView?.id ?: return
      }

    }
    sudoView?.requestFocus()
  }

  override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
    if (event?.action == KeyEvent.ACTION_DOWN) {
      when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN -> {
          if (isExitingFocusDown()) {
            notifyExitDirection("down")
            sudoFocusView(View.FOCUS_DOWN)
            return true
          }
        }

        KeyEvent.KEYCODE_DPAD_UP -> {
          if (isExitingFocusUp()) {
            notifyExitDirection("up")
            sudoFocusView(View.FOCUS_UP)
            return true
          }
        }

        KeyEvent.KEYCODE_DPAD_LEFT -> {
          if (isExitingFocusLeft()) {
            notifyExitDirection("left")
            sudoFocusView(View.FOCUS_LEFT)
            return true
          }
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          if (isExitingFocusRight()) {
            notifyExitDirection("right")
            sudoFocusView(View.FOCUS_RIGHT)
            return true
          }
        }
      }
    }

    return super.dispatchKeyEvent(event)
  }

  private fun notifyExitDirection(event: String) {
    val directionMap = Arguments.createMap().apply {
      putString("direction", event)
    }
    emitter.emit("exitDirection", directionMap)
  }


  private fun updateLiveChannelTile(data: LiveChannelTile?): LiveChannelTile? {
    // Return null if data is null
    if (data == null) return null

    // Create a deep copy of the data object
    val tile = data.deepCopy()

    when (data.type) {
      LiveChannelType.ON_NOW.value -> {
        data.liveShow?.let { liveShow ->
          val liveShowEndTime = liveShow.endTime
          tile.liveShow = data.schedule.find { show -> liveShowEndTime <= show.startTime }
        }
      }

      LiveChannelType.ON_NEXT.value -> {
        data.nextShow?.let { nextShow ->
          val nextShowEndTime = nextShow.endTime
          tile.nextShow = data.schedule.find { show -> nextShowEndTime <= show.startTime }
        }
      }

      LiveChannelType.ON_LATER.value -> {
        data.laterShow?.let { laterShow ->
          val laterShowEndTime = laterShow.endTime
          tile.laterShow = data.schedule.find { show -> laterShowEndTime <= show.startTime }
        }
      }
    }

    return tile
  }


}


