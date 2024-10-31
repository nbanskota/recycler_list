package com.recyclerlist.utils

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class Emitter(private val context: ReactContext) {

  fun emit(eventName: String, event: WritableMap){
    Log.d("Emitter", "Send event to JS: name: $eventName  event: $event")
    context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit(
      eventName,
      event
    )
  }

}
