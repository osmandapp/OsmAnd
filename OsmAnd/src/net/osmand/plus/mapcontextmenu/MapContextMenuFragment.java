package net.osmand.plus.mapcontextmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.LockableScrollView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.BoundsChangeListener;
import net.osmand.plus.helpers.MapDisplayPositionManager.ICoveredScreenRectProvider;
import net.osmand.plus.helpers.MapDisplayPositionManager.IMapDisplayPositionProvider;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.settings.backend.menuitems.MainContextMenuItemsSettings;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.HorizontalSwipeConfirm;
import net.osmand.plus.views.controls.SingleTapConfirm;
import net.osmand.plus.views.layers.TransportStopsLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.router.TransportRouteResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MORE_ID;
import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;
import static net.osmand.plus.settings.fragments.configureitems.RearrangeItemsHelper.MAIN_BUTTONS_QUANTITY;

public class MapContextMenuFragment extends BaseOsmAndFragment implements DownloadEvents,
		ICoveredScreenRectProvider, IMapDisplayPositionProvider {
	public static final String TAG = "MapContextMenuFragment";

	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;
	public static final int ZOOM_IN_STANDARD = 17;

	public static final int CURRENT_Y_UNDEFINED = Integer.MAX_VALUE;

	private static final String TRANSPORT_BADGE_MORE_ITEM = "...";

	private View view;
	private InterceptorLinearLayout mainView;

	private View toolbarContainer;
	private TextView toolbarTextView;
	private View topButtonContainer;

	private LinearLayout mainRouteBadgeContainer;
	private LinearLayout nearbyRoutesLayout;
	private LinearLayout routesBadgesContainer;
	private GridView localTransportStopRoutesGrid;
	private GridView nearbyTransportStopRoutesGrid;
	private TextView nearbyRoutesWithinTv;
	private TextView localRoutesMoreTv;

	private View zoomButtonsView;

	private MapContextMenu menu;
	private OnLayoutChangeListener containerLayoutListener;
	private BoundsChangeListener mainViewBoundsChangeListener;
	private boolean forceUpdateLayout;

	private boolean portrait;

	private int menuTopViewHeight;
	private int menuTopShadowAllHeight;
	private int menuTitleHeight;
	private int menuBottomViewHeight;
	private int menuButtonsHeight;
	private int menuFullHeight;
	private int menuFullHeightMax;
	private int menuTopViewHeightExcludingTitle;
	private int menuTitleTopBottomPadding;

	private int screenHeight;
	private int viewHeight;
	private int zoomButtonsHeight;
	private int statusBarHeight;

	private int markerPaddingPx;
	private int markerPaddingXPx;
	private int topScreenPosY;
	private int bottomToolbarPosY;
	private int minHalfY;
	private int zoomPaddingTop;

	private OsmandMapTileView map;
	private MapDisplayPositionManager displayPositionManager;
	private LatLon mapCenter;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;
	private boolean moving;
	private boolean centered;
	private boolean initLayout = true;
	private boolean wasDrawerDisabled;
	private boolean zoomIn;

	private boolean created;
	private boolean destroyed;

	private boolean transportBadgesCreated;

	private UpdateLocationViewCache updateLocationViewCache;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapActivity mapActivity = requireMapActivity();

		map = mapActivity.getMapView();
		displayPositionManager = mapActivity.getMapPositionManager();
		menu = mapActivity.getContextMenu();
		mainViewBoundsChangeListener = new BoundsChangeListener(displayPositionManager, false);
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		boolean enabled = mapActivity.getFragmentsHelper().getQuickSearchDialogFragment() == null;
		mapActivity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(enabled) {
			public void handleOnBackPressed() {
				if (menu.isVisible() && menu.isClosable()) {
					if (menu.getCurrentMenuState() != MenuState.HEADER_ONLY && !menu.isLandscapeLayout()) {
						menu.openMenuHeaderOnly();
					} else {
						menu.close();
					}
				}
			}
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		if (menu.getLatLon() == null) {
			return null;
		}

		updateNightMode();
		menu.updateNightMode();
		processScreenHeight(container);

		MapActivity mapActivity = requireMapActivity();
		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(mapActivity);

		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);
		int shadowHeight = dpToPx(SHADOW_HEIGHT_TOP_DP);
		topScreenPosY = addStatusBarHeightIfNeeded(-shadowHeight);
		bottomToolbarPosY = addStatusBarHeightIfNeeded(getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar));
		minHalfY = viewHeight - (int) (viewHeight * menu.getHalfScreenMaxHeightKoef());
		zoomPaddingTop = getDimensionPixelSize(R.dimen.map_button_margin);

		view = themedInflater.inflate(R.layout.fragment_map_context_menu, container, false);
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		mainView = view.findViewById(R.id.context_menu_main);

		toolbarContainer = view.findViewById(R.id.context_menu_toolbar_container);
		ImageView toolbarBackButton = view.findViewById(R.id.context_menu_toolbar_back);
		toolbarTextView = view.findViewById(R.id.context_menu_toolbar_text);
		updateVisibility(toolbarContainer, 0);
		toolbarBackButton.setOnClickListener(v -> openMenuHeaderOnly());
		toolbarBackButton.setImageResource(AndroidUtils.getNavigationIconResId(mapActivity));

		topButtonContainer = view.findViewById(R.id.context_menu_top_button_container);
		ImageView backButton = view.findViewById(R.id.context_menu_top_back);
		backButton.setOnClickListener(v -> openMenuHeaderOnly());
		backButton.setImageResource(AndroidUtils.getNavigationIconResId(mapActivity));
		updateVisibility(topButtonContainer, 0);

		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			LatLon latLon = menu.getLatLon();
			if (latLon == null) {
				origMarkerX = box.getCenterPixelX();
				origMarkerY = box.getCenterPixelY();
			} else {
				PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(map.getMapRenderer(), box, latLon);
				origMarkerX = (int) pixel.x;
				origMarkerY = (int) pixel.y;
			}
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}

		// Left title button
		View leftTitleButtonView = view.findViewById(R.id.title_button_view);
		leftTitleButtonView.setOnClickListener(v -> {
			TitleButtonController leftTitleButtonController = menu.getLeftTitleButtonController();
			if (leftTitleButtonController != null) {
				leftTitleButtonController.buttonPressed();
			}
		});

		// Right title button
		View rightTitleButtonView = view.findViewById(R.id.title_button_right_view);
		rightTitleButtonView.setOnClickListener(v -> {
			TitleButtonController rightTitleButtonController = menu.getRightTitleButtonController();
			if (rightTitleButtonController != null) {
				rightTitleButtonController.buttonPressed();
			}
		});

		// Left download button
		View leftDownloadButtonView = view.findViewById(R.id.download_button_left_view);
		leftDownloadButtonView.setOnClickListener(v -> {
			TitleButtonController leftDownloadButtonController = menu.getLeftDownloadButtonController();
			if (leftDownloadButtonController != null) {
				leftDownloadButtonController.buttonPressed();
			}
		});

		// Right download button
		View rightDownloadButtonView = view.findViewById(R.id.download_button_right_view);
		rightDownloadButtonView.setOnClickListener(v -> {
			TitleButtonController rightDownloadButtonController = menu.getRightDownloadButtonController();
			if (rightDownloadButtonController != null) {
				rightDownloadButtonController.buttonPressed();
			}
		});

		// Bottom title button
		View bottomTitleButtonView = view.findViewById(R.id.title_button_bottom_view);
		bottomTitleButtonView.setOnClickListener(v -> {
			TitleButtonController bottomTitleButtonController = menu.getBottomTitleButtonController();
			if (bottomTitleButtonController != null) {
				bottomTitleButtonController.buttonPressed();
			}
		});

		// Progress bar
		ImageView progressButton = view.findViewById(R.id.progressButton);
		progressButton.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark, R.color.icon_color_default_light));
		progressButton.setOnClickListener(v -> {
			TitleProgressController titleProgressController = menu.getTitleProgressController();
			if (titleProgressController != null) {
				titleProgressController.buttonPressed();
			}
		});

		menu.updateData();
		updateButtonsAndProgress();

		if (menu.isLandscapeLayout()) {
			TypedValue typedValueAttr = new TypedValue();
			int bgAttrId = AndroidUtils.isLayoutRtl(app) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
			mapActivity.getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
			View fabContainer = view.findViewById(R.id.context_menu_fab_container);
			fabContainer.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

		runLayoutListener();

		GestureDetector singleTapDetector = new GestureDetector(view.getContext(), new SingleTapConfirm());
		GestureDetector swipeDetector = new GestureDetector(view.getContext(), new HorizontalSwipeConfirm(true));

		View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
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
				scroller = new OverScroller(getContext());
				ViewConfiguration configuration = ViewConfiguration.get(getContext());
				minimumVelocity = configuration.getScaledMinimumFlingVelocity();
				maximumVelocity = configuration.getScaledMaximumFlingVelocity();
			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (!hasMoved && event.getY() <= menuTopViewHeight) {
					if (singleTapDetector.onTouchEvent(event)) {
						moving = false;
						openMenuHalfScreen();

						recycleVelocityTracker();
						return true;
					}
				}
				if (menu.isLandscapeLayout()) {
					if (swipeDetector.onTouchEvent(event)) {
						menu.close();

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
						if (Math.abs(event.getRawY() - mDownY) > mainView.getTouchSlop()) {
							moving = true;
						}
						if (moving) {
							hasMoved = true;
							float y = event.getY();
							float newY = getViewY() + (y - dy);
							if (menu.isLandscapeLayout() && newY > topScreenPosY) {
								newY = topScreenPosY;
							}
							setViewY((int) newY, false, false, 0);

							menuFullHeight = view.getHeight() - (int) newY + 10;
							ViewGroup.LayoutParams lp = mainView.getLayoutParams();
							lp.height = Math.max(menuFullHeight, menuTitleHeight);
							mainView.setLayoutParams(lp);
							mainView.requestLayout();

							float newEventY = newY - (dyMain - dy);
							MotionEvent ev = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(),
									event.getX(), newEventY, event.getMetaState());

							initVelocityTrackerIfNotExists();
							velocityTracker.addMovement(ev);

							updateToolbar();
							updateTopButton();
						}

						break;

					case MotionEvent.ACTION_UP:
						if (moving) {
							moving = false;
							hasMoved = false;
							int currentY = getViewY();

							VelocityTracker velocityTracker = this.velocityTracker;
							velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
							int initialVelocity = (int) velocityTracker.getYVelocity();

							if ((Math.abs(initialVelocity) > minimumVelocity)) {

								scroller.abortAnimation();
								scroller.fling(0, currentY, 0, initialVelocity, 0, 0,
										Math.min(viewHeight - menuFullHeightMax, getFullScreenTopPosY()),
										screenHeight, 0, 0);
								currentY = scroller.getFinalY();
								scroller.abortAnimation();

								slidingUp = initialVelocity < -2000;
								slidingDown = initialVelocity > 2000;
							} else {
								slidingUp = false;
								slidingDown = false;
							}

							changeMenuState(currentY, slidingUp, slidingDown);
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

		View topShadowAllView = view.findViewById(R.id.context_menu_top_shadow_all);
		AndroidUtils.setBackground(mapActivity, topShadowAllView, nightMode, R.drawable.bg_map_context_menu_light,
				R.drawable.bg_map_context_menu_dark);

		mainView.setListener(slideTouchListener);
		mainView.setOnTouchListener(slideTouchListener);

		buildHeader();

		((TextView) view.findViewById(R.id.context_menu_line1))
				.setTextColor(ColorUtilities.getPrimaryTextColor(mapActivity, nightMode));
		View menuLine2 = view.findViewById(R.id.context_menu_line2);
		if (menuLine2 != null) {
			((TextView) menuLine2).setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_light));
		}
		((TextView) view.findViewById(R.id.distance)).setTextColor(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light));

		AndroidUtils.setTextSecondaryColor(mapActivity,
				view.findViewById(R.id.progressTitle), nightMode);

		// Zoom buttons
		zoomButtonsView = view.findViewById(R.id.context_menu_zoom_buttons);
		if (menu.zoomButtonsVisible()) {
			ImageButton zoomInButtonView = view.findViewById(R.id.context_menu_zoom_in_button);
			ImageButton zoomOutButtonView = view.findViewById(R.id.context_menu_zoom_out_button);
			int bgId = nightMode ? R.drawable.btn_circle_night : R.drawable.btn_circle_trans;
			int iconColorId = ColorUtilities.getMapButtonIconColorId(nightMode);
			updateImageButton(app, zoomInButtonView, R.drawable.ic_zoom_in, iconColorId, bgId);
			updateImageButton(app, zoomOutButtonView, R.drawable.ic_zoom_out, iconColorId, bgId);
			zoomInButtonView.setOnClickListener(v -> menu.zoomInPressed());
			zoomOutButtonView.setOnClickListener(v -> menu.zoomOutPressed());
			zoomButtonsView.setVisibility(View.VISIBLE);
		} else {
			zoomButtonsView.setVisibility(View.GONE);
		}

		localTransportStopRoutesGrid = view.findViewById(R.id.transport_stop_routes_grid);
		nearbyTransportStopRoutesGrid = view.findViewById(R.id.transport_stop_nearby_routes_grid);
		nearbyRoutesWithinTv = view.findViewById(R.id.nearby_routes_within_text_view);
		localRoutesMoreTv = view.findViewById(R.id.local_routes_more_text_view);
		nearbyRoutesLayout = view.findViewById(R.id.nearby_routes);
		routesBadgesContainer = view.findViewById(R.id.transport_badges_container);
		mainRouteBadgeContainer = view.findViewById(R.id.main_transport_route_badge);

		if (nightMode) {
			nearbyRoutesWithinTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_dark));
			localRoutesMoreTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_dark));
		} else {
			nearbyRoutesWithinTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_light));
			localRoutesMoreTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_light));
		}

		View buttonsBottomBorder = view.findViewById(R.id.buttons_bottom_border);
		int buttonsBorderColor = ContextCompat.getColor(mapActivity,
				nightMode ? R.color.divider_color_dark : R.color.divider_color_light);
		buttonsBottomBorder.setBackgroundColor(buttonsBorderColor);
		View bottomButtons = view.findViewById(R.id.context_menu_bottom_buttons);
		bottomButtons.setBackgroundColor(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light));
		bottomButtons.findViewById(R.id.context_menu_directions_button)
				.setVisibility(menu.navigateButtonVisible() ? View.VISIBLE : View.GONE);
		View buttonsTopBorder = view.findViewById(R.id.buttons_top_border);
		buttonsTopBorder.setBackgroundColor(buttonsBorderColor);
		buttonsTopBorder.setVisibility(menu.buttonsVisible() ? View.VISIBLE : View.GONE);

		//Bottom buttons
		int bottomButtonsColor = nightMode ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n;
		TextView detailsButton = view.findViewById(R.id.context_menu_details_button);
		detailsButton.setTextColor(ContextCompat.getColor(mapActivity, bottomButtonsColor));
		detailsButton.setOnClickListener(view -> openMenuHalfScreen());
		TextView directionsButton = view.findViewById(R.id.context_menu_directions_button);
		int iconResId = R.drawable.ic_action_gdirections_dark;
		if (menu.navigateInPedestrianMode()) {
			iconResId = R.drawable.ic_action_pedestrian_dark;
		}
		Drawable drawable = getIcon(iconResId, bottomButtonsColor);
		directionsButton.setTextColor(ContextCompat.getColor(mapActivity, bottomButtonsColor));
		AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
				directionsButton, null, null, drawable, null);
		int contentPaddingHalf = (int) getResources().getDimension(R.dimen.content_padding_half);
		directionsButton.setCompoundDrawablePadding(contentPaddingHalf);
		directionsButton.setOnClickListener(view -> menu.navigateButtonPressed());

		buildBottomView();

		LockableScrollView bottomScrollView = view.findViewById(R.id.context_menu_bottom_scroll);
		bottomScrollView.setScrollingEnabled(false);
		bottomScrollView.setBackgroundColor(getColor(
				nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light));
		view.findViewById(R.id.context_menu_bottom_view).setBackgroundColor(getColor(
				nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light));

		//getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(false);

		containerLayoutListener = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
			if (!transportBadgesCreated) {
				createTransportBadges();
			}
			if (forceUpdateLayout || bottom != oldBottom) {
				forceUpdateLayout = false;
				processScreenHeight((View) view.getParent());
				runLayoutListener();
			}
		};

		created = true;
		return view;
	}

	private void updateActionButtons(MapActivity mapActivity) {
		LinearLayout buttons = view.findViewById(R.id.context_menu_buttons);
		buttons.setBackgroundColor(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light));
		buttons.setVisibility(menu.buttonsVisible() ? View.VISIBLE : View.GONE);
		// Action buttons
		ContextMenuAdapter adapter = menu.getActionsContextMenuAdapter(false);
		List<ContextMenuItem> items = adapter.getVisibleItems();
		List<String> mainIds = ((MainContextMenuItemsSettings) settings.CONTEXT_MENU_ACTIONS_ITEMS.get()).getMainIds();
		ContextMenuAdapter mainAdapter = new ContextMenuAdapter(app);
		ContextMenuAdapter additionalAdapter = new ContextMenuAdapter(app);

		if (!mainIds.isEmpty()) {
			for (ContextMenuItem item : items) {
				if (mainIds.contains(item.getId())) {
					mainAdapter.addItem(item);
				} else {
					additionalAdapter.addItem(item);
				}
			}
		} else {
			for (int i = 0; i < items.size(); i++) {
				if (i < MAIN_BUTTONS_QUANTITY) {
					mainAdapter.addItem(items.get(i));
				} else {
					additionalAdapter.addItem(items.get(i));
				}
			}
		}
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
				1f
		);
		buttons.removeAllViews();
		ContextMenuItemClickListener mainListener = menu.getContextMenuItemClickListener(mainAdapter);
		ContextMenuItemClickListener additionalListener = menu.getContextMenuItemClickListener(additionalAdapter);

		if (!mainIds.isEmpty()) {
			for (ContextMenuItem item : mainAdapter.getItems()) {
				buttons.addView(getActionView(item, mainAdapter.getItems().indexOf(item), additionalAdapter, mainListener, additionalListener), params);
			}
		} else {
			int mainButtonsQuantity = Math.min(MAIN_BUTTONS_QUANTITY, items.size());
			for (int i = 0; i < mainButtonsQuantity; i++) {
				buttons.addView(getActionView(items.get(i), i, additionalAdapter, mainListener, additionalListener), params);
			}
		}
		buttons.setGravity(Gravity.CENTER);
	}

	private View getActionView(ContextMenuItem contextMenuItem,
	                           int position,
	                           ContextMenuAdapter additionalAdapter,
	                           ContextMenuItemClickListener mainListener,
	                           ContextMenuItemClickListener additionalListener) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = inflater.inflate(R.layout.context_menu_action_item, null);
		LinearLayout item = view.findViewById(R.id.item);
		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.text);
		icon.setImageDrawable(uiUtilities.getIcon(contextMenuItem.getIcon(), nightMode));
		title.setText(contextMenuItem.getTitle());
		String id = contextMenuItem.getId();
		if (MAP_CONTEXT_MENU_MORE_ID.equals(id)) {
			item.setOnClickListener(v -> menu.showAdditionalActionsFragment(additionalAdapter, additionalListener));
		} else {
			item.setOnClickListener(v -> mainListener.onItemClick(v, position));
		}
		return view;
	}

	private void updateImageButton(@NonNull OsmandApplication ctx, @NonNull ImageButton button,
	                               @DrawableRes int iconId, @ColorRes int iconColorId, @DrawableRes int bgId) {
		int btnSizePx = button.getLayoutParams().height;
		int iconSizePx = ctx.getResources().getDimensionPixelSize(R.dimen.map_widget_icon);
		int iconPadding = (btnSizePx - iconSizePx) / 2;
		button.setBackground(ContextCompat.getDrawable(ctx, bgId));
		button.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
		button.setScaleType(ImageView.ScaleType.FIT_CENTER);
		button.setImageDrawable(ctx.getUIUtilities().getIcon(iconId, iconColorId));
	}

	@Nullable
	private TransportStopRouteAdapter createTransportStopRouteAdapter(List<TransportStopRoute> routes, boolean needMoreItem) {
		List<Object> items = new ArrayList<Object>(routes);
		if (needMoreItem) {
			items.add(TRANSPORT_BADGE_MORE_ITEM);
		}
		TransportStopRouteAdapter adapter = new TransportStopRouteAdapter(app, items, nightMode);
		adapter.setListener(position -> {
			Object object = adapter.getItem(position);
			MapActivity mapActivity = getMapActivity();
			if (object != null && mapActivity != null) {
				if (object instanceof TransportStopRoute) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					TransportStopRoute route = (TransportStopRoute) object;
					PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
							route.getDescription(app, false));
					menu.show(menu.getLatLon(), pd, route);
					TransportStopsLayer stopsLayer = mapActivity.getMapLayers().getTransportStopsLayer();
					stopsLayer.setRoute(route);
					int zoom = route.calculateZoom(0, mapView.getCurrentRotatedTileBox());
					mapView.setIntZoom(zoom);
				} else if (object instanceof String) {
					if (object.equals(TRANSPORT_BADGE_MORE_ITEM)) {
						if (menu.isLandscapeLayout()) {
							changeMenuState(getFullScreenTopPosY(), false, false);
						} else {
							openMenuFullScreen();
						}
					}
				}
			}
		});
		return adapter;
	}

	private List<TransportStopRoute> filterTransportRoutes(List<TransportStopRoute> routes) {
		List<TransportStopRoute> filteredRoutes = new ArrayList<>();
		for (TransportStopRoute route : routes) {
			if (!containsRef(filteredRoutes, route)) {
				filteredRoutes.add(route);
			}
		}
		return filteredRoutes;
	}

	private List<TransportStopRoute> filterNearbyTransportRoutes(List<TransportStopRoute> routes, List<TransportStopRoute> filterFromRoutes) {
		List<TransportStopRoute> nearbyFilteredTransportStopRoutes = filterTransportRoutes(routes);
		if (filterFromRoutes == null || filterFromRoutes.isEmpty()) {
			return nearbyFilteredTransportStopRoutes;
		}
		List<TransportStopRoute> filteredRoutes = new ArrayList<>();
		for (TransportStopRoute route : nearbyFilteredTransportStopRoutes) {
			if (!containsRef(filterFromRoutes, route)) {
				filteredRoutes.add(route);
			}
		}
		return filteredRoutes;
	}

	private boolean containsRef(List<TransportStopRoute> routes, TransportStopRoute transportRoute) {
		for (TransportStopRoute route : routes) {
			if (route.type == transportRoute.type && route.route.getRef().equals(transportRoute.route.getRef())) {
				return true;
			}
		}
		return false;
	}

	private float getToolbarAlpha(int y) {
		float a = 0;
		if (menu != null && !menu.isLandscapeLayout()) {
			if (y < bottomToolbarPosY) {
				a = 1f - (y - topScreenPosY) * (1f / (bottomToolbarPosY - topScreenPosY));
			}
			if (a < 0) {
				a = 0;
			} else if (a > 1) {
				a = 1;
			}
		}
		return a;
	}

	private void updateToolbar() {
		float a = getToolbarAlpha(getViewY());
		updateVisibility(toolbarContainer, a);
	}

	private float getTopButtonAlpha(int y) {
		float a = 0;
		if (menu != null && !menu.isLandscapeLayout() && !menu.hasActiveToolbar()) {
			int headerTopY = getHeaderOnlyTopY();
			if (y < headerTopY) {
				a = 1f - (y - minHalfY) * (1f / (headerTopY - minHalfY));
			}
			if (a < 0) {
				a = 0;
			} else if (a > 1) {
				a = 1;
			}
		}
		return a;
	}

	private void updateTopButton() {
		float a = getTopButtonAlpha(getViewY());
		updateVisibility(topButtonContainer, a);
	}

	private void updateVisibility(View v, float alpha) {
		boolean visible = alpha > 0;
		v.setAlpha(alpha);
		if (visible && v.getVisibility() != View.VISIBLE) {
			v.setVisibility(View.VISIBLE);
		} else if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	private void updateVisibility(View v, boolean visible) {
		if (visible && v.getVisibility() != View.VISIBLE) {
			v.setVisibility(View.VISIBLE);
		} else if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	private void toggleDetailsHideButton() {
		int menuState = menu.getCurrentMenuState();
		boolean showShowHideButton = menuState == MenuState.HALF_SCREEN || (!menu.isLandscapeLayout() && menuState == MenuState.FULL_SCREEN);
		TextView detailsButton = view.findViewById(R.id.context_menu_details_button);
		detailsButton.setText(showShowHideButton ? R.string.shared_string_collapse : R.string.rendering_category_details);
		detailsButton.setOnClickListener(view -> {
			if (showShowHideButton) {
				openMenuHeaderOnly();
			} else {
				openMenuHalfScreen();
			}
		});
	}

	@Override
	public int getStatusBarColorId() {
		if (menu != null && (menu.getCurrentMenuState() == MenuState.FULL_SCREEN || menu.isLandscapeLayout())) {
			return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
		}
		return -1;
	}

	private void processScreenHeight(@NonNull View container) {
		FragmentActivity activity = requireActivity();
		viewHeight = container.getHeight();
		screenHeight = AndroidUtils.getScreenHeight(activity);
		statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
	}

	public void openMenuFullScreen() {
		changeMenuState(getMenuStatePosY(MenuState.FULL_SCREEN), false, false);
	}

	public void openMenuHeaderOnly() {
		if (!menu.isLandscapeLayout()) {
			changeMenuState(getMenuStatePosY(MenuState.HEADER_ONLY), false, false);
		}
	}

	public void openMenuHalfScreen() {
		if (!menu.isLandscapeLayout()) {
			changeMenuState(getMenuStatePosY(MenuState.HALF_SCREEN), false, false);
		}
	}

	private void changeMenuState(int currentY, boolean slidingUp, boolean slidingDown) {
		boolean needCloseMenu = false;

		int currentMenuState = menu.getCurrentMenuState();
		if (!menu.isLandscapeLayout()) {
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
				while (menu.getCurrentMenuState() != newState) {
					if (!menu.slideUp()) {
						break;
					}
				}
			} else if (slidingDown) {
				if (currentMenuState == MenuState.HEADER_ONLY) {
					needCloseMenu = true;
				} else {
					while (menu.getCurrentMenuState() != newState) {
						if (!menu.slideDown()) {
							needCloseMenu = true;
							break;
						}
					}
				}
			} else {
				if (currentMenuState < newState) {
					while (menu.getCurrentMenuState() != newState) {
						if (!menu.slideUp()) {
							break;
						}
					}
				} else {
					while (menu.getCurrentMenuState() != newState) {
						if (!menu.slideDown()) {
							break;
						}
					}
				}
			}
		}
		int newMenuState = menu.getCurrentMenuState();
		boolean needMapAdjust = currentMenuState != newMenuState && (currentMenuState == MenuState.HEADER_ONLY || newMenuState == MenuState.HEADER_ONLY);

		if (newMenuState != currentMenuState) {
			menu.updateControlsVisibility(true);
			doBeforeMenuStateChange(currentMenuState, newMenuState);
			toggleDetailsHideButton();
		}

		applyPosY(currentY, needCloseMenu, needMapAdjust, currentMenuState, newMenuState, 0);
	}

	private void restoreCustomMapRatio() {
		if (map != null && displayPositionManager.hasCustomMapRatio()) {
			map.restoreScreenCenter();
		}
	}

	private void setCustomMapRatio() {
		LatLon latLon = menu.getLatLon();
		RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
		PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(map.getMapRenderer(), tb, latLon);
		float ratioX = pixel.x / tb.getPixWidth();
		float ratioY = pixel.y / tb.getPixHeight();
		app.getMapViewTrackingUtilities().getMapDisplayPositionManager().setCustomMapRatio(ratioX, ratioY);
		map.setLatLon(latLon.getLatitude(), latLon.getLongitude(), ratioX, ratioY);
	}

	public void doZoomIn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
			boolean containsLatLon = NativeUtilities.containsLatLon(map.getMapRenderer(), tb, menu.getLatLon());
			if (containsLatLon) {
				setCustomMapRatio();
			} else {
				restoreCustomMapRatio();
			}
			map.zoomIn();
		}
	}

	public void doZoomOut() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
			boolean containsLatLon = NativeUtilities.containsLatLon(map.getMapRenderer(), tb, menu.getLatLon());
			if (containsLatLon) {
				setCustomMapRatio();
			} else {
				restoreCustomMapRatio();
			}
			map.zoomOut();
		}
	}

	private void applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust,
	                       int previousMenuState, int newMenuState, int dZoom) {
		int posY = getPosY(currentY, needCloseMenu, previousMenuState);
		if (getViewY() != posY || dZoom != 0) {
			if (posY < getViewY()) {
				updateMainViewLayout(posY);
			}

			float topButtonAlpha = getTopButtonAlpha(posY);
			if (topButtonAlpha > 0) {
				updateVisibility(topButtonContainer, true);
			}
			topButtonContainer.animate().alpha(topButtonAlpha)
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							updateVisibility(topButtonContainer, topButtonAlpha);
						}
					})
					.start();

			float toolbarAlpha = getToolbarAlpha(posY);
			if (toolbarAlpha > 0) {
				updateVisibility(toolbarContainer, true);
			}
			toolbarContainer.animate().alpha(toolbarAlpha)
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							updateVisibility(toolbarContainer, toolbarAlpha);
						}
					})
					.start();

			mainView.animate().y(posY)
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.setListener(new AnimatorListenerAdapter() {

						boolean canceled;

						@Override
						public void onAnimationCancel(Animator animation) {
							canceled = true;
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							if (!canceled) {
								if (needCloseMenu) {
									menu.close(false);
								} else {
									updateMainViewLayout(posY);
									if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
										doAfterMenuStateChange(previousMenuState, newMenuState);
									}
								}
							}
						}
					})
					.start();

			zoomButtonsView.animate().y(getZoomButtonsY(posY))
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.start();

			if (needMapAdjust) {
				int mapPosY = posY;
				if (newMenuState == MenuState.FULL_SCREEN) {
					mapPosY = getMenuStatePosY(MenuState.HALF_SCREEN);
				}
				adjustMapPosition(mapPosY, true, centered, dZoom);
			}
		}
	}

	public void updateMapCenter(LatLon mapCenter) {
		customMapCenter = true;
		if (menu != null) {
			menu.setMapCenter(mapCenter);
		}
		this.mapCenter = mapCenter;
		if (map != null) {
			RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}
	}

	private void setupButton(View buttonView, boolean enabled, CharSequence text) {
		buttonView.setEnabled(enabled);
		UiUtilities.setupDialogButton(nightMode, buttonView, DialogButtonType.STROKED, text);
	}

	public void updateButtonsAndProgress() {
		if (view != null) {
			TitleButtonController leftTitleButtonController = menu.getLeftTitleButtonController();
			TitleButtonController rightTitleButtonController = menu.getRightTitleButtonController();
			TitleButtonController bottomTitleButtonController = menu.getBottomTitleButtonController();
			TitleButtonController leftDownloadButtonController = menu.getLeftDownloadButtonController();
			TitleButtonController rightDownloadButtonController = menu.getRightDownloadButtonController();
			List<Pair<TitleButtonController, TitleButtonController>> additionalButtonsControllers = menu.getAdditionalButtonsControllers();
			TitleProgressController titleProgressController = menu.getTitleProgressController();

			// Title buttons
			boolean showTitleButtonsContainer = (leftTitleButtonController != null || rightTitleButtonController != null);
			View titleButtonsContainer = view.findViewById(R.id.title_button_container);
			titleButtonsContainer.setVisibility(showTitleButtonsContainer ? View.VISIBLE : View.GONE);

			// Left title button
			View leftTitleButtonView = view.findViewById(R.id.title_button_view);
			TextView leftTitleButton = leftTitleButtonView.findViewById(R.id.button_text);
			if (leftTitleButtonController != null) {
				setupButton(leftTitleButtonView, leftTitleButtonController.enabled, createRightTextCaption(leftTitleButtonController));
				if (leftTitleButtonController.visible) {
					leftTitleButtonView.setVisibility(View.VISIBLE);
					Drawable startIcon = leftTitleButtonController.getStartIcon();
					Drawable endIcon = leftTitleButtonController.getEndIcon();
					AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
							leftTitleButton, startIcon, null, endIcon, null);
					leftTitleButton.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
					((LinearLayout) leftTitleButtonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
				} else {
					leftTitleButtonView.setVisibility(View.INVISIBLE);
				}
			} else {
				leftTitleButtonView.setVisibility(View.INVISIBLE);
			}

			// Right title button
			View rightTitleButtonView = view.findViewById(R.id.title_button_right_view);
			TextView rightTitleButton = rightTitleButtonView.findViewById(R.id.button_text);
			if (rightTitleButtonController != null) {
				setupButton(rightTitleButtonView, rightTitleButtonController.enabled, rightTitleButtonController.caption);
				rightTitleButtonView.setVisibility(rightTitleButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable startIcon = rightTitleButtonController.getStartIcon();
				Drawable endIcon = rightTitleButtonController.getEndIcon();
				AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
						rightTitleButton, startIcon, null, endIcon, null);
				rightTitleButton.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
				((LinearLayout) rightTitleButtonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
			} else {
				rightTitleButtonView.setVisibility(View.INVISIBLE);
			}

			// Bottom title button
			View bottomTitleButtonView = view.findViewById(R.id.title_button_bottom_view);
			TextView bottomTitleButton = bottomTitleButtonView.findViewById(R.id.button_text);
			if (bottomTitleButtonController != null) {
				setupButton(bottomTitleButtonView, bottomTitleButtonController.enabled, bottomTitleButtonController.caption);
				bottomTitleButtonView.setVisibility(bottomTitleButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable startIcon = bottomTitleButtonController.getStartIcon();
				Drawable endIcon = bottomTitleButtonController.getEndIcon();
				AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
						bottomTitleButton, startIcon, null, endIcon, null);
				bottomTitleButton.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
				((LinearLayout) bottomTitleButtonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
			} else {
				bottomTitleButtonView.setVisibility(View.GONE);
			}

			// Download buttons
			boolean showDownloadButtonsContainer =
					((leftDownloadButtonController != null && leftDownloadButtonController.visible)
							|| (rightDownloadButtonController != null && rightDownloadButtonController.visible))
							&& (titleProgressController == null || !titleProgressController.visible);
			View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
			downloadButtonsContainer.setVisibility(showDownloadButtonsContainer ? View.VISIBLE : View.GONE);

			// Left download button
			View leftDownloadButtonView = view.findViewById(R.id.download_button_left_view);
			TextView leftDownloadButton = leftDownloadButtonView.findViewById(R.id.button_text);
			if (leftDownloadButtonController != null) {
				setupButton(leftDownloadButtonView, leftDownloadButtonController.enabled, leftDownloadButtonController.caption);
				leftDownloadButtonView.setVisibility(leftDownloadButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable startIcon = leftDownloadButtonController.getStartIcon();
				Drawable endIcon = leftDownloadButtonController.getEndIcon();
				AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
						leftDownloadButton, startIcon, null, endIcon, null);
				leftDownloadButton.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
				((LinearLayout) leftDownloadButtonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
			} else {
				leftDownloadButtonView.setVisibility(View.INVISIBLE);
			}

			// Right download button
			View rightDownloadButtonView = view.findViewById(R.id.download_button_right_view);
			TextView rightDownloadButton = rightDownloadButtonView.findViewById(R.id.button_text);
			if (rightDownloadButtonController != null) {
				setupButton(rightDownloadButtonView, rightDownloadButtonController.enabled, rightDownloadButtonController.caption);
				rightDownloadButtonView.setVisibility(rightDownloadButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable startIcon = rightDownloadButtonController.getStartIcon();
				Drawable endIcon = rightDownloadButtonController.getEndIcon();
				AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
						rightDownloadButton, startIcon, null, endIcon, null);
				rightDownloadButton.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
				((LinearLayout) rightDownloadButtonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
			} else {
				rightDownloadButtonView.setVisibility(View.INVISIBLE);
			}

			LinearLayout additionalButtonsContainer = view.findViewById(R.id.additional_buttons_container);
			if (additionalButtonsControllers != null && !additionalButtonsControllers.isEmpty()) {
				additionalButtonsContainer.removeAllViews();
				for (Pair<TitleButtonController, TitleButtonController> buttonControllers : additionalButtonsControllers) {
					attachButtonsRow(additionalButtonsContainer, buttonControllers.first, buttonControllers.second);
				}
				additionalButtonsContainer.setVisibility(View.VISIBLE);
			} else {
				additionalButtonsContainer.setVisibility(View.GONE);
			}

			// Progress bar
			View titleProgressContainer = view.findViewById(R.id.title_progress_container);
			if (titleProgressController != null) {
				titleProgressContainer.setVisibility(titleProgressController.visible ? View.VISIBLE : View.GONE);
				if (titleProgressController.visible && showTitleButtonsContainer) {
					LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) titleProgressContainer.getLayoutParams();
					if (ll.topMargin != 0) {
						ll.setMargins(0, 0, 0, 0);
					}
				}

				ProgressBar progressBar = view.findViewById(R.id.progressBar);
				TextView progressTitle = view.findViewById(R.id.progressTitle);
				progressTitle.setText(titleProgressController.caption);
				progressBar.setIndeterminate(titleProgressController.indeterminate);
				progressBar.setProgress((int) titleProgressController.progress);
				progressBar.setVisibility(titleProgressController.progressVisible ? View.VISIBLE : View.GONE);

				ImageView progressButton = view.findViewById(R.id.progressButton);
				progressButton.setVisibility(titleProgressController.buttonVisible ? View.VISIBLE : View.GONE);
			} else {
				titleProgressContainer.setVisibility(View.GONE);
			}
			updateActionButtons(getMapActivity());
			updateAdditionalInfoVisibility();
		}
	}

	private void attachButtonsRow(ViewGroup container, TitleButtonController leftButtonController, TitleButtonController rightButtonController) {
		ContextThemeWrapper ctx = new ContextThemeWrapper(getMapActivity(), !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(R.layout.context_menu_buttons, container, false);

		// Left button
		View leftButtonView = view.findViewById(R.id.additional_button_left_view);
		TextView leftButton = leftButtonView.findViewById(R.id.button_text);
		fillButtonInfo(leftButtonController, leftButtonView, leftButton);

		// Right button
		View rightButtonView = view.findViewById(R.id.additional_button_right_view);
		TextView rightButton = rightButtonView.findViewById(R.id.button_text);
		fillButtonInfo(rightButtonController, rightButtonView, rightButton);

		container.addView(view);
	}

	private void fillButtonInfo(TitleButtonController buttonController, View buttonView, TextView buttonText) {
		if (buttonController != null) {
			setupButton(buttonView, buttonController.enabled, createRightTextCaption(buttonController));
			buttonView.setVisibility(buttonController.visible ? View.VISIBLE : View.INVISIBLE);

			Drawable startIcon = buttonController.getStartIcon();
			Drawable endIcon = buttonController.getEndIcon();
			AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
					buttonText, startIcon, null, endIcon, null);
			buttonText.setCompoundDrawablePadding(view.getResources().getDimensionPixelSize(R.dimen.content_padding_half));
			((LinearLayout) buttonView).setGravity(endIcon != null ? Gravity.END : Gravity.START);
			buttonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					buttonController.buttonPressed();
				}
			});
		} else {
			buttonView.setVisibility(View.INVISIBLE);
		}
	}

	private SpannableStringBuilder createRightTextCaption(@NonNull TitleButtonController buttonController) {
		SpannableStringBuilder title = new SpannableStringBuilder(buttonController.caption);
		if (buttonController.needRightText) {
			int startIndex = title.length();
			title.append(" ").append(buttonController.rightTextCaption);
			Context context = view.getContext();
			Typeface typeface = FontCache.getRobotoRegular(context);
			title.setSpan(new CustomTypefaceSpan(typeface), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			title.setSpan(new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(context, nightMode)),
					startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return title;
	}

	private void buildHeader() {
		if (view != null) {
			ImageView iconView = view.findViewById(R.id.context_menu_icon_view);
			Drawable icon = menu.getRightIcon();
			int iconId = menu.getRightIconId();
			int sizeId = menu.isBigRightIcon() ? R.dimen.context_menu_big_icon_size : R.dimen.standard_icon_size;
			if (menu.getPointDescription().isFavorite() || menu.getPointDescription().isWpt()) {
				sizeId = R.dimen.favorites_my_places_icon_size;
			}
			int iconViewSize = getResources().getDimensionPixelSize(sizeId);
			ViewGroup.LayoutParams params = iconView.getLayoutParams();
			params.width = iconViewSize;
			params.height = iconViewSize;

			if (icon != null) {
				iconView.setImageDrawable(icon);
				iconView.setVisibility(View.VISIBLE);
			} else if (iconId != 0) {
				iconView.setImageDrawable(getIcon(iconId,
						!nightMode ? R.color.osmand_orange : R.color.osmand_orange_dark));
				iconView.setVisibility(View.VISIBLE);
			} else {
				iconView.setVisibility(View.GONE);
			}
			setAddressLocation();
		}
	}

	private void buildBottomView() {
		if (view != null) {
			ViewGroup bottomView = view.findViewById(R.id.context_menu_bottom_view);
			if (menu.isExtended()) {
				menu.build(bottomView);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (view == null || !menu.isActive() || menu.getLatLon() == null
					|| MapRouteInfoMenu.waypointsVisible
					|| mapActivity.getMapRouteInfoMenu().isVisible()) {
				dismissMenu();
				return;
			}
			if (MapRouteInfoMenu.chooseRoutesVisible) {
				mapActivity.getFragmentsHelper().getChooseRouteFragment().dismiss();
			}
			updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(mapActivity, false);
			mapActivity.getMapViewTrackingUtilities().setContextMenu(menu);
			mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			ViewParent parent = view.getParent();
			if (parent != null && containerLayoutListener != null) {
				((View) parent).addOnLayoutChangeListener(containerLayoutListener);
			}
			updateMapDisplayPosition(true);
			menu.updateControlsVisibility(true);
			menu.onFragmentResume();
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
		}
	}

	@Override
	public void onPause() {
		if (view != null) {
			updateMapDisplayPosition(false);
			restoreCustomMapRatio();
			ViewParent parent = view.getParent();
			if (parent != null && containerLayoutListener != null) {
				((View) parent).removeOnLayoutChangeListener(containerLayoutListener);
			}
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapViewTrackingUtilities().setContextMenu(null);
				mapActivity.getMapViewTrackingUtilities().setMapLinkedToLocation(false);
				if (!wasDrawerDisabled) {
					mapActivity.enableDrawer();
				}
				menu.updateControlsVisibility(false);
			}
		}
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		destroyed = true;
		menu.setMapCenter(null);
		menu.setMapZoom(0);
	}

	public void rebuildMenu(boolean centered) {
		if (view != null) {
			buildHeader();

			LinearLayout bottomLayout = view.findViewById(R.id.context_menu_bottom_view);
			bottomLayout.removeAllViews();
			buildBottomView();

			if (centered) {
				this.initLayout = true;
				this.centered = true;
			}
			updateButtonsAndProgress();
			runLayoutListener();
		}
	}

	private void createTransportBadges() {
		if (!transportBadgesCreated) {
			List<TransportStopRoute> localTransportStopRoutes = menu.getLocalTransportStopRoutes();
			List<TransportStopRoute> nearbyTransportStopRoutes = menu.getNearbyTransportStopRoutes();
			int maxLocalRows = 0;
			if (localTransportStopRoutes != null && !localTransportStopRoutes.isEmpty()) {
				List<TransportStopRoute> localFilteredTransportStopRoutes = filterTransportRoutes(localTransportStopRoutes);
				int minBadgeWidth = getMinBadgeWidth(localFilteredTransportStopRoutes);
				int localColumnsPerRow = getRoutesBadgesColumnsPerRow(null, minBadgeWidth);
				maxLocalRows = (int) Math.round(Math.ceil((double) localFilteredTransportStopRoutes.size() / localColumnsPerRow));
				localTransportStopRoutesGrid.setColumnWidth(minBadgeWidth);
				updateLocalRoutesBadges(localFilteredTransportStopRoutes, localColumnsPerRow);
			}
			if (nearbyTransportStopRoutes != null && !nearbyTransportStopRoutes.isEmpty()) {
				updateNearbyRoutesBadges(maxLocalRows, filterNearbyTransportRoutes(nearbyTransportStopRoutes, localTransportStopRoutes));
			}
			transportBadgesCreated = true;
		}
	}

	private View createRouteBadge(TransportStopRoute transportStopRoute) {
		LinearLayout convertView = null;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
			if (transportStopRoute != null) {
				String routeDescription = transportStopRoute.getDescription(app);
				String routeRef = transportStopRoute.route.getAdjustedRouteRef(true);
				int bgColor = transportStopRoute.getColor(app, nightMode);

				TextView transportStopRouteTextView = convertView.findViewById(R.id.transport_stop_route_text);
				ImageView transportStopRouteImageView = convertView.findViewById(R.id.transport_stop_route_icon);

				int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
				transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, ColorUtilities.getContrastColor(mapActivity, bgColor, true)));
				transportStopRouteTextView.setText(routeRef + ": " + routeDescription);
				GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
				gradientDrawableBg.setColor(bgColor);
				transportStopRouteTextView.setTextColor(ColorUtilities.getContrastColor(mapActivity, bgColor, true));
			}
		}
		return convertView;
	}

	private void updateLocalRoutesBadges(List<TransportStopRoute> localTransportStopRoutes, int localColumnsPerRow) {
		int localRoutesSize = localTransportStopRoutes.size();
		TransportRouteResult activeRoute = app.getRoutingHelper().getTransportRoutingHelper().getActiveRoute();
		if (localRoutesSize > 0 && activeRoute != null) {
			for (int i = 0; i < localTransportStopRoutes.size(); i++) {
				TransportStopRoute stopRoute = localTransportStopRoutes.get(i);
				if (activeRoute.isRouteStop(stopRoute.stop)) {
					View routeBadge = createRouteBadge(stopRoute);
					mainRouteBadgeContainer.addView(routeBadge);
					mainRouteBadgeContainer.setVisibility(View.VISIBLE);
					mainRouteBadgeContainer.setOnClickListener(v -> {
						dismissMenu();
						ChooseRouteFragment.showInstance(requireMyActivity().getSupportFragmentManager(),
								app.getRoutingHelper().getTransportRoutingHelper().getCurrentRoute(),
								ContextMenuFragment.MenuState.FULL_SCREEN);
						/* fit route segment on map
						TransportRouteResult activeRoute = requireMyApplication().getRoutingHelper().getTransportRoutingHelper().getActiveRoute();
						if (activeRoute != null) {
							TransportRouteResultSegment segment = activeRoute.getRouteStopSegment(stopRoute.stop);
							if (segment != null) {
								QuadRect rect = segment.getSegmentRect();
								if (rect != null) {
									//openMenuHeaderOnly();
									fitRectOnMap(rect);
								}
							}
						}
						*/
					});
					localTransportStopRoutes.remove(i);
					localRoutesSize--;
					break;
				}
			}
		}
		if (localRoutesSize > 0) {
			int maxLocalBadges = localColumnsPerRow * 5;
			TransportStopRouteAdapter adapter;
			if (localRoutesSize > maxLocalBadges) {
				adapter = createTransportStopRouteAdapter(localTransportStopRoutes.subList(0, maxLocalBadges), false);
				localRoutesMoreTv.setVisibility(View.VISIBLE);
			} else {
				adapter = createTransportStopRouteAdapter(localTransportStopRoutes, false);
				localRoutesMoreTv.setVisibility(View.GONE);
			}
			localTransportStopRoutesGrid.setAdapter(adapter);
			localTransportStopRoutesGrid.setVisibility(View.VISIBLE);
		} else {
			localTransportStopRoutesGrid.setVisibility(View.GONE);
			localRoutesMoreTv.setVisibility(View.GONE);
		}
	}

	private void updateNearbyRoutesBadges(int maxLocalRows, List<TransportStopRoute> nearbyTransportStopRoutes) {
		int nearbyRoutesSize = nearbyTransportStopRoutes.size();
		boolean moreLocalItems = localRoutesMoreTv.getVisibility() == View.VISIBLE;
		if (maxLocalRows <= 5 && !moreLocalItems && nearbyRoutesSize > 0) {
			String nearInDistance = getString(R.string.transport_nearby_routes) + " "
					+ OsmAndFormatter.getFormattedDistance(TransportStopController.SHOW_STOPS_RADIUS_METERS, app) + ":";
			nearbyRoutesWithinTv.setText(nearInDistance);
			int minBadgeWidth = getMinBadgeWidth(nearbyTransportStopRoutes);
			int nearbyColumnsPerRow = getRoutesBadgesColumnsPerRow(nearInDistance, minBadgeWidth);
			int maxNearbyRows = Math.min(3, 6 - maxLocalRows);
			int nearbyMaxItems = maxNearbyRows * nearbyColumnsPerRow - 1;
			TransportStopRouteAdapter adapter;
			if (nearbyRoutesSize > nearbyMaxItems) {
				adapter = createTransportStopRouteAdapter(nearbyTransportStopRoutes.subList(0, nearbyMaxItems), true);
			} else {
				adapter = createTransportStopRouteAdapter(nearbyTransportStopRoutes, false);
			}
			nearbyTransportStopRoutesGrid.setColumnWidth(minBadgeWidth);
			nearbyTransportStopRoutesGrid.setAdapter(adapter);
			nearbyTransportStopRoutesGrid.setVisibility(View.VISIBLE);
			nearbyRoutesLayout.setVisibility(View.VISIBLE);
		} else {
			nearbyRoutesLayout.setVisibility(View.GONE);
		}
	}

	private int getRoutesBadgesColumnsPerRow(@Nullable String nearInDistance, int minBadgeWidth) {
		try {
			double gridSpacing = getResources().getDimension(R.dimen.context_menu_transport_grid_spacing);
			double gridPadding = getResources().getDimension(R.dimen.content_padding);
			int availableSpace;
			if (nearInDistance == null) {
				availableSpace = (int) (routesBadgesContainer.getWidth() - gridPadding * 2);
			} else {
				int textWidth = AndroidUtils.getTextWidth(getResources().getDimensionPixelSize(R.dimen.default_sub_text_size), nearInDistance);
				double paddingTv = getResources().getDimension(R.dimen.context_menu_padding_margin_small);
				availableSpace = (int) (routesBadgesContainer.getWidth() - gridPadding * 2 - paddingTv - textWidth);
			}
			return (int) ((availableSpace + gridSpacing) / (minBadgeWidth + gridSpacing));
		} catch (Resources.NotFoundException e) {
			return -1;
		}
	}

	private int getMinBadgeWidth(List<TransportStopRoute> transportStopRoutes) {
		try {
			int minBadgeWidth = getResources().getDimensionPixelSize(R.dimen.context_menu_transport_grid_item_width);
			int textPadding = getResources().getDimensionPixelSize(R.dimen.context_menu_subtitle_margin);
			float textSizeSmall = getResources().getDimensionPixelSize(R.dimen.default_sub_text_size_small);

			for (TransportStopRoute transportStopRoute : transportStopRoutes) {
				String routeRef = transportStopRoute.route.getAdjustedRouteRef(false);
				int textWidth = AndroidUtils.getTextWidth(textSizeSmall, routeRef) + textPadding * 2;
				if (textWidth > minBadgeWidth) {
					minBadgeWidth = textWidth;
				}
			}

			return minBadgeWidth;
		} catch (Resources.NotFoundException e) {
			return dpToPx(32);
		}
	}

	private void runLayoutListener() {
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

						int newMenuTopViewHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
						int newMenuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
						menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();
						zoomButtonsHeight = zoomButtonsView.getHeight();

						int dy = 0;
						if (!menu.isLandscapeLayout()) {
							TextView line1 = view.findViewById(R.id.context_menu_line1);
							TextView line2 = view.findViewById(R.id.context_menu_line2);
							int line2LineCount = 0;
							int line2LineHeight = 0;
							int line2MeasuredHeight = 0;
							if (line2 != null) {
								line2LineCount = line2.getLineCount();
								line2LineHeight = line2.getLineHeight();
								line2MeasuredHeight = line2.getMeasuredHeight();
							}

							int customAddressLineHeight = 0;
							View customAddressLine = view.findViewById(R.id.context_menu_custom_address_line);
							if (customAddressLine.getVisibility() == View.VISIBLE) {
								customAddressLineHeight = customAddressLine.getMeasuredHeight();
							}

							int line3Height = 0;
							View line3Container = view.findViewById(R.id.additional_info_row_container);
							if (line3Container.getVisibility() == View.VISIBLE) {
								line3Height = line3Container.getMeasuredHeight();
							}

							int titleButtonHeight = 0;
							View titleButtonContainer = view.findViewById(R.id.title_button_container);
							if (titleButtonContainer.getVisibility() == View.VISIBLE) {
								titleButtonHeight = titleButtonContainer.getMeasuredHeight();
							}

							int downloadButtonsHeight = 0;
							View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
							if (downloadButtonsContainer.getVisibility() == View.VISIBLE) {
								downloadButtonsHeight = downloadButtonsContainer.getMeasuredHeight();
							}

							int titleBottomButtonHeight = 0;
							View titleBottomButtonContainer = view.findViewById(R.id.title_bottom_button_container);
							if (titleBottomButtonContainer.getVisibility() == View.VISIBLE) {
								titleBottomButtonHeight = titleBottomButtonContainer.getMeasuredHeight();
							}

							int additionalButtonsHeight = 0;
							View additionalButtonsContainer = view.findViewById(R.id.additional_buttons_container);
							if (additionalButtonsContainer.getVisibility() == View.VISIBLE) {
								additionalButtonsHeight = additionalButtonsContainer.getMeasuredHeight();
							}

							int titleProgressHeight = 0;
							View titleProgressContainer = view.findViewById(R.id.title_progress_container);
							if (titleProgressContainer.getVisibility() == View.VISIBLE) {
								titleProgressHeight = titleProgressContainer.getMeasuredHeight();
							}

							if (menuTopViewHeight != 0) {
								int titleHeight = line1.getLineCount() * line1.getLineHeight()
										+ line2LineCount * line2LineHeight + menuTitleTopBottomPadding;
								if (titleHeight < line1.getMeasuredHeight() + line2MeasuredHeight) {
									titleHeight = line1.getMeasuredHeight() + line2MeasuredHeight;
								}
								newMenuTopViewHeight = menuTopViewHeightExcludingTitle + titleHeight
										+ titleButtonHeight + customAddressLineHeight + downloadButtonsHeight
										+ titleBottomButtonHeight + additionalButtonsHeight + titleProgressHeight + line3Height;
								dy = Math.max(0, newMenuTopViewHeight - menuTopViewHeight
										- (newMenuTopShadowAllHeight - menuTopShadowAllHeight));
							} else {
								menuTopViewHeightExcludingTitle = newMenuTopViewHeight - line1.getMeasuredHeight() - line2MeasuredHeight - customAddressLineHeight
										- titleButtonHeight - downloadButtonsHeight - titleBottomButtonHeight - additionalButtonsHeight - titleProgressHeight - line3Height;
								menuTitleTopBottomPadding = (line1.getMeasuredHeight() - line1.getLineCount() * line1.getLineHeight())
										+ (line2MeasuredHeight - line2LineCount * line2LineHeight);
								menuButtonsHeight = view.findViewById(R.id.context_menu_bottom_buttons).getHeight()
										+ view.findViewById(R.id.buttons_bottom_border).getHeight()
										+ view.findViewById(R.id.context_menu_buttons).getHeight();
							}
						}
						menuTopViewHeight = newMenuTopViewHeight;
						menuTopShadowAllHeight = newMenuTopShadowAllHeight;
						menuTitleHeight = menuTopShadowAllHeight + dy;
						menuBottomViewHeight = view.findViewById(R.id.context_menu_bottom_view).getHeight();

						menuFullHeightMax = menuTitleHeight + menuBottomViewHeight;

						if (origMarkerX == 0 && origMarkerY == 0) {
							origMarkerX = view.getWidth() / 2;
							origMarkerY = view.getHeight() / 2;
						}

						if (initLayout && centered) {
							centerMarkerLocation();
						}
						if (!moving) {
							doLayoutMenu();
						}
						initLayout = false;
					}
				}

			});
		}
	}

	public void centerMarkerLocation() {
		centered = true;
		showOnMap(menu.getLatLon(), true, false, getZoom());
	}

	private int getZoom() {
		int zoom;
		if (zoomIn) {
			zoom = ZOOM_IN_STANDARD;
		} else {
			zoom = menu.getMapZoom();
		}
		if (zoom == 0) {
			zoom = map.getZoom();
		}
		return zoom;
	}

	private LatLon calculateCenterLatLon(LatLon latLon, int zoom, boolean updateOrigXY) {
		double flat = latLon.getLatitude();
		double flon = latLon.getLongitude();

		RotatedTileBox cp = map.getCurrentRotatedTileBox().copy();
		cp.setCenterLocation(0.5f, displayPositionManager.getNavigationMapPosition() == MapPosition.BOTTOM ? 0.15f : 0.5f);
		cp.setLatLonCenter(flat, flon);
		cp.setZoom(zoom);
		flat = cp.getLatFromPixel(cp.getPixWidth() / 2f, cp.getPixHeight() / 2f);
		flon = cp.getLonFromPixel(cp.getPixWidth() / 2f, cp.getPixHeight() / 2f);

		if (updateOrigXY) {
			origMarkerX = cp.getCenterPixelX();
			origMarkerY = cp.getCenterPixelY();
		}
		return new LatLon(flat, flon);
	}

	private void showOnMap(LatLon latLon, boolean updateCoords, boolean alreadyAdjusted, int zoom) {
		AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
		int calculatedZoom = menu.isZoomOutOnly() ? thread.calculateMoveZoom(null, latLon.getLatitude(), latLon.getLongitude(), null) : 0;
		if (calculatedZoom > 0) {
			zoom = Math.min(zoom, calculatedZoom);
		}
		menu.setZoomOutOnly(false);
		LatLon calcLatLon = calculateCenterLatLon(latLon, zoom, updateCoords);
		if (updateCoords) {
			mapCenter = calcLatLon;
			menu.setMapCenter(mapCenter);
		}
		if (!alreadyAdjusted) {
			calcLatLon = getAdjustedMarkerLocation(getPosY(), calcLatLon, true, zoom);
		}
		thread.startMoving(calcLatLon.getLatitude(), calcLatLon.getLongitude(), zoom);
	}

	private void setAddressLocation() {
		if (view != null) {
			// Text line 1
			TextView line1 = view.findViewById(R.id.context_menu_line1);
			line1.setText(menu.getTitleStr());
			toolbarTextView.setText(menu.getTitleStr());
			// Text line 2
			TextView line2 = view.findViewById(R.id.context_menu_line2);
			LinearLayout customAddressLine = view.findViewById(R.id.context_menu_custom_address_line);
			customAddressLine.removeAllViews();
			if (menu.hasCustomAddressLine()) {
				menu.buildCustomAddressLine(customAddressLine);
				AndroidUiHelper.updateVisibility(line2, false);
				AndroidUiHelper.updateVisibility(customAddressLine, true);
			} else {
				AndroidUiHelper.updateVisibility(line2, true);
				AndroidUiHelper.updateVisibility(customAddressLine, false);
				String typeStr = menu.getTypeStr();
				String streetStr = menu.getStreetStr();
				StringBuilder line2Str = new StringBuilder();
				if (!Algorithms.isEmpty(typeStr)) {
					line2Str.append(typeStr);
					Drawable icon = menu.getTypeIcon();
					AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
							line2, icon, null, null, null);
					line2.setCompoundDrawablePadding(dpToPx(5f));
				}
				if (!Algorithms.isEmpty(streetStr) && !menu.displayStreetNameInTitle()) {
					if (line2Str.length() > 0) {
						line2Str.append(", ");
					}
					line2Str.append(streetStr);
				}
				if (!TextUtils.isEmpty(line2Str)) {
					line2.setText(line2Str.toString());
					line2.setVisibility(View.VISIBLE);
				} else {
					line2.setVisibility(View.GONE);
				}
			}

			TextView line3 = view.findViewById(R.id.context_menu_line3);
			CharSequence subtypeStr = menu.getSubtypeStr();
			if (TextUtils.isEmpty(subtypeStr)) {
				line3.setVisibility(View.GONE);
			} else {
				line3.setVisibility(View.VISIBLE);
				line3.setText(subtypeStr);
				Drawable icon = menu.getSubtypeIcon();
				AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(
						line3, icon, null, null, null);
				line3.setCompoundDrawablePadding(dpToPx(5f));
			}

			View additionalInfoLayout = view.findViewById(R.id.additional_info_layout);
			ImageView additionalInfoImageView = view.findViewById(R.id.additional_info_image_view);
			TextView additionalInfoTextView = view.findViewById(R.id.additional_info_text_view);
			CharSequence additionalInfoStr = menu.getAdditionalInfo();
			boolean showAdditionalImage = false;
			boolean showAdditionalInfo = !TextUtils.isEmpty(additionalInfoStr);
			if (showAdditionalInfo) {
				int colorId = menu.getAdditionalInfoColor();
				int additionalInfoIconRes = menu.getAdditionalInfoIconRes();
				if (colorId != 0) {
					additionalInfoTextView.setTextColor(ContextCompat.getColor(additionalInfoTextView.getContext(), colorId));
					if (additionalInfoIconRes != 0) {
						Drawable additionalIcon = getIcon(additionalInfoIconRes, colorId);
						additionalInfoImageView.setImageDrawable(additionalIcon);
						showAdditionalImage = true;
					}
				}
				additionalInfoTextView.setText(additionalInfoStr);
				additionalInfoTextView.setVisibility(View.VISIBLE);
				additionalInfoLayout.setVisibility(View.VISIBLE);
			} else {
				additionalInfoTextView.setVisibility(View.GONE);
				additionalInfoLayout.setVisibility(View.GONE);
			}
			additionalInfoImageView.setVisibility(showAdditionalImage ? View.VISIBLE : View.GONE);

			boolean showCompass = menu.displayDistanceDirection();
			boolean showCompassSeparator = showAdditionalInfo && showCompass;
			View compassSeparator = view.findViewById(R.id.info_compass_separator);
			compassSeparator.setVisibility(showCompassSeparator ? View.VISIBLE : View.GONE);

			updateAltitudeText(showAdditionalInfo || showCompass);
		}

		updateCompassVisibility();
		updateAdditionalInfoVisibility();
	}

	private void updateAltitudeText(boolean addSeparator) {
		View altitudeSeparator = view.findViewById(R.id.info_altitude_separator);
		View altitudeLayout = view.findViewById(R.id.altitude_layout);
		TextView tvAltitude = view.findViewById(R.id.altitude);

		if (tvAltitude.length() > 0) {
			AndroidUiHelper.updateVisibility(altitudeSeparator, addSeparator);
		} else {
			altitudeSeparator.setVisibility(View.GONE);
			altitudeLayout.setVisibility(View.GONE);

			menu.getFormattedAltitude(formattedAltitude -> {
				if (!TextUtils.isEmpty(formattedAltitude) && !destroyed) {
					AndroidUiHelper.updateVisibility(altitudeSeparator, addSeparator);

					tvAltitude.setText(formattedAltitude);
					altitudeLayout.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	private void updateCompassVisibility() {
		if (view != null) {
			View compassView = view.findViewById(R.id.compass_layout);
			if (menu.displayDistanceDirection()) {
				updateDistanceDirection();
				compassView.setVisibility(View.VISIBLE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateAdditionalInfoVisibility() {
		View line3 = view.findViewById(R.id.context_menu_line3);
		View additionalInfoImageView = view.findViewById(R.id.additional_info_image_view);
		View additionalInfoTextView = view.findViewById(R.id.additional_info_text_view);
		View compassView = view.findViewById(R.id.compass_layout);
		View altitudeView = view.findViewById(R.id.altitude_layout);
		View titleButtonContainer = view.findViewById(R.id.title_button_container);
		View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
		View titleBottomButtonContainer = view.findViewById(R.id.title_bottom_button_container);
		View titleProgressContainer = view.findViewById(R.id.title_progress_container);

		if (line3.getVisibility() == View.GONE
				&& additionalInfoImageView.getVisibility() == View.GONE
				&& additionalInfoTextView.getVisibility() == View.GONE
				&& compassView.getVisibility() == View.INVISIBLE
				&& altitudeView.getVisibility() == View.GONE
				&& titleButtonContainer.getVisibility() == View.GONE
				&& downloadButtonsContainer.getVisibility() == View.GONE
				&& titleBottomButtonContainer.getVisibility() == View.GONE) {
			if (titleProgressContainer.getVisibility() == View.VISIBLE) {
				view.findViewById(R.id.additional_info_row_container).setVisibility(View.GONE);
			}
			view.findViewById(R.id.additional_info_row).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.additional_info_row_container).setVisibility(View.VISIBLE);
			view.findViewById(R.id.additional_info_row).setVisibility(View.VISIBLE);
		}
	}

	private void updateDistanceDirection() {
		FragmentActivity activity = getActivity();
		if (activity != null && view != null) {
			TextView distanceText = view.findViewById(R.id.distance);
			ImageView direction = view.findViewById(R.id.direction);
			UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, direction, distanceText, menu.getLatLon());
		}
	}

	private void updateZoomButtonsVisibility(int menuState) {
		if (zoomButtonsView == null) {
			return;
		}
		boolean zoomButtonsVisible = menu.zoomButtonsVisible() && menuState == MenuState.HEADER_ONLY;
		if (zoomButtonsVisible) {
			if (zoomButtonsView.getVisibility() != View.VISIBLE) {
				zoomButtonsView.setVisibility(View.VISIBLE);
			}
		} else {
			if (zoomButtonsView.getVisibility() == View.VISIBLE) {
				zoomButtonsView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private int getHeaderOnlyTopY() {
		return viewHeight - menuTitleHeight;
	}

	private int getFullScreenTopPosY() {
		return -menuTitleHeight + menuButtonsHeight + bottomToolbarPosY;
	}

	private int getMenuStatePosY(int menuState) {
		if (menu.isLandscapeLayout()) {
			return topScreenPosY;
		}
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

	private int getPosY() {
		return getPosY(CURRENT_Y_UNDEFINED, false);
	}

	private int getPosY(int currentY, boolean needCloseMenu) {
		return getPosY(currentY, needCloseMenu, 0);
	}

	private int getPosY(int currentY, boolean needCloseMenu, int previousState) {
		if (needCloseMenu) {
			return screenHeight + statusBarHeight;
		}

		int destinationState;
		if (menu.isExtended()) {
			destinationState = menu.getCurrentMenuState();
		} else {
			destinationState = MenuState.HEADER_ONLY;
		}

		updateZoomButtonsVisibility(destinationState);

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
					int maxPosY = viewHeight - menuFullHeightMax;
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
		MapActivity mapActivity = getMapActivity();
		if (!menu.isLandscapeLayout() && mapActivity != null) {
			mapActivity.updateStatusBarColor();
		}
		return posY;
	}

	private int addStatusBarHeightIfNeeded(int res) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			// One pixel is needed to fill a thin gap between the status bar and the fragment.
			return res + AndroidUtils.getStatusBarHeight(mapActivity) - 1;
		}
		return res;
	}

	private void updateMainViewLayout(int posY) {
		if (view != null) {
			menuFullHeight = view.getHeight() - posY;
			menuTopShadowAllHeight = menuTitleHeight;
			ViewGroup.LayoutParams lp = mainView.getLayoutParams();
			lp.height = Math.max(menuFullHeight, menuTitleHeight);
			mainView.setLayoutParams(lp);
			mainView.requestLayout();
		}
	}

	private int getViewY() {
		return (int) mainView.getY();
	}

	private void setViewY(int y, boolean animated, boolean adjustMapPos, int mapY) {
		mainView.setY(y);
		zoomButtonsView.setY(getZoomButtonsY(y));
		if (!customMapCenter) {
			if (adjustMapPos) {
				adjustMapPosition(mapY, animated, centered, 0);
			}
		}
	}

	private void adjustMapPosition(int y, boolean animated, boolean center, int dZoom) {
		map.getAnimatedDraggingThread().stopAnimatingSync();
		int zoom = getZoom() + dZoom;
		LatLon latlon = getAdjustedMarkerLocation(y, menu.getLatLon(), center, zoom);

		if (displayPositionManager.hasCustomMapRatio()
				|| (map.getLatitude() == latlon.getLatitude() && map.getLongitude() == latlon.getLongitude() && dZoom == 0)) {
			return;
		}

		if (animated) {
			showOnMap(latlon, false, true, zoom);
		} else {
			if (dZoom != 0) {
				map.setIntZoom(zoom);
			}
			map.setLatLon(latlon.getLatitude(), latlon.getLongitude());
		}
	}

	private LatLon getAdjustedMarkerLocation(int y, LatLon reqMarkerLocation, boolean center, int zoom) {
		double markerLat = reqMarkerLocation.getLatitude();
		double markerLon = reqMarkerLocation.getLongitude();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		box.setCenterLocation(0.5f, displayPositionManager.getNavigationMapPosition() == MapPosition.BOTTOM ? 0.15f : 0.5f);
		box.setZoom(zoom);
		boolean hasMapCenter = mapCenter != null;
		int markerMapCenterX = 0;
		int markerMapCenterY = 0;
		if (hasMapCenter) {
			markerMapCenterX = (int) box.getPixXFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
			markerMapCenterY = (int) box.getPixYFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		}
		float cpyOrig = box.getCenterPixelPoint().y;

		box.setCenterLocation(0.5f, 0.5f);
		int markerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
		int markerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		QuadPoint cp = box.getCenterPixelPoint();
		float cpx = cp.x;
		float cpy = cp.y;

		float cpyDelta = menu.isLandscapeLayout() ? 0 : cpyOrig - cpy;

		markerY += cpyDelta;
		y += cpyDelta;
		float origMarkerY = this.origMarkerY + cpyDelta;

		LatLon latlon;
		if (center || !hasMapCenter) {
			latlon = reqMarkerLocation;
		} else {
			latlon = box.getLatLonFromPixel(markerMapCenterX, markerMapCenterY);
		}
		if (menu.isLandscapeLayout()) {
			int x = menu.getLandscapeWidthPx();
			if (markerX - markerPaddingXPx < x || markerX > origMarkerX) {
				int dx = (x + markerPaddingXPx) - markerX;
				int dy = 0;
				if (center) {
					dy = (int) cpy - markerY;
				} else {
					cpy = cpyOrig;
				}
				if (dx >= 0 || center) {
					latlon = box.getLatLonFromPixel(cpx - dx, cpy - dy);
				}
			}
		} else {
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dx = 0;
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					if (center) {
						dx = markerX - (int) cpx;
					}
					latlon = box.getLatLonFromPixel(cpx + dx, cpy + dy);
				}
			}
		}
		return latlon;
	}

	private int getZoomButtonsY(int y) {
		int zoomButtonsY = y - mainView.getTop() - zoomButtonsHeight + zoomPaddingTop;
		int maxZoomButtonsY = screenHeight - (zoomButtonsHeight + zoomPaddingTop);
		return Math.min(zoomButtonsY, maxZoomButtonsY);
	}

	private void doLayoutMenu() {
		int state = menu.getCurrentMenuState();
		int posY = getPosY(getViewY(), false, state);
		int mapPosY = state == MenuState.FULL_SCREEN ? getMenuStatePosY(MenuState.HALF_SCREEN) : posY;
		setViewY(posY, true, !initLayout || !centered, mapPosY);
		updateMainViewLayout(posY);
	}

	public void dismissMenu() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	public void updateLayout() {
		runLayoutListener();
	}

	public void refreshTitle() {
		setAddressLocation();
		runLayoutListener();
	}

	public void setFragmentVisibility(boolean visible) {
		if (view != null) {
			if (visible) {
				forceUpdateLayout = true;
				view.setVisibility(View.VISIBLE);
				if (mapCenter != null) {
					map.setLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
				}
				int posY = getPosY();
				int mapPosY = menu.getCurrentMenuState() == MenuState.FULL_SCREEN ? getMenuStatePosY(MenuState.HALF_SCREEN) : posY;
				adjustMapPosition(mapPosY, true, false, 0);
			} else {
				view.setVisibility(View.GONE);
			}
		}
	}

	public static boolean showInstance(MapContextMenu menu, MapActivity mapActivity,
	                                   boolean centered) {
		if (menu.getLatLon() == null || mapActivity == null || mapActivity.isActivityDestroyed()) {
			return false;
		}

		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		int slideInAnim = 0;
		int slideOutAnim = 0;
		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			slideInAnim = R.anim.slide_in_bottom;
			slideOutAnim = R.anim.slide_out_bottom;

			if (menu.isExtended()) {
				slideInAnim = menu.getSlideInAnimation();
				slideOutAnim = menu.getSlideOutAnimation();
			}
		}

		MapContextMenuFragment fragment = new MapContextMenuFragment();
		fragment.centered = centered;
		fragmentManager.beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
		return true;
	}

	//DownloadEvents
	@Override
	public void onUpdatedIndexesList() {
		updateOnDownload();
	}

	@Override
	public void downloadInProgress() {
		updateOnDownload();
	}

	@Override
	public void downloadHasFinished() {
		updateOnDownload();
		if (menu != null && menu.isVisible() && menu.isMapDownloaded()) {
			rebuildMenu(false);
		}
	}

	private void updateOnDownload() {
		if (menu != null) {
			boolean wasProgressVisible = menu.getTitleProgressController() != null && menu.getTitleProgressController().visible;
			menu.updateData();
			boolean progressVisible = menu.getTitleProgressController() != null && menu.getTitleProgressController().visible;
			updateButtonsAndProgress();
			if (wasProgressVisible != progressVisible) {
				refreshTitle();
			}
		}
	}

	public void updateMenu() {
		if (created) {
			menu.updateData();
			updateButtonsAndProgress();
			refreshTitle();
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
	}

	private int dpToPx(float dp) {
		MapActivity mapActivity = getMapActivity();
		return mapActivity != null ? AndroidUtils.dpToPx(mapActivity, dp) : (int) dp;
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		updateDistanceDirection();
	}

	private void doBeforeMenuStateChange(int previousState, int newState) {
		if (newState == MenuState.HALF_SCREEN) {
			centered = true;
			if (!zoomIn && menu.supportZoomIn()) {
				if (getZoom() < ZOOM_IN_STANDARD) {
					zoomIn = true;
				}
			}
			calculateCenterLatLon(menu.getLatLon(), getZoom(), true);
		}
	}

	private void doAfterMenuStateChange(int previousState, int newState) {
		updateCompassVisibility();
		updateAdditionalInfoVisibility();
		runLayoutListener();
	}

	private void updateMapDisplayPosition(boolean newPosition) {
		if (portrait) {
			displayPositionManager.updateMapPositionProviders(this, newPosition);
			displayPositionManager.updateMapDisplayPosition();
		} else {
			displayPositionManager.updateCoveredScreenRectProvider(this, newPosition);
			if (newPosition) {
				view.addOnLayoutChangeListener(mainViewBoundsChangeListener);
			} else {
				view.removeOnLayoutChangeListener(mainViewBoundsChangeListener);
			}
			if (view.getWidth() > 0 && view.getHeight() > 0) {
				displayPositionManager.updateMapDisplayPosition();
			}
		}
	}

	@Nullable
	@Override
	public MapPosition getMapDisplayPosition() {
		return portrait ? MapPosition.CENTER : null;
	}

	@NonNull
	@Override
	public List<Rect> getCoveredScreenRects() {
		Rect rect = portrait ? null : AndroidUtils.getViewBoundOnScreen(mainView);
		return rect == null ? Collections.emptyList() : Collections.singletonList(rect);
	}
}