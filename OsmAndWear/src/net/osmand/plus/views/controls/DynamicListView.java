package net.osmand.plus.views.controls;
/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;

import java.util.List;

public class DynamicListView extends ObservableListView {

	protected final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 15;
	protected final int MOVE_DURATION = 150;

	protected List<Object> mItemsList;
	protected List<Object> mActiveItemsList;

	private DynamicListViewCallbacks dCallbacks;

	private int mLastEventY = -1;

	protected int mDownY = -1;
	protected int mDownX = -1;

	private int mTotalOffset;

	private boolean mCellIsMobile;
	private boolean mIsMobileScrolling;
	private int mSmoothScrollAmountAtEdge;
	private boolean itemsSwapped;

	protected final int INVALID_ID = -1;
	private long mAboveItemId = INVALID_ID;
	private long mMobileItemId = INVALID_ID;
	private long mBelowItemId = INVALID_ID;

	private BitmapDrawable mHoverCell;
	private Rect mHoverCellCurrentBounds;
	private Rect mHoverCellOriginalBounds;

	protected final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;

	private boolean mIsWaitingForScrollFinish;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	private GestureDetector singleTapDetector;
	private DragIcon tag;

	public interface DragIcon {
		void onClick();
	}

	public DynamicListView(Context context) {
		super(context);
		init(context);
	}

	public DynamicListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DynamicListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		setOnScrollListener(mScrollListener);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE / metrics.density);
		singleTapDetector = new GestureDetector(context, new SingleTapConfirm());
	}

	public void setDynamicListViewCallbacks(DynamicListViewCallbacks callbacks) {
		dCallbacks = callbacks;
	}

	/**
	 * Creates the hover cell with the appropriate bitmap and of appropriate
	 * size. The hover cell's BitmapDrawable is drawn on top of the bitmap every
	 * single time an invalidate call is made.
	 */
	private BitmapDrawable getAndAddHoverView(View v) {

		int w = v.getWidth();
		int h = v.getHeight();
		int top = v.getTop();
		int left = v.getLeft();

		Bitmap b = getBitmapFromView(v);

		BitmapDrawable drawable = new BitmapDrawable(getResources(), b);

		mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
		mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);

		drawable.setBounds(mHoverCellCurrentBounds);

		return drawable;
	}

	/**
	 * Returns a bitmap showing a screenshot of the view passed in.
	 */
	private Bitmap getBitmapFromView(View v) {
		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		v.draw(canvas);

		Bitmap bitmapOut = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvasOut = new Canvas(bitmapOut);
		canvasOut.drawColor(Color.TRANSPARENT);
		Paint p = new Paint();
		p.setAlpha(200);
		canvasOut.drawBitmap(bitmap, 0, 0, p);

		return bitmapOut;
	}

	public StableArrayAdapter getStableAdapter() {
		ListAdapter listAdapter = getAdapter();
		if (listAdapter instanceof HeaderViewListAdapter) {
			listAdapter = ((HeaderViewListAdapter) listAdapter).getWrappedAdapter();
		}
		if (listAdapter instanceof StableArrayAdapter) {
			return (StableArrayAdapter) listAdapter;
		}
		return null;
	}

	/**
	 * Stores a reference to the views above and below the item currently
	 * corresponding to the hover cell. It is important to note that if this
	 * item is either at the top or bottom of the list, mAboveItemId or mBelowItemId
	 * may be invalid.
	 */
	private void updateNeighborViewsForID(long itemID) {
		ListAdapter adapter = getAdapter();
		int position = getPositionForID(itemID);
		int pos = position;
		mAboveItemId = INVALID_ID;
		while (mAboveItemId == INVALID_ID && pos > 0) {
			pos--;
			mAboveItemId = adapter.getItemId(pos);
			if (mAboveItemId != INVALID_ID) {
				Object obj = adapter.getItem(pos);
				if (mActiveItemsList == null || !mActiveItemsList.contains(obj)) {
					mAboveItemId = INVALID_ID;
				}
			}
		}
		pos = position;
		mBelowItemId = INVALID_ID;
		while (mBelowItemId == INVALID_ID && pos < mItemsList.size()) {
			pos++;
			mBelowItemId = adapter.getItemId(pos);
			if (mBelowItemId != INVALID_ID) {
				Object obj = adapter.getItem(pos);
				if (mActiveItemsList == null || !mActiveItemsList.contains(obj)) {
					mBelowItemId = INVALID_ID;
				}
			}
		}
	}

	/**
	 * Retrieves the view in the list corresponding to itemID
	 */
	public View getViewForID(long itemID) {
		if (itemID != INVALID_ID) {
			int firstVisiblePosition = getFirstVisiblePosition();
			ListAdapter adapter = getAdapter();
			for (int i = 0; i < getChildCount(); i++) {
				View v = getChildAt(i);
				int position = firstVisiblePosition + i;
				long id = adapter.getItemId(position);
				if (id == itemID) {
					return v;
				}
			}
		}
		return null;
	}

	public void setAllVisible() {
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			if (v != null && v.getVisibility() != VISIBLE) {
				v.setVisibility(VISIBLE);
			}
		}
	}

	/**
	 * Retrieves the position in the list corresponding to itemID
	 */
	public int getPositionForID(long itemID) {
		View v = getViewForID(itemID);
		if (v == null) {
			return -1;
		} else {
			return getPositionForView(v);
		}
	}

	/**
	 * dispatchDraw gets invoked when all the child views are about to be drawn.
	 * By overriding this method, the hover cell (BitmapDrawable) can be drawn
	 * over the listview's items whenever the listview is redrawn.
	 */
	@Override
	protected void dispatchDraw(@NonNull Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHoverCell != null) {
			mHoverCell.draw(canvas);
		}

		// Draw dividers
		StableArrayAdapter stableAdapter = getStableAdapter();
		if (getDivider() == null && stableAdapter != null && stableAdapter.hasDividers()) {
			List<Drawable> dividers = stableAdapter.getDividers();

			int count = getChildCount();
			int first = getFirstVisiblePosition();
			int headerCount = getHeaderViewsCount();
			int itemCount = getCount();
			int footerLimit = (itemCount - getFooterViewsCount());

			Rect bounds = new Rect();
			bounds.left = getPaddingLeft();
			bounds.right = getRight() - getLeft() - getPaddingRight();

			int listBottom = getBottom() - getTop() + getScrollY();
			for (int i = 0; i < count; i++) {
				int itemIndex = (first + i);
				boolean isHeader = (itemIndex < headerCount);
				boolean isFooter = (itemIndex >= footerLimit);
				if (!isHeader && !isFooter && itemIndex < dividers.size()) {
					Drawable divider = dividers.get(itemIndex - headerCount);
					if (divider != null) {
						View child = getChildAt(i);
						int bottom = child.getBottom();
						boolean isLastItem = (i == (count - 1));

						if (bottom < listBottom && !isLastItem) {
							int nextIndex = (itemIndex + 1);
							if (nextIndex >= headerCount && nextIndex < footerLimit) {
								bounds.top = bottom;
								bounds.bottom = bottom + divider.getIntrinsicHeight();
								drawDivider(canvas, divider, bounds);
							}
						}
					}
				}
			}
		}
	}

	void drawDivider(Canvas canvas, Drawable divider, Rect bounds) {
		divider.setBounds(bounds);
		divider.draw(canvas);
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {

		if (singleTapDetector.onTouchEvent(event)) {
			if (tag != null) {
				tag.onClick();
			}
			touchEventsCancelled();
			return super.onTouchEvent(event);
		}

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (!mCellIsMobile && mHoverCell == null) {
					// Find the view that the user pressed their finger down on.
					View v = findViewAtPositionWithDragIconTag(getRootView(), (int) event.getRawX(), (int) event.getRawY());

					// If the view contains a tag set to "DragIcon" class, it means that the user wants to
					// reorder the list item.
					if ((v != null) && (v.getTag() != null) && (v.getTag() instanceof DragIcon)) {
						mDownX = (int) event.getX();
						mDownY = (int) event.getY();
						mActivePointerId = event.getPointerId(0);
						mTotalOffset = 0;
						tag = (DragIcon) v.getTag();

						int position = pointToPosition(mDownX, mDownY);
						if (position != INVALID_POSITION) {
							Object item = getAdapter().getItem(position);
							if (mActiveItemsList == null || mActiveItemsList.contains(item)) {

								int itemNum = position - getFirstVisiblePosition();
								itemsSwapped = false;

								View selectedView = getChildAt(itemNum);
								mMobileItemId = getAdapter().getItemId(position);
								mHoverCell = getAndAddHoverView(selectedView);
								selectedView.setVisibility(INVISIBLE);

								mCellIsMobile = true;
								updateNeighborViewsForID(mMobileItemId);

								if (dCallbacks != null) {
									dCallbacks.onItemSwapping(position);
								}
							}
						}
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (mActivePointerId == INVALID_POINTER_ID) {
					break;
				}

				int pointerIndex = event.findPointerIndex(mActivePointerId);

				mLastEventY = (int) event.getY(pointerIndex);
				int deltaY = mLastEventY - mDownY;

				if (mCellIsMobile && mHoverCell != null) {
					mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left,
							mHoverCellOriginalBounds.top + deltaY + mTotalOffset);
					mHoverCell.setBounds(mHoverCellCurrentBounds);
					invalidate();

					handleCellSwitch();

					mIsMobileScrolling = false;
					handleMobileCellScroll();

					return false;
				}
				break;

			case MotionEvent.ACTION_UP:
				touchEventsEnded();
				break;

			case MotionEvent.ACTION_CANCEL:
				touchEventsCancelled();
				break;

			case MotionEvent.ACTION_POINTER_UP:
				/* If a multitouch event took place and the original touch dictating
				 * the movement of the hover cell has ended, then the dragging event
                 * ends and the hover cell is animated to its corresponding position
                 * in the listview. */
				pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
						MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				int pointerId = event.getPointerId(pointerIndex);
				if (pointerId == mActivePointerId) {
					touchEventsEnded();
				}
				break;

			default:
				break;
		}

		return super.onTouchEvent(event);
	}

	/**
	 * This method determines whether the hover cell has been shifted far enough
	 * to invoke a cell swap. If so, then the respective cell swap candidate is
	 * determined and the data set is changed. Upon posting a notification of the
	 * data set change, a layout is invoked to place the cells in the right place.
	 * Using a ViewTreeObserver and a corresponding OnPreDrawListener, we can
	 * offset the cell being swapped to where it previously was and then animate it to
	 * its new position.
	 */
	private void handleCellSwitch() {
		int deltaY = mLastEventY - mDownY;
		int deltaYTotal = mHoverCellOriginalBounds.top + mTotalOffset + deltaY;

		View belowView = getViewForID(mBelowItemId);
		View mobileView = getViewForID(mMobileItemId);
		View aboveView = getViewForID(mAboveItemId);

		boolean isBelow = (belowView != null) && (deltaYTotal > belowView.getTop());
		boolean isAbove = (aboveView != null) && (deltaYTotal < aboveView.getTop());

		if (isBelow || isAbove) {

			long switchItemID = isBelow ? mBelowItemId : mAboveItemId;
			View switchView = isBelow ? belowView : aboveView;
			int originalItem = getPositionForView(mobileView) - getHeaderViewsCount();
			int switchItem = getPositionForView(switchView) - getHeaderViewsCount();
			swapElements(originalItem, switchItem);

			getStableAdapter().notifyDataSetChanged();

			mDownY = mLastEventY;

			int switchViewStartTop = switchView.getTop();

			if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.KITKAT) {
				mobileView.setVisibility(View.VISIBLE);
				switchView.setVisibility(View.INVISIBLE);
			}
			updateNeighborViewsForID(mMobileItemId);

			ViewTreeObserver observer = getViewTreeObserver();
			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);

					View switchView = getViewForID(switchItemID);

					mTotalOffset += deltaY;

					int switchViewNewTop = switchView.getTop();
					int delta = switchViewStartTop - switchViewNewTop;

					switchView.setTranslationY(delta);

					if (android.os.Build.VERSION.SDK_INT < 12) {
						ViewCompat.animate(switchView)
								.translationY(0)
								.setDuration(MOVE_DURATION);
					} else {
						ObjectAnimator animator = ObjectAnimator.ofFloat(switchView,
								View.TRANSLATION_Y, 0);
						animator.setDuration(MOVE_DURATION);
						animator.start();
					}

					return true;
				}
			});
		}
	}

	private void swapElements(int indexOne, int indexTwo) {
		Object obj1 = mItemsList.get(indexOne);
		Object obj2 = mItemsList.get(indexTwo);
		mItemsList.set(indexOne, obj2);
		mItemsList.set(indexTwo, obj1);

		int index1 = mActiveItemsList.indexOf(obj1);
		int index2 = mActiveItemsList.indexOf(obj2);
		if (index1 != -1 && index2 != -1) {
			mActiveItemsList.set(index1, obj2);
			mActiveItemsList.set(index2, obj1);
			itemsSwapped = true;
		}
	}


	/**
	 * Resets all the appropriate fields to a default state while also animating
	 * the hover cell back to its correct location.
	 */
	private void touchEventsEnded() {
		View mobileView = getViewForID(mMobileItemId);
		if (mCellIsMobile || mIsWaitingForScrollFinish) {
			mCellIsMobile = false;
			mIsWaitingForScrollFinish = false;
			mIsMobileScrolling = false;
			mActivePointerId = INVALID_POINTER_ID;

			// If the autoscroller has not completed scrolling, we need to wait for it to
			// finish in order to determine the final location of where the hover cell
			// should be animated to.
			if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				mIsWaitingForScrollFinish = true;
				return;
			}

			mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left, mobileView.getTop());

			if (android.os.Build.VERSION.SDK_INT < 12) {
				finishTouch();
			} else {
				/**
				 * This TypeEvaluator is used to animate the BitmapDrawable back to its
				 * final location when the user lifts his finger by modifying the
				 * BitmapDrawable's bounds.
				 */
				TypeEvaluator<Rect> sBoundEvaluator = new TypeEvaluator<Rect>() {
					public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
						return new Rect(interpolate(startValue.left, endValue.left, fraction),
								interpolate(startValue.top, endValue.top, fraction),
								interpolate(startValue.right, endValue.right, fraction),
								interpolate(startValue.bottom, endValue.bottom, fraction));
					}

					public int interpolate(int start, int end, float fraction) {
						return (int) (start + fraction * (end - start));
					}
				};

				ObjectAnimator hoverViewAnimator = ObjectAnimator.ofObject(mHoverCell, "bounds",
						sBoundEvaluator, mHoverCellCurrentBounds);
				hoverViewAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator valueAnimator) {
						invalidate();
					}
				});
				hoverViewAnimator.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation) {
						setEnabled(false);
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						finishTouch();
					}
				});
				hoverViewAnimator.start();
			}
		} else {
			touchEventsCancelled();
		}
	}

	/**
	 * Resets all the appropriate fields to a default state.
	 */
	private void touchEventsCancelled() {
		if (mCellIsMobile) {
			finishTouch();
		}

		mCellIsMobile = false;
		mIsMobileScrolling = false;
		mActivePointerId = INVALID_POINTER_ID;
	}

	private void finishTouch() {
		mAboveItemId = INVALID_ID;
		mMobileItemId = INVALID_ID;
		mBelowItemId = INVALID_ID;
		setAllVisible();
		mHoverCell = null;
		tag = null;
		setEnabled(true);
		invalidate();
		processSwapped();
	}

	private void processSwapped() {
		if (itemsSwapped) {
			itemsSwapped = false;
			if (dCallbacks != null) {
				dCallbacks.onItemsSwapped(mActiveItemsList);
			}
		}
	}

	/**
	 * Determines whether this listview is in a scrolling state invoked
	 * by the fact that the hover cell is out of the bounds of the listview;
	 */
	private void handleMobileCellScroll() {
		mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
	}

	/**
	 * This method is in charge of determining if the hover cell is above
	 * or below the bounds of the listview. If so, the listview does an appropriate
	 * upward or downward smooth scroll so as to reveal new items.
	 */
	public boolean handleMobileCellScroll(Rect r) {
		int offset = computeVerticalScrollOffset();
		int height = getHeight();
		int extent = computeVerticalScrollExtent();
		int range = computeVerticalScrollRange();
		int hoverViewTop = r.top;
		int hoverHeight = r.height();

		if (hoverViewTop <= 0 && offset > 0) {
			smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
			smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
			return true;
		}

		return false;
	}

	public void setItemsList(List<Object> itemsList) {
		mItemsList = itemsList;
	}

	/*
	 * Define items which can be moved
	 * mActiveItemsList == null means all items are movable
	 */
	public void setActiveItemsList(List<Object> mActiveItemsList) {
		this.mActiveItemsList = mActiveItemsList;
	}

	/**
	 * This scroll listener is added to the listview in order to handle cell swapping
	 * when the cell is either at the top or bottom edge of the listview. If the hover
	 * cell is at either edge of the listview, the listview will begin scrolling. As
	 * scrolling takes place, the listview continuously checks if new cells became visible
	 * and determines whether they are potential candidates for a cell swap.
	 */
	private final AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener() {

		private int mPreviousFirstVisibleItem = -1;
		private int mPreviousVisibleItemCount = -1;
		private int mCurrentFirstVisibleItem;
		private int mCurrentVisibleItemCount;
		private int mCurrentScrollState;

		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
							 int totalItemCount) {
			mCurrentFirstVisibleItem = firstVisibleItem;
			mCurrentVisibleItemCount = visibleItemCount;

			mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
					: mPreviousFirstVisibleItem;
			mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
					: mPreviousVisibleItemCount;

			checkAndHandleFirstVisibleCellChange();
			checkAndHandleLastVisibleCellChange();

			mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
			mPreviousVisibleItemCount = mCurrentVisibleItemCount;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			mCurrentScrollState = scrollState;
			mScrollState = scrollState;
			isScrollCompleted();
		}

		/**
		 * This method is in charge of invoking 1 of 2 actions. Firstly, if the listview
		 * is in a state of scrolling invoked by the hover cell being outside the bounds
		 * of the listview, then this scrolling event is continued. Secondly, if the hover
		 * cell has already been released, this invokes the animation for the hover cell
		 * to return to its correct position after the listview has entered an idle scroll
		 * state.
		 */
		private void isScrollCompleted() {
			if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == SCROLL_STATE_IDLE) {
				if (mCellIsMobile && mIsMobileScrolling) {
					handleMobileCellScroll();
				} else if (mIsWaitingForScrollFinish) {
					touchEventsEnded();
				}
			}
		}

		/**
		 * Determines if the listview scrolled up enough to reveal a new cell at the
		 * top of the list. If so, then the appropriate parameters are updated.
		 */
		public void checkAndHandleFirstVisibleCellChange() {
			if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}

		/**
		 * Determines if the listview scrolled down enough to reveal a new cell at the
		 * bottom of the list. If so, then the appropriate parameters are updated.
		 */
		public void checkAndHandleLastVisibleCellChange() {
			int currentLastVisibleItem = mCurrentFirstVisibleItem + mCurrentVisibleItemCount;
			int previousLastVisibleItem = mPreviousFirstVisibleItem + mPreviousVisibleItemCount;
			if (currentLastVisibleItem != previousLastVisibleItem) {
				if (mCellIsMobile && mMobileItemId != INVALID_ID) {
					updateNeighborViewsForID(mMobileItemId);
					handleCellSwitch();
				}
			}
		}
	};

	/**
	 * Returns the most inner view that contains the xy coordinate.
	 *
	 * @param v This method gets called recursively. The initial call should be the root view.
	 * @param x The X location to be tested.
	 * @param y The Y location to be tested.
	 * @return Returns the most inner view that contains the XY coordinate or null if no view could be found.
	 */
	private View findViewAtPositionWithDragIconTag(View v, int x, int y) {
		View vXY = null;

		if (v instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) v;

			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View c = viewGroup.getChildAt(i);

				int[] loc = new int[2];
				c.getLocationOnScreen(loc);

				if ((x >= loc[0] && (x <= (loc[0] + c.getWidth()))) && (y >= loc[1] && (y <= (loc[1] + c.getHeight())))) {
					vXY = c;
					View viewAtPosition = findViewAtPositionWithDragIconTag(c, x, y);

					if ((viewAtPosition != null) && (viewAtPosition.getTag() != null) && viewAtPosition.getTag() instanceof DragIcon) {
						vXY = viewAtPosition;
						break;
					}
				}
			}
		}

		return vXY;
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		if (dCallbacks != null) {
			dCallbacks.onWindowVisibilityChanged(visibility);
		}
	}
}