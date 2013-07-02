/**
 * 
 */

package net.osmand.plus.activities;



import java.util.List;

import net.osmand.Location;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.RouteInfoLayer;
import net.osmand.plus.views.TurnPathHelper;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * 
 */
public class ShowRouteInfoActivity extends OsmandListActivity {


	private static final int SAVE = 0;
	private RoutingHelper helper;
	private TextView header;
	private DisplayMetrics dm;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		header = new TextView(this);
		helper = ((OsmandApplication)getApplication()).getRoutingHelper();
		lv.addHeaderView(header);
		setContentView(lv);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SAVE) {
			MapActivityActions.createSaveDirections(ShowRouteInfoActivity.this).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		header.setText(helper.getGeneralRouteInformation());
		float f = Math.min(dm.widthPixels/(dm.density*160),dm.heightPixels/(dm.density*160));
		if (f >= 3) {
			// large screen
			header.setTextSize(dm.scaledDensity * 23);
		}
		setListAdapter(new RouteInfoAdapter(((OsmandApplication)getApplication()).getRoutingHelper().getRouteDirections()));
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {
		// headers are included
		if(position < 1){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			RouteInfoLayer.directionInfo = position - 1;
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			settings.setMapLocationToShow(loc.getLatitude(),loc.getLongitude(),
					Math.max(13, settings.getLastKnownMapZoom()), null, item.getDescriptionRoute() + " " + getTimeDescription(item), null);
			MapActivity.launchMapActivityMoveToTop(this);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenuItem(menu, SAVE, R.string.save_route_as_gpx, 
				R.drawable.ic_action_gsave_light, R.drawable.ic_action_gsave_dark,
				MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	class RouteInfoAdapter extends ArrayAdapter<RouteDirectionInfo> {
		RouteInfoAdapter(List<RouteDirectionInfo> list) {
			super(ShowRouteInfoActivity.this, R.layout.route_info_list_item, list);
			this.setNotifyOnChange(false);
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.route_info_list_item, parent, false);
			}
			RouteDirectionInfo model = (RouteDirectionInfo) getItem(position);
			TextView label = (TextView) row.findViewById(R.id.description);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
			TextView timeLabel = (TextView) row.findViewById(R.id.time);
			ImageView icon = (ImageView) row.findViewById(R.id.direction);
			
			TurnPathHelper.RouteDrawable drawable = new TurnPathHelper.RouteDrawable(getResources());
			drawable.setRouteType(model.getTurnType());
			icon.setImageDrawable(drawable);
			
			
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, getMyApplication()));
			label.setText(model.getDescriptionRoute());
			String timeText = getTimeDescription(model);
			timeLabel.setText(timeText);
			return row;
		}


	}
	private String getTimeDescription(RouteDirectionInfo model) {
		int seconds = model.getExpectedTime() % 60;
		int min = (model.getExpectedTime() / 60) % 60;
		int hours = (model.getExpectedTime() / 3600);
		String timeText;
		if (hours == 0) {
			timeText = String.format("%02d:%02d", min, seconds); //$NON-NLS-1$
		} else {
			timeText = String.format("%d:%02d:%02d", hours, min, seconds); //$NON-NLS-1$ 
		}
		return timeText;
	}
}

