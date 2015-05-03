package android.support.v7.internal.widget;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

class ActionBarBackgroundDrawableV21 extends ActionBarBackgroundDrawable {

    public ActionBarBackgroundDrawableV21(ActionBarContainer container) {
        super(container);
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        if (mContainer.mIsSplit) {
            if (mContainer.mSplitBackground != null) {
                mContainer.mSplitBackground.getOutline(outline);
            }
        } else {
            // ignore the stacked background for shadow casting
            if (mContainer.mBackground != null) {
                mContainer.mBackground.getOutline(outline);
            }
        }
    }
}
