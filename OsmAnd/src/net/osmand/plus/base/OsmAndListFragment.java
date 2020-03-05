package net.osmand.plus.base;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.fragment.app.ListFragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class OsmAndListFragment extends ListFragment {
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(
				getResources().getColor(
						getMyApplication().getSettings().isLightContent() ? R.color.list_background_color_light
								: R.color.list_background_color_dark));
	}
	
	public abstract ArrayAdapter<?> getAdapter();

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
}
