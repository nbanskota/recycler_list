import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.recyclerlist.utils.Debounce
import kotlinx.coroutines.MainScope

class CustomLayoutManager(private val mContext: Context, spanCount: Int, @param:RecyclerView.Orientation private val orientation: Int, reverseLayout: Boolean) :
    GridLayoutManager(mContext, spanCount, orientation, reverseLayout) {
    private var isScrolling = false
    private var debounce = Debounce(MainScope())


  companion object {
    private const val MILLISECONDS_PER_INCH = 200f
  }

    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        val extraSpace = 3000 // Adjust this value  (in pixels)
        extraLayoutSpace[0] = extraSpace // Extra space at the top (above the viewable area)
        extraLayoutSpace[1] = extraSpace // Extra space at the bottom
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        if (isScrolling) return
        isScrolling = true
        val smoothScroller: SmoothScroller =
            object : LinearSmoothScroller(mContext) {
                override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                    return boxStart - viewStart
                }

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return (MILLISECONDS_PER_INCH / displayMetrics.densityDpi)
                }

              override fun calculateTimeForScrolling(dx: Int): Int {
                return super.calculateTimeForScrolling(dx )
              }

                override fun onStop() {
                    val targetView = findViewByPosition(targetPosition)
                    isScrolling = false
                    targetView?.requestFocus()
                    super.onStop()
                }
            }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    fun smoothScrollToPositionWithDelay(recyclerView: RecyclerView, state: RecyclerView.State?, position: Int) {
      debounce.withDelay(50L){
        this@CustomLayoutManager.smoothScrollToPosition(recyclerView, RecyclerView.State(), position)
      }
    }

  override fun onFocusSearchFailed(focused: View, direction: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): View? {
    Log.d("CustomLayoutManager", "onFocusSearchFailed: ")
    val nextFocus = super.onFocusSearchFailed(focused, direction, recycler, state)
    return nextFocus ?: focused
  }


}
