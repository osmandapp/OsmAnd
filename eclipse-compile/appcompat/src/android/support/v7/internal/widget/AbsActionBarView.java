/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.appcompat.R;
import android.support.v7.internal.view.ViewPropertyAnimatorCompatSet;
import android.support.v7.widget.ActionMenuPresenter;
import android.support.v7.widget.ActionMenuView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

abstract class AbsActionBarView extends ViewGroup {
    private static final Interpolator sAlphaInterpolator = new DecelerateInterpolator();

    private static final int FADE_DURATION = 200;

    protected final VisibilityAnimListener mVisAnimListener = new VisibilityAnimListener();

    /** Context against which to inflate popup menus. */
    protected final Context mPopupContext;

    protected ActionMenuView mMenuView;
    protected ActionMenuPresenter mActionMenuPresenter;
    protected ViewGroup mSplitView;
    protected boolean mSplitActionBar;
    protected boolean mSplitWhenNarrow;
    protected int mContentHeight;

    protected ViewPropertyAnimatorCompat mVisibilityAnim;

    AbsActionBarView(Context context) {
        this(context, null);
    }

    AbsActionBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    AbsActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarPopupTheme, tv, true)
                && tv.resourceId != 0) {
            mPopupContext = new ContextThemeWrapper(context, tv.resourceId);
        } else {
            mPopupContext = context;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= 8) {
            super.onConfigurationChanged(newConfig);
        }

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionBar_height, 0));
        a.recycle();

        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Sets whether the bar should be split right now, no questions asked.
     * @param split true if the bar should split
     */
    public void setSplitToolbar(boolean split) {
        mSplitActionBar = split;
    }

    /**
     * Sets whether the bar should split if we enter a narrow screen configuration.
     * @param splitWhenNarrow true if the bar should check to split after a config change
     */
    public void setSplitWhenNarrow(boolean splitWhenNarrow) {
        mSplitWhenNarrow = splitWhenNarrow;
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
        requestLayout();
    }

    public int getContentHeight() {
        return mContentHeight;
    }

    public void setSplitView(ViewGroup splitView) {
        mSplitView = splitView;
    }

    /**
     * @return Current visibility or if animating, the visibility being animated to.
     */
    public int getAnimatedVisibility() {
        if (mVisibilityAnim != null) {
            return mVisAnimListener.mFinalVisibility;
        }
        return getVisibility();
    }

    public void animateToVisibility(int visibility) {
        if (mVisibilityAnim != null) {
            mVisibilityAnim.cancel();
        }
        if (visibility == VISIBLE) {
            if (getVisibility() != VISIBLE) {
                ViewCompat.setAlpha(this, 0f);
                if (mSplitView != null && mMenuView != null) {
                    ViewCompat.setAlpha(mMenuView, 0f);
                }
            }
            ViewPropertyAnimatorCompat anim = ViewCompat.animate(this).alpha(1f);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);
            if (mSplitView != null && mMenuView != null) {
                ViewPropertyAnimatorCompatSet set = new ViewPropertyAnimatorCompatSet();
                ViewPropertyAnimatorCompat splitAnim = ViewCompat.animate(mMenuView).alpha(1f);
                splitAnim.setDuration(FADE_DURATION);
                set.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
                set.play(anim).play(splitAnim);
                set.start();
            } else {
                anim.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
                anim.start();
            }
        } else {
            ViewPropertyAnimatorCompat anim = ViewCompat.animate(this).alpha(0f);
            anim.setDuration(FADE_DURATION);
            anim.setInterpolator(sAlphaInterpolator);
            if (mSplitView != null && mMenuView != null) {
                ViewPropertyAnimatorCompatSet set = new ViewPropertyAnimatorCompatSet();
                ViewPropertyAnimatorCompat splitAnim = ViewCompat.animate(mMenuView).alpha(0f);
                splitAnim.setDuration(FADE_DURATION);
                set.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
                set.play(anim).play(splitAnim);
                set.start();
            } else {
                anim.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
                anim.start();
            }
        }
    }

    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    public void postShowOverflowMenu() {
        post(new Runnable() {
            public void run() {
                showOverflowMenu();
            }
        });
    }

    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    public boolean isOverflowMenuShowPending() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowPending();
        }
        return false;
    }

    public boolean isOverflowReserved() {
        return mActionMenuPresenter != null && mActionMenuPresenter.isOverflowReserved();
    }

    public boolean canShowOverflowMenu() {
        return isOverflowReserved() && getVisibility() == VISIBLE;
    }

    public void dismissPopupMenus() {
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
    }

    protected int measureChildView(View child, int availableWidth, int childSpecHeight,
            int spacing) {
        child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                childSpecHeight);

        availableWidth -= child.getMeasuredWidth();
        availableWidth -= spacing;

        return Math.max(0, availableWidth);
    }

    static protected int next(int x, int val, boolean isRtl) {
        return isRtl ? x - val : x + val;
    }

    protected int positionChild(View child, int x, int y, int contentHeight, boolean reverse) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        if (reverse) {
            child.layout(x - childWidth, childTop, x, childTop + childHeight);
        } else {
            child.layout(x, childTop, x + childWidth, childTop + childHeight);
        }

        return  (reverse ? -childWidth : childWidth);
    }

    protected class VisibilityAnimListener implements ViewPropertyAnimatorListener {
        private boolean mCanceled = false;
        int mFinalVisibility;

        public VisibilityAnimListener withFinalVisibility(ViewPropertyAnimatorCompat animation,
                int visibility) {
            mVisibilityAnim = animation;
            mFinalVisibility = visibility;
            return this;
        }

        @Override
        public void onAnimationStart(View view) {
            setVisibility(VISIBLE);
            mCanceled = false;
        }

        @Override
        public void onAnimationEnd(View view) {
            if (mCanceled) return;

            mVisibilityAnim = null;
            setVisibility(mFinalVisibility);
            if (mSplitView != null && mMenuView != null) {
                mMenuView.setVisibility(mFinalVisibility);
            }
        }

        @Override
        public void onAnimationCancel(View view) {
            mCanceled = true;
        }
    }
}
