package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

public abstract class OsmAndListFragment extends ListFragment {
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(
				getResources().getColor(
						getMyApplication().getSettings().isLightContent() ? R.color.bg_color_light
								: R.color.bg_color_dark));
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
}
