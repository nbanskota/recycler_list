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
  val liveShow: LiveShowMetadata? = null,
  val nextShow: LiveShowMetadata? = null,
  val laterShow: LiveShowMetadata? = null,
  val relatedCategoryIndex: Int? = null
){

  fun toJson(): String{
    val gson = Gson()
    return gson.toJson(this)
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
  val id: String,
  val title: String,
  val description: String,
  val dataUrl: String,
  val iconUrl: String,
  val startTime: Long,
  val endTime: Long,
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
      return CtaButtonType.entries.find { it.value == value }
    }
  }
}
