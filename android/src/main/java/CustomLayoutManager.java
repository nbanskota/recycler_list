import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class CustomLayoutManager extends GridLayoutManager {
  private static final float MILLISECONDS_PER_INCH = 160f;
  private Context mContext;

  public CustomLayoutManager(Context context, int spanCount) {
    super(context, spanCount);
    mContext = context;
  }

  @Override
  public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {

    RecyclerView.SmoothScroller smoothScroller =
      new LinearSmoothScroller(mContext)
      {
//
//        @Override
//        public PointF computeScrollVectorForPosition
//          (int targetPosition) {
//          return CustomLayoutManager.this
//            .computeScrollVectorForPosition(targetPosition);
//        }


        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
          return (boxStart + (boxEnd - boxStart) / 4) - (viewStart + (viewEnd - viewStart) / 4);
        }

        @Override
        protected float calculateSpeedPerPixel
          (DisplayMetrics displayMetrics) {
          return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }
      };

    smoothScroller.setTargetPosition(position);
    startSmoothScroll(smoothScroller);
  }

}
