package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashSearchFragment extends DashBaseFragment {

	public static final String TAG = "DASH_SEARCH_FRAGMENT";

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_search_fragment, container, false);
		setupButtons(view);
		Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Medium.ttf");
		((TextView) view.findViewById(R.id.search_for)).setTypeface(typeface);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	private void setupButtons(View view){
		final Activity activity = getActivity();
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		(view.findViewById(R.id.poi)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.POI_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(view.findViewById(R.id.address)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.ADDRESS_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(view.findViewById(R.id.coord)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.LOCATION_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(view.findViewById(R.id.fav_btn)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.FAVORITES_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(view.findViewById(R.id.history)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.HISTORY_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(view.findViewById(R.id.transport)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.TRANSPORT_TAB_INDEX);
				activity.startActivity(search);
			}
		});
	}
}
