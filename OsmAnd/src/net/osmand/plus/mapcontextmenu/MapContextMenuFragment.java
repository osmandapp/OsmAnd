package net.osmand.plus.mapcontextmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
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

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.plus.LockableScrollView;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapcontextmenu.MenuController.MenuState;
import net.osmand.plus.mapcontextmenu.MenuController.TitleButtonController;
import net.osmand.plus.mapcontextmenu.MenuController.TitleProgressController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.views.controls.HorizontalSwipeConfirm;
import net.osmand.plus.views.controls.SingleTapConfirm;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;


public class MapContextMenuFragment extends BaseOsmAndFragment implements DownloadEvents {
	public static final String TAG = "MapContextMenuFragment";

	public static final float ZOOM_PADDING_TOP_DP = 4f;
	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;
	public static final int ZOOM_IN_STANDARD = 17;

	public static final int CURRENT_Y_UNDEFINED = Integer.MAX_VALUE;

	private static final String TRANSPORT_BADGE_MORE_ITEM = "...";

	private View view;
	private InterceptorLinearLayout mainView;

	private View toolbarContainer;
	private View toolbarView;
	private View toolbarBackButton;
	private TextView toolbarTextView;
	private View topButtonContainer;
	private LockableScrollView menuScrollView;

	private LinearLayout mainRouteBadgeContainer;
	private LinearLayout nearbyRoutesLayout;
	private LinearLayout routesBadgesContainer;
	private GridView localTransportStopRoutesGrid;
	private GridView nearbyTransportStopRoutesGrid;
	private TextView nearbyRoutesWithinTv;
	private TextView localRoutesMoreTv;

	private View zoomButtonsView;
	private ImageButton zoomInButtonView;
	private ImageButton zoomOutButtonView;

	private MapContextMenu menu;
	private OnLayoutChangeListener containerLayoutListener;
	private boolean forceUpdateLayout;

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

	private int markerPaddingPx;
	private int markerPaddingXPx;
	private int topScreenPosY;
	private int bottomToolbarPosY;
	private int minHalfY;
	private int shadowHeight;
	private int zoomPaddingTop;

	private OsmandMapTileView map;
	private LatLon mapCenter;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;
	private boolean moving;
	private boolean nightMode;
	private boolean centered;
	private boolean initLayout = true;
	private boolean wasDrawerDisabled;
	private boolean zoomIn;

	private int screenOrientation;
	private boolean created;
	
	private boolean transportBadgesCreated;

	private UpdateLocationViewCache updateLocationViewCache;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}

		processScreenHeight(container);

		menu = mapActivity.getContextMenu();
		OsmandApplication app = mapActivity.getMyApplication();
		updateLocationViewCache = app.getUIUtilities().getUpdateLocationViewCache();

		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);
		shadowHeight = dpToPx(SHADOW_HEIGHT_TOP_DP);
		topScreenPosY = addStatusBarHeightIfNeeded(-shadowHeight);
		bottomToolbarPosY = addStatusBarHeightIfNeeded(getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar));
		minHalfY = viewHeight - (int) (viewHeight * menu.getHalfScreenMaxHeightKoef());
		zoomPaddingTop = dpToPx(ZOOM_PADDING_TOP_DP);

		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);
		if (!menu.isActive()) {
			return view;
		}
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);

		nightMode = menu.isNightMode();
		mainView = view.findViewById(R.id.context_menu_main);

		toolbarContainer = view.findViewById(R.id.context_menu_toolbar_container);
		toolbarView = view.findViewById(R.id.context_menu_toolbar);
		toolbarBackButton = view.findViewById(R.id.context_menu_toolbar_back);
		toolbarTextView = (TextView) view.findViewById(R.id.context_menu_toolbar_text);
		updateVisibility(toolbarContainer, 0);
		toolbarBackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openMenuHeaderOnly();
			}
		});

		topButtonContainer = view.findViewById(R.id.context_menu_top_button_container);
		view.findViewById(R.id.context_menu_top_back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openMenuHeaderOnly();
			}
		});
		updateVisibility(topButtonContainer, 0);

		map = mapActivity.getMapView();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			double markerLat = menu.getLatLon().getLatitude();
			double markerLon = menu.getLatLon().getLongitude();
			origMarkerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
			origMarkerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}

		// Left title button
		final View leftTitleButtonView = view.findViewById(R.id.title_button_view);
		leftTitleButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController leftTitleButtonController = menu.getLeftTitleButtonController();
				if (leftTitleButtonController != null) {
					leftTitleButtonController.buttonPressed();
				}
			}
		});

		// Right title button
		final View rightTitleButtonView = view.findViewById(R.id.title_button_right_view);
		rightTitleButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController rightTitleButtonController = menu.getRightTitleButtonController();
				if (rightTitleButtonController != null) {
					rightTitleButtonController.buttonPressed();
				}
			}
		});

		// Left download button
		final View leftDownloadButtonView = view.findViewById(R.id.download_button_left_view);
		leftDownloadButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController leftDownloadButtonController = menu.getLeftDownloadButtonController();
				if (leftDownloadButtonController != null) {
					leftDownloadButtonController.buttonPressed();
				}
			}
		});

		// Right download button
		final View rightDownloadButtonView = (View) view.findViewById(R.id.download_button_right_view);
		rightDownloadButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController rightDownloadButtonController = menu.getRightDownloadButtonController();
				if (rightDownloadButtonController != null) {
					rightDownloadButtonController.buttonPressed();
				}
			}
		});

		// Bottom title button
		final View bottomTitleButtonView = view.findViewById(R.id.title_button_bottom_view);
		bottomTitleButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleButtonController bottomTitleButtonController = menu.getBottomTitleButtonController();
				if (bottomTitleButtonController != null) {
					bottomTitleButtonController.buttonPressed();
				}
			}
		});

		// Progress bar
		final ImageView progressButton = (ImageView) view.findViewById(R.id.progressButton);
		progressButton.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark, R.color.ctx_menu_buttons_icon_color));
		progressButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TitleProgressController titleProgressController = menu.getTitleProgressController();
				if (titleProgressController != null) {
					titleProgressController.buttonPressed();
				}
			}
		});

		menu.updateData();
		updateButtonsAndProgress();

		if (menu.isLandscapeLayout()) {
			final TypedValue typedValueAttr = new TypedValue();
			mapActivity.getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
			View fabContainer = view.findViewById(R.id.context_menu_fab_container);
			fabContainer.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

		runLayoutListener();

		final GestureDetector singleTapDetector = new GestureDetector(view.getContext(), new SingleTapConfirm());
		final GestureDetector swipeDetector = new GestureDetector(view.getContext(), new HorizontalSwipeConfirm(true));

		final View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
			private float dy;
			private float dyMain;
			private float mDownY;

			private int minimumVelocity;
			private int maximumVelocity;
			private VelocityTracker velocityTracker;
			private OverScroller scroller;

			private boolean slidingUp;
			private boolean slidingDown;

			private boolean hasMoved;

			{
				scroller = new OverScroller(getContext());
				final ViewConfiguration configuration = ViewConfiguration.get(getContext());
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

							final VelocityTracker velocityTracker = this.velocityTracker;
							velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
							int initialVelocity = (int) velocityTracker.getYVelocity();

							if ((Math.abs(initialVelocity) > minimumVelocity)) {

								scroller.abortAnimation();
								scroller.fling(0, currentY, 0, initialVelocity, 0, 0,
										Math.min(viewHeight - menuFullHeightMax, getFullScreenTopPosY()),
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

		((InterceptorLinearLayout) mainView).setListener(slideTouchListener);
		mainView.setOnTouchListener(slideTouchListener);

		buildHeader();

		((TextView) view.findViewById(R.id.context_menu_line1)).setTextColor(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light));
		View menuLine2 = view.findViewById(R.id.context_menu_line2);
		if (menuLine2 != null) {
			((TextView) menuLine2).setTextColor(ContextCompat.getColor(mapActivity, R.color.ctx_menu_subtitle_color));
		}
		((TextView) view.findViewById(R.id.distance)).setTextColor(ContextCompat.getColor(mapActivity,
				nightMode ? R.color.ctx_menu_direction_color_dark : R.color.ctx_menu_direction_color_light));

		AndroidUtils.setTextSecondaryColor(mapActivity,
				(TextView) view.findViewById(R.id.progressTitle), nightMode);

		// Zoom buttons
		zoomButtonsView = view.findViewById(R.id.context_menu_zoom_buttons);
		zoomInButtonView = (ImageButton) view.findViewById(R.id.context_menu_zoom_in_button);
		zoomOutButtonView = (ImageButton) view.findViewById(R.id.context_menu_zoom_out_button);
		if (menu.zoomButtonsVisible()) {
			AndroidUtils.updateImageButton(mapActivity, zoomInButtonView, R.drawable.map_zoom_in, R.drawable.map_zoom_in_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			AndroidUtils.updateImageButton(mapActivity, zoomOutButtonView, R.drawable.map_zoom_out, R.drawable.map_zoom_out_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			zoomInButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.zoomInPressed();
				}
			});
			zoomOutButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.zoomOutPressed();
				}
			});
			zoomButtonsView.setVisibility(View.VISIBLE);
		} else {
			zoomButtonsView.setVisibility(View.GONE);
		}

		localTransportStopRoutesGrid = (GridView) view.findViewById(R.id.transport_stop_routes_grid);
		nearbyTransportStopRoutesGrid = (GridView) view.findViewById(R.id.transport_stop_nearby_routes_grid);
		nearbyRoutesWithinTv = (TextView) view.findViewById(R.id.nearby_routes_within_text_view);
		localRoutesMoreTv = (TextView) view.findViewById(R.id.local_routes_more_text_view);
		nearbyRoutesLayout = (LinearLayout) view.findViewById(R.id.nearby_routes);
		routesBadgesContainer = (LinearLayout) view.findViewById(R.id.transport_badges_container);
		mainRouteBadgeContainer = (LinearLayout) view.findViewById(R.id.main_transport_route_badge);

		if (nightMode) {
			nearbyRoutesWithinTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_dark));
			localRoutesMoreTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_dark));
		} else {
			nearbyRoutesWithinTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_light));
			localRoutesMoreTv.setTextColor(ContextCompat.getColor(mapActivity, R.color.text_color_secondary_light));
		}

		View buttonsBottomBorder = view.findViewById(R.id.buttons_bottom_border);
		View buttonsTopBorder = view.findViewById(R.id.buttons_top_border);
		buttonsBottomBorder.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.ctx_menu_buttons_divider_dark : R.color.ctx_menu_buttons_divider_light));
		buttonsTopBorder.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.ctx_menu_buttons_divider_dark : R.color.ctx_menu_buttons_divider_light));
		View buttons = view.findViewById(R.id.context_menu_buttons);
		buttons.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light));
		if (!menu.buttonsVisible()) {
			buttonsTopBorder.setVisibility(View.GONE);
			buttons.setVisibility(View.GONE);
		}
		View bottomButtons = view.findViewById(R.id.context_menu_bottom_buttons);
		bottomButtons.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light));
		if (!menu.navigateButtonVisible()) {
			bottomButtons.findViewById(R.id.context_menu_directions_button).setVisibility(View.GONE);
		}

		// Action buttons
		final ImageView imageFavorite = (ImageView) view.findViewById(R.id.context_menu_fav_image_view);
		imageFavorite.setImageDrawable(getIcon(menu.getFavActionIconId(),
				R.color.ctx_menu_buttons_icon_color));
		((TextView) view.findViewById(R.id.context_menu_fav_text_view)).setText(menu.getFavActionStringId());
		View favView = view.findViewById(R.id.context_menu_fav_view);
		if (menu.isFavButtonEnabled()) {
			favView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.buttonFavoritePressed();
				}
			});
		} else {
			deactivate(favView);
		}

		final ImageView imageWaypoint = (ImageView) view.findViewById(R.id.context_menu_route_image_view);
		imageWaypoint.setImageDrawable(getIcon(menu.getWaypointActionIconId(),
				R.color.ctx_menu_buttons_icon_color));
		((TextView) view.findViewById(R.id.context_menu_route_text_view)).setText(menu.getWaypointActionStringId());
		View waypointView = view.findViewById(R.id.context_menu_route_view);
		if (menu.isButtonWaypointEnabled()) {
			waypointView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.buttonWaypointPressed();
				}
			});
		} else {
			deactivate(waypointView);
		}

		final ImageView imageShare = (ImageView) view.findViewById(R.id.context_menu_share_image_view);
		imageShare.setImageDrawable(getIcon(R.drawable.map_action_gshare_dark,
				R.color.ctx_menu_buttons_icon_color));
		View shareView = view.findViewById(R.id.context_menu_share_view);
		shareView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonSharePressed();
			}
		});

		final ImageView imageMore = (ImageView) view.findViewById(R.id.context_menu_more_image_view);
		imageMore.setImageDrawable(getIcon(R.drawable.map_overflow_menu_white,
				R.color.ctx_menu_buttons_icon_color));
		View moreView = view.findViewById(R.id.context_menu_more_view);
		moreView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonMorePressed();
			}
		});

		//Bottom buttons
		int bottomButtonsColor = nightMode ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n;
		TextView detailsButton = (TextView) view.findViewById(R.id.context_menu_details_button);
		detailsButton.setTextColor(ContextCompat.getColor(mapActivity, bottomButtonsColor));
		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openMenuHalfScreen();
			}
		});
		TextView directionsButton = (TextView) view.findViewById(R.id.context_menu_directions_button);
		int iconResId = R.drawable.map_directions;
		if (menu.navigateInPedestrianMode()) {
			iconResId = R.drawable.map_action_pedestrian_dark;
		}
		Drawable drawable = getIcon(iconResId, bottomButtonsColor);
		directionsButton.setTextColor(ContextCompat.getColor(mapActivity, bottomButtonsColor));
		directionsButton.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
		directionsButton.setCompoundDrawablePadding(dpToPx(8));
		directionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				menu.navigateButtonPressed();
			}
		});

		buildBottomView();

		LockableScrollView bottomScrollView = (LockableScrollView) view.findViewById(R.id.context_menu_bottom_scroll);
		bottomScrollView.setScrollingEnabled(false);
		bottomScrollView.setBackgroundColor(getResources()
				.getColor(nightMode ? R.color.ctx_menu_bottom_view_bg_dark : R.color.ctx_menu_bottom_view_bg_light));
		view.findViewById(R.id.context_menu_bottom_view).setBackgroundColor(getResources()
				.getColor(nightMode ? R.color.ctx_menu_bottom_view_bg_dark : R.color.ctx_menu_bottom_view_bg_light));

		//getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(false);

		containerLayoutListener = new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View view, int left, int top, int right, int bottom,
									   int oldLeft, int oldTop, int oldRight, int oldBottom) {
				if (!transportBadgesCreated) {
					createTransportBadges();
				}
				if (forceUpdateLayout || bottom != oldBottom) {
					forceUpdateLayout = false;
					processScreenHeight(view.getParent());
					runLayoutListener();
				}
			}
		};

		created = true;
		return view;
	}

	@Nullable
	private TransportStopRouteAdapter createTransportStopRouteAdapter(List<TransportStopRoute> routes, boolean needMoreItem) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return null;
		}
		List<Object> items = new ArrayList<Object>(routes);
		if (needMoreItem) {
			items.add(TRANSPORT_BADGE_MORE_ITEM);
		}
		final TransportStopRouteAdapter adapter = new TransportStopRouteAdapter(app, items, nightMode);
		adapter.setListener(new TransportStopRouteAdapter.OnClickListener() {
			@Override
			public void onClick(int position) {
				Object object = adapter.getItem(position);
				MapActivity mapActivity = getMapActivity();
				if (object != null && mapActivity != null) {
					OsmandApplication app = mapActivity.getMyApplication();
					if (object instanceof TransportStopRoute) {
						TransportStopRoute route = (TransportStopRoute) object;
						PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
								route.getDescription(app, false));
						menu.show(menu.getLatLon(), pd, route);
						TransportStopsLayer stopsLayer = mapActivity.getMapLayers().getTransportStopsLayer();
						stopsLayer.setRoute(route);
						int cz = route.calculateZoom(0, mapActivity.getMapView().getCurrentRotatedTileBox());
						mapActivity.changeZoom(cz - mapActivity.getMapView().getZoom());
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
			}
		});
		return adapter;
	}

	private List<TransportStopRoute> filterTransportRoutes(List<TransportStopRoute> routes) {
		List<TransportStopRoute> filteredRoutes = new ArrayList<>();
		for (TransportStopRoute route : routes) {
			if (!containsRef(filteredRoutes, route.route)) {
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
			if (!containsRef(filterFromRoutes, route.route)) {
				filteredRoutes.add(route);
			}
		}
		return filteredRoutes;
	}

	private boolean containsRef(List<TransportStopRoute> routes, TransportRoute transportRoute) {
		for (TransportStopRoute route : routes) {
			if (route.route.getRef().equals(transportRoute.getRef())) {
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
		} else  if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	private void updateVisibility(View v, boolean visible) {
		if (visible && v.getVisibility() != View.VISIBLE) {
			v.setVisibility(View.VISIBLE);
		} else  if (!visible && v.getVisibility() == View.VISIBLE) {
			v.setVisibility(View.INVISIBLE);
		}
	}

	private void toggleDetailsHideButton() {
		int menuState = menu.getCurrentMenuState();
		final boolean showShowHideButton = menuState == MenuState.HALF_SCREEN || (!menu.isLandscapeLayout() && menuState == MenuState.FULL_SCREEN);
		TextView detailsButton = (TextView) view.findViewById(R.id.context_menu_details_button);
		detailsButton.setText(showShowHideButton ? R.string.shared_string_collapse : R.string.rendering_category_details);
		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (showShowHideButton) {
					openMenuHeaderOnly();
				} else {
					openMenuHalfScreen();
				}
			}
		});
	}

	private void deactivate(View view) {
		view.setEnabled(false);
		view.setAlpha(0.5f);
	}

	@Override
	public int getStatusBarColorId() {
		if (menu != null && (menu.getCurrentMenuState() == MenuState.FULL_SCREEN || menu.isLandscapeLayout())) {
			return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_route_light;
		}
		return -1;
	}

	private void updateImageButton(ImageButton button, int iconLightId, int iconDarkId, int bgLightId, int bgDarkId, boolean night) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			button.setImageDrawable(mapActivity.getMyApplication().getUIUtilities().getIcon(night ? iconDarkId : iconLightId));
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				button.setBackground(mapActivity.getResources().getDrawable(night ? bgDarkId : bgLightId,
						mapActivity.getTheme()));
			} else {
				button.setBackgroundDrawable(mapActivity.getResources().getDrawable(night ? bgDarkId : bgLightId));
			}
		}
	}

	private void processScreenHeight(ViewParent parent) {
		View container = (View) parent;
		screenHeight = container.getHeight() + AndroidUtils.getStatusBarHeight(container.getContext());
		viewHeight = screenHeight - AndroidUtils.getStatusBarHeight(container.getContext());
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
		if (map != null && map.hasCustomMapRatio()) {
			map.restoreMapRatio();
		}
	}

	private void setCustomMapRatio() {
		LatLon latLon = menu.getLatLon();
		RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
		float px = tb.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		float py = tb.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
		float ratioX = px / tb.getPixWidth();
		float ratioY = py / tb.getPixHeight();
		map.setCustomMapRatio(ratioX, ratioY);
		map.setLatLon(latLon.getLatitude(), latLon.getLongitude());
	}

	public void doZoomIn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
			boolean containsLatLon = tb.containsLatLon(menu.getLatLon());
			if (!containsLatLon) {
				restoreCustomMapRatio();
			}
			if (map.isZooming() && (map.hasCustomMapRatio() || !containsLatLon)) {
				mapActivity.changeZoom(2, System.currentTimeMillis());
			} else {
				if (containsLatLon) {
					setCustomMapRatio();
				}
				mapActivity.changeZoom(1, System.currentTimeMillis());
			}
		}
	}

	public void doZoomOut() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = map.getCurrentRotatedTileBox().copy();
			boolean containsLatLon = tb.containsLatLon(menu.getLatLon());
			if (containsLatLon) {
				setCustomMapRatio();
			} else {
				restoreCustomMapRatio();
			}
			mapActivity.changeZoom(-1, System.currentTimeMillis());
		}
	}

	private void applyPosY(final int currentY, final boolean needCloseMenu, boolean needMapAdjust,
						   final int previousMenuState, final int newMenuState, int dZoom) {
		final int posY = getPosY(currentY, needCloseMenu, previousMenuState);
		if (getViewY() != posY || dZoom != 0) {
			if (posY < getViewY()) {
				updateMainViewLayout(posY);
			}

			final float topButtonAlpha = getTopButtonAlpha(posY);
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

			final float toolbarAlpha = getToolbarAlpha(posY);
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

						boolean canceled = false;

						@Override
						public void onAnimationCancel(Animator animation) {
							canceled = true;
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							if (!canceled) {
								if (needCloseMenu) {
									menu.close();
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
			final View titleButtonsContainer = view.findViewById(R.id.title_button_container);
			titleButtonsContainer.setVisibility(showTitleButtonsContainer ? View.VISIBLE : View.GONE);

			// Left title button
			final View leftTitleButtonView = view.findViewById(R.id.title_button_view);
			final TextView leftTitleButton = (TextView) leftTitleButtonView.findViewById(R.id.button_text);
			if (leftTitleButtonController != null) {
				SpannableStringBuilder title = new SpannableStringBuilder(leftTitleButtonController.caption);
				if (leftTitleButtonController.needRightText) {
					int startIndex = title.length();
					title.append(" ").append(leftTitleButtonController.rightTextCaption);
					Context context = view.getContext();
					Typeface typeface = FontCache.getRobotoRegular(context);
					title.setSpan(new CustomTypefaceSpan(typeface), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					title.setSpan(new ForegroundColorSpan(
							ContextCompat.getColor(context, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light)),
							startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				setupButton(leftTitleButtonView, leftTitleButtonController.enabled, title);
				if (leftTitleButtonController.visible) {
					leftTitleButtonView.setVisibility(View.VISIBLE);
					Drawable leftIcon = leftTitleButtonController.getLeftIcon();
					Drawable rightIcon = leftTitleButtonController.getRightIcon();
					leftTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
					leftTitleButton.setCompoundDrawablePadding(dpToPx(8f));
					((LinearLayout) leftTitleButtonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
				} else {
					leftTitleButtonView.setVisibility(View.INVISIBLE);
				}
			} else {
				leftTitleButtonView.setVisibility(View.INVISIBLE);
			}

			// Right title button
			final View rightTitleButtonView = view.findViewById(R.id.title_button_right_view);
			final TextView rightTitleButton = (TextView) rightTitleButtonView.findViewById(R.id.button_text);
			if (rightTitleButtonController != null) {
				setupButton(rightTitleButtonView, rightTitleButtonController.enabled, rightTitleButtonController.caption);
				rightTitleButtonView.setVisibility(rightTitleButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable leftIcon = rightTitleButtonController.getLeftIcon();
				Drawable rightIcon = rightTitleButtonController.getRightIcon();
				rightTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
				rightTitleButton.setCompoundDrawablePadding(dpToPx(8f));
				((LinearLayout) rightTitleButtonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
			} else {
				rightTitleButtonView.setVisibility(View.INVISIBLE);
			}

			// Bottom title button
			final View bottomTitleButtonView = view.findViewById(R.id.title_button_bottom_view);
			final TextView bottomTitleButton = (TextView) bottomTitleButtonView.findViewById(R.id.button_text);
			if (bottomTitleButtonController != null) {
				setupButton(bottomTitleButtonView, bottomTitleButtonController.enabled, bottomTitleButtonController.caption);
				bottomTitleButtonView.setVisibility(bottomTitleButtonController.visible ? View.VISIBLE : View.GONE);

				Drawable leftIcon = bottomTitleButtonController.getLeftIcon();
				Drawable rightIcon = bottomTitleButtonController.getRightIcon();
				bottomTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
				bottomTitleButton.setCompoundDrawablePadding(dpToPx(8f));
				((LinearLayout) bottomTitleButtonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
			} else {
				bottomTitleButtonView.setVisibility(View.GONE);
			}

			// Download buttons
			boolean showDownloadButtonsContainer =
					((leftDownloadButtonController != null && leftDownloadButtonController.visible)
							|| (rightDownloadButtonController != null && rightDownloadButtonController.visible))
							&& (titleProgressController == null || !titleProgressController.visible);
			final View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
			downloadButtonsContainer.setVisibility(showDownloadButtonsContainer ? View.VISIBLE : View.GONE);

			// Left download button
			final View leftDownloadButtonView = view.findViewById(R.id.download_button_left_view);
			final TextView leftDownloadButton = (TextView) leftDownloadButtonView.findViewById(R.id.button_text);
			if (leftDownloadButtonController != null) {
				setupButton(leftDownloadButtonView, leftDownloadButtonController.enabled, leftDownloadButtonController.caption);
				leftDownloadButtonView.setVisibility(leftDownloadButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable leftIcon = leftDownloadButtonController.getLeftIcon();
				Drawable rightIcon = leftDownloadButtonController.getRightIcon();
				leftDownloadButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
				leftDownloadButton.setCompoundDrawablePadding(dpToPx(8f));
				((LinearLayout) leftDownloadButtonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
			} else {
				leftDownloadButtonView.setVisibility(View.INVISIBLE);
			}

			// Right download button
			final View rightDownloadButtonView = view.findViewById(R.id.download_button_right_view);
			final TextView rightDownloadButton = (TextView) rightDownloadButtonView.findViewById(R.id.button_text);
			if (rightDownloadButtonController != null) {
				setupButton(rightDownloadButtonView, rightDownloadButtonController.enabled, rightDownloadButtonController.caption);
				rightDownloadButtonView.setVisibility(rightDownloadButtonController.visible ? View.VISIBLE : View.INVISIBLE);

				Drawable leftIcon = rightDownloadButtonController.getLeftIcon();
				Drawable rightIcon = rightDownloadButtonController.getRightIcon();
				rightDownloadButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
				rightDownloadButton.setCompoundDrawablePadding(dpToPx(8f));
				((LinearLayout) rightDownloadButtonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
			} else {
				rightDownloadButtonView.setVisibility(View.INVISIBLE);
			}

			final LinearLayout additionalButtonsContainer = (LinearLayout) view.findViewById(R.id.additional_buttons_container);
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
			final View titleProgressContainer = view.findViewById(R.id.title_progress_container);
			if (titleProgressController != null) {
				titleProgressContainer.setVisibility(titleProgressController.visible ? View.VISIBLE : View.GONE);
				if (titleProgressController.visible && showTitleButtonsContainer) {
					LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams) titleProgressContainer.getLayoutParams();
					if (ll.topMargin != 0) {
						ll.setMargins(0, 0, 0, 0);
					}
				}

				final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
				final TextView progressTitle = (TextView) view.findViewById(R.id.progressTitle);
				progressTitle.setText(titleProgressController.caption);
				progressBar.setIndeterminate(titleProgressController.indeterminate);
				progressBar.setProgress(titleProgressController.progress);
				progressBar.setVisibility(titleProgressController.progressVisible ? View.VISIBLE : View.GONE);

				final ImageView progressButton = (ImageView) view.findViewById(R.id.progressButton);
				progressButton.setVisibility(titleProgressController.buttonVisible ? View.VISIBLE : View.GONE);
			} else {
				titleProgressContainer.setVisibility(View.GONE);
			}
			updateAdditionalInfoVisibility();
		}
	}

	private void attachButtonsRow(ViewGroup container, final TitleButtonController leftButtonController, final TitleButtonController rightButtonController) {
		ContextThemeWrapper ctx = new ContextThemeWrapper(getMapActivity(), !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(R.layout.context_menu_buttons, container, false);

		// Left button
		final View leftButtonView = view.findViewById(R.id.additional_button_left_view);
		final TextView leftButton = (TextView) leftButtonView.findViewById(R.id.button_text);
		fillButtonInfo(leftButtonController, leftButtonView, leftButton);

		// Right button
		final View rightButtonView = view.findViewById(R.id.additional_button_right_view);
		final TextView rightButton = (TextView) rightButtonView.findViewById(R.id.button_text);
		fillButtonInfo(rightButtonController, rightButtonView, rightButton);

		container.addView(view);
	}

	private void fillButtonInfo(final TitleButtonController buttonController, View buttonView, TextView buttonText) {
		if (buttonController != null) {
			setupButton(buttonView, buttonController.enabled, buttonController.caption);
			buttonView.setVisibility(buttonController.visible ? View.VISIBLE : View.INVISIBLE);

			Drawable leftIcon = buttonController.getLeftIcon();
			Drawable rightIcon = buttonController.getRightIcon();
			buttonText.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, rightIcon, null);
			buttonText.setCompoundDrawablePadding(dpToPx(8f));
			((LinearLayout) buttonView).setGravity(rightIcon != null ? Gravity.END : Gravity.START);
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

	private void buildHeader() {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
			final ImageView iconView = (ImageView) view.findViewById(R.id.context_menu_icon_view);
			Drawable icon = menu.getRightIcon();
			int iconId = menu.getRightIconId();

			int sizeId = menu.isBigRightIcon() ? R.dimen.context_menu_big_icon_size : R.dimen.map_widget_icon;
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
			View bottomView = view.findViewById(R.id.context_menu_bottom_view);
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
			if (!menu.isActive() || (mapActivity.getMapRouteInfoMenu().isVisible()) || MapRouteInfoMenu.waypointsVisible) {
				dismissMenu();
				return;
			}
			if (MapRouteInfoMenu.chooseRoutesVisible) {
				mapActivity.getChooseRouteFragment().dismiss();
			}
			updateLocationViewCache = mapActivity.getMyApplication().getUIUtilities().getUpdateLocationViewCache();
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
			menu.updateControlsVisibility(true);
			menu.onFragmentResume();
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
		}
	}

	@Override
	public void onPause() {
		if (view != null) {
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
		menu.setMapCenter(null);
		menu.setMapZoom(0);
	}

	public void rebuildMenu(boolean centered) {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
			final ImageView buttonFavorite = (ImageView) view.findViewById(R.id.context_menu_fav_image_view);
			buttonFavorite.setImageDrawable(getIcon(menu.getFavActionIconId(), R.color.ctx_menu_buttons_icon_color));
			String favActionString = getString(menu.getFavActionStringId());
			((TextView) view.findViewById(R.id.context_menu_fav_text_view)).setText(favActionString);

			buildHeader();

			LinearLayout bottomLayout = (LinearLayout) view.findViewById(R.id.context_menu_bottom_view);
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
			OsmandApplication app = mapActivity.getMyApplication();
			convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
			if (transportStopRoute != null) {
				String routeDescription = transportStopRoute.getDescription(app);
				String routeRef = transportStopRoute.route.getAdjustedRouteRef(true);
				int bgColor = transportStopRoute.getColor(app, nightMode);

				TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
				ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

				int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus : transportStopRoute.type.getResourceId();
				transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, UiUtilities.getContrastColor(mapActivity, bgColor, true)));
				transportStopRouteTextView.setText(routeRef + ": " + routeDescription);
				GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
				gradientDrawableBg.setColor(bgColor);
				transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(mapActivity, bgColor, true));
			}
		}
		return convertView;
	}

	public void fitRectOnMap(QuadRect rect) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx;
			if (menu.isLandscapeLayout()) {
				tileBoxWidthPx = tb.getPixWidth() - mainView.getWidth();
				tileBoxHeightPx = viewHeight;
			} else {
				tileBoxHeightPx = viewHeight - menuFullHeight;
			}
			if (tileBoxHeightPx > 0 || tileBoxWidthPx > 0) {
				int topMarginPx = AndroidUtils.getStatusBarHeight(mapActivity);
				int leftMarginPx = mainView.getWidth();
				restoreCustomMapRatio();
				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, topMarginPx, leftMarginPx);
			}
		}
	}

	private void updateLocalRoutesBadges(List<TransportStopRoute> localTransportStopRoutes, int localColumnsPerRow) {
		int localRoutesSize = localTransportStopRoutes.size();
		OsmandApplication app = requireMyApplication();
		TransportRouteResult activeRoute = app.getRoutingHelper().getTransportRoutingHelper().getActiveRoute();
		if (localRoutesSize > 0 && activeRoute != null) {
			for (int i = 0; i < localTransportStopRoutes.size(); i++) {
				final TransportStopRoute stopRoute = localTransportStopRoutes.get(i);
				if (activeRoute.isRouteStop(stopRoute.stop)) {
					View routeBadge = createRouteBadge(stopRoute);
					mainRouteBadgeContainer.addView(routeBadge);
					mainRouteBadgeContainer.setVisibility(View.VISIBLE);
					mainRouteBadgeContainer.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismissMenu();
							ChooseRouteFragment.showInstance(requireMyActivity().getSupportFragmentManager(),
									requireMyApplication().getRoutingHelper().getTransportRoutingHelper().getCurrentRoute(),
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
						}
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
					+ OsmAndFormatter.getFormattedDistance(TransportStopController.SHOW_STOPS_RADIUS_METERS, getMyApplication()) + ":";
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
			double gridPadding = getResources().getDimension(R.dimen.context_menu_padding_margin_default);
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
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void runLayoutListener() {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {

					if (view != null) {
						ViewTreeObserver obs = view.getViewTreeObserver();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							obs.removeOnGlobalLayoutListener(this);
						} else {
							obs.removeGlobalOnLayoutListener(this);
						}

						if (getActivity() == null) {
							return;
						}

						int newMenuTopViewHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
						int newMenuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
						menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();
						zoomButtonsHeight = zoomButtonsView.getHeight();

						int dy = 0;
						if (!menu.isLandscapeLayout()) {
							TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
							TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
							int line2LineCount = 0;
							int line2LineHeight = 0;
							int line2MeasuredHeight = 0;
							if (line2 != null) {
								line2LineCount = line2.getLineCount();
								line2LineHeight = line2.getLineHeight();
								line2MeasuredHeight = line2.getMeasuredHeight();
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
										+ titleButtonHeight + downloadButtonsHeight
										+ titleBottomButtonHeight + additionalButtonsHeight + titleProgressHeight + line3Height;
								dy = Math.max(0, newMenuTopViewHeight - menuTopViewHeight
										- (newMenuTopShadowAllHeight - menuTopShadowAllHeight));
							} else {
								menuTopViewHeightExcludingTitle = newMenuTopViewHeight - line1.getMeasuredHeight() - line2MeasuredHeight
										- titleButtonHeight - downloadButtonsHeight - titleBottomButtonHeight - additionalButtonsHeight - titleProgressHeight-line3Height;
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
		cp.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
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
		thread.startMoving(calcLatLon.getLatitude(), calcLatLon.getLongitude(), zoom, true);
	}

	private void setAddressLocation() {
		if (view != null) {
			// Text line 1
			TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
			line1.setText(menu.getTitleStr());
			toolbarTextView.setText(menu.getTitleStr());

			// Text line 2
			LinearLayout line2layout = (LinearLayout) view.findViewById(R.id.context_menu_line2_layout);
			TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
			if (menu.hasCustomAddressLine()) {
				line2layout.removeAllViews();
				menu.buildCustomAddressLine(line2layout);
			} else {
				String typeStr = menu.getTypeStr();
				String streetStr = menu.getStreetStr();
				StringBuilder line2Str = new StringBuilder();
				if (!Algorithms.isEmpty(typeStr)) {
					line2Str.append(typeStr);
					Drawable icon = menu.getTypeIcon();
					line2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
					line2.setCompoundDrawablePadding(dpToPx(5f));
				}
				if (!Algorithms.isEmpty(streetStr) && !menu.displayStreetNameInTitle()) {
					if (line2Str.length() > 0) {
						line2Str.append(": ");
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

			TextView line3 = (TextView) view.findViewById(R.id.context_menu_line3);
			String subtypeStr = menu.getSubtypeStr();
			if (TextUtils.isEmpty(subtypeStr)) {
				line3.setVisibility(View.GONE);
			} else {
				line3.setVisibility(View.VISIBLE);
				line3.setText(subtypeStr);
				Drawable icon = menu.getSubtypeIcon();
				line3.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				line3.setCompoundDrawablePadding(dpToPx(5f));
			}

			ImageView additionalInfoImageView = (ImageView) view.findViewById(R.id.additional_info_image_view);
			TextView additionalInfoTextView = (TextView) view.findViewById(R.id.additional_info_text_view);
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
			} else {
				additionalInfoTextView.setVisibility(View.GONE);
			}
			additionalInfoImageView.setVisibility(showAdditionalImage ? View.VISIBLE : View.GONE);

			boolean showSeparator = showAdditionalInfo && menu.displayDistanceDirection();
			view.findViewById(R.id.info_compass_separator)
					.setVisibility(showSeparator ? View.VISIBLE : View.GONE);
		}
		updateCompassVisibility();
		updateAdditionalInfoVisibility();
	}

	private void updateCompassVisibility() {
		OsmandApplication app = getMyApplication();
		if (app != null && view != null) {
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
		View titleButtonContainer = view.findViewById(R.id.title_button_container);
		View downloadButtonsContainer = view.findViewById(R.id.download_buttons_container);
		View titleBottomButtonContainer = view.findViewById(R.id.title_bottom_button_container);
		View titleProgressContainer = view.findViewById(R.id.title_progress_container);

		if (line3.getVisibility() == View.GONE
				&& additionalInfoImageView.getVisibility() == View.GONE
				&& additionalInfoTextView.getVisibility() == View.GONE
				&& compassView.getVisibility() == View.INVISIBLE
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
		OsmandApplication app = getMyApplication();
		FragmentActivity activity = getActivity();
		if (app != null && activity != null && view != null) {
			TextView distanceText = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			app.getUIUtilities().updateLocationView(updateLocationViewCache, direction, distanceText, menu.getLatLon());
		}
	}

	private void updateZoomButtonsVisibility(int menuState) {
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

	private int getPosY(final int currentY, boolean needCloseMenu) {
		return getPosY(currentY, needCloseMenu, 0);
	}

	private int getPosY(final int currentY, boolean needCloseMenu, int previousState) {
		if (needCloseMenu) {
			return screenHeight;
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
		if (Build.VERSION.SDK_INT >= 21 && mapActivity != null) {
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
		} else {
			customMapCenter = false;
		}
	}

	private void adjustMapPosition(int y, boolean animated, boolean center, int dZoom) {
		map.getAnimatedDraggingThread().stopAnimatingSync();
		int zoom = getZoom() + dZoom;
		LatLon latlon = getAdjustedMarkerLocation(y, menu.getLatLon(), center, zoom);

		if (map.hasCustomMapRatio()
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
		box.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
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
		return y - zoomButtonsHeight - shadowHeight - zoomPaddingTop;
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
			try {
				activity.getSupportFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
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

	@Nullable
	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public static boolean showInstance(final MapContextMenu menu, final MapActivity mapActivity,
									   final boolean centered) {
		try {

			if (menu.getLatLon() == null || mapActivity == null || mapActivity.isActivityDestroyed()) {
				return false;
			}

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
			mapActivity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	//DownloadEvents
	@Override
	public void newDownloadIndexes() {
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

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
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
}

