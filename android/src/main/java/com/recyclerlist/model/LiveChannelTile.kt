package com.recyclerlist.model

import com.google.gson.Gson

data class LiveChannelTile(
  val type: String,
  val key: String,
  val name: String,
  val color: String,
  val logoUrl: String,
  val iconUrl: String,
  val nowOnUrl: String,
  val sponsorLogoUrl: String,
  val slateUrl: String,
  val streamUrl: String,
  val streamKey: String,
  val publisherId: String,
  val isMemberGated: Boolean,
  val schedule: List<LiveShowMetadata>,
  var liveShow: LiveShowMetadata? = null,
  var nextShow: LiveShowMetadata? = null,
  var laterShow: LiveShowMetadata? = null,
  val relatedCategoryIndex: Int? = null
){

  fun toJson(): String{
    val gson = Gson()
    return gson.toJson(this)
  }

  fun deepCopy(): LiveChannelTile {
    return LiveChannelTile(
      type = this.type,
      liveShow = this.liveShow?.copy(),
      nextShow = this.nextShow?.copy(),
      laterShow = this.laterShow?.copy(),
      schedule = this.schedule.map { it.copy() },
      key = this.key,
      name = this.name,
      color = this.color,
      logoUrl = this.logoUrl,
      iconUrl = this.iconUrl,
      nowOnUrl = this.nowOnUrl,
      sponsorLogoUrl = this.sponsorLogoUrl,
      slateUrl = this.slateUrl,
      streamUrl = this.streamUrl,
      streamKey = this.streamKey,
      publisherId = this.publisherId,
      isMemberGated = this.isMemberGated
    )
  }
}

enum class LiveChannelType(val value: String) {
  ON_NOW("onNow"),
  ON_NEXT("onNext"),
  ON_LATER("onLater");

  companion object {
    fun fromValue(value: String): LiveChannelType? {
      return entries.find { it.value == value }
    }
  }
}

data class LiveShowMetadata(
  val id: String?,
  val title: String?,
  val description: String?,
  val dataUrl: String?,
  val iconUrl: String?,
  var startTime: Long,
  var endTime: Long,
  val posterImageUrl: String,
  val landscapeImageUrl: String,
  val genre: String,
  val rating: String? = null,
  val ratings: String,
  val ctaButtons: List<CtaButtons>? = null,
  val consumerAdvice: String? = null,
  val displayOverlay: Boolean? = null,
  val ctaButtonsFormatted: List<CtaButtons>? = null
)

data class CtaButtons(
  val type: CtaButtonType,
  val endpoint: String,
  val text: String
)

enum class CtaButtonType(val value: String) {
  SHOW("show"),
  VIDEO("video");

  companion object {
    fun fromValue(value: String): CtaButtonType? {
      return entries.find { it.value == value }
    }
  }
}
