package com.recyclerlist

import android.view.View

interface ItemActionListener<T> {
  fun onItemClicked(item: T, position: Int)
  fun onItemFocused(view: View, position: Int, isFocused: Boolean)
}
