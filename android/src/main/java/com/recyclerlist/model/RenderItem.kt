package com.recyclerlist.model


data class RenderItem<T>(
  var title: String? = null,
  val index: Int? = null,
  var items: MutableList<T> = mutableListOf()
)
//
//data class Item<T>(
//  val index: Int = 0,
//  val isPressed: ((T) -> Unit)? = null,
// var data : T
//)
