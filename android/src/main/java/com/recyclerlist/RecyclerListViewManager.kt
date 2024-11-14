package com.recyclerlist

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp


class RecyclerListViewManager(private val applicationContext: ReactApplicationContext) : SimpleViewManager<View>() {
  private val TAG = "RecyclerListViewManager"
  override fun getName() = "RecyclerListView"
  private lateinit var recyclerListView: RecyclerList

  companion object {
    const val ON_ITEM_PRESS: String = "onItemPress"
    const val ON_FOCUS_CHANGE: String = "onFocusChange"
    const val COMMAND_REQUEST_FOCUS = 1
    const val COMMAND_CLEAR_FOCUS = 2
    const val COMMAND_SET_SURROUNDING_VIEWS = 3
  }


  override fun createViewInstance(reactContext: ThemedReactContext): View {

    this.recyclerListView = RecyclerList(applicationContext).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusable = View.FOCUSABLE
      }
      isFocusableInTouchMode = true
    }
    return recyclerListView
  }

  override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Map<String, String>> {
    return mapOf(
      ON_ITEM_PRESS to mapOf("onItemPress" to ON_ITEM_PRESS),
      ON_FOCUS_CHANGE to mapOf("onFocusChange" to ON_FOCUS_CHANGE),
    )
  }

  override fun getCommandsMap(): Map<String, Int> {
    return mapOf(
      "requestFocus" to COMMAND_REQUEST_FOCUS,
      "clearFocus" to COMMAND_CLEAR_FOCUS,
      "setSurroundingViews" to COMMAND_SET_SURROUNDING_VIEWS
    )
  }


  override fun receiveCommand(root: View, commandId: String?, args: ReadableArray?) {
    Log.d(TAG, "receiveCommand: $args ")
    super.receiveCommand(root, commandId, args)
    when (commandId?.toInt()) {
      COMMAND_REQUEST_FOCUS -> root.requestFocus()
      COMMAND_CLEAR_FOCUS -> root.clearFocus()
      COMMAND_SET_SURROUNDING_VIEWS -> setSurroundingViews(root, args)
    }
  }

  private fun setSurroundingViews(view: View, args: ReadableArray?) {
    if (args != null && args.size() == 4) {
      val topFocusableViewId = if (args.getInt(0) != -1) args.getInt(0) else null
      val bottomFocusableViewId = if (args.getInt(1) != -1) args.getInt(1) else null
      val leftFocusableViewId = if (args.getInt(2) != -1) args.getInt(2) else null
      val rightFocusableViewId = if (args.getInt(3) != -1) args.getInt(3) else null
      (view as RecyclerList).setSurroundingViews(topFocusableViewId, bottomFocusableViewId, leftFocusableViewId, rightFocusableViewId)
    }

  }

  @ReactProp(name = "data")
  fun setData(view: RecyclerList, data: ReadableArray) {
    view.setData(data)
  }

  // Add a method to set the column count from React Native
  @ReactProp(name = "config")
  fun setConfig(view: RecyclerList, config: ReadableMap) {
    var columnCount = 1
    var direction = 0
    var spans = arrayListOf<Int>()
    if (config.hasKey("columnCount")) {
      columnCount = config.getInt("columnCount")
    }

    if (config.hasKey("direction")) {
      direction = config.getInt("direction")
      direction = if (direction >= 1) 1 else 0
    }
    if (config.hasKey("itemSpan")) {
      config.getArray("itemSpan").also { readableArray ->
        readableArray?.let {
          for (item in it.toArrayList()) {
            spans.add((item as Double).toInt())
          }
        }
      }
    }
    Log.d("RecyclerList", "setColumnCount: $columnCount $direction $spans")
    view.setColumnCount(columnCount, direction, spans)
  }


}
