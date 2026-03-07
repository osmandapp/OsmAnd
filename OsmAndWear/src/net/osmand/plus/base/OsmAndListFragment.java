package net.osmand.plus.base;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.fragment.app.ListFragment;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;

public abstract class OsmAndListFragment extends ListFragment {
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		OsmandApplication app = getMyApplication();
		boolean nightMode = !app.getSettings().isLightContent();
		getListView().setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
	}
	
	public abstract ArrayAdapter<?> getAdapter();

	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
}
