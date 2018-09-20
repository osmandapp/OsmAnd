package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.PointHistoryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashRecentsFragment extends DashLocationFragment {
	public static final String TAG = "DASH_RECENTS_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_history;

	private List<ImageView> arrows = new ArrayList<ImageView>();
	List<PointHistoryEntry> points = new ArrayList<>();
	private static final String ROW_NUMBER_TAG = TAG + "_row_number";
	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			TAG, DashRecentsFragment.class, SHOULD_SHOW_FUNCTION, 80, ROW_NUMBER_TAG);

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		((TextView)view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDashboard();
				MapActivity activity = (MapActivity) getActivity();
				activity.showQuickSearch(MapActivity.ShowQuickSearchMode.NEW, false);
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		setupRecents();
	}

	public void setupRecents() {
		View mainView = getView();
		SearchHistoryHelper helper = SearchHistoryHelper.getInstance((OsmandApplication) getActivity()
				.getApplicationContext());
		points = helper.getHistoryPoints();
		arrows.clear();
		if (points.size() == 0) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		LinearLayout recents = (LinearLayout) mainView.findViewById(R.id.items);
		recents.removeAllViews();
		DashboardOnMap.handleNumberOfRows(points, getMyApplication().getSettings(), ROW_NUMBER_TAG);
		LatLon loc = getDefaultLocation();
		List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();
		for (final PointHistoryEntry point : points) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.search_history_list_item, null, false);
			SearchHistoryFragment.udpateHistoryItem(point, view, loc, getActivity(), getMyApplication());
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);
			((ImageView) view.findViewById(R.id.navigate_to)).setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_gdirections_dark));
			view.findViewById(R.id.navigate_to).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), point.getLat(),
							point.getLon(), point.getName());
				}
			});
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getMyApplication().getSettings().setMapLocationToShow(point.getLat(), point.getLon(),
							15, point.getName(), true, point); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			DashLocationView dv = new DashLocationView((ImageView) view.findViewById(R.id.direction),
					(TextView) view.findViewById(R.id.distance), new LatLon(point.getLat(),
							point.getLon()));
			distances.add(dv);
			recents.addView(view);
		}
		this.distances = distances;
	}
}
