package net.osmand.plus.routepreparationmenu;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_ROUTE_DETAILS_OPTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;
import static net.osmand.plus.activities.MapActivityActions.SaveDirectionsAsyncTask;
import static net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PrintDialogActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment.CumulativeInfo;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment.RouteDetailsFragmentListener;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.router.TransportRouteResult;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChooseRouteFragment extends BaseOsmAndFragment implements ContextMenuFragmentListener,
		RouteDetailsFragmentListener, SaveAsNewTrackFragmentListener {

	public static final String TAG = "ChooseRouteFragment";
	public static final String ROUTE_INDEX_KEY = "route_index_key";
	public static final String ROUTE_INFO_STATE_KEY = "route_info_state_key";
	public static final String INITIAL_MENU_STATE_KEY = "initial_menu_state_key";
	public static final String ADJUST_MAP_KEY = "adjust_map_key";

	public static final String ZOOM_IN_BUTTON_ID = ZOOM_IN_HUD_ID + TAG;
	public static final String ZOOM_OUT_BUTTON_ID = ZOOM_OUT_HUD_ID + TAG;
	public static final String BACK_TO_LOC_BUTTON_ID = BACK_TO_LOC_HUD_ID + TAG;

	@Nullable
	private LockableViewPager viewPager;
	protected List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = new ArrayList<>();

	@Nullable
	private View solidToolbarView;
	@Nullable
	private View zoomButtonsView;
	@Nullable
	private ViewGroup pagesView;

	private boolean portrait;
	private boolean wasDrawerDisabled;
	private int currentMenuState;
	private int routesCount;
	private boolean paused;

	private boolean publicTransportMode;
	private boolean needAdjustMap;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss(true);
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = (MapActivity) requireActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		OsmandApplication app = mapActivity.getMyApplication();
		TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
		List<TransportRouteResult> routes = transportRoutingHelper.getRoutes();
		int routeIndex = 0;
		int initialMenuState = MenuState.HEADER_ONLY;
		Bundle args = getArguments();
		if (args == null) {
			args = savedInstanceState;
		}
		if (args != null) {
			routeIndex = args.getInt(ROUTE_INDEX_KEY);
			needAdjustMap = args.getBoolean(ADJUST_MAP_KEY, false);
			initialMenuState = args.getInt(INITIAL_MENU_STATE_KEY, initialMenuState);
		}
		routesCount = 1;
		if (routes != null && !routes.isEmpty()) {
			publicTransportMode = true;
			routesCount = routes.size();
		}
		ContextThemeWrapper context =
				new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		View view = LayoutInflater.from(context).inflate(R.layout.fragment_show_all_routes, null);
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		View solidToolbarView = view.findViewById(R.id.toolbar_layout);
		this.solidToolbarView = solidToolbarView;
		LockableViewPager viewPager = view.findViewById(R.id.pager);
		this.viewPager = viewPager;
		if (!portrait) {
			initialMenuState = MenuState.FULL_SCREEN;
			int width = getDimensionPixelSize(R.dimen.dashboard_land_width) - getDimensionPixelSize(R.dimen.dashboard_land_shadow_width);
			solidToolbarView.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
			solidToolbarView.setVisibility(View.VISIBLE);
			TypedValue typedValueAttr = new TypedValue();
			int bgAttrId = AndroidUtils.isLayoutRtl(mapActivity) ? R.attr.right_menu_view_bg : R.attr.left_menu_view_bg;
			mapActivity.getTheme().resolveAttribute(bgAttrId, typedValueAttr, true);
			view.findViewById(R.id.pager_container).setBackgroundResource(typedValueAttr.resourceId);
			view.setLayoutParams(new FrameLayout.LayoutParams(getDimensionPixelSize(R.dimen.dashboard_land_width), ViewGroup.LayoutParams.MATCH_PARENT));
		}
		viewPager.setClipToPadding(false);
		currentMenuState = initialMenuState;
		RoutesPagerAdapter pagerAdapter = new RoutesPagerAdapter(getChildFragmentManager(), routesCount);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(routeIndex);
		viewPager.setOffscreenPageLimit(1);
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			public void onPageSelected(int position) {
				MapActivity mapActivity = getMapActivity();
				View view = getView();
				if (mapActivity != null && view != null) {
					app.getTransportRoutingHelper().setCurrentRoute(position);
					mapActivity.refreshMap();
					buildPagesControl(view);
					List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = ChooseRouteFragment.this.routeDetailsFragments;
					RouteDetailsFragment current = getCurrentFragment();
					for (WeakReference<RouteDetailsFragment> ref : routeDetailsFragments) {
						RouteDetailsFragment fragment = ref.get();
						if (fragment != null) {
							PublicTransportCard card = fragment.getTransportCard();
							if (card != null) {
								card.updateButtons();
							}
							if (fragment == current) {
								updateElementsPosition(fragment);
							}
							Bundle args = fragment.getArguments();
							if (args != null) {
								args.putInt(ContextMenuFragment.MENU_STATE_KEY, currentMenuState);
							}
						}
					}
				}
			}
		});
		this.pagesView = view.findViewById(R.id.pages_control);
		buildPagesControl(view);
		buildZoomButtons(view);
		buildMenuButtons(view);
		return view;
	}

	private void updateElementsPosition(@NonNull RouteDetailsFragment fragment) {
		View fragmentView = fragment.getView();
		if (fragmentView != null) {
			fragmentView.getViewTreeObserver().addOnGlobalLayoutListener(
					new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							fragmentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
							updatePagesViewPos(fragment, fragment.getViewY(), true);
							updateZoomButtonsPos(fragment, fragment.getViewY(), true);
						}
					});
		}
	}

	@Override
	public void onAttachFragment(@NonNull Fragment childFragment) {
		if (childFragment instanceof RouteDetailsFragment) {
			RouteDetailsFragment detailsFragment = (RouteDetailsFragment) childFragment;
			routeDetailsFragments.add(new WeakReference<>(detailsFragment));
			detailsFragment.setListener(this);
			detailsFragment.setRouteDetailsListener(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			MapRouteInfoMenu.chooseRoutesVisible = true;
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			updateControlsVisibility(false);
		}
	}

	public void onPause() {
		super.onPause();
		paused = true;
		MapRouteInfoMenu.chooseRoutesVisible = false;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (!wasDrawerDisabled) {
				mapActivity.enableDrawer();
			}
			updateControlsVisibility(true);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapControlsLayer mapControlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
			mapControlsLayer.clearCustomMapButtons();
		}
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean toolbarVisible = solidToolbarView != null && solidToolbarView.getVisibility() == View.VISIBLE;
			if (toolbarVisible || !portrait) {
				if (!nightMode) {
					AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), true);
				}
				return ColorUtilities.getDividerColorId(nightMode);
			} else if (!nightMode) {
				AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), false);
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
		return getIcon(id, ColorUtilities.getDefaultIconColorId(nightMode));
	}

	public boolean isPaused() {
		return paused;
	}

	public void analyseOnMap(LatLon location, GpxDisplayItem gpxItem) {
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
				false,
				gpxItem);

		dismiss();
		MapActivity.launchMapActivityMoveToTop(getMapActivity());
	}

	public void dismiss() {
		dismiss(false);
	}

	public void dismiss(boolean backPressed) {
		try {
			MapActivity mapActivity = getMapActivity();
			LockableViewPager viewPager = this.viewPager;
			if (mapActivity != null && viewPager != null) {
				mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
				Bundle args = getArguments();
				if (args == null) {
					args = new Bundle();
				}
				args.putInt(ROUTE_INDEX_KEY, viewPager.getCurrentItem());
				args.putInt(INITIAL_MENU_STATE_KEY, currentMenuState);
				args.putBoolean(ADJUST_MAP_KEY, false);
				mapActivity.getMapRouteInfoMenu().onDismiss(this, currentMenuState, args, backPressed);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private void buildPagesControl(@NonNull View view) {
		ViewGroup pagesView = this.pagesView;
		if (pagesView != null) {
			pagesView.removeAllViews();
			LockableViewPager viewPager = this.viewPager;
			if (portrait && routesCount > 1 && viewPager != null) {
				int itemSize = getResources().getDimensionPixelSize(R.dimen.pages_item_size);
				int itemMargin = getResources().getDimensionPixelSize(R.dimen.pages_item_margin);
				int itemPadding = getResources().getDimensionPixelSize(R.dimen.pages_item_padding);
				for (int i = 0; i < routesCount; i++) {
					boolean active = i == viewPager.getCurrentItem();
					Context ctx = view.getContext();
					View itemView = new View(ctx);
					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemSize, itemSize);
					AndroidUtils.setBackground(ctx, itemView, nightMode,
							active ? R.drawable.pages_active_light : R.drawable.pages_inactive_light,
							active ? R.drawable.pages_active_dark : R.drawable.pages_inactive_dark);
					if (i == 0) {
						AndroidUtils.setMargins(layoutParams, itemMargin, 0, itemPadding, 0);
					} else if (i == routesCount - 1) {
						AndroidUtils.setMargins(layoutParams, 0, 0, itemMargin, 0);
					} else {
						AndroidUtils.setMargins(layoutParams, 0, 0, itemPadding, 0);
					}
					itemView.setLayoutParams(layoutParams);
					pagesView.addView(itemView);
				}
				pagesView.requestLayout();
			}
			updatePagesViewVisibility(currentMenuState);
		}
	}

	private void buildZoomButtons(@NonNull View view) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		// Zoom buttons
		View zoomButtonsView = view.findViewById(R.id.map_hud_controls);
		this.zoomButtonsView = zoomButtonsView;

		MapControlsLayer controlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_zoom_in_button));
		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_zoom_out_button));
		controlsLayer.addCustomMapButton(view.findViewById(R.id.map_my_location_button));

		AndroidUiHelper.updateVisibility(zoomButtonsView, true);
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

	private void updatePagesViewVisibility(int menuState) {
		View pagesView = this.pagesView;
		if (pagesView != null) {
			if (portrait && routesCount > 1) {
				if (menuState != MenuState.FULL_SCREEN) {
					if (pagesView.getVisibility() != View.VISIBLE) {
						pagesView.setVisibility(View.VISIBLE);
					}
				} else {
					if (pagesView.getVisibility() == View.VISIBLE) {
						pagesView.setVisibility(View.INVISIBLE);
					}
				}
			} else {
				if (pagesView.getVisibility() == View.VISIBLE) {
					pagesView.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	private int getPagesViewHeight() {
		ViewGroup pagesView = this.pagesView;
		return pagesView != null ? pagesView.getHeight() : 0;
	}

	private int getZoomButtonsHeight() {
		View zoomButtonsView = this.zoomButtonsView;
		return zoomButtonsView != null ? zoomButtonsView.getHeight() : 0;
	}

	private void buildMenuButtons(@NonNull View view) {
		AppCompatImageView backButton = view.findViewById(R.id.back_button);
		AppCompatImageButton backButtonFlow = view.findViewById(R.id.back_button_flow);
		OnClickListener backOnClick = v -> dismiss(true);
		backButton.setOnClickListener(backOnClick);
		backButtonFlow.setOnClickListener(backOnClick);
		int navigationIconResId = AndroidUtils.getNavigationIconResId(getContext());
		backButton.setImageResource(navigationIconResId);
		backButtonFlow.setImageResource(navigationIconResId);

		OnClickListener printOnClick = v -> {
			print();
		};
		View printRoute = view.findViewById(R.id.print_route);
		View printRouteFlow = view.findViewById(R.id.print_route_flow);
		printRoute.setOnClickListener(printOnClick);
		printRouteFlow.setOnClickListener(printOnClick);

		View saveRoute = view.findViewById(R.id.save_as_gpx);
		View saveRouteFlow = view.findViewById(R.id.save_as_gpx_flow);
		OnClickListener saveOnClick = v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();

				String fileName = null;
				if (paramsBuilder != null && paramsBuilder.getFile() != null) {
					GpxFile gpxFile = paramsBuilder.getFile();
					if (!Algorithms.isEmpty(gpxFile.getPath())) {
						fileName = Algorithms.getFileNameWithoutExtension(new File(gpxFile.getPath()).getName());
					} else if (!Algorithms.isEmpty(gpxFile.getTracks())) {
						fileName = gpxFile.getTracks().get(0).getName();
					}
				}
				if (Algorithms.isEmpty(fileName)) {
					String suggestedName = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date());
					fileName = FileUtils.createUniqueFileName(app, suggestedName, IndexConstants.GPX_INDEX_DIR, GPX_FILE_EXT);
				}
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				SaveAsNewTrackBottomSheetDialogFragment.showInstance(manager, fileName, this, false, true);
			}
		};
		saveRoute.setOnClickListener(saveOnClick);
		saveRouteFlow.setOnClickListener(saveOnClick);

		ImageView shareRoute = view.findViewById(R.id.share_as_gpx);
		ImageView shareRouteFlow = view.findViewById(R.id.share_as_gpx_flow);
		Drawable shareIcon = getIcon(R.drawable.ic_action_gshare_dark, ColorUtilities.getSecondaryTextColorId(nightMode));
		shareIcon = AndroidUtils.getDrawableForDirection(app, shareIcon);
		shareRoute.setImageDrawable(shareIcon);
		shareRouteFlow.setImageDrawable(shareIcon);
		OnClickListener shareOnClick = v -> {
			List<PopUpMenuItem> items = new ArrayList<>();
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(getString(R.string.share_as_file))
					.setIcon(getContentIcon(R.drawable.ic_action_file_routing))
					.setOnClickListener(menuItem -> shareFile(app))
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(getString(R.string.share_link))
					.setIcon(getContentIcon(R.drawable.ic_action_link))
					.setOnClickListener(menuItem -> shareLink(app))
					.create());

			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = v;
			displayData.menuItems = items;
			displayData.nightMode = nightMode;
			PopUpMenu.show(displayData);
		};
		shareRoute.setOnClickListener(shareOnClick);
		shareRouteFlow.setOnClickListener(shareOnClick);

		OsmAndAppCustomization customization = app.getAppCustomization();
		boolean featureEnabled = customization.isFeatureEnabled(NAVIGATION_ROUTE_DETAILS_OPTIONS_ID);
		if (publicTransportMode || !featureEnabled) {
			view.findViewById(R.id.toolbar_options).setVisibility(View.GONE);
		}
		if (publicTransportMode || !portrait || !featureEnabled) {
			view.findViewById(R.id.toolbar_options_flow).setVisibility(View.GONE);
			view.findViewById(R.id.toolbar_options_flow_bg).setVisibility(View.GONE);
		}
	}

	private void shareLink(@NonNull OsmandApplication app) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT, IntentHelper.generateRouteUrl(app));
			Intent chooserIntent = Intent.createChooser(sendIntent, app.getString(R.string.shared_string_share));

			AndroidUtils.startActivityIfSafe(activity, chooserIntent);
		}
	}

	private void shareFile(@NonNull OsmandApplication app) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			RoutingHelper routingHelper = app.getRoutingHelper();
			String trackName = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
			GpxFile gpx = routingHelper.generateGPXFileWithRoute(trackName);
			Uri fileUri = AndroidUtils.getUriForFile(app, new File(gpx.getPath()));
			File dir = new File(app.getCacheDir(), "share");
			if (!dir.exists()) {
				dir.mkdir();
			}
			File dst = new File(dir, "route.gpx");

			SaveGpxHelper.saveGpx(dst, gpx, errorMessage -> {
				if (errorMessage == null) {
					Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(generateHtml(routingHelper.getRouteDirections(),
							routingHelper.getGeneralRouteInformation(), IntentHelper.generateRouteUrl(app)).toString()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_route_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
					sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, dst));
					sendIntent.setType("text/plain");
					sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					Intent chooserIntent = Intent.createChooser(sendIntent, app.getString(R.string.shared_string_share));

					AndroidUtils.startActivityIfSafe(activity, chooserIntent);
				}
			});
		}
	}

	void print() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			RoutingHelper routingHelper = app.getRoutingHelper();
			File file = generateRouteInfoHtml(routingHelper.getRouteDirections(), routingHelper.getGeneralRouteInformation());
			if (file != null && file.exists()) {
				Uri uri = AndroidUtils.getUriForFile(app, file);
				Intent intent = new Intent(getActivity(), PrintDialogActivity.class)
						.setDataAndType(uri, "text/html")
						.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				AndroidUtils.startActivityIfSafe(activity, intent);
			}
		}
	}

	private File generateRouteInfoHtml(List<RouteDirectionInfo> directionsInfo, String title) {
		if (directionsInfo == null) {
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

	private StringBuilder generateHtml(List<RouteDirectionInfo> directionInfos, String title, String url) {
		StringBuilder html = new StringBuilder();
		if (!TextUtils.isEmpty(title)) {
			html.append("<h1>");
			html.append(url);
			html.append("<br>");
			html.append("<br>");
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
					.append(i + 1).append(". ")
					.append(NBSP).append(description).append(NBSP)
					.append("(").append(distance).append(")</p>");
		}
		return html;
	}

	private StringBuilder generateHtmlPrint(List<RouteDirectionInfo> directionsInfo, String title) {
		StringBuilder html = new StringBuilder();
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
			html.append(i + 1).append(". ").append(description);
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

	@Nullable
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

	public void updateToolbars(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			View solidToolbarView = this.solidToolbarView;
			if (solidToolbarView != null && portrait) {
				if (animated) {
					float toolbarAlpha = fragment.getToolbarAlpha(y);
					if (toolbarAlpha > 0) {
						fragment.updateVisibility(solidToolbarView, true);
					}
					solidToolbarView.animate().alpha(toolbarAlpha)
							.setDuration(ContextMenuFragment.ANIMATION_DURATION)
							.setInterpolator(new DecelerateInterpolator())
							.setListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									fragment.updateVisibility(solidToolbarView, toolbarAlpha);
									mapActivity.updateStatusBarColor();
								}
							})
							.start();
				} else {
					fragment.updateToolbarVisibility(solidToolbarView, y);
					mapActivity.updateStatusBarColor();
				}
			}
		}
	}

	public void updatePagesViewPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		ViewGroup pagesView = this.pagesView;
		if (pagesView != null) {
			int pagesY = y - getPagesViewHeight() + fragment.getShadowHeight();
			if (Build.VERSION.SDK_INT >= 21) {
				pagesY += AndroidUtils.getStatusBarHeight(pagesView.getContext());
			}
			if (animated) {
				fragment.animateView(pagesView, pagesY, null);
			} else {
				pagesView.setY(pagesY);
			}
		}
	}

	public void updateZoomButtonsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		View zoomButtonsView = this.zoomButtonsView;
		if (zoomButtonsView != null) {
			int zoomY = y - getZoomButtonsHeight();
			if (Build.VERSION.SDK_INT >= 21) {
				zoomY += AndroidUtils.getStatusBarHeight(zoomButtonsView.getContext());
			}
			if (animated) {
				fragment.animateView(zoomButtonsView, zoomY, null);
			} else {
				zoomButtonsView.setY(zoomY);
			}
		}
	}

	public void updateControlsVisibility(boolean visible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int visibility = visible ? View.VISIBLE : View.GONE;
			AndroidUiHelper.setVisibility(mapActivity, visibility,
					R.id.map_center_info,
					R.id.map_left_widgets_panel);
			if (!visible) {
				mapActivity.findViewById(R.id.map_right_widgets_panel).setVisibility(visibility);
				if (!portrait) {
					mapActivity.getMapPositionManager().setMapPositionShiftedX(true);
				}
			}
			mapActivity.updateStatusBarColor();
			mapActivity.refreshMap();
		}
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated) {
		if (fragment == getCurrentFragment()) {
			updateToolbars(fragment, y, animated);
			updatePagesViewPos(fragment, y, animated);
			updateZoomButtonsPos(fragment, y, animated);
			updateViewPager(fragment.getViewY());
		}
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState, int previousMenuState) {
		LockableViewPager viewPager = this.viewPager;
		RouteDetailsFragment current = getCurrentFragment();
		if (viewPager != null && fragment == current) {
			currentMenuState = menuState;
			List<WeakReference<RouteDetailsFragment>> routeDetailsFragments = this.routeDetailsFragments;
			for (WeakReference<RouteDetailsFragment> ref : routeDetailsFragments) {
				RouteDetailsFragment f = ref.get();
				if (f != null) {
					if (f != current && f.getCurrentMenuState() != menuState) {
						f.openMenuScreen(menuState, false);
					}
					if (f == current) {
						updatePagesViewVisibility(menuState);
						updateZoomButtonsVisibility(menuState);
						updateViewPager(fragment.getViewY());
						if (needAdjustMap) {
							needAdjustMap = false;
							f.showRouteOnMap();
						}
					}
				}
			}
		}
	}

	@Override
	public void onContextMenuDismiss(@NonNull ContextMenuFragment fragment) {
		dismiss();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager) {
		return showInstance(fragmentManager, 0);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, int routeIndex) {
		Bundle args = new Bundle();
		args.putInt(ROUTE_INDEX_KEY, routeIndex);
		return showInstance(fragmentManager, args);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, int routeIndex, int initialMenuState) {
		Bundle args = new Bundle();
		args.putInt(ROUTE_INDEX_KEY, routeIndex);
		args.putInt(INITIAL_MENU_STATE_KEY, initialMenuState);
		args.putBoolean(ADJUST_MAP_KEY, initialMenuState != MenuState.FULL_SCREEN);
		return showInstance(fragmentManager, args);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, @Nullable Bundle args) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ChooseRouteFragment fragment = new ChooseRouteFragment();
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.add(R.id.routeMenuContainer, fragment, TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void onNavigationRequested() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			dismiss(false);
			if (!mapActivity.getMyApplication().getRoutingHelper().isPublicTransportMode()) {
				mapActivity.getMapLayers().getMapActionsHelper().startNavigation();
			}
		}
	}

	@Override
	public void onSaveAsNewTrack(@NonNull String folderPath, @NonNull String fileName, boolean showOnMap, boolean simplifiedTrack) {
		File fileDir = new File(folderPath);
		File toSave = new File(fileDir, fileName + GPX_FILE_EXT);
		new SaveDirectionsAsyncTask(app, showOnMap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, toSave);
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	public class RoutesPagerAdapter extends FragmentPagerAdapter {
		private final int routesCount;

		RoutesPagerAdapter(FragmentManager fm, int routesCount) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.routesCount = routesCount;
		}

		@Override
		public int getCount() {
			return routesCount;
		}

		@Override
		@NonNull
		public Fragment getItem(int position) {
			Bundle args = new Bundle();
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, currentMenuState);
			args.putInt(RouteDetailsFragment.ROUTE_ID_KEY, position);
			return Fragment.instantiate(ChooseRouteFragment.this.getContext(), RouteDetailsFragment.class.getName(), args);
		}
	}
}