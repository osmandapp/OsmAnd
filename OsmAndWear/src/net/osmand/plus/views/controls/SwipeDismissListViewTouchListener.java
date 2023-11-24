package net.osmand.plus.views.controls;

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link View.OnTouchListener} that makes the list items in a {@link ListView}
 * dismissable. {@link ListView} is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 * <p/>
 * <p>After creating the listener, the caller should also call
 * {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link SwipeDismissListViewTouchListener} is paused during list view
 * scrolling.</p>
 * <p/>
 * <p>Example usage:</p>
 * <p/>
 * <pre>
 * SwipeDismissListViewTouchListener touchListener =
 *         new SwipeDismissListViewTouchListener(
 *                 listView,
 *                 new SwipeDismissListViewTouchListener.OnDismissCallback() {
 *                     public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 *                         for (int position : reverseSortedPositions) {
 *                             adapter.remove(adapter.getItem(position));
 *                         }
 *                         adapter.notifyDataSetChanged();
 *                     }
 *                 });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 * <p/>
 * <p>This class Requires API level 12 or later due to use of {@link
 * ViewPropertyAnimator}.</p>
 * <p/>
 */
public class SwipeDismissListViewTouchListener implements View.OnTouchListener {
	// Cached ViewConfiguration and system-wide constant values
	private final int mSlop;
	private final int mMinFlingVelocity;
	private final int mMaxFlingVelocity;
	private final long mAnimationTime;

	// Fixed properties
	private final ListView mListView;
	private final DismissCallbacks mCallbacks;
	private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

	// Transient properties
	//private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
	private int mDismissAnimationRefCount;
	private float mDownX;
	private float mDownY;
	private boolean mSwiping;
	private VelocityTracker mVelocityTracker;
	private int mDownPosition;
	private View mSwipeDownView;
	private boolean mSwipePaused;
	private boolean mSwipeCanceled;

	private final PopupWindow mUndoPopup;
	private int mValidDelayedMsgId;
	private final Handler mHideUndoHandler = new HideUndoPopupHandler();
	private final Button mUndoButton;

	private UndoStyle mUndoStyle = UndoStyle.SINGLE_POPUP;
	private boolean mTouchBeforeAutoHide;
	private SwipeDirection mSwipeDirection = SwipeDirection.BOTH;
	private int mUndoHideDelay = 5000;
	private int mSwipingLayout;

	private final Object[] mAnimationLock = new Object[0];
	private final List<Undoable> mUndoActions = new ArrayList<>();
	private final SortedSet<PendingDismissData> mPendingDismisses = new TreeSet<>();
	private final List<View> mAnimatedViews = new LinkedList<>();

	private View mSwipeDownChild;
	private final TextView mUndoPopupTextView;
	private final float mScreenDensity;

	/**
	 * Defines the direction in which list items can be swiped out to delete them.
	 * Use {@code #setSwipeDirection(SwipeDirection)}
	 * to change the default behavior.
	 */
	public enum SwipeDirection {

		/**
		 * The user can swipe each item into both directions (left and right) to delete it.
		 */
		BOTH,

		/**
		 * The user can only swipe the items to the beginning of the item to
		 * delete it. The start of an item is in Left-To-Right languages the left
		 * side and in Right-To-Left languages the right side. Before API level
		 * 17 this is always the left side.
		 */
		START,

		/**
		 * The user can only swipe the items to the end of the item to delete it.
		 * This is in Left-To-Right languages the right side in Right-To-Left
		 * languages the left side. Before API level 17 this will always be the
		 * right side.
		 */
		END

	}

	/**
	 * Extend this abstract class and return it from
	 * {@code OnDismissCallback#onDismiss(EnhancedListView, int)}
	 * to let the user undo the deletion you've done with your {@code OnDismissCallback}.
	 * You have at least to implement the {@link #undo()} method, and can override {@link #discard()}
	 * and {@link #getTitle()} to offer more functionality. See the README file for example implementations.
	 */
	public abstract static class Undoable {

		/**
		 * This method must undo the deletion you've done in
		 * OnDismissCallback.onDismiss(ListView, int) and reinsert
		 * the element into the adapter.
		 * <p/>
		 * In the most implementations, you will only remove the list item from your adapter
		 * in the {@code onDismiss} method and delete it from the database (or your permanent
		 * storage) in {@link #discard()}. In that case you only need to reinsert the item
		 * to the adapter.
		 */
		public abstract void undo();

		/**
		 * Returns the individual undo message for this undo. This will be displayed in the undo
		 * window, beside the undo button. The default implementation returns {@code null},
		 * what will lead in a default message to be displayed in the undo window.
		 * Don't call the super method, when overriding this method.
		 *
		 * @return The title for a special string.
		 */
		public String getTitle() {
			return null;
		}

		/**
		 * Discard the undo, meaning the user has no longer the possibility to undo the deletion.
		 * Implement this, to finally delete your stuff from permanent storages like databases
		 * (whereas in onKeyDown(int, android.view.KeyEvent) you should only remove it from the
		 * list adapter).
		 */
		public void discard() {
		}

	}

	/**
	 * Defines the style in which <i>undos</i> should be displayed and handled in the list.
	 */
	public enum UndoStyle {

		/**
		 * Shows a popup window, that allows the user to undo the last
		 * dismiss. If another element is deleted, the undo popup will undo that deletion.
		 * The user is only able to undo the last deletion.
		 */
		SINGLE_POPUP,

		/**
		 * Shows a popup window, that allows the user to undo the last dismiss.
		 * If another item is deleted, this will be added to the chain of undos. So pressing
		 * undo will undo the last deletion, pressing it again will undo the deletion before that,
		 * and so on. As soon as the popup vanished (e.g. because {@link #setUndoHideDelay(int) autoHideDelay}
		 * is over) all saved undos will be discarded.
		 */
		MULTILEVEL_POPUP,

		/**
		 * Shows a popup window, that allows the user to undo the last dismisses.
		 * If another item is deleted, while there is still an undo popup visible, the label
		 * of the button changes to <i>Undo all</i> and a press on the button, will discard
		 * all stored undos. As soon as the popup vanished (e.g. because {@link #setUndoHideDelay(int) autoHideDelay}
		 * is over) all saved undos will be discarded.
		 */
		COLLAPSED_POPUP

	}

	private class PendingDismissData implements Comparable<PendingDismissData> {

		public int position;
		/**
		 * The view that should get swiped out.
		 */
		public View view;
		/**
		 * The whole list item view.
		 */
		public View childView;

		PendingDismissData(int position, View view, View childView) {
			this.position = position;
			this.view = view;
			this.childView = childView;
		}

		@Override
		public int compareTo(@NonNull PendingDismissData other) {
			// Sort by descending position
			return other.position - position;
		}

	}

	private class UndoClickListener implements View.OnClickListener {

		/**
		 * Called when a view has been clicked.
		 *
		 * @param v The view that was clicked.
		 */
		@Override
		public void onClick(View v) {
			if (!mUndoActions.isEmpty()) {
				switch (mUndoStyle) {
					case SINGLE_POPUP:
						mUndoActions.get(0).undo();
						mUndoActions.clear();
						break;
					case COLLAPSED_POPUP:
						Collections.reverse(mUndoActions);
						for (Undoable undo : mUndoActions) {
							undo.undo();
						}
						mUndoActions.clear();
						break;
					case MULTILEVEL_POPUP:
						mUndoActions.get(mUndoActions.size() - 1).undo();
						mUndoActions.remove(mUndoActions.size() - 1);
						break;
				}
			}

			// Dismiss dialog or change text
			if (mUndoActions.isEmpty()) {
				if (mUndoPopup.isShowing()) {
					mUndoPopup.dismiss();
				}
			} else {
				changePopupText();
				changeButtonLabel();
			}

			mValidDelayedMsgId++;
		}
	}

	private class HideUndoPopupHandler extends Handler {

		/**
		 * Subclasses must implement this to receive messages.
		 */
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == mValidDelayedMsgId) {
				discardUndo();
			}
		}
	}

	/**
	 * The callback interface used by {@link SwipeDismissListViewTouchListener} to inform its client
	 * about a successful dismissal of one or more list item positions.
	 */
	public interface DismissCallbacks {
		/**
		 * Called to determine whether the given position can be dismissed.
		 */
		boolean canDismiss(int position);

		/**
		 * Called when the user has indicated they she would like to dismiss one or more list item
		 * positions.
		 *
		 * @param position               Position to dismiss
		 */
		Undoable onDismiss(int position);

		/**
		 * Called when popup is going to be hidden after time expired (not when UNDO clicked)
		 */
		void onHidePopup();
	}

	/**
	 * Constructs a new swipe-to-dismiss touch listener for the given list view.
	 *
	 * @param listView  The list view whose items should be dismissable.
	 * @param callbacks The callback to trigger when the user has indicated that she would like to
	 *                  dismiss one or more list items.
	 */
	public SwipeDismissListViewTouchListener(Context ctx, ListView listView, DismissCallbacks callbacks) {
		ViewConfiguration vc = ViewConfiguration.get(ctx);
		mSlop = vc.getScaledTouchSlop();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mAnimationTime = ctx.getResources().getInteger(
				android.R.integer.config_shortAnimTime);
		mListView = listView;
		mCallbacks = callbacks;

		// Initialize undo popup
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View undoView = inflater.inflate(R.layout.undo_popup, null);
		mUndoButton = undoView.findViewById(R.id.undo);
		mUndoButton.setOnClickListener(new UndoClickListener());
		mUndoButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// If the user touches the screen invalidate the current running delay by incrementing
				// the valid message id. So this delay won't hide the undo popup anymore
				mValidDelayedMsgId++;
				return false;
			}
		});
		mUndoPopupTextView = undoView.findViewById(R.id.text);

		mUndoPopup = new PopupWindow(undoView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
		mUndoPopup.setAnimationStyle(R.style.Animations_PopUpMenu_Fade);

		mScreenDensity = listView.getResources().getDisplayMetrics().density;
		// END initialize undo popup

		mListView.setOnTouchListener(this);
		// Setting this scroll listener is required to ensure that during ListView scrolling,
		// we don't look for swipes.
		mListView.setOnScrollListener(makeScrollListener());
	}

	/**
	 * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
	 *
	 * @param enabled Whether or not to watch for gestures.
	 */
	public void setEnabled(boolean enabled) {
		mSwipePaused = !enabled;
	}

	/**
	 * Sets the undo style of this list.
	 *
	 * @param undoStyle The style of this listview.
	 */
	public void setUndoStyle(UndoStyle undoStyle) {
		mUndoStyle = undoStyle;
	}

	/**
	 * Sets the time in milliseconds after which the undo popup automatically disappears.
	 * The countdown will start when the user touches the screen. If you want to start the countdown
	 * immediately when the popups appears, call {@link #setRequireTouchBeforeDismiss(boolean)} with
	 * {@code false}.
	 *
	 * @param hideDelay The delay in milliseconds.
	 */
	public void setUndoHideDelay(int hideDelay) {
		mUndoHideDelay = hideDelay;
	}

	/**
	 * Sets whether another touch on the view is required before the popup counts down to dismiss
	 * the undo popup. By default this is set to {@code true}.
	 *
	 * @param touchBeforeDismiss Whether the screen needs to be touched before the countdown starts.
	 * @see #setUndoHideDelay(int)
	 */
	public void setRequireTouchBeforeDismiss(boolean touchBeforeDismiss) {
		mTouchBeforeAutoHide = touchBeforeDismiss;
	}

	/**
	 * Sets the directions in which a list item can be swiped to delete.
	 * By default this is set to {@link SwipeDirection#BOTH} so that an item
	 * can be swiped into both directions.
	 */
	public void setSwipeDirection(SwipeDirection direction) {
		mSwipeDirection = direction;
	}

	/**
	 * Sets the id of the view, that should be moved, when the user swipes an item.
	 * Only the view with the specified id will move, while all other views in the list item, will
	 * stay where they are. This might be useful to have a background behind the view that is swiped
	 * out, to stay where it is (and maybe explain that the item is going to be deleted).
	 * If you never call this method (or call it with 0), the whole view will be swiped. Also if there
	 * is no view in a list item, with the given id, the whole view will be swiped.
	 */
	public void setSwipingLayout(int swipingLayoutId) {
		mSwipingLayout = swipingLayoutId;
	}

	/**
	 * Discard all stored undos and hide the undo popup dialog.
	 * This method must be called in {@link android.app.Activity#onStop()}. Otherwise
	 * might not be called for several items, what might
	 * break your data consistency.
	 */
	public void discardUndo() {
		for (Undoable undoable : mUndoActions) {
			undoable.discard();
		}
		mUndoActions.clear();
		if (mUndoPopup.isShowing()) {
			if (mCallbacks != null) {
				mCallbacks.onHidePopup();
			}
			mUndoPopup.dismiss();
		}
	}

	/**
	 * Delete the list item at the specified position. This will animate the item sliding out of the
	 * list and then collapsing until it vanished (same as if the user slides out an item).
	 * <p/>
	 */
	public void delete(int position) {
		if (mCallbacks == null) {
			throw new IllegalStateException("You must set an OnDismissCallback, before deleting items.");
		}

		int pos = position + mListView.getHeaderViewsCount();

		if (pos < 0 || pos >= mListView.getCount()) {
			throw new IndexOutOfBoundsException(String.format("Tried to delete item %d. #items in list: %d", pos, mListView.getCount()));
		}
		View childView = mListView.getChildAt(pos - mListView.getFirstVisiblePosition());
		View view = null;
		if (mSwipingLayout > 0) {
			view = childView.findViewById(mSwipingLayout);
		}
		if (view == null) {
			view = childView;
		}
		slideOutView(view, childView, position, true);
	}

	/**
	 * Slide out a view to the right or left of the list. After the animation has finished, the
	 * view will be dismissed by calling {@link #performDismiss(android.view.View, android.view.View, int)}.
	 *
	 * @param view        The view, that should be slided out.
	 * @param childView   The whole view of the list item.
	 * @param position    The item position of the item.
	 * @param toRightSide Whether it should slide out to the right side.
	 */
	private void slideOutView(View view, View childView, int position, boolean toRightSide) {

		// Only start new animation, if this view isn't already animated (too fast swiping bug)
		synchronized (mAnimationLock) {
			if (mAnimatedViews.contains(view)) {
				return;
			}
			++mDismissAnimationRefCount;
			mAnimatedViews.add(view);
		}

		ViewCompat.animate(view)
				.translationX(toRightSide ? mViewWidth : -mViewWidth)
				.alpha(0)
				.setDuration(mAnimationTime)
				.setListener(new ViewPropertyAnimatorListener() {
					@Override
					public void onAnimationStart(View view) {
					}

					@Override
					public void onAnimationEnd(View view) {
						performDismiss(view, childView, position);
					}

					@Override
					public void onAnimationCancel(View view) {

					}
				});
	}

	/**
	 * Returns an {@link AbsListView.OnScrollListener} to be added to the {@link
	 * ListView} using {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}.
	 * If a scroll listener is already assigned, the caller should still pass scroll changes through
	 * to this listener. This will ensure that this {@link SwipeDismissListViewTouchListener} is
	 * paused during list view scrolling.</p>
	 *
	 * @see SwipeDismissListViewTouchListener
	 */
	public AbsListView.OnScrollListener makeScrollListener() {
		return new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView absListView, int scrollState) {
				setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
			}

			@Override
			public void onScroll(AbsListView absListView, int i, int i1, int i2) {
			}
		};
	}

	@Override
	public boolean onTouch(View view, MotionEvent ev) {
		if (mViewWidth < 2) {
			mViewWidth = mListView.getWidth();
		}

		switch (ev.getActionMasked()) {
			case MotionEvent.ACTION_DOWN: {
				if (mSwipePaused) {
					return false;
				}

				// Find the child view that was touched (perform a hit test)
				Rect rect = new Rect();
				int childCount = mListView.getChildCount();
				int[] listViewCoords = new int[2];
				mListView.getLocationOnScreen(listViewCoords);
				int x = (int) ev.getRawX() - listViewCoords[0];
				int y = (int) ev.getRawY() - listViewCoords[1];
				View child;
				for (int i = mListView.getHeaderViewsCount(); i < childCount; i++) {
					child = mListView.getChildAt(i);
					if (child != null) {
						child.getHitRect(rect);
						if (rect.contains(x, y)) {
							// if a specific swiping layout has been giving, use this to swipe.
							if (mSwipingLayout > 0) {
								View swipingView = child.findViewById(mSwipingLayout);
								if (swipingView != null) {
									mSwipeDownView = swipingView;
									mSwipeDownChild = child;
									break;
								}
							}
							// If no swiping layout has been found, swipe the whole child
							mSwipeDownView = mSwipeDownChild = child;
							break;
						}
					}
				}

				if (mSwipeDownView != null) {
					// test if the item should be swiped
					int position = mListView.getPositionForView(mSwipeDownView) - mListView.getHeaderViewsCount();
					if (mCallbacks == null || mCallbacks.canDismiss(position)) {
						mDownX = ev.getRawX();
						mDownY = ev.getRawY();
						mDownPosition = position;

						mVelocityTracker = VelocityTracker.obtain();
						mVelocityTracker.addMovement(ev);
					} else {
						// set back to null to revert swiping
						mSwipeDownView = mSwipeDownChild = null;
					}
				}
				return false;
			}

			case MotionEvent.ACTION_CANCEL: {
				if (mVelocityTracker == null) {
					break;
				}

				if (mSwipeDownView != null && mSwiping) {
					// cancel
					ViewCompat.animate(mSwipeDownView)
							.translationX(0)
							.alpha(1)
							.setDuration(mAnimationTime)
							.setListener(null);
				}
				mVelocityTracker.recycle();
				mVelocityTracker = null;
				mDownX = 0;
				mDownY = 0;
				mSwipeDownView = mSwipeDownChild = null;
				mDownPosition = ListView.INVALID_POSITION;
				mSwiping = false;
				mSwipeCanceled = false;
				break;
			}

			case MotionEvent.ACTION_UP: {
				if (mVelocityTracker == null) {
					break;
				}

				float deltaX = ev.getRawX() - mDownX;
				mVelocityTracker.addMovement(ev);
				mVelocityTracker.computeCurrentVelocity(1000);
				float velocityX = Math.abs(mVelocityTracker.getXVelocity());
				float velocityY = Math.abs(mVelocityTracker.getYVelocity());
				boolean dismiss = false;
				boolean dismissRight = false;
				if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
					dismiss = true;
					dismissRight = deltaX > 0;
				} else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity
						&& velocityY < velocityX && mSwiping && isSwipeDirectionValid(mVelocityTracker.getXVelocity())
						&& deltaX >= mViewWidth * 0.2f) {
					dismiss = true;
					dismissRight = mVelocityTracker.getXVelocity() > 0;
				}
				if (dismiss) {
					// dismiss
					slideOutView(mSwipeDownView, mSwipeDownChild, mDownPosition, dismissRight);
				} else if (mSwiping) {
					// Swipe back to regular position
					ViewCompat.animate(mSwipeDownView)
							.translationX(0)
							.alpha(1)
							.setDuration(mAnimationTime)
							.setListener(null);
				}
				mVelocityTracker = null;
				mDownX = 0;
				mDownY = 0;
				mSwipeDownView = null;
				mSwipeDownChild = null;
				mDownPosition = AbsListView.INVALID_POSITION;
				mSwiping = false;
				mSwipeCanceled = false;
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				if (mVelocityTracker == null || mSwipePaused || mSwipeCanceled) {
					break;
				}

				mVelocityTracker.addMovement(ev);
				float deltaX = ev.getRawX() - mDownX;
				float deltaY = ev.getRawY() - mDownY;

				if (!mSwiping && Math.abs(deltaY) > mSlop * 2) {
					mSwipeCanceled = true;
					break;
				}

				// Only start swipe in correct direction
				if (isSwipeDirectionValid(deltaX)) {
					ViewParent parent = mListView.getParent();
					if (parent != null) {
						// If we swipe don't allow parent to intercept touch (e.g. like NavigationDrawer does)
						// otherwise swipe would not be working.
						parent.requestDisallowInterceptTouchEvent(true);
					}
					if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
						mSwiping = true;
						mListView.requestDisallowInterceptTouchEvent(true);

						// Cancel ListView's touch (un-highlighting the item)
						MotionEvent cancelEvent = MotionEvent.obtain(ev);
						cancelEvent.setAction(MotionEvent.ACTION_CANCEL
								| (ev.getActionIndex()
								<< MotionEvent.ACTION_POINTER_INDEX_SHIFT));
						mListView.onTouchEvent(cancelEvent);
					}
				} else {
					// If we swiped into wrong direction, act like this was the new
					// touch down point
					mDownX = ev.getRawX();
					deltaX = 0;
				}

				if (mSwiping) {
					mSwipeDownView.setTranslationX(deltaX);
					mSwipeDownView.setAlpha(Math.max(0f, Math.min(1f,
							1f - 2f * Math.abs(deltaX) / mViewWidth)));
					return true;
				}
				break;
			}
		}
		return false;
	}

	/**
	 * Animate the dismissed list item to zero-height and fire the dismiss callback when
	 * all dismissed list item animations have completed.
	 *
	 * @param dismissView     The view that has been slided out.
	 * @param listItemView    The list item view. This is the whole view of the list item, and not just
	 *                        the part, that the user swiped.
	 * @param dismissPosition The position of the view inside the list.
	 */
	private void performDismiss(View dismissView, View listItemView, int dismissPosition) {

		ViewGroup.LayoutParams lp = listItemView.getLayoutParams();
		int originalLayoutHeight = lp.height;

		if (android.os.Build.VERSION.SDK_INT < 12) {
			mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
			finishDismiss(dismissView, originalLayoutHeight);
		} else {
			int originalHeight = listItemView.getHeight();
			ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					finishDismiss(dismissView, originalLayoutHeight);
				}
			});

			animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					lp.height = (Integer) valueAnimator.getAnimatedValue();
					listItemView.setLayoutParams(lp);
				}
			});

			mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView, listItemView));
			animator.start();
		}
	}

	private void finishDismiss(View dismissView, int originalLayoutHeight) {
		// Make sure no other animation is running. Remove animation from running list, that just finished
		boolean noAnimationLeft;
		synchronized (mAnimationLock) {
			--mDismissAnimationRefCount;
			mAnimatedViews.remove(dismissView);
			noAnimationLeft = mDismissAnimationRefCount == 0;
		}

		if (noAnimationLeft) {
			// No active animations, process all pending dismisses.

			for (PendingDismissData dismiss : mPendingDismisses) {
				if (mUndoStyle == UndoStyle.SINGLE_POPUP) {
					for (Undoable undoable : mUndoActions) {
						undoable.discard();
					}
					mUndoActions.clear();
				}
				Undoable undoable = mCallbacks.onDismiss(dismiss.position);
				if (undoable != null) {
					mUndoActions.add(undoable);
				}
				mValidDelayedMsgId++;
			}

			if (!mUndoActions.isEmpty()) {
				changePopupText();
				changeButtonLabel();

				// Show undo popup
				float yLocationOffset = mListView.getResources().getDimension(R.dimen.undo_bottom_offset);
				mUndoPopup.setWidth((int) Math.min(mScreenDensity * 400, mListView.getWidth() * 0.9f));
				mUndoPopup.showAtLocation(mListView,
						Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM,
						0, (int) yLocationOffset);

				// Queue the dismiss only if required
				if (!mTouchBeforeAutoHide) {
					// Send a delayed message to hide popup
					mHideUndoHandler.sendMessageDelayed(mHideUndoHandler.obtainMessage(mValidDelayedMsgId),
							mUndoHideDelay);
				}
			}

			ViewGroup.LayoutParams lp;
			for (PendingDismissData pendingDismiss : mPendingDismisses) {
				pendingDismiss.view.setAlpha(1f);
				pendingDismiss.view.setTranslationX(0);
				lp = pendingDismiss.childView.getLayoutParams();
				lp.height = originalLayoutHeight;
				pendingDismiss.childView.setLayoutParams(lp);
			}

			mPendingDismisses.clear();
		}
	}

	/**
	 * Changes the text of the undo popup. If more then one item can be undone, the number of deleted
	 * items will be shown. If only one deletion can be undone, the title of this deletion (or a default
	 * string in case the title is {@code null}) will be shown.
	 */
	private void changePopupText() {
		String msg = null;
		if (mUndoActions.size() > 1) {
			msg = mUndoActions.size() + " " + mListView.getResources().getString(R.string.n_items_removed);
		} else if (mUndoActions.size() >= 1) {
			// Set title from single undoable or when no multiple deletion string
			// is given
			msg = mUndoActions.get(mUndoActions.size() - 1).getTitle();

			if (msg == null) {
				msg = mListView.getResources().getString(R.string.item_removed);
			}
		}
		mUndoPopupTextView.setText(msg);
	}

	/**
	 * Changes the label of the undo button.
	 */
	private void changeButtonLabel() {
		String msg;
		if (mUndoActions.size() > 1 && mUndoStyle == UndoStyle.COLLAPSED_POPUP) {
			msg = mListView.getResources().getString(R.string.shared_string_undo_all);
		} else {
			msg = mListView.getResources().getString(R.string.shared_string_undo);
		}
		mUndoButton.setText(msg);
	}

	private boolean isSwipeDirectionValid(float deltaX) {

		int rtlSign = 1;
		// On API level 17 and above, check if we are in a Right-To-Left layout
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			if (mListView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
				rtlSign = -1;
			}
		}

		// Check if swipe has been done in the correct direction
		switch (mSwipeDirection) {
			default:
			case BOTH:
				return true;
			case START:
				return rtlSign * deltaX < 0;
			case END:
				return rtlSign * deltaX > 0;
		}
	}
}

