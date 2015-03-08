package net.osmand.plus.dashboard;

import android.app.Activity;
import android.location.Location;
import android.support.v4.app.Fragment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

/**
 * Created by Denis on 24.11.2014.
 */
public abstract class DashBaseFragment extends Fragment {

	private DashboardOnMap dashboard;

	public OsmandApplication getMyApplication(){
		if (getActivity() == null){
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(activity instanceof MapActivity) {
			dashboard = ((MapActivity) activity).getDashboard();
			dashboard.onAttach(this);
		}
	}
	
	public void onOpenUpdate() {
		// TODO
	}
	
	
	public void onLocationCompassChanged(Location l, double compassValue) {
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		if(dashboard != null) {
			dashboard.onDetach(this);
			dashboard = null;
		}
	}

}
