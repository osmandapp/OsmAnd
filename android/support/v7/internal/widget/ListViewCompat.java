/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.reflect.Field;

/**
 * This class contains a number of useful things for ListView. Mainly used by
 * {@link android.support.v7.widget.ListPopupWindow}.
 *
 * @hide
 */
public class ListViewCompat extends ListView {

    public static final int INVALID_POSITION = -1;
    public static final int NO_POSITION = -1;

    private static final int[] STATE_SET_NOTHING = new int[] { 0 };

    final Rect mSelectorRect = new Rect();
    int mSelectionLeftPadding = 0;
    int mSelectionTopPadding = 0;
    int mSelectionRightPadding = 0;
    int mSelectionBottomPadding = 0;

    private Field mIsChildViewEnabled;

    private GateKeeperDrawable mSelector;

    public ListViewCompat(Context context) {
        this(context, null);
    }

    public ListViewCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListViewCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        try {
            mIsChildViewEnabled = AbsListView.class.getDeclaredField("mIsChildViewEnabled");
            mIsChildViewEnabled.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSelector(Drawable sel) {
        mSelector = sel != null ? new GateKeeperDrawable(sel) : null;
        super.setSelector(mSelector);

        final Rect padding = new Rect();
        if (sel != null) {
            sel.getPadding(padding);
        }

        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        setSelectorEnabled(true);
        updateSelectorStateCompat();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        final boolean drawSelectorOnTop = false;
        if (!drawSelectorOnTop) {
            drawSelectorCompat(canvas);
        }

        super.dispatchDraw(canvas);
    }

    protected void updateSelectorStateCompat() {
        Drawable selector = getSelector();
        if (selector != null && shouldShowSelectorCompat()) {
            selector.setState(getDrawableState());
        }
    }

    protected boolean shouldShowSelectorCompat() {
        return touchModeDrawsInPressedStateCompat() && isPressed();
    }

    protected boolean touchModeDrawsInPressedStateCompat() {
        return false;
    }

    protected void drawSelectorCompat(Canvas canvas) {
        if (!mSelectorRect.isEmpty()) {
            final Drawable selector = getSelector();
            if (selector != null) {
                selector.setBounds(mSelectorRect);
                selector.draw(canvas);
            }
        }
    }

    /**
     * Find a position that can be selected (i.e., is not a separator).
     *
     * @param position The starting position to look at.
     * @param lookDown Whether to look down for other positions.
     * @return The next selectable position starting at position and then searching either up or
     *         down. Returns {@link #INVALID_POSITION} if nothing can be found.
     */
    public int lookForSelectablePosition(int position, boolean lookDown) {
        final ListAdapter adapter = getAdapter();
        if (adapter == null || isInTouchMode()) {
            return INVALID_POSITION;
        }

        final int count = adapter.getCount();
        if (!getAdapter().areAllItemsEnabled()) {
            if (lookDown) {
                position = Math.max(0, position);
                while (position < count && !adapter.isEnabled(position)) {
                    position++;
                }
            } else {
                position = Math.min(position, count - 1);
                while (position >= 0 && !adapter.isEnabled(position)) {
                    position--;
                }
            }

            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        } else {
            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        }
    }

    protected void positionSelectorLikeTouchCompat(int position, View sel, float x, float y) {
        positionSelectorLikeFocusCompat(position, sel);

        Drawable selector = getSelector();
        if (selector != null && position != INVALID_POSITION) {
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    protected void positionSelectorLikeFocusCompat(int position, View sel) {
        // If we're changing position, update the visibility since the selector
        // is technically being detached from the previous selection.
        final Drawable selector = getSelector();
        final boolean manageState = selector != null && position != INVALID_POSITION;
        if (manageState) {
            selector.setVisible(false, false);
        }

        positionSelectorCompat(position, sel);

        if (manageState) {
            final Rect bounds = mSelectorRect;
            final float x = bounds.exactCenterX();
            final float y = bounds.exactCenterY();
            selector.setVisible(getVisibility() == VISIBLE, false);
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    protected void positionSelectorCompat(int position, View sel) {
        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());

        // Adjust for selection padding.
        selectorRect.left -= mSelectionLeftPadding;
        selectorRect.top -= mSelectionTopPadding;
        selectorRect.right += mSelectionRightPadding;
        selectorRect.bottom += mSelectionBottomPadding;

        try {
            // AbsListView.mIsChildViewEnabled controls the selector's state so we need to
            // modify it's value
            final boolean isChildViewEnabled = mIsChildViewEnabled.getBoolean(this);
            if (sel.isEnabled() != isChildViewEnabled) {
                mIsChildViewEnabled.set(this, !isChildViewEnabled);
                if (position != INVALID_POSITION) {
                    refreshDrawableState();
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Measures the height of the given range of children (inclusive) and returns the height
     * with this ListView's padding and divider heights included. If maxHeight is provided, the
     * measuring will stop when the current height reaches maxHeight.
     *
     * @param widthMeasureSpec             The width measure spec to be given to a child's
     *                                     {@link View#measure(int, int)}.
     * @param startPosition                The position of the first child to be shown.
     * @param endPosition                  The (inclusive) position of the last child to be
     *                                     shown. Specify {@link #NO_POSITION} if the last child
     *                                     should be the last available child from the adapter.
     * @param maxHeight                    The maximum height that will be returned (if all the
     *                                     children don't fit in this value, this value will be
     *                                     returned).
     * @param disallowPartialChildPosition In general, whether the returned height should only
     *                                     contain entire children. This is more powerful--it is
     *                                     the first inclusive position at which partial
     *                                     children will not be allowed. Example: it looks nice
     *                                     to have at least 3 completely visible children, and
     *                                     in portrait this will most likely fit; but in
     *                                     landscape there could be times when even 2 children
     *                                     can not be completely shown, so a value of 2
     *                                     (remember, inclusive) would be good (assuming
     *                                     startPosition is 0).
     * @return The height of this ListView with the given children.
     */
    public int measureHeightOfChildrenCompat(int widthMeasureSpec, int startPosition,
            int endPosition, final int maxHeight,
            int disallowPartialChildPosition) {

        final int paddingTop = getListPaddingTop();
        final int paddingBottom = getListPaddingBottom();
        final int paddingLeft = getListPaddingLeft();
        final int paddingRight = getListPaddingRight();
        final int reportedDividerHeight = getDividerHeight();
        final Drawable divider = getDivider();

        final ListAdapter adapter = getAdapter();

        if (adapter == null) {
            return paddingTop + paddingBottom;
        }

        // Include the padding of the list
        int returnedHeight = paddingTop + paddingBottom;
        final int dividerHeight = ((reportedDividerHeight > 0) && divider != null)
                ? reportedDividerHeight : 0;

        // The previous height value that was less than maxHeight and contained
        // no partial children
        int prevHeightWithoutPartialChild = 0;

        View child = null;
        int viewType = 0;
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            int newType = adapter.getItemViewType(i);
            if (newType != viewType) {
                child = null;
                viewType = newType;
            }
            child = adapter.getView(i, child, this);

            // Compute child height spec
            int heightMeasureSpec;
            final ViewGroup.LayoutParams childLp = child.getLayoutParams();
            if (childLp != null && childLp.height > 0) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(childLp.height,
                        MeasureSpec.EXACTLY);
            } else {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            child.measure(widthMeasureSpec, heightMeasureSpec);

            if (i > 0) {
                // Count the divider for all but one child
                returnedHeight += dividerHeight;
            }

            returnedHeight += child.getMeasuredHeight();

            if (returnedHeight >= maxHeight) {
                // We went over, figure out which height to return.  If returnedHeight >
                // maxHeight, then the i'th position did not fit completely.
                return (disallowPartialChildPosition >= 0) // Disallowing is enabled (> -1)
                        && (i > disallowPartialChildPosition) // We've past the min pos
                        && (prevHeightWithoutPartialChild > 0) // We have a prev height
                        && (returnedHeight != maxHeight) // i'th child did not fit completely
                        ? prevHeightWithoutPartialChild
                        : maxHeight;
            }

            if ((disallowPartialChildPosition >= 0) && (i >= disallowPartialChildPosition)) {
                prevHeightWithoutPartialChild = returnedHeight;
            }
        }

        // At this point, we went through the range of children, and they each
        // completely fit, so return the returnedHeight
        return returnedHeight;
    }

    protected void setSelectorEnabled(boolean enabled) {
        if (mSelector != null) {
            mSelector.setEnabled(enabled);
        }
    }

    private static class GateKeeperDrawable extends DrawableWrapper {
        private boolean mEnabled;

        public GateKeeperDrawable(Drawable drawable) {
            super(drawable);
            mEnabled = true;
        }

        void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        @Override
        public boolean setState(int[] stateSet) {
            if (mEnabled) {
                return super.setState(stateSet);
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas) {
            if (mEnabled) {
                super.draw(canvas);
            }
        }

        @Override
        public void setHotspot(float x, float y) {
            if (mEnabled) {
                super.setHotspot(x, y);
            }
        }

        @Override
        public void setHotspotBounds(int left, int top, int right, int bottom) {
            if (mEnabled) {
                super.setHotspotBounds(left, top, right, bottom);
            }
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (mEnabled) {
                return super.setVisible(visible, restart);
            }
            return false;
        }
    }


}
