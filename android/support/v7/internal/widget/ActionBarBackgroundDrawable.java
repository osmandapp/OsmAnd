package android.support.v7.internal.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

class ActionBarBackgroundDrawable extends Drawable {

    final ActionBarContainer mContainer;

    public ActionBarBackgroundDrawable(ActionBarContainer container) {
        mContainer = container;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mContainer.mIsSplit) {
            if (mContainer.mSplitBackground != null) {
                mContainer.mSplitBackground.draw(canvas);
            }
        } else {
            if (mContainer.mBackground != null) {
                mContainer.mBackground.draw(canvas);
            }
            if (mContainer.mStackedBackground != null && mContainer.mIsStacked) {
                mContainer.mStackedBackground.draw(canvas);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

}
