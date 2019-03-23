package net.osmand.plus.routepreparationmenu;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.Location;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.PrintDialogActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment.CumulativeInfo;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChooseRouteFragment extends BaseOsmAndFragment implements ContextMenuFragmentListener {

	public static final String TAG = "ChooseRouteFragment";
	public static final String ROUTE_INDEX_KEY = "route_index_key";
	public static final String ROUTE_INFO_STATE_KEY = "route_info_state_key";
	public static final String INITIAL_MENU_STATE_KEY = "initial_menu_state_key";

	@Nullable
	private LockableViewPager viewPager;
	protected List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = new ArrayList<>();

	@Nullable
	private View solidToolbarView;
	private int solidToolbarHeight;
	@Nullable
	private View zoomButtonsView;
	@Nullable
	private ImageButton myLocButtonView;

	private boolean portrait;
	private boolean nightMode;
	private boolean wasDrawerDisabled;

	private boolean publicTransportMode;
	private int routeInfoMenuState = -1;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireActivity();
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		OsmandApplication app = mapActivity.getMyApplication();
		TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
		List<TransportRouteResult> routes = transportRoutingHelper.getRoutes();
		int routeIndex = 0;
		int initialMenuState = MenuState.HEADER_ONLY;
		Bundle args = getArguments();
		if (args != null) {
			routeIndex = args.getInt(ROUTE_INDEX_KEY);
			routeInfoMenuState = args.getInt(ROUTE_INFO_STATE_KEY, -1);
			initialMenuState = args.getInt(INITIAL_MENU_STATE_KEY, initialMenuState);
		}
		if (routes != null && !routes.isEmpty()) {
			publicTransportMode = true;
		}
		ContextThemeWrapper context =
				new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		View view = LayoutInflater.from(context).inflate(R.layout.fragment_show_all_routes, null);
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		View solidToolbarView = view.findViewById(R.id.toolbar_layout);
		this.solidToolbarView = solidToolbarView;
		solidToolbarHeight = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);
		LockableViewPager viewPager = view.findViewById(R.id.pager);
		this.viewPager = viewPager;
		if (!portrait) {
			initialMenuState = MenuState.FULL_SCREEN;
			solidToolbarView.setLayoutParams(new FrameLayout.LayoutParams(AndroidUtils.dpToPx(mapActivity, 345f), ViewGroup.LayoutParams.WRAP_CONTENT));
			view.setLayoutParams(new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dashboard_land_width), ViewGroup.LayoutParams.MATCH_PARENT));
		}
		viewPager.setClipToPadding(false);
		//viewPager.setPageMargin(-60);
		final RoutesPagerAdapter pagerAdapter = new RoutesPagerAdapter(getChildFragmentManager(), publicTransportMode ? routes.size() : 1, initialMenuState);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(routeIndex);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) {
			}

			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			public void onPageSelected(int position) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.getMyApplication().getTransportRoutingHelper().setCurrentRoute(position);
					mapActivity.refreshMap();
					List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = ChooseRouteFragment.this.routeDetailsFragments;
					for (WeakReference<RouteDetailsFragment> ref : routeDetailsFragments) {
						RouteDetailsFragment f = ref.get();
						if (f != null) {
							PublicTransportCard card = f.getTransportCard();
							if (card != null) {
								card.update();
							}
						}
					}
				}
			}
		});
		buildZoomButtons(view);
		buildMenuButtons(view);
		return view;
	}

	@Override
	public void onAttachFragment(Fragment childFragment) {
		if (childFragment instanceof RouteDetailsFragment) {
			RouteDetailsFragment detailsFragment = (RouteDetailsFragment) childFragment;
			routeDetailsFragments.add(new WeakReference<>(detailsFragment));
			detailsFragment.setListener(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			MapRouteInfoMenu.chooseRoutesVisible = true;
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			updateControlsVisibility(false, false);
		}
	}

	public void onPause() {
		super.onPause();
		MapRouteInfoMenu.chooseRoutesVisible = false;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (!wasDrawerDisabled) {
				mapActivity.enableDrawer();
			}
			updateControlsVisibility(true, routeInfoMenuState != -1);
		}
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		View solidToolbarView = this.solidToolbarView;
		if (view != null) {
			if ((solidToolbarView != null && solidToolbarView.getVisibility() == View.VISIBLE) || !portrait) {
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

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color);
	}

	public void dismiss() {
		try {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
				if (routeInfoMenuState != -1) {
					mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoMenu(routeInfoMenuState);
				}
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private void buildZoomButtons(@NonNull View view) {
		OsmandApplication app = requireMyApplication();
		// Zoom buttons
		View zoomButtonsView = view.findViewById(R.id.map_hud_controls);
		this.zoomButtonsView = zoomButtonsView;
		ImageButton zoomInButtonView = (ImageButton) view.findViewById(R.id.map_zoom_in_button);
		ImageButton zoomOutButtonView = (ImageButton) view.findViewById(R.id.map_zoom_out_button);
		ImageButton myLocButtonView = (ImageButton) view.findViewById(R.id.map_my_location_button);
		this.myLocButtonView = myLocButtonView;
		AndroidUtils.updateImageButton(app, zoomInButtonView, R.drawable.map_zoom_in, R.drawable.map_zoom_in_night,
				R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
		AndroidUtils.updateImageButton(app, zoomOutButtonView, R.drawable.map_zoom_out, R.drawable.map_zoom_out_night,
				R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
		zoomInButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doZoomIn();
			}
		});
		zoomOutButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doZoomOut();
			}
		});

		myLocButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
					} else {
						ActivityCompat.requestPermissions(mapActivity,
								new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
								OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
					}
				}
			}
		});
		updateMyLocation();

		zoomButtonsView.setVisibility(View.VISIBLE);
	}

	private void updateMyLocation() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		boolean enabled = lastKnownLocation != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		ImageButton myLocButtonView = this.myLocButtonView;
		if (myLocButtonView != null) {
			if (!enabled) {
				myLocButtonView.setImageDrawable(getIcon(R.drawable.map_my_location, R.color.icon_color));
				AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle, R.drawable.btn_circle_night);
				myLocButtonView.setContentDescription(mapActivity.getString(R.string.unknown_location));
			} else if (tracked) {
				myLocButtonView.setImageDrawable(getIcon(R.drawable.map_my_location, R.color.color_myloc_distance));
				AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle, R.drawable.btn_circle_night);
			} else {
				myLocButtonView.setImageResource(R.drawable.map_my_location);
				AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle_blue, R.drawable.btn_circle_blue);
				myLocButtonView.setContentDescription(mapActivity.getString(R.string.map_widget_back_to_loc));
			}
			if (app.accessibilityEnabled()) {
				myLocButtonView.setClickable(enabled && !tracked && app.getRoutingHelper().isFollowingMode());
			}
		}
	}

	public void doZoomIn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandMapTileView map = mapActivity.getMapView();
			if (map.isZooming() && map.hasCustomMapRatio()) {
				mapActivity.changeZoom(2, System.currentTimeMillis());
			} else {
				mapActivity.changeZoom(1, System.currentTimeMillis());
			}
		}
	}

	public void doZoomOut() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.changeZoom(-1, System.currentTimeMillis());
		}
	}

	private void updateZoomButtonsVisibility(int menuState) {
		View zoomButtonsView = this.zoomButtonsView;
		if (zoomButtonsView != null) {
			if (menuState == MenuState.HEADER_ONLY) {
				if (zoomButtonsView.getVisibility() != View.VISIBLE) {
					zoomButtonsView.setVisibility(View.VISIBLE);
				}
			} else {
				if (zoomButtonsView.getVisibility() == View.VISIBLE) {
					zoomButtonsView.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	private int getZoomButtonsHeight() {
		View zoomButtonsView = this.zoomButtonsView;
		return zoomButtonsView != null ? zoomButtonsView.getHeight() : 0;
	}

	private void buildMenuButtons(@NonNull View view) {
		View backButton = view.findViewById(R.id.back_button);
		View backButtonFlow = view.findViewById(R.id.back_button_flow);
		OnClickListener backOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		};
		backButton.setOnClickListener(backOnClick);
		backButtonFlow.setOnClickListener(backOnClick);

		OnClickListener printOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				print();
			}
		};
		View printRoute = view.findViewById(R.id.print_route);
		View printRouteFlow = view.findViewById(R.id.print_route_flow);
		printRoute.setOnClickListener(printOnClick);
		printRouteFlow.setOnClickListener(printOnClick);

		View saveRoute = view.findViewById(R.id.save_as_gpx);
		View saveRouteFlow = view.findViewById(R.id.save_as_gpx_flow);
		OnClickListener saveOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
					MapActivityActions.createSaveDirections(mapActivity, routingHelper).show();
				}
			}
		};
		saveRoute.setOnClickListener(saveOnClick);
		saveRouteFlow.setOnClickListener(saveOnClick);

		View shareRoute = view.findViewById(R.id.share_as_gpx);
		View shareRouteFlow = view.findViewById(R.id.share_as_gpx_flow);
		OnClickListener shareOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					RoutingHelper routingHelper = app.getRoutingHelper();
					final String trackName = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
					final GPXUtilities.GPXFile gpx = routingHelper.generateGPXFileWithRoute(trackName);
					final Uri fileUri = AndroidUtils.getUriForFile(app, new File(gpx.path));
					File dir = new File(app.getCacheDir(), "share");
					if (!dir.exists()) {
						dir.mkdir();
					}
					File dst = new File(dir, "route.gpx");
					try {
						FileWriter fw = new FileWriter(dst);
						GPXUtilities.writeGpx(fw, gpx);
						fw.close();
						final Intent sendIntent = new Intent();
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(generateHtml(routingHelper.getRouteDirections(),
								routingHelper.getGeneralRouteInformation()).toString()));
						sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_route_subject));
						sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
						sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, dst));
						sendIntent.setType("text/plain");
						sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(sendIntent);
					} catch (IOException e) {
						// Toast.makeText(getActivity(), "Error sharing favorites: " + e.getMessage(),
						// Toast.LENGTH_LONG).show();
					}
				}
			}
		};
		shareRoute.setOnClickListener(shareOnClick);
		shareRouteFlow.setOnClickListener(shareOnClick);

		if (publicTransportMode) {
			view.findViewById(R.id.toolbar_options).setVisibility(View.GONE);
		}
		if (publicTransportMode || !portrait) {
			view.findViewById(R.id.toolbar_options_flow).setVisibility(View.GONE);
			view.findViewById(R.id.toolbar_options_flow_bg).setVisibility(View.GONE);
		}
	}

	void print() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		final RoutingHelper routingHelper = app.getRoutingHelper();
		File file = generateRouteInfoHtml(routingHelper.getRouteDirections(), routingHelper.getGeneralRouteInformation());
		if (file != null && file.exists()) {
			Uri uri = AndroidUtils.getUriForFile(app, file);
			Intent browserIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // use Android Print Framework
				browserIntent = new Intent(getActivity(), PrintDialogActivity.class)
						.setDataAndType(uri, "text/html");
			} else { // just open html document
				browserIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(
						uri, "text/html");
			}
			browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(browserIntent);
		}
	}

	private File generateRouteInfoHtml(List<RouteDirectionInfo> directionsInfo, String title) {
		OsmandApplication app = getMyApplication();
		if (app == null || directionsInfo == null) {
			return null;
		}
		final String fileName = "route_info.html";
		StringBuilder html = generateHtmlPrint(directionsInfo, title);
		FileOutputStream fos = null;
		File file;
		try {
			file = app.getAppPath(fileName);
			fos = new FileOutputStream(file);
			fos.write(html.toString().getBytes("UTF-8"));
			fos.flush();
		} catch (IOException e) {
			file = null;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					file = null;
					e.printStackTrace();
				}
			}
		}
		return file;
	}

	private StringBuilder generateHtml(List<RouteDirectionInfo> directionInfos, String title) {
		StringBuilder html = new StringBuilder();
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return html;
		}
		if (!TextUtils.isEmpty(title)) {
			html.append("<h1>");
			html.append(title);
			html.append("</h1>");
		}
		final String NBSP = "&nbsp;";
		final String BR = "<br>";
		for (int i = 0; i < directionInfos.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = directionInfos.get(i);
			String sb = OsmAndFormatter.getFormattedDistance(routeDirectionInfo.distance, app) +
					", " + NBSP +
					RouteDetailsFragment.getTimeDescription(app, routeDirectionInfo);
			String distance = sb.replaceAll("\\s", NBSP);
			String description = routeDirectionInfo.getDescriptionRoutePart();
			html.append(BR);
			html.append("<p>")
					.append(String.valueOf(i + 1)).append(". ")
					.append(NBSP).append(description).append(NBSP)
					.append("(").append(distance).append(")</p>");
		}
		return html;
	}

	private StringBuilder generateHtmlPrint(List<RouteDirectionInfo> directionsInfo, String title) {
		StringBuilder html = new StringBuilder();
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return html;
		}
		boolean accessibilityEnabled = app.accessibilityEnabled();
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		html.append("<head>");
		html.append("<title>Route info</title>");
		html.append("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />");
		html.append("<style>");
		html.append("table, th, td {");
		html.append("border: 1px solid black;");
		html.append("border-collapse: collapse;}");
		html.append("th, td {");
		html.append("padding: 5px;}");
		html.append("</style>");
		html.append("</head>");
		html.append("<body>");

		if (!TextUtils.isEmpty(title)) {
			html.append("<h1>");
			html.append(title);
			html.append("</h1>");
		}
		html.append("<table style=\"width:100%\">");
		final String NBSP = "&nbsp;";
		final String BR = "<br>";
		for (int i = 0; i < directionsInfo.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = directionsInfo.get(i);
			html.append("<tr>");
			StringBuilder sb = new StringBuilder();
			sb.append(OsmAndFormatter.getFormattedDistance(routeDirectionInfo.distance, app));
			sb.append(", ");
			sb.append(RouteDetailsFragment.getTimeDescription(app, routeDirectionInfo));
			String distance = sb.toString().replaceAll("\\s", NBSP);
			html.append("<td>");
			html.append(distance);
			html.append("</td>");
			String description = routeDirectionInfo.getDescriptionRoutePart();
			html.append("<td>");
			html.append(String.valueOf(i + 1)).append(". ").append(description);
			html.append("</td>");
			CumulativeInfo cumulativeInfo = RouteDetailsFragment.getRouteDirectionCumulativeInfo(i, directionsInfo);
			html.append("<td>");
			sb = new StringBuilder();
			sb.append(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance, app));
			sb.append(" - ");
			sb.append(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance + routeDirectionInfo.distance, app));
			sb.append(BR);
			sb.append(Algorithms.formatDuration(cumulativeInfo.time, accessibilityEnabled));
			sb.append(" - ");
			sb.append(Algorithms.formatDuration(cumulativeInfo.time + routeDirectionInfo.getExpectedTime(), accessibilityEnabled));
			String cumulativeTimeAndDistance = sb.toString().replaceAll("\\s", NBSP);
			html.append(cumulativeTimeAndDistance);
			html.append("</td>");
			html.append("</tr>");
		}
		html.append("</table>");
		html.append("</body>");
		html.append("</html>");
		return html;
	}

	private RouteDetailsFragment getCurrentFragment() {
		LockableViewPager viewPager = this.viewPager;
		if (viewPager != null) {
			int currentItem = viewPager.getCurrentItem();
			List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = this.routeDetailsFragments;
			for (WeakReference<RouteDetailsFragment> ref : routeDetailsFragments) {
				RouteDetailsFragment f = ref.get();
				if (f != null && f.getRouteId() == currentItem) {
					return f;
				}
			}
		}
		return null;
	}

	public void updateViewPager(int y) {
		LockableViewPager viewPager = this.viewPager;
		if (viewPager != null) {
			if (viewPager.getChildCount() > 1) {
				viewPager.setSwipeLockedPosY(y);
			} else {
				viewPager.setSwipeLocked(true);
			}
		}
	}

	public void updateToolbars(@NonNull ContextMenuFragment fragment, int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			View solidToolbarView = this.solidToolbarView;
			if (solidToolbarView != null) {
				solidToolbarView.setVisibility(y - fragment.getTopShadowMargin() <= solidToolbarHeight ? View.VISIBLE : View.GONE);
			}
			mapActivity.updateStatusBarColor();
		}
	}

	public void updateZoomButtonsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		View zoomButtonsView = this.zoomButtonsView;
		if (zoomButtonsView != null) {
			int zoomY = y - getZoomButtonsHeight() - fragment.getTopShadowMargin();
			if (animated) {
				fragment.animateView(zoomButtonsView, zoomY);
			} else {
				zoomButtonsView.setY(zoomY);
			}
		}
	}

	public void updateControlsVisibility(boolean visible, boolean openingRouteInfo) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int visibility = visible ? View.VISIBLE : View.GONE;
			mapActivity.findViewById(R.id.map_center_info).setVisibility(visibility);
			mapActivity.findViewById(R.id.map_left_widgets_panel).setVisibility(visibility);
			if (!openingRouteInfo) {
				mapActivity.findViewById(R.id.map_right_widgets_panel).setVisibility(visibility);
				if (!portrait) {
					mapActivity.getMapView().setMapPositionX(visible ? 0 : 1);
				}
			}
			mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(visibility);
			mapActivity.refreshMap();
		}
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		if (fragment == getCurrentFragment()) {
			updateToolbars(fragment, y);
			updateZoomButtonsPos(fragment, y, animated);
			updateViewPager(fragment.getViewY());
		}
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState) {
		LockableViewPager viewPager = this.viewPager;
		if (viewPager != null) {
			int currentItem = viewPager.getCurrentItem();
			int i = 0;
			List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = this.routeDetailsFragments;
			for (WeakReference<RouteDetailsFragment> ref : routeDetailsFragments) {
				RouteDetailsFragment f = ref.get();
				if (f != null) {
					boolean current = f.getRouteId() == currentItem;
					if (!current && f.getCurrentMenuState() != menuState) {
						f.openMenuScreen(menuState, false);
					}
					if (current) {
						updateZoomButtonsVisibility(menuState);
						updateViewPager(fragment.getViewY());
					}
				}
				i++;
			}
		}
	}

	@Override
	public void onContextMenuDismiss(@NonNull ContextMenuFragment fragment) {
		dismiss();
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		return showInstance(fragmentManager, 0);
	}

	public static boolean showInstance(FragmentManager fragmentManager, int routeIndex) {
		try {
			ChooseRouteFragment fragment = new ChooseRouteFragment();
			Bundle args = new Bundle();
			args.putInt(ROUTE_INDEX_KEY, routeIndex);
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.add(R.id.routeMenuContainer, fragment, TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static boolean showFromRouteInfo(FragmentManager fragmentManager, int routeIndex,
									 int routeInfoState, int initialMenuState) {
		try {
			ChooseRouteFragment fragment = new ChooseRouteFragment();
			Bundle args = new Bundle();
			args.putInt(ROUTE_INDEX_KEY, routeIndex);
			args.putInt(ROUTE_INFO_STATE_KEY, routeInfoState);
			args.putInt(INITIAL_MENU_STATE_KEY, initialMenuState);
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.add(R.id.routeMenuContainer, fragment, TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public class RoutesPagerAdapter extends FragmentPagerAdapter {
		private int routesCount;
		private int initialMenuState;

		RoutesPagerAdapter(FragmentManager fm, int routesCount, int initialMenuState) {
			super(fm);
			this.routesCount = routesCount;
			this.initialMenuState = initialMenuState;
		}

		@Override
		public int getCount() {
			return routesCount;
		}

		@Override
		public Fragment getItem(int position) {
			Bundle args = new Bundle();
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, initialMenuState);
			args.putInt(RouteDetailsFragment.ROUTE_ID_KEY, position);
			return Fragment.instantiate(ChooseRouteFragment.this.getContext(), RouteDetailsFragment.class.getName(), args);
		}
	}
}