package com.recyclerlist.utils

import com.recyclerlist.model.LiveChannelTile
import com.recyclerlist.model.LiveShowMetadata
import com.recyclerlist.model.RenderItem
import kotlin.random.Random

// Function to update startTime and endTime for all LiveShowMetadata in RenderItems
fun updateStartAndEndTimes(renderItems: List<RenderItem<LiveChannelTile>>) {
  val currentTime = System.currentTimeMillis()/1000

  for (renderItem in renderItems) {
    for (liveChannelTile in renderItem.items) {
      liveChannelTile.liveShow?.let { updateShowTimes(it, currentTime) }
      liveChannelTile.nextShow?.let { updateShowTimes(it, currentTime) }
      liveChannelTile.laterShow?.let { updateShowTimes(it, currentTime) }
    }
  }
}

// Helper function to update start and end times for a single LiveShowMetadata
fun updateShowTimes(liveShow: LiveShowMetadata, currentTime: Long) {
  // Randomly generate start time between 10 to 30 seconds ago
  val randomStartOffset = Random.nextLong(10, 30)
  liveShow.startTime = currentTime - randomStartOffset

  // Randomly generate end time between 10 to 30 seconds in the future
  val randomEndOffset = Random.nextLong(10, 30)
  liveShow.endTime = currentTime + randomEndOffset
}
