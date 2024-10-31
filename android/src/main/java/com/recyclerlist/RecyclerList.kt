package com.recyclerlist

import CustomLayoutManager
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.ThemedReactContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.recyclerlist.model.Item
import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveChannelType
import com.recyclerlist.model.RenderItem
import com.recyclerlist.utils.Debouncer
import com.recyclerlist.utils.Emitter
import com.recyclerlist.utils.toJson
import kotlinx.coroutines.MainScope


//NOTE: span count is set to 4 for now.

class RecyclerList(private val context: ThemedReactContext, focusManager: FocusManager) : RecyclerView(context),
  ItemActionListener<LiveChannelTile> {
  private val TAG = "RecyclerList"
  private var columnCount: Int = 1
  private var recyclerListAdapter: RecyclerListAdapter? = null
  private var layoutManager: CustomLayoutManager
  private var isGridLayout: Boolean = false
  private var items = listOf<RenderItem<LiveChannelTile>>()
  private val debounce = Debouncer(MainScope())
  private val emitter = Emitter(context)
  private var focusManager: FocusManager? = null

  private var indexMap: MutableMap<Int, Int> = mutableMapOf()

  init {
    context.reactApplicationContext.currentActivity?.let {
      this.focusManager = focusManager
    }

    this.layoutManager = CustomLayoutManager(context, 1)
    setLayoutManager(this.layoutManager)

    this.recyclerListAdapter = RecyclerListAdapter(emptyList(), this)

    this.setAdapter(recyclerListAdapter)
    this.recyclerListAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
      override fun onChanged() {
        super.onChanged()
        Log.d(TAG, "Items changed")
        // setFocusMap()
      }
    })
  }

  fun updateRow(row: Int) {
    val startPosition = row * 4
    val endPosition = startPosition + 4
    recyclerListAdapter?.notifyItemRangeChanged(startPosition, 4) // Notify changes
  }

  private fun parseJsonToRenderItems(jsonString: String): RenderItem<LiveChannelTile> {
    val gson = Gson()
    val listType = object : TypeToken<RenderItem<LiveChannelTile>>() {}.type
    return gson.fromJson(jsonString, listType)
  }

  fun setData(data: ReadableArray) {
    if (this.recyclerListAdapter == null) return
    val items = mutableListOf<RenderItem<LiveChannelTile>>()
    for (i in 0 until data.size()) {
      val jsonString = data.getMap(i).toJson()
      val objects = parseJsonToRenderItems(jsonString)
      for (j in 0 until objects.items.size) {
        indexMap[i + j] = i * j
      }
      items.add(objects)
    }
    this.items = items
    recyclerListAdapter?.updateData(items)
  }

  fun setColumnCount(columns: Int) {
    this.columnCount = columns
    this.layoutManager = CustomLayoutManager(context, columns)
    if (columns > 1) {
      this.isGridLayout = true
      this.layoutManager.spanCount = 4
      this.layoutManager.spanSizeLookup = object :
        GridLayoutManager.SpanSizeLookup() {

        override fun getSpanSize(position: Int): Int {
          return when {
            recyclerListAdapter?.getItemViewType(position) == RecyclerListAdapter.TYPE_GROUP_HEADER -> layoutManager.spanCount
            else -> {
              val item = recyclerListAdapter?.getItem(position)
              return if (item?.type == LiveChannelType.ON_NOW.value) 2 else 1
            }
          }
        }
      }
    }

    setLayoutManager(this.layoutManager)
    recyclerListAdapter?.notifyDataSetChanged()
  }

  override fun onItemClicked(item: LiveChannelTile, index: Int) {
    val event = Arguments.createMap().apply {
      putInt("index", index)
      putString("item", item.toJson())
    }
    emitter.emit("onItemPress", event)
  }

  override fun onItemFocused(view: View, position: Int, isFocused: Boolean) {
    if (!isFocused) return
    this.smoothScrollToPosition(position)
    debounce.debounce(100L) {
      val event = Arguments.createMap().apply {
        putInt("index", position)
        putInt("viewId", (view.id))
      }
      emitter.emit("onFocusChange", event)
    }
  }


}


