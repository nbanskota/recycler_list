package com.recyclerlist

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.facebook.react.ReactActivity
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.google.gson.Gson
import com.recyclerlist.utils.InputDetector

class FocusManager(private val context: ReactContext, private val inputListener: InputListener) : InputDetector(context, inputListener){
    private val TAG = "FocusManager::"
    private val componentRefMap = mutableMapOf<String, Int>()

  fun createMap(){

  }

}
