package net.osmand.plus.routepreparationmenu;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.SpannableString;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.mapcontextmenu.other.FavouritesBottomSheetMenuFragment;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import java.util.List;

public class AddPointBottomSheetDialog extends MenuBottomSheetDialogFragment {

	public static final String TAG = "AddPointBottomSheetDialog";
	public static final String TARGET_KEY = "target";
	public static final String INTERMEDIATE_KEY = "intermediate";

	public static final int ADD_FAVOURITE_TO_ROUTE_REQUEST_CODE = 1;

	private boolean target;
	private boolean intermediate;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null && args.containsKey(TARGET_KEY) && args.containsKey(INTERMEDIATE_KEY)) {
			target = args.getBoolean(TARGET_KEY);
			intermediate = args.getBoolean(INTERMEDIATE_KEY);
		}
		String title;
		if (intermediate) {
			title = getString(R.string.add_intermediate_point);
		} else if (target) {
			title = getString(R.string.add_destination_point);
		} else {
			title = getString(R.string.add_start_point);
		}
		items.add(new TitleItem(title));

		createSearchItem();
		if (intermediate) {
			createSelectOnTheMapItem();
			createFavouritesItem();
			createMarkersItem();
		} else if (target) {
			createMyLocItem();
			createSelectOnTheMapItem();
			createFavouritesItem();
			createMarkersItem();
			items.add(new DividerHalfItem(getContext()));
			createSwitchStartAndEndItem();
		} else {
			createMyLocItem();
			createSelectOnTheMapItem();
			createFavouritesItem();
			createMarkersItem();
			items.add(new DividerHalfItem(getContext()));
			createSwitchStartAndEndItem();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ADD_FAVOURITE_TO_ROUTE_REQUEST_CODE) {
			dismiss();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void createSearchItem() {
		View searchView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_double_item, null);
		TextView firstTitle = (TextView) searchView.findViewById(R.id.first_title);
		TextView secondTitle = (TextView) searchView.findViewById(R.id.second_title);
		ImageView firstIcon = (ImageView) searchView.findViewById(R.id.first_icon);
		ImageView secondIcon = (ImageView) searchView.findViewById(R.id.second_icon);

		firstTitle.setText(R.string.shared_string_search);
		secondTitle.setText(R.string.shared_string_address);
		firstIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_search_dark));
		secondIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_street_name));

		AndroidUtils.setBackground(getContext(), searchView.findViewById(R.id.first_divider),
				nightMode, R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(getContext(), searchView.findViewById(R.id.second_divider),
				nightMode, R.color.dashboard_divider_light, R.color.dashboard_divider_dark);

		searchView.findViewById(R.id.first_item).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					mapActivity.showQuickSearch(getSearchMode(), QuickSearchDialogFragment.QuickSearchTab.HISTORY);
				}
				dismiss();
			}
		});
		searchView.findViewById(R.id.second_item).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					mapActivity.showQuickSearch(getSearchMode(), false);
				}
				dismiss();
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(searchView).create());
	}

	private MapActivity.ShowQuickSearchMode getSearchMode() {
		if (intermediate) {
			return MapActivity.ShowQuickSearchMode.INTERMEDIATE_SELECTION;
		} else if (target) {
			return MapActivity.ShowQuickSearchMode.DESTINATION_SELECTION;
		} else {
			return MapActivity.ShowQuickSearchMode.START_POINT_SELECTION;
		}
	}

	private void createMyLocItem() {
		BaseBottomSheetItem myLocationItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getIcon(R.drawable.ic_action_location_color, 0))
				.setTitle(getString(R.string.shared_string_my_location))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = getMyApplication();
						if (app != null) {
							TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
							Location myLocation = app.getLocationProvider().getLastKnownLocation();
							if (myLocation != null) {
								LatLon ll = new LatLon(myLocation.getLatitude(), myLocation.getLongitude());
								if (intermediate) {
									app.showShortToastMessage(R.string.add_intermediate_point);
									targetPointsHelper.navigateToPoint(ll, true, targetPointsHelper.getIntermediatePoints().size());
								} else if (target) {
									app.showShortToastMessage(R.string.add_destination_point);
									targetPointsHelper.navigateToPoint(ll, true, -1);
								} else {
									if (targetPointsHelper.getPointToStart() != null) {
										targetPointsHelper.clearStartPoint(true);
										app.getSettings().backupPointToStart();
									}
								}
							}
						}
						dismiss();
					}
				}).create();
		items.add(myLocationItem);
	}

	private void createSelectOnTheMapItem() {
		BaseBottomSheetItem selectOnTheMapItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setTitle(getString(R.string.shared_string_select_on_map))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = (MapActivity) getActivity();
						if (mapActivity != null) {
							MapRouteInfoMenu menu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
							menu.selectOnScreen(target, intermediate);
						}
						dismiss();
					}
				})
				.create();
		items.add(selectOnTheMapItem);
	}

	private void createFavouritesItem() {
		BaseBottomSheetItem favouritesItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_fav_dark))
				.setTitle(getString(R.string.shared_string_favorites))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = (MapActivity) getActivity();
						if (mapActivity != null) {
							FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
							FavouritesBottomSheetMenuFragment fragment = new FavouritesBottomSheetMenuFragment();
							Bundle args = new Bundle();
							args.putBoolean(FavouritesBottomSheetMenuFragment.TARGET, target);
							args.putBoolean(FavouritesBottomSheetMenuFragment.INTERMEDIATE, intermediate);
							fragment.setTargetFragment(AddPointBottomSheetDialog.this, ADD_FAVOURITE_TO_ROUTE_REQUEST_CODE);
							fragment.setArguments(args);
							fragment.show(fragmentManager, FavouritesBottomSheetMenuFragment.TAG);
						}
					}
				})
				.create();
		items.add(favouritesItem);
	}

	private void createMarkersItem() {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		final View markersView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_double_item, null);

		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarkersHelper.MapMarker> markers = markersHelper.getMapMarkers();
		MapMarkersHelper.MapMarker marker = null;
		if (markers.size() > 0) {
			marker = markers.get(0);
		}
		TextView firstTitle = (TextView) markersView.findViewById(R.id.first_title);
		TextView secondTitle = (TextView) markersView.findViewById(R.id.second_title);
		ImageView firstIcon = (ImageView) markersView.findViewById(R.id.first_icon);
		ImageView secondIcon = (ImageView) markersView.findViewById(R.id.second_icon);

		firstTitle.setText(R.string.shared_string_markers);
		firstIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_flag_dark));

		if (marker != null) {
			secondTitle.setText(marker.getName(getContext()));
			secondIcon.setImageDrawable(MapMarkerDialogHelper.getMapMarkerIcon(app, marker.colorIndex));

		}
		markersView.findViewById(R.id.first_item).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					MapRouteInfoMenu menu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
					menu.selectMapMarker(-1, target, intermediate);
					dismiss();
				}
			}
		});
		markersView.findViewById(R.id.second_item).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					MapRouteInfoMenu menu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
					menu.selectMapMarker(0, target, intermediate);
					dismiss();
				}
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(markersView).create());
	}

	private void createSwitchStartAndEndItem() {
		final View switchStartAndEndView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.bottom_sheet_item_simple_56dp, null);
		TextView title = (TextView) switchStartAndEndView.findViewById(R.id.title);
		ImageView iconIv = (ImageView) switchStartAndEndView.findViewById(R.id.icon);
		iconIv.setImageDrawable(getContentIcon(R.drawable.ic_action_change_navigation_points));

		String titleS = getString(R.string.swap_start_and_destination);
		SpannableString titleSpan = new SpannableString(titleS);
		int firstIndex = titleS.indexOf(" ");
		if (firstIndex != -1) {
			Typeface typeface = FontCache.getRobotoMedium(getContext());
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), firstIndex, titleS.indexOf(" ", firstIndex + 1), 0);
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), titleS.lastIndexOf(" "), titleS.length(), 0);
		}
		title.setText(titleSpan);

		switchStartAndEndView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					TargetPointsHelper targetsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
					WaypointDialogHelper.switchStartAndFinish(targetsHelper, targetsHelper.getPointToNavigate(),
							mapActivity, targetsHelper.getPointToStart(), mapActivity.getMyApplication(),
							mapActivity.getDashboard().getWaypointDialogHelper());
				}
				dismiss();
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(switchStartAndEndView).create());
	}
}