package net.osmand.plus.base;

import static net.osmand.plus.mapcontextmenu.MapContextMenuFragment.CURRENT_Y_UNDEFINED;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.LockableScrollView;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.InterceptorLinearLayout;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.HorizontalSwipeConfirm;
import net.osmand.plus.views.controls.SingleTapConfirm;
import net.osmand.plus.views.layers.MapControlsLayer.MapControlsThemeProvider;

public abstract class ContextMenuFragment extends BaseOsmAndFragment implements MapControlsThemeProvider {

	public static class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	public static final int ANIMATION_DURATION = 200;
	public static final float MIDDLE_STATE_KOEF = .7f;
	public static final int MIDDLE_STATE_MIN_HEIGHT_DP = 520;
	public static final String MENU_STATE_KEY = "menu_state_key";

	private LinearLayout mainView;
	private View view;
	private OnLayoutChangeListener containerLayoutListener;
	private View topShadow;
	private ViewGroup topView;
	private ViewGroup bottomScrollView;
	private LinearLayout cardsContainer;
	private FrameLayout bottomContainer;

	private boolean portrait;
	private boolean moving;
	private boolean forceUpdateLayout;
	private boolean initLayout = true;
	private boolean wasDrawerDisabled;
	private boolean paused;
	private boolean dismissing;

	private int minHalfY;
	private int topScreenPosY;
	private int topToolbarPosY;
	private int bottomToolbarPosY;
	private int menuFullHeightMax;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int screenHeight;
	private int viewHeight;
	private int currentMenuState;
	private int shadowHeight;
	private int statusBarHeight;

	private String preferredMapLang;
	private boolean transliterateNames;

	private ContextMenuFragmentListener listener;

	public interface ContextMenuFragmentListener {
		void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated);

		void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState, int previousMenuState);

		void onContextMenuDismiss(@NonNull ContextMenuFragment fragment);
	}

	@LayoutRes
	public abstract int getMainLayoutId();

	@IdRes
	public int getMainViewId() {
		return R.id.main_view;
	}

	@IdRes
	public int getTopShadowViewId() {
		return R.id.context_menu_top_shadow;
	}

	@IdRes
	public int getBottomContainerViewId() {
		return R.id.bottom_container;
	}

	@IdRes
	public int getCardsContainerViewId() {
		return R.id.route_menu_cards_container;
	}

	@IdRes
	public int getBottomScrollViewId() {
		return R.id.route_menu_bottom_scroll;
	}

	@IdRes
	public int getTopViewId() {
		return 0;
	}

	public abstract int getHeaderViewHeight();

	public abstract boolean isHeaderViewDetached();

	public int getLandscapeWidth() {
		return getResources().getDimensionPixelSize(R.dimen.dashboard_land_width);
	}

	public int getLandscapeNoShadowWidth() {
		return getLandscapeWidth() - getResources().getDimensionPixelSize(R.dimen.dashboard_land_shadow_width);
	}

	public float getMiddleStateKoef() {
		return MIDDLE_STATE_KOEF;
	}

	public abstract int getToolbarHeight();

	public boolean isSingleFragment() {
		return true;
	}

	public String getFragmentTag() {
		return this.getClass().getName();
	}

	@Nullable
	public LinearLayout getMainView() {
		return mainView;
	}

	@Nullable
	public ViewGroup getTopView() {
		return topView;
	}

	@Override
	public boolean isNightModeForMapControls() {
		return nightMode;
	}

	protected String getThemeInfoProviderTag() {
		return null;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public String getPreferredMapLang() {
		return preferredMapLang;
	}

	public boolean isTransliterateNames() {
		return transliterateNames;
	}

	public boolean isPaused() {
		return paused;
	}

	public int getMenuFullHeightMax() {
		return menuFullHeightMax;
	}

	public int getMenuFullHeight() {
		return menuFullHeight;
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public ContextMenuFragmentListener getListener() {
		return listener;
	}

	public void setListener(ContextMenuFragmentListener listener) {
		this.listener = listener;
	}

	public boolean isPortrait() {
		return portrait;
	}

	public View getTopShadow() {
		return topShadow;
	}

	public LinearLayout getCardsContainer() {
		return cardsContainer;
	}

	@Nullable
	public FrameLayout getBottomContainer() {
		return bottomContainer;
	}

	public ViewGroup getBottomScrollView() {
		return bottomScrollView;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = requireMapActivity();

		preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();

		ContextThemeWrapper context =
				new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		view = LayoutInflater.from(context).inflate(getMainLayoutId(), container, false);
		initLayout = true;
		currentMenuState = getInitialMenuState();
		Bundle args = getArguments();
		if (args != null) {
			currentMenuState = args.getInt(MENU_STATE_KEY);
		}

		if (isSingleFragment()) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		}

		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		statusBarHeight = AndroidUtils.getStatusBarHeight(mapActivity);
		shadowHeight = getResources().getDimensionPixelSize(R.dimen.contex_menu_top_shadow_height);
		topScreenPosY = addStatusBarHeightIfNeeded(-shadowHeight) + getToolbarHeight();
		topToolbarPosY = getMenuStatePosY(MenuState.FULL_SCREEN);
		bottomToolbarPosY = topToolbarPosY + getToolbarHeight();

		mainView = view.findViewById(getMainViewId());
		topShadow = view.findViewById(getTopShadowViewId());
		cardsContainer = view.findViewById(getCardsContainerViewId());
		bottomContainer = view.findViewById(getBottomContainerViewId());
		bottomScrollView = view.findViewById(getBottomScrollViewId());

		if (bottomScrollView instanceof LockableScrollView) {
			((LockableScrollView) bottomScrollView).setScrollingEnabled(false);
		}

		ViewConfiguration vc = ViewConfiguration.get(context);
		int touchSlop = vc.getScaledTouchSlop();

		if (getTopViewId() != 0) {
			topView = view.findViewById(getTopViewId());
			AndroidUtils.setBackground(app, topView, ColorUtilities.getCardAndListBackgroundColorId(nightMode));
		}
		if (!portrait) {
			currentMenuState = MenuState.FULL_SCREEN;
			if (isSingleFragment()) {
				TypedValue typedValueAttr = new TypedValue();
				int bgAttrId = AndroidUtils.isLayoutRtl(app) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
				mapActivity.getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
				mainView.setBackgroundResource(typedValueAttr.resourceId);
				mainView.setLayoutParams(new FrameLayout.LayoutParams(getLandscapeWidth(), ViewGroup.LayoutParams.MATCH_PARENT));
			} else {
				mainView.setLayoutParams(new FrameLayout.LayoutParams(getLandscapeNoShadowWidth(), ViewGroup.LayoutParams.MATCH_PARENT));
			}
		}

		processScreenHeight(container);
		minHalfY = getMinHalfY(mapActivity);

		GestureDetector singleTapDetector = new GestureDetector(view.getContext(), new SingleTapConfirm());
		GestureDetector swipeDetector = new GestureDetector(view.getContext(), new HorizontalSwipeConfirm(true));

		OnTouchListener slideTouchListener = new OnTouchListener() {
			private float dy;
			private float dyMain;
			private float mDownY;

			private final int minimumVelocity;
			private final int maximumVelocity;
			private VelocityTracker velocityTracker;
			private final OverScroller scroller;

			private boolean slidingUp;
			private boolean slidingDown;

			private boolean hasMoved;

			{
				scroller = new OverScroller(app);
				ViewConfiguration configuration = ViewConfiguration.get(requireContext());
				minimumVelocity = configuration.getScaledMinimumFlingVelocity();
				maximumVelocity = configuration.getScaledMaximumFlingVelocity();
			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (!hasMoved && getHeaderViewHeight() > 0 && event.getY() <= getHeaderViewHeight()) {
					if (singleTapDetector.onTouchEvent(event)) {
						moving = false;
						onHeaderClick();

						recycleVelocityTracker();
						return true;
					}
				}
				if (!portrait) {
					if (swipeDetector.onTouchEvent(event)) {
						dismiss();

						recycleVelocityTracker();
						return true;
					}
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						hasMoved = false;
						mDownY = event.getRawY();
						dy = event.getY();
						dyMain = getViewY();

						initOrResetVelocityTracker();
						velocityTracker.addMovement(event);
						break;

					case MotionEvent.ACTION_MOVE:
						if (Math.abs(event.getRawY() - mDownY) > touchSlop) {
							moving = true;
						}
						if (moving) {
							hasMoved = true;
							float y = event.getY();
							float newY = getViewY() + (y - dy);
							if (!portrait && newY > topScreenPosY) {
								newY = topScreenPosY;
							}
							setViewY((int) newY, false, false);

							ViewGroup.LayoutParams lp = mainView.getLayoutParams();
							lp.height = view.getHeight() - (int) newY + 10;
							mainView.setLayoutParams(lp);
							mainView.requestLayout();

							float newEventY = newY - (dyMain - dy);
							MotionEvent ev = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(),
									event.getX(), newEventY, event.getMetaState());

							initVelocityTrackerIfNotExists();
							velocityTracker.addMovement(ev);
						}

						break;

					case MotionEvent.ACTION_UP:
						if (moving) {
							moving = false;
							hasMoved = false;
							int currentY = getViewY();
							int fullScreenTopPosY = getMenuStatePosY(MenuState.FULL_SCREEN);
							VelocityTracker velocityTracker = this.velocityTracker;
							velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
							int initialVelocity = (int) velocityTracker.getYVelocity();
							if ((Math.abs(initialVelocity) > minimumVelocity) && currentY != fullScreenTopPosY) {
								scroller.abortAnimation();
								scroller.fling(0, currentY, 0, initialVelocity, 0, 0,
										Math.min(getMinY(), fullScreenTopPosY),
										screenHeight,
										0, 0);
								currentY = scroller.getFinalY();
								scroller.abortAnimation();

								slidingUp = initialVelocity < -2000;
								slidingDown = initialVelocity > 2000;
							} else {
								slidingUp = false;
								slidingDown = false;
							}

							changeMenuState(currentY, slidingUp, slidingDown, true);
						}
						recycleVelocityTracker();
						break;
					case MotionEvent.ACTION_CANCEL:
						moving = false;
						hasMoved = false;
						recycleVelocityTracker();
						break;

				}
				return true;
			}

			private void initOrResetVelocityTracker() {
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
				} else {
					velocityTracker.clear();
				}
			}

			private void initVelocityTrackerIfNotExists() {
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
					velocityTracker.clear();
				}
			}

			private void recycleVelocityTracker() {
				if (velocityTracker != null) {
					velocityTracker.recycle();
					velocityTracker = null;
				}
			}
		};

		if (mainView instanceof InterceptorLinearLayout) {
			((InterceptorLinearLayout) mainView).setListener(slideTouchListener);
		}
		mainView.setOnTouchListener(slideTouchListener);

		return view;
	}

	public float getToolbarAlpha(int y) {
		float a = 0;
		if (portrait) {
			if (y < bottomToolbarPosY) {
				a = 1f - (y - topToolbarPosY) * (1f / (bottomToolbarPosY - topToolbarPosY));
			}
			if (a < 0) {
				a = 0;
			} else if (a > 1) {
				a = 1;
			}
		}
		return a;
	}

	public void updateToolbarVisibility(View view) {
		updateToolbarVisibility(view, getViewY());
	}

	public void updateToolbarVisibility(View view, int y) {
		float a = getToolbarAlpha(y);
		updateVisibility(view, a);
	}

	public void updateVisibility(View v, float alpha) {
		boolean visible = alpha > 0;
		v.setAlpha(alpha);
		if (visible && v.getVisibility() != View.VISIBLE) {
			v.setVisibility(View.VISIBLE);
		} else if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	public void updateVisibility(View v, boolean visible) {
		if (visible && v.getVisibility() != View.VISIBLE) {
			v.setVisibility(View.VISIBLE);
		} else if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	public int getMinY() {
		return viewHeight - menuFullHeightMax - (portrait ? getToolbarHeight() : 0);
	}

	private int addStatusBarHeightIfNeeded(int res) {
		MapActivity mapActivity = getMapActivity();
		if (Build.VERSION.SDK_INT >= 21 && mapActivity != null) {
			return res + (isSingleFragment() ? statusBarHeight : 0);
		}
		return res;
	}

	public int getInitialMenuState() {
		return MenuState.FULL_SCREEN;
	}

	public boolean slideUp() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v << 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public boolean slideDown() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v >> 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public int getCurrentMenuState() {
		return currentMenuState;
	}

	public int getSupportedMenuStates() {
		if (!portrait) {
			return MenuState.FULL_SCREEN;
		} else {
			return getSupportedMenuStatesPortrait();
		}
	}

	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateContainerLayoutListener(view, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		dismissing = false;
		updateContainerLayoutListener(view, true);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapLayers mapLayers = mapActivity.getMapLayers();
			if (mapLayers.hasMapActivity()) {
				mapLayers.getMapControlsLayer().showMapControlsIfHidden();
			}
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			String tag = getThemeInfoProviderTag();
			if (tag != null) {
				mapActivity.getMapLayers().getMapControlsLayer().addThemeInfoProviderTag(tag);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		updateContainerLayoutListener(view, false);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (!wasDrawerDisabled) {
				mapActivity.enableDrawer();
			}
			String tag = getThemeInfoProviderTag();
			if (tag != null) {
				mapActivity.getMapLayers().getMapControlsLayer().removeThemeInfoProviderTag(tag);
			}
		}
	}

	private void updateContainerLayoutListener(@Nullable View view, boolean add) {
		ViewParent parent = view != null ? view.getParent() : null;
		if (parent == null) {
			return;
		}
		View container = (View) parent;
		OnLayoutChangeListener listener = getContainerLayoutListener();
		container.removeOnLayoutChangeListener(listener);
		if (add) {
			container.addOnLayoutChangeListener(listener);
		}
	}

	@NonNull
	private OnLayoutChangeListener getContainerLayoutListener() {
		if (containerLayoutListener == null) {
			containerLayoutListener = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
				if (forceUpdateLayout || bottom != oldBottom) {
					forceUpdateLayout = false;
					processScreenHeight(view.getParent());
					runLayoutListener();
				}
			};
		}
		return containerLayoutListener;
	}

	public int getViewY() {
		return (int) mainView.getY();
	}

	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		mainView.setY(y);
		ContextMenuFragmentListener listener = this.listener;
		if (listener != null) {
			listener.onContextMenuYPosChanged(this, y, adjustMapPos, false);
		}
	}

	protected boolean isHideable() {
		return true;
	}

	private void processScreenHeight(ViewParent parent) {
		View container = (View) parent;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			screenHeight = container.getHeight() + statusBarHeight;
			viewHeight = screenHeight - statusBarHeight;
			minHalfY = getMinHalfY(mapActivity);
		}
	}

	private int getMinHalfY(MapActivity mapActivity) {
		return viewHeight - (int) Math.min(viewHeight * getMiddleStateKoef(),
				MIDDLE_STATE_MIN_HEIGHT_DP * mapActivity.getMapView().getDensity());
	}

	public boolean isMoving() {
		return moving;
	}

	public int getWidth() {
		LinearLayout mainView = getMainView();
		if (mainView != null) {
			return mainView.getWidth();
		} else {
			return 0;
		}
	}

	public int getHeight() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int height = getMenuStatePosY(getCurrentMenuState());
			return viewHeight - height - statusBarHeight;
		} else {
			return 0;
		}
	}

	public int getViewHeight() {
		return viewHeight;
	}

	public int getShadowHeight() {
		return shadowHeight;
	}

	public int getFullScreenTopPosY() {
		return topScreenPosY;
	}

	public int getHeaderOnlyTopY() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int shadowHeight = getShadowHeight();
			if (getHeaderViewHeight() > 0) {
				return viewHeight - getHeaderViewHeight() - shadowHeight;
			} else {
				return viewHeight - AndroidUtils.dpToPx(mapActivity, 48f) - shadowHeight;
			}
		} else {
			return 0;
		}
	}

	public int getMenuStatePosY(int menuState) {
		switch (menuState) {
			case MenuState.HEADER_ONLY:
				return getHeaderOnlyTopY();
			case MenuState.HALF_SCREEN:
				return minHalfY;
			case MenuState.FULL_SCREEN:
				return getFullScreenTopPosY();
			default:
				return 0;
		}
	}

	public void openMenuFullScreen() {
		changeMenuState(getMenuStatePosY(MenuState.FULL_SCREEN), false, false, true);
	}

	public void openMenuHeaderOnly() {
		if (portrait) {
			changeMenuState(getMenuStatePosY(MenuState.HEADER_ONLY), false, false, true);
		}
	}

	public void openMenuHalfScreen() {
		if (portrait) {
			changeMenuState(getMenuStatePosY(MenuState.HALF_SCREEN), false, false, true);
		}
	}

	public void openMenuScreen(int menuState, boolean animated) {
		if (portrait) {
			changeMenuState(getMenuStatePosY(menuState), false, false, animated);
		}
	}

	protected void changeMenuState(int currentY, boolean slidingUp, boolean slidingDown, boolean animated) {
		boolean needCloseMenu = false;

		int currentMenuState = getCurrentMenuState();
		if (portrait) {
			int headerDist = Math.abs(currentY - getMenuStatePosY(MenuState.HEADER_ONLY));
			int halfDist = Math.abs(currentY - getMenuStatePosY(MenuState.HALF_SCREEN));
			int fullDist = Math.abs(currentY - getMenuStatePosY(MenuState.FULL_SCREEN));
			int newState;
			if (headerDist < halfDist && headerDist < fullDist) {
				newState = MenuState.HEADER_ONLY;
			} else if (halfDist < headerDist && halfDist < fullDist) {
				newState = MenuState.HALF_SCREEN;
			} else {
				newState = MenuState.FULL_SCREEN;
			}

			if (slidingDown && currentMenuState == MenuState.FULL_SCREEN && getViewY() < getFullScreenTopPosY()) {
				slidingDown = false;
				newState = MenuState.FULL_SCREEN;
			}
			if (menuBottomViewHeight > 0 && slidingUp) {
				while (getCurrentMenuState() != newState) {
					if (!slideUp()) {
						break;
					}
				}
			} else if (slidingDown) {
				if (currentMenuState == MenuState.HEADER_ONLY) {
					needCloseMenu = true;
				} else {
					while (getCurrentMenuState() != newState) {
						if (!slideDown()) {
							needCloseMenu = true;
							break;
						}
					}
				}
			} else {
				if (currentMenuState < newState) {
					while (getCurrentMenuState() != newState) {
						if (!slideUp()) {
							break;
						}
					}
				} else {
					while (getCurrentMenuState() != newState) {
						if (!slideDown()) {
							break;
						}
					}
				}
			}
		}
		int newMenuState = getCurrentMenuState();
		boolean needMapAdjust = currentMenuState != newMenuState && newMenuState != MenuState.FULL_SCREEN;

		updateMenuState(currentMenuState, newMenuState);

		applyPosY(currentY, needCloseMenu, needMapAdjust, currentMenuState, newMenuState, 0, animated);

		ContextMenuFragmentListener listener = this.listener;
		if (listener != null) {
			listener.onContextMenuStateChanged(this, newMenuState, currentMenuState);
		}
	}

	protected void updateMenuState(int currentMenuState, int newMenuState) {
	}


	private int getPosY(int currentY, boolean needCloseMenu, int previousState) {
		if (needCloseMenu && isHideable()) {
			return screenHeight;
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return 0;
		}

		int destinationState = getCurrentMenuState();
		int posY = 0;
		switch (destinationState) {
			case MenuState.HEADER_ONLY:
				posY = getMenuStatePosY(MenuState.HEADER_ONLY);
				break;
			case MenuState.HALF_SCREEN:
				posY = getMenuStatePosY(MenuState.HALF_SCREEN);
				break;
			case MenuState.FULL_SCREEN:
				if (currentY != CURRENT_Y_UNDEFINED) {
					int maxPosY = getMinY();
					int minPosY = getMenuStatePosY(MenuState.FULL_SCREEN);
					if (maxPosY > minPosY) {
						maxPosY = minPosY;
					}
					if (currentY > minPosY || previousState != MenuState.FULL_SCREEN) {
						posY = minPosY;
					} else if (currentY < maxPosY) {
						posY = maxPosY;
					} else {
						posY = currentY;
					}
				} else {
					posY = getMenuStatePosY(MenuState.FULL_SCREEN);
				}
				break;
			default:
				break;
		}

		return posY;
	}

	protected void updateMainViewLayout(int posY) {
		MapActivity mapActivity = getMapActivity();
		if (view != null && mapActivity != null) {
			ViewGroup.LayoutParams lp = mainView.getLayoutParams();
			lp.height = view.getHeight() - posY;
			mainView.setLayoutParams(lp);
			mainView.requestLayout();
		}
	}

	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust,
	                        int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int posY = getPosY(currentY, needCloseMenu, previousMenuState);
		if (getViewY() != posY || dZoom != 0) {
			if (posY < getViewY()) {
				updateMainViewLayout(posY);
			}
			if (animated) {
				animateMainView(posY, needCloseMenu, previousMenuState, newMenuState);
			} else {
				if (needCloseMenu && isHideable()) {
					dismiss();
				} else {
					mainView.setY(posY);
					updateMainViewLayout(posY);
					if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
						doAfterMenuStateChange(previousMenuState, newMenuState);
					}
				}
			}
			ContextMenuFragmentListener listener = this.listener;
			if (listener != null) {
				listener.onContextMenuYPosChanged(this, posY, needMapAdjust, true);
			}
		}
		return posY;
	}

	protected void animateMainView(int posY, boolean needCloseMenu, int previousMenuState, int newMenuState) {
		animateView(mainView, posY, new AnimatorListenerAdapter() {

			boolean canceled;

			@Override
			public void onAnimationCancel(Animator animation) {
				canceled = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!canceled) {
					if (needCloseMenu && isHideable()) {
						dismiss();
					} else {
						updateMainViewLayout(posY);
						if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
							doAfterMenuStateChange(previousMenuState, newMenuState);
						}
					}
				}
			}
		});
	}

	public void animateView(@NonNull View view, int y, @Nullable AnimatorListener listener) {
		view.animate().y(y)
				.setDuration(ANIMATION_DURATION)
				.setInterpolator(new DecelerateInterpolator())
				.setListener(listener)
				.start();
	}

	protected void doAfterMenuStateChange(int previousState, int newState) {
		runLayoutListener();
	}

	protected void onHeaderClick() {
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void runLayoutListener() {
		runLayoutListener(null);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void runLayoutListener(Runnable runnable) {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (view != null) {
						ViewTreeObserver obs = view.getViewTreeObserver();
						obs.removeOnGlobalLayoutListener(this);

						if (getActivity() == null) {
							return;
						}
						calculateLayout(view, initLayout);

						if (!moving) {
							doLayoutMenu();
						}
						initLayout = false;

						ContextMenuFragmentListener listener = ContextMenuFragment.this.listener;
						if (listener != null) {
							int menuState = getCurrentMenuState();
							listener.onContextMenuStateChanged(ContextMenuFragment.this, menuState, menuState);
						}
						if (runnable != null) {
							runnable.run();
						}
					}
				}
			});
		}
	}

	protected void calculateLayout(View view, boolean initLayout) {
		menuFullHeight = mainView.getHeight();
		menuBottomViewHeight = menuFullHeight;
		menuFullHeightMax = view.findViewById(R.id.route_menu_cards_container).getHeight() +
				(isHeaderViewDetached() ? getHeaderViewHeight() : 0);
	}

	private void doLayoutMenu() {
		int posY = getPosY(initLayout ? CURRENT_Y_UNDEFINED : getViewY(), false, getCurrentMenuState());
		setViewY(posY, true, !initLayout);
		updateMainViewLayout(posY);
	}

	public boolean isDismissing() {
		return dismissing;
	}

	public void dismiss() {
		dismissing = true;
		if (isSingleFragment()) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				if (!fragmentManager.isStateSaved()) {
					fragmentManager.popBackStack(getFragmentTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
				}
			}
		}
		ContextMenuFragmentListener listener = this.listener;
		if (listener != null) {
			listener.onContextMenuDismiss(this);
		}
	}

	public void showLocationOnMap(LatLon latLon) {
		if (latLon == null) {
			Location lastLocation = app.getLocationProvider().getLastKnownLocation();
			if (lastLocation != null) {
				latLon = new LatLon(lastLocation.getLatitude(), lastLocation.getLongitude());
			}
		}
		if (latLon != null) {
			openMenuHeaderOnly();
			showOnMap(latLon);
		}
	}

	public void showOnMap(@NonNull LatLon latLon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int currentZoom = mapActivity.getMapView().getZoom();
			mapActivity.getMapView().getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), Math.max(15, currentZoom));
		}
	}

	public void showOnMap(@NonNull LatLon start, @NonNull LatLon end) {
		double left = Math.min(start.getLongitude(), end.getLongitude());
		double right = Math.max(start.getLongitude(), end.getLongitude());
		double top = Math.max(start.getLatitude(), end.getLatitude());
		double bottom = Math.min(start.getLatitude(), end.getLatitude());
		QuadRect rect = new QuadRect(left, top, right, bottom);
		openMenuHeaderOnly();
		fitRectOnMap(rect);
	}

	public void fitRectOnMap(QuadRect rect) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			if (!portrait) {
				tileBoxWidthPx = tb.getPixWidth() - view.getWidth();
			} else {
				tileBoxHeightPx = getHeaderOnlyTopY() - getShadowHeight();
			}
			if (tileBoxHeightPx > 0 || tileBoxWidthPx > 0) {
				int topMarginPx = getToolbarHeight();
				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, topMarginPx);
			}
		}
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	protected void copyToClipboard(@NonNull String text, @NonNull Context ctx) {
		ShareMenu.copyToClipboardWithToast(ctx, text, Toast.LENGTH_SHORT);
	}

	public static boolean showInstance(@NonNull FragmentManager manager, @NonNull ContextMenuFragment fragment) {
		String tag = fragment.getFragmentTag();
		if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
			manager.beginTransaction()
					.replace(R.id.routeMenuContainer, fragment, tag)
					.addToBackStack(tag)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}
