package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.FontCache;

/**
 * Created by Denis on
 * 24.11.2014.
 */
public class DashSearchFragment extends DashBaseFragment {
	public static final String TAG = "DASH_SEARCH_FRAGMENT";
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return R.string.search_for;
				}
			};

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_search_fragment, container, false);
		setupButtons(view);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.search_for)).setTypeface(typeface);
		((Button) view.findViewById(R.id.recents)).setTypeface(typeface);

		return view;
	}


	protected void searchActivity(final Activity activity, final OsmAndAppCustomization appCustomization, int tab) {
		Intent newIntent = new Intent(activity, appCustomization.getSearchActivity());
		// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		LatLon loc = ((MapActivity)activity).getMapLocation();
		newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
		newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
		if(((MapActivity)activity).getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
		}
		newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		getMyApplication().getSettings().SEARCH_TAB.set(tab);
		activity.startActivity(newIntent);
	}

	private void setupButtons(View view) {
		final Activity activity = getActivity();
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();

		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();
		Button btn = (Button) view.findViewById(R.id.poi);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDashboard();
				searchActivity(activity, appCustomization, SearchActivity.POI_TAB_INDEX);
			}
		});
		btn.setCompoundDrawablesWithIntrinsicBounds(null, iconsCache.getIcon(R.drawable.ic_action_info2,
				light ? R.color.dash_search_icon_light : R.color.dashboard_subheader_text_dark), null, null);

		btn = (Button)view.findViewById(R.id.address);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDashboard();
				searchActivity(activity, appCustomization, SearchActivity.ADDRESS_TAB_INDEX);
			}
		});
		btn.setCompoundDrawablesWithIntrinsicBounds(null, iconsCache.getIcon(R.drawable.ic_action_home2,
				light ? R.color.dash_search_icon_light : R.color.dashboard_subheader_text_dark), null, null);

		btn = (Button) view.findViewById(R.id.coord);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDashboard();
				searchActivity(activity, appCustomization, SearchActivity.LOCATION_TAB_INDEX);
			}
		});
		btn.setCompoundDrawablesWithIntrinsicBounds(null, iconsCache.getIcon(R.drawable.ic_action_marker2,
				light ? R.color.dash_search_icon_light : R.color.dashboard_subheader_text_dark), null, null);

		(view.findViewById(R.id.recents)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeDashboard();
				searchActivity(activity, appCustomization, SearchActivity.HISTORY_TAB_INDEX);
			}
		});
	}

	@Override
	public void onOpenDash() {

	}
}
