package net.osmand.plus.dashboard;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.helpers.FontCache;
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

/**
 * Created by Denis on 24.11.2014.
 */
public class DashSearchFragment extends DashBaseFragment {

	public static final String TAG = "DASH_SEARCH_FRAGMENT";

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_search_fragment, container, false);
		setupButtons(view);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.search_for)).setTypeface(typeface);
		((Button) view.findViewById(R.id.recents)).setTypeface(typeface);
		return view;
	}


	protected void searchActivity(final Activity activity, final OsmAndAppCustomization appCustomization, int tab) {
		final Intent search = new Intent(activity, appCustomization.getSearchActivity());
		//search.putExtra(SearchActivity.SHOW_ONLY_ONE_TAB, true);
		search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		getMyApplication().getSettings().SEARCH_TAB.set(tab);
		activity.startActivity(search);
	}
	
	private void setupButtons(View view){
		final Activity activity = getActivity();
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
//		EditText searchText = (EditText) view.findViewById(R.id.search_text);
//		searchText.addTextChangedListener(textWatcher);
		(view.findViewById(R.id.poi)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchActivity(activity, appCustomization, SearchActivity.POI_TAB_INDEX);
			}
		});

		(view.findViewById(R.id.address)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchActivity(activity, appCustomization, SearchActivity.ADDRESS_TAB_INDEX);
			}
		});

		(view.findViewById(R.id.coord)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchActivity(activity, appCustomization, SearchActivity.LOCATION_TAB_INDEX);
			}
		});
		(view.findViewById(R.id.recents)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchActivity(activity, appCustomization, SearchActivity.HISTORY_TAB_INDEX);
			}
		});
	}
	
	@Override
	public void onOpenDash() {
		
	}
}
