package net.osmand.plus.dashboard;

import com.actionbarsherlock.app.SherlockFragment;
import net.osmand.plus.OsmandApplication;
import android.view.View;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashBaseFragment extends SherlockFragment {

	public OsmandApplication getMyApplication(){
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onResume() {
		super.onResume();
		//This fixes the "list are empty when returning to the Dashboard" issue
		View view = getView();
		if (view != null) {
			view.invalidate();
		}
	}

}
