package com.recyclerlist.utils

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatTimeRange(startTime: Long?, endTime: Long?): String? {
  if (startTime == null || endTime == null) {
    return null
  }

  val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault() // Set the time zone
  }

  val startDate = Date(startTime * 1000) // Convert seconds to milliseconds
  val endDate = Date(endTime * 1000)

  val startFormatted = formatter.format(startDate)
  val endFormatted = formatter.format(endDate)

  return "$startFormatted - $endFormatted"
}


fun ReadableMap.toJson(): String {
  val jsonObject = JSONObject()

  val keys = this.keySetIterator()
  while (keys.hasNextKey()) {
    val key = keys.nextKey()
    when (this.getType(key)) {
      ReadableType.String -> jsonObject.put(key, this.getString(key))
      ReadableType.Number -> jsonObject.put(key, this.getDouble(key))
      ReadableType.Boolean -> jsonObject.put(key, this.getBoolean(key))
      ReadableType.Map -> jsonObject.put(key, this.getMap(key)?.toJson()?.let { JSONObject(it) })
      ReadableType.Array -> jsonObject.put(key, this.getArray(key)?.toJson())
      else -> {}
    }
  }
  return jsonObject.toString()
}

fun ReadableArray.toJson(): JSONArray {
  val jsonArray = JSONArray()
  for (i in 0 until this.size()) {
    when (this.getType(i)) {
      ReadableType.String -> jsonArray.put(this.getString(i))
      ReadableType.Number -> jsonArray.put(this.getDouble(i))
      ReadableType.Boolean -> jsonArray.put(this.getBoolean(i))
      ReadableType.Map -> jsonArray.put(JSONObject(this.getMap(i).toJson()))
      ReadableType.Array -> jsonArray.put(this.getArray(i).toJson())
      else -> {}
    }
  }
  return jsonArray
}

inline fun <reified T> jsonToObject(json: String): T {
  val gson = Gson()
  return gson.fromJson(json, object : TypeToken<T>() {}.type)
}


fun animateViewOnPress(view: View) {
  // Scale up animation
  val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f)
  val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)

  // Scale down animation
  val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
  val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f)

  // Set duration for animations
  scaleUpX.duration = 200
  scaleUpY.duration = 200
  scaleDownX.duration = 200
  scaleDownY.duration = 200

  // Start the scale up animation and then scale down
  scaleDownX.start()
  scaleDownY.start()

  // Listener to start scale down after scaling up is complete
  scaleDownX.addListener(object : Animator.AnimatorListener {
    override fun onAnimationEnd(animation: Animator) {
      scaleUpX.start()
      scaleUpY.start()
    }
    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
  })
}
