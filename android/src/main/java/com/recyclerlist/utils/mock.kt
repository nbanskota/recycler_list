package com.recyclerlist.utils

import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveShowMetadata
import com.recyclerlist.model.RenderItem

fun updateStartAndEndTimes(renderItems: List<RenderItem<LiveChannelTile>>) {
  val currentTime = System.currentTimeMillis() / 1000

  for (renderItem in renderItems) {
    var startTime = 0L
    var endTime = 20L
    for (liveChannelTile in renderItem.items) {
      liveChannelTile.liveShow?.let { updateShowTimes(it, currentTime, 0, 10) }
      liveChannelTile.nextShow?.let { updateShowTimes(it, currentTime, 10, 20) }
      liveChannelTile.laterShow?.let { updateShowTimes(it, currentTime, 20, 30) }
      for(i in 0 until liveChannelTile.schedule.size){
        startTime += 10 * i
        endTime += 10 * i
        updateShowTimes(liveChannelTile.schedule[i], currentTime, startTime, endTime)
      }

    }
  }
}

fun updateShowTimes(liveShow: LiveShowMetadata, currentTime: Long, startTime: Long, endTime: Long) {
  liveShow.startTime = currentTime + startTime
  liveShow.endTime = currentTime + endTime
}
