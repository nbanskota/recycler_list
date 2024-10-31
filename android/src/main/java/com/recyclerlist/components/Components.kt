package com.recyclerlist.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

fun setBorder(borderColor: Int, strokeWidth: Int,  radius: Float, backgroundColor: Int? = null): GradientDrawable {
  val borderDrawable = GradientDrawable()
  borderDrawable.setStroke(borderColor, strokeWidth)
  borderDrawable.cornerRadius = radius
  backgroundColor?.let {
    borderDrawable.setColor(it)
  }
  return borderDrawable

}
