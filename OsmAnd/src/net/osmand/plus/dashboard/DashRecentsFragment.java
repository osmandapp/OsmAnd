package net.osmand.plus.dashboard;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashRecentsFragment extends DashLocationFragment implements OnSegmentSelectedListener {

	public static final String TAG = "DASH_RECENTS_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_history;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";
	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			TAG, DashRecentsFragment.class, SHOULD_SHOW_FUNCTION, 80, ROW_NUMBER_TAG);

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		View view = inflate(R.layout.dash_common_fragment, container, false);

		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			closeDashboard();
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, false);
			}
		});

		return view;
	}

	@Override
	public void onOpenDash() {
		setupRecent();
	}

	public void setupRecent() {
		View mainView = getView();
		if (mainView == null) return;

		SearchHistoryHelper helper = app.getSearchHistoryHelper();
		List<HistoryEntry> historyEntries = helper.getHistoryEntries(true);

		if (!settings.SEARCH_HISTORY.get() || Algorithms.isEmpty(historyEntries)) {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), false);
			return;
		}

		AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), true);

		LinearLayout llRecentItems = mainView.findViewById(R.id.items);
		llRecentItems.removeAllViews();

		DashboardOnMap.handleNumberOfRows(historyEntries, settings, ROW_NUMBER_TAG);

		LatLon loc = getDefaultLocation();
		for (HistoryEntry historyEntry : historyEntries) {
			View itemView = inflate(R.layout.search_history_list_item);
			updateHistoryItem(historyEntry, itemView, loc);
			itemView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			itemView.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);

			Drawable navIcon = uiUtilities.getThemedIcon(R.drawable.ic_action_gdirections_dark);
			((ImageView) itemView.findViewById(R.id.navigate_to)).setImageDrawable(navIcon);

			itemView.findViewById(R.id.navigate_to).setOnClickListener(v -> runNavigation(historyEntry));
			itemView.setOnClickListener(v -> showContextMenu(historyEntry));
			setupDistanceAndDirection(historyEntry, itemView);

			llRecentItems.addView(itemView);
		}
	}

	private void updateHistoryItem(@NonNull HistoryEntry historyEntry, @NonNull View row, @Nullable LatLon location) {
		TextView nameText = row.findViewById(R.id.name);
		TextView distanceText = row.findViewById(R.id.distance);
		ImageView direction = row.findViewById(R.id.direction);
		direction.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_direction_arrow, R.color.color_distance));

		String distance = "";
		if (location != null) {
			int dist = (int) (MapUtils.getDistance(location, historyEntry.getLat(), historyEntry.getLon()));
			distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  ";
		}
		distanceText.setText(distance);

		PointDescription pointDescription = historyEntry.getName();
		nameText.setText(pointDescription.getSimpleName(app, false), BufferType.SPANNABLE);
		ImageView icon = row.findViewById(R.id.icon);
		icon.setImageDrawable(uiUtilities.getThemedIcon(pointDescription.getItemIcon()));

		String typeName = pointDescription.getTypeName();
		if (!Algorithms.isEmpty(typeName)) {
			ImageView group = row.findViewById(R.id.type_name_icon);
			group.setVisibility(View.VISIBLE);
			group.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_group_name_16));
			((TextView) row.findViewById(R.id.type_name)).setText(typeName);
		} else {
			row.findViewById(R.id.type_name_icon).setVisibility(View.GONE);
			((TextView) row.findViewById(R.id.type_name)).setText("");
		}
	}

	private void runNavigation(@NonNull HistoryEntry historyEntry) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) return;

		PointDescription pd = historyEntry.getName();
		if (pd.isGpxFile()) {
			File file = new File(app.getAppPath(GPX_INDEX_DIR), pd.getName());
			GpxSelectionHelper.getGpxFile(mapActivity, file, true, gpxFile -> {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					navigateGpxFile(gpxFile, activity);
				}
				return false;
			});
		} else {
			DirectionsDialogs.directionsToDialogAndLaunchMap(mapActivity,
					historyEntry.getLat(), historyEntry.getLon(), historyEntry.getName());
		}
	}

	private void navigateGpxFile(@NonNull GpxFile gpxFile, @NonNull MapActivity mapActivity) {
		if (TrackSelectSegmentBottomSheet.shouldShowForGpxFile(gpxFile)) {
			FragmentManager fm = mapActivity.getSupportFragmentManager();
			TrackSelectSegmentBottomSheet.showInstance(fm, gpxFile, this);
		} else {
			mapActivity.getMapActions().startNavigationForGpx(gpxFile, mapActivity);
			closeDashboard();
		}
	}

	private void showContextMenu(@NonNull HistoryEntry historyEntry) {
		Activity activity = getActivity();
		if (activity == null) return;

		PointDescription pointDescription = historyEntry.getName();
		if (pointDescription.isGpxFile()) {
			File tracksDir = app.getAppPath(GPX_INDEX_DIR);
			String relativeGpxPath = pointDescription.getName();
			File gpxFile = new File(tracksDir, relativeGpxPath);
			if (gpxFile.isFile()) {
				app.getSearchHistoryHelper().addNewItemToHistory(0, 0, pointDescription, HistorySource.SEARCH);
				TrackMenuFragment.openTrack(activity, gpxFile, null);
				closeDashboard();
			}
		} else {
			settings.setMapLocationToShow(historyEntry.getLat(), historyEntry.getLon(),
					15, historyEntry.getName(), true, historyEntry);
			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	private void setupDistanceAndDirection(@NonNull HistoryEntry historyEntry, @NonNull View itemView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) return;

		ImageView directionArrow = itemView.findViewById(R.id.direction);
		TextView distanceText = itemView.findViewById(R.id.distance);
		PointDescription pd = historyEntry.getName();

		if (pd.isGpxFile()) {
			String relativeGpxPath = pd.getName();
			File file = new File(app.getAppPath(GPX_INDEX_DIR), relativeGpxPath);
			GpxSelectionHelper.getGpxFile(mapActivity, file, false, gpxFile -> {
				KQuadRect gpxRect = gpxFile.getRect();
				LatLon latLon = new LatLon(gpxRect.centerY(), gpxRect.centerX());
				DashLocationView locationView = new DashLocationView(directionArrow, distanceText, latLon);
				distances.add(locationView);
				updateAllWidgets();
				return true;
			});
		} else {
			LatLon latLon = new LatLon(historyEntry.getLat(), historyEntry.getLon());
			DashLocationView locationView = new DashLocationView(directionArrow, distanceText, latLon);
			distances.add(locationView);
		}
	}

	@Override
	public void onSegmentSelect(@NonNull GpxFile gpxFile, int selectedSegment) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForSegment(gpxFile, selectedSegment, mapActivity);
			closeDashboard();
		}
	}

	@Override
	public void onRouteSelected(@NonNull GpxFile gpxFile, int selectedRoute) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForRoute(gpxFile, selectedRoute, mapActivity);
			closeDashboard();
		}
	}
}