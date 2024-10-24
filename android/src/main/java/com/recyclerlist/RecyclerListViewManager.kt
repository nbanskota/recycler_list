package com.recyclerlist


import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp


class RecyclerListViewManager : SimpleViewManager<View>() {
  override fun getName() = "RecyclerListView"

  private var columnCount: Int = 0
  private var adapter: RecyclerListAdapter? = null
  private lateinit var layoutManager: LayoutManager


  override fun createViewInstance(reactContext: ThemedReactContext): View {
    val recyclerView = RecyclerView(reactContext).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        focusable = View.FOCUSABLE
      }
      isFocusableInTouchMode = true
    }
    recyclerView.layoutManager = LinearLayoutManager(reactContext)

    this.adapter = RecyclerListAdapter(emptyList()) { position ->
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit(
        "onItemFocusChange",
        position
      )
    }
    recyclerView.adapter = adapter

    recyclerView.viewTreeObserver.addOnGlobalLayoutListener {
      Log.d("RecyclerListViewManager", "Width: ${recyclerView.width}")
      Log.d("RecyclerListViewManager", "Height: ${recyclerView.height}")
    }
    return recyclerView
  }

  @ReactProp(name = "data")
  fun setData(view: RecyclerView, data: ReadableArray) {
    if (adapter == null) return
    val items = mutableListOf<Item>()
    for (i in 0 until data.size()) {
      val obj = data.getMap(i)
      val title = obj.getString("title")
      items.add(Item(title!!, "Item at index: $i"))
    }
    adapter!!.updateData(items)
    adapter!!.notifyDataSetChanged()
  }

  // Add a method to set the column count from React Native
  @ReactProp(name = "columns")
  fun setColumnCount(view: RecyclerView, columns: Int) {
    if (columns > 1) {
      view.layoutManager = GridLayoutManager(view.context, columns)
    } else {
      view.layoutManager = LinearLayoutManager(view.context)
    }
    adapter?.let { it.notifyDataSetChanged() }
  }


  @ReactProp(name = "focus")
  fun onFocus(view: RecyclerView, data: ReadableArray) {
    // view.setData(data)
  }


  @ReactProp(name = "onPress")
  fun onPress(view: RecyclerView, data: ReadableArray) {
    // view.setData(data)
  }

}
