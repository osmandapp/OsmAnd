package net.osmand.plus.dashboard;

import com.actionbarsherlock.app.SherlockFragment;
import net.osmand.plus.OsmandApplication;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashBaseFragment extends SherlockFragment {

	public OsmandApplication getMyApplication(){
		return (OsmandApplication) getActivity().getApplication();
	}
}
