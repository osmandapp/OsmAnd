package net.osmand.plus.dashboard;

import android.support.v4.app.Fragment;
import net.osmand.plus.OsmandApplication;

/**
 * Created by Denis
 * on 24.11.2014.
 */
public class DashBaseFragment extends Fragment {

	public OsmandApplication getMyApplication(){
		if (getActivity() == null){
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public void refreshCard(){

	}

}
