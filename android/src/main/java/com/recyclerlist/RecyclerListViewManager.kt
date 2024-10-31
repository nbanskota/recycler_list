package com.recyclerlist

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.BrowseFrameLayout
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.recyclerlist.utils.InputDetector


class RecyclerListViewManager : SimpleViewManager<View>(), InputDetector.InputListener {
  override fun getName() = "RecyclerListView"

  private var focusManager : FocusManager? = null

  companion object{
    const val ON_ITEM_PRESS: String = "onItemPress"
    const val ON_FOCUS_CHANGE: String = "onFocusChange"
  }



  override fun createViewInstance(reactContext: ThemedReactContext): View {
    focusManager = FocusManager(reactContext, this)

    val browseFrameLayout = BrowseFrameLayout(reactContext)
    val recyclerView = RecyclerList(reactContext, focusManager!!).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusable = View.FOCUSABLE
      }
      isFocusableInTouchMode = true
    }
    return recyclerView
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
  @ReactProp(name = "columns")
  fun setColumnCount(view: RecyclerList, columns: Int) {
   view.setColumnCount(columns)
  }

  override fun dispatchUserAction(writableMap: WritableMap?) {
    TODO("Not yet implemented")
  }


}
