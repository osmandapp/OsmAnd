package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import android.support.v4.app.ListFragment;

public abstract class OsmAndListFragment extends ListFragment {
	


	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
}
