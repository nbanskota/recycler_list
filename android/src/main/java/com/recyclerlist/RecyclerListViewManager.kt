package com.recyclerlist

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp


class RecyclerListViewManager : SimpleViewManager<View>() {
  override fun getName() = "RecyclerListView"
  private lateinit var recyclerListView: RecyclerList


  companion object{
    const val ON_ITEM_PRESS: String = "onItemPress"
    const val ON_FOCUS_CHANGE: String = "onFocusChange"
  }


  override fun createViewInstance(reactContext: ThemedReactContext): View {

    this.recyclerListView = RecyclerList(reactContext).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusable = View.FOCUSABLE
      }
      isFocusableInTouchMode = true
    }
//    val animator = DefaultItemAnimator()
//    recyclerListView.setItemAnimator(animator)
    return recyclerListView
  }

  override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Map<String, String>> {
    return mapOf(
      ON_ITEM_PRESS to mapOf("registrationName" to ON_ITEM_PRESS),
        ON_FOCUS_CHANGE to mapOf("registrationName" to ON_FOCUS_CHANGE)
    )
  }

  @ReactProp(name = "data")
  fun setData(view: RecyclerList, data: ReadableArray) {
    view.setData(data)
  }

  // Add a method to set the column count from React Native
  @ReactProp(name = "config")
  fun setConfig(view: RecyclerList, config: ReadableMap)  {
    var columnCount = 1
    var direction = 0
    var spans = arrayListOf<Int>()
    if (config.hasKey("columnCount")) {
      columnCount = config.getInt("columnCount")
    }

    if (config.hasKey("direction")) {
       direction = config.getInt("direction")
      direction = if(direction >= 1) 1 else 0
    }
    if (config.hasKey("itemSpan")) {
       config.getArray("itemSpan").also { readableArray ->
        readableArray?.let {
          for(item in it.toArrayList()){
            spans.add((item as Double).toInt())
          }
        }
      }
    }
    Log.d("RecyclerList", "setColumnCount: $columnCount $direction $spans")
   view.setColumnCount(columnCount, direction, spans)
  }

}
