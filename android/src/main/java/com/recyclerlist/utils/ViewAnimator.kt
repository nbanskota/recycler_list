package com.recyclerlist.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

class ViewAnimator {

  // Function to animate alpha (opacity) of a view with a completion callback
  fun animateAlpha(view: View, fromAlpha: Float, toAlpha: Float, duration: Long, onAnimationEnd: (() -> Unit)?) {
    val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", fromAlpha, toAlpha)
    alphaAnimator.duration = duration
    alphaAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        onAnimationEnd?.let { it() }
      }
    })
    alphaAnimator.start()
  }

  // Function to animate translationY (move vertically) with a completion callback
  fun animateTranslationY(view: View, fromY: Float, toY: Float, duration: Long, onAnimationEnd: (() -> Unit)?) {
    val translationYAnimator = ObjectAnimator.ofFloat(view, "translationY", fromY, toY)
    translationYAnimator.duration = duration
    translationYAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        onAnimationEnd?.let { it() }
      }
    })
    translationYAnimator.start()
  }

  // Function to animate both alpha and translationY at the same time with a completion callback
  fun animateAlphaAndTranslation(
    view: View,
    fromAlpha: Float,
    toAlpha: Float,
    fromY: Float,
    toY: Float,
    duration: Long,
    onAnimationEnd: (() -> Unit)?
  ) {
    val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", fromAlpha, toAlpha)
    val translationYAnimator = ObjectAnimator.ofFloat(view, "translationY", fromY, toY)
    val animatorSet = AnimatorSet()
    animatorSet.playTogether(alphaAnimator, translationYAnimator)
    animatorSet.duration = duration
    animatorSet.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        onAnimationEnd?.let { it() }
      }
    })

    animatorSet.start()
  }

  // Function to animate rotation with a completion callback
  fun animateRotation(view: View, fromRotation: Float, toRotation: Float, duration: Long, onAnimationEnd: (() -> Unit)?) {
    val rotationAnimator = ObjectAnimator.ofFloat(view, "rotation", fromRotation, toRotation)
    rotationAnimator.duration = duration
    rotationAnimator.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        onAnimationEnd?.let { it() }
      }
    })
    rotationAnimator.start()
  }

  // Function to scale a view (scaleX and scaleY together) with a completion callback
  fun animateScale(
    view: View,
    fromScaleX: Float,
    toScaleX: Float,
    fromScaleY: Float,
    toScaleY: Float,
    duration: Long,
    onAnimationEnd: (() -> Unit)? = null
  ) {
    val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", fromScaleX, toScaleX)
    val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", fromScaleY, toScaleY)

    val animatorSet = AnimatorSet()
    animatorSet.playTogether(scaleXAnimator, scaleYAnimator)
    animatorSet.duration = duration
    animatorSet.addListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        onAnimationEnd?.let { it() }
      }
    })
    animatorSet.start()
  }
}
