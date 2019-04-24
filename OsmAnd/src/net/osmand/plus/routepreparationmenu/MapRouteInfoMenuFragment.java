package net.osmand.plus.routepreparationmenu;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.widgets.TextViewExProgress;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapRouteInfoMenuFragment extends ContextMenuFragment {
	public static final String TAG = MapRouteInfoMenuFragment.class.getName();

	@Nullable
	private MapRouteInfoMenu menu;
	private FrameLayout bottomContainer;
	private View modesLayoutToolbar;
	private View modesLayoutToolbarContainer;
	private View modesLayoutListContainer;
	private View modesLayout;

	private int menuTitleHeight;

	@Override
	public int getMainLayoutId() {
		return R.layout.plan_route_info;
	}

	@Override
	public int getTopViewId() {
		return R.id.route_menu_top_shadow_all;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return true;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	@Override
	public int getInitialMenuState() {
		if (menu != null) {
			if (isPortrait()) {
				return menu.getInitialMenuStatePortrait();
			} else {
				return menu.getInitialMenuState();
			}
		}
		return super.getInitialMenuState();
	}

	@Override
	public int getSupportedMenuStates() {
		if (menu != null) {
			return menu.getSupportedMenuStates();
		}
		return super.getSupportedMenuStates();
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		if (menu != null) {
			return menu.getSupportedMenuStatesPortrait();
		}
		return super.getSupportedMenuStatesPortrait();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		MapActivity mapActivity = requireMapActivity();
		menu = mapActivity.getMapRouteInfoMenu();

		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			bottomContainer = (FrameLayout) view.findViewById(R.id.bottom_container);
			modesLayoutToolbar = view.findViewById(R.id.modes_layout_toolbar);
			modesLayoutToolbarContainer = view.findViewById(R.id.modes_layout_toolbar_container);
			modesLayoutListContainer = view.findViewById(R.id.modes_layout_list_container);
			modesLayout = view.findViewById(R.id.modes_layout);

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});

			buildBottomView();

			if (!isPortrait()) {
				view.findViewById(R.id.app_modes_fold_container).setVisibility(View.GONE);
				int widthNoShadow = getLandscapeNoShadowWidth();
				modesLayoutToolbar.setLayoutParams(new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT));
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
				View appModesView = view.findViewById(R.id.app_modes);
				appModesView.setPadding(0, 0, appModesView.getPaddingRight(), 0);
			}
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		OsmandApplication app = getMyApplication();
		if (app != null) {
			if (menu == null) {
				dismiss();
				return;
			}
			updateInfo();
			View mainView = getMainView();
			if (mainView != null) {
				View progressBar = mainView.findViewById(R.id.progress_bar);
				boolean progressVisible = progressBar != null && progressBar.getVisibility() == View.VISIBLE;
				boolean routeCalculating = app.getRoutingHelper().isRouteBeingCalculated() || app.getTransportRoutingHelper().isRouteBeingCalculated();
				if (progressVisible && !routeCalculating) {
					hideRouteCalculationProgressBar();
					openMenuHalfScreen();
				}
			}
			menu.addTargetPointListener();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (menu != null) {
			menu.onDismiss(getCurrentMenuState());
		}
	}

	private void buildBottomView() {
		LinearLayout cardsContainer = getCardsContainer();
		if (cardsContainer != null && menu != null) {
			menu.build(cardsContainer);
		}
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateToolbar();
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateToolbar();
	}

	@Override
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
				return nightMode ? R.color.dialog_divider_dark : R.color.dialog_divider_light;
			} else {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
			}
		}
		return -1;
	}

	private void updateToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (menu == null || mapActivity == null) {
			return;
		}
		int y = getViewY();
		if (y < getFullScreenTopPosY()) {
			ViewGroup parent = (ViewGroup) modesLayout.getParent();
			if (parent != null && parent != modesLayoutToolbarContainer) {
				parent.removeView(modesLayout);
				((ViewGroup) modesLayoutToolbarContainer).addView(modesLayout);
			}
			modesLayoutToolbar.setVisibility(View.VISIBLE);
		} else {
			ViewGroup parent = (ViewGroup) modesLayout.getParent();
			if (parent != null && parent != modesLayoutListContainer) {
				parent.removeView(modesLayout);
				((ViewGroup) modesLayoutListContainer).addView(modesLayout);
			}
			modesLayoutToolbar.setVisibility(View.GONE);
		}
		mapActivity.updateStatusBarColor();
		menu.updateApplicationModesOptions();
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		super.calculateLayout(view, initLayout);
	}

	private void adjustMapPosition(int y) {
		OsmandApplication app = getMyApplication();
		MapActivity mapActivity = getMapActivity();
		if (menu == null || menu.isSelectFromMapTouch() || app == null || mapActivity == null) {
			return;
		}

		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRoutePlanningMode() && mapActivity.getMapView() != null) {
			QuadRect r = new QuadRect(0, 0, 0, 0);
			if (menu.isTransportRouteCalculated()) {
				TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
				TransportRouteResult result = transportRoutingHelper.getCurrentRouteResult();
				if (result != null) {
					QuadRect transportRouteRect = transportRoutingHelper.getTransportRouteRect(result);
					if (transportRouteRect != null) {
						r = transportRouteRect;
					}
				}
			} else if (rh.isRouteCalculated()) {
				Location lt = rh.getLastProjection();
				if (lt == null) {
					lt = app.getTargetPointsHelper().getPointToStartLocation();
				}
				if (lt == null) {
					lt = app.getLocationProvider().getLastKnownLocation();
				}
				if (lt != null) {
					MapUtils.insetLatLonRect(r, lt.getLatitude(), lt.getLongitude());
				}
				List<Location> list = rh.getCurrentCalculatedRoute();
				for (Location l : list) {
					MapUtils.insetLatLonRect(r, l.getLatitude(), l.getLongitude());
				}
				List<TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
				for (TargetPoint l : targetPoints) {
					MapUtils.insetLatLonRect(r, l.getLatitude(), l.getLongitude());
				}
			}
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(app);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (r.left != 0 && r.right != 0) {
				mapActivity.getMapView().fitRectToMap(r.left, r.right, r.top, r.bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	public void setBottomShadowVisible(boolean visible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && bottomContainer != null) {
			if (visible) {
				AndroidUtils.setForeground(mapActivity, bottomContainer, isNightMode(),
						R.drawable.bg_contextmenu_shadow, R.drawable.bg_contextmenu_shadow);
			} else {
				bottomContainer.setForeground(null);
			}
		}
	}

	public void updateInfo() {
		updateInfo(getView());
	}

	public void updateInfo(View view) {
		if (menu != null && view != null) {
			menu.updateInfo(view);
			applyDayNightMode();
			if (!isMoving()) {
				runLayoutListener();
			}
		}
	}

	public void updateLayout() {
		if (menu != null) {
			runLayoutListener();
		}
	}

	public void updateFromIcon() {
		View mainView = getMainView();
		if (menu != null && mainView != null) {
			menu.updateFromIcon(mainView);
		}
	}

	private boolean isPublicTransportMode() {
		OsmandApplication app = getMyApplication();
		return app != null && app.getRoutingHelper().getAppMode() == ApplicationMode.PUBLIC_TRANSPORT;
	}
	
	public void updateRouteCalculationProgress(int progress) {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (mapActivity == null || mainView == null || view == null) {
			return;
		}
		boolean publicTransportMode = isPublicTransportMode();
		ProgressBar progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);
		if (progressBar != null) {
			if (progress == 0) {
				progressBar.setIndeterminate(publicTransportMode);
			}
			if (progressBar.getVisibility() != View.VISIBLE) {
				progressBar.setVisibility(View.VISIBLE);
			}
			progressBar.setProgress(progress);
		}
		ProgressBar progressBarButton = (ProgressBar) view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			int visibility = publicTransportMode ? View.GONE : View.VISIBLE;
			if (progressBarButton.getVisibility() != visibility) {
				progressBarButton.setVisibility(visibility);
			}
			progressBarButton.setProgress(progress);
		}
		TextViewExProgress textViewExProgress = (TextViewExProgress) view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = progress / 100f;
		textViewExProgress.invalidate();
	}

	public void hideRouteCalculationProgressBar() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (mapActivity == null || mainView == null || view == null) {
			return;
		}

		View progressBar = mainView.findViewById(R.id.progress_bar);
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
		View progressBarButton = view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			progressBarButton.setVisibility(View.GONE);
		}
		TextViewExProgress textViewExProgress = (TextViewExProgress) view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = 1;
	}

	public void show(MapActivity mapActivity) {
		int slideInAnim = 0;
		int slideOutAnim = 0;
		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			slideInAnim = R.anim.slide_in_bottom;
			slideOutAnim = R.anim.slide_out_bottom;
		}
		mapActivity.getSupportFragmentManager()
				.beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.routeMenuContainer, this, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
	}

	public void applyDayNightMode() {
		MapActivity ctx = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (ctx == null || mainView == null || view == null) {
			return;
		}

		boolean nightMode = isNightMode();
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.modes_layout_toolbar_container), nightMode,
				R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerFromDropDown), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.viaLayoutDivider), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerButtons), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.controls_divider), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.app_modes_options_container), nightMode,
				R.drawable.route_info_trans_gradient_light, R.drawable.route_info_trans_gradient_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.app_modes_fold_container), nightMode,
				R.drawable.route_info_trans_gradient_left_light, R.drawable.route_info_trans_gradient_left_dark);

		int activeColor = ContextCompat.getColor(ctx, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		((TextView) view.findViewById(R.id.cancel_button_descr)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.from_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.via_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.to_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.map_options_route_button_title)).setTextColor(activeColor);

		int mainFontColor = ContextCompat.getColor(ctx, nightMode ? R.color.main_font_dark : R.color.main_font_light);
		((TextView) mainView.findViewById(R.id.fromText)).setTextColor(mainFontColor);
		((TextView) mainView.findViewById(R.id.ViaView)).setTextColor(mainFontColor);
		((TextView) mainView.findViewById(R.id.toText)).setTextColor(mainFontColor);

		int descriptionColor = ContextCompat.getColor(ctx, R.color.description_font_and_bottom_sheet_icons);
		((TextView) mainView.findViewById(R.id.fromTitle)).setTextColor(descriptionColor);
		((TextView) mainView.findViewById(R.id.ViaSubView)).setTextColor(descriptionColor);
		((TextView) mainView.findViewById(R.id.toTitle)).setTextColor(descriptionColor);

		ctx.setupRouteCalculationProgressBar((ProgressBar) mainView.findViewById(R.id.progress_bar));
		setupRouteCalculationButtonProgressBar((ProgressBar) view.findViewById(R.id.progress_bar_button));
	}

	public static boolean showInstance(final MapActivity mapActivity, int initialMenuState) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		int slideInAnim = 0;
		int slideOutAnim = 0;
		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			if (portrait) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;
			} else {
				slideInAnim = R.anim.slide_in_left;
				slideOutAnim = R.anim.slide_out_left;
			}
		}

		try {
			mapActivity.getContextMenu().hideMenues();

			MapRouteInfoMenuFragment fragment = new MapRouteInfoMenuFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, initialMenuState);
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	public void setupRouteCalculationButtonProgressBar(@NonNull ProgressBar pb) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int bgColor = ContextCompat.getColor(mapActivity, isNightMode() ? R.color.activity_background_dark : R.color.activity_background_light);
			int progressColor = ContextCompat.getColor(mapActivity, isNightMode() ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
			pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, progressColor));
			pb.getIndeterminateDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}
	}
}