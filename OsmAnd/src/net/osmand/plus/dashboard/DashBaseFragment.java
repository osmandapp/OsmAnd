package net.osmand.plus.dashboard;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.Fragment;

/**
 * Created by Denis on 24.11.2014.
 */
public abstract class DashBaseFragment extends Fragment {

	protected DashboardOnMap dashboard;

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
	
	public abstract void onOpenDash() ;
	
	public void onCloseDash() {
	}
	
	@Override
	public final void onPause() {
		// use on close 
		super.onPause();
		onCloseDash();
	}
	
	@Override
	public final void onResume() {
		// use on open update
		super.onResume();
		if(dashboard != null && dashboard.isVisible() && getView() != null) {
			onOpenDash();
		}
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

	protected void startFavoritesActivity(int tab) {
		Activity activity = getActivity();
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
		favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		getMyApplication().getSettings().FAVORITES_TAB.set(tab);
		activity.startActivity(favorites);
	}

}
