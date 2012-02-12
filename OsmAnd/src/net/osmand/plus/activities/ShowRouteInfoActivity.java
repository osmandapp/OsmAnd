/**
 * 
 */
package net.osmand.plus.activities;

import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.views.TurnPathHelper;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 */
public class ShowRouteInfoActivity extends OsmandListActivity {


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
		if(position == 0){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			OsmandSettings settings = OsmandSettings.getOsmandSettings(this);
			settings.setMapLocationToShow(loc.getLatitude(),loc.getLongitude(),
					Math.max(13, settings.getLastKnownMapZoom()));
			MapActivity.launchMapActivityMoveToTop(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, 0, 0, getString(R.string.edit_filter_save_as_menu_item)+"...");
		item.setIcon(android.R.drawable.ic_menu_save);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == 0){
			//mapActivityActions.saveDirections();
		} else {
			return false;
		}
		return true;
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
			
			if(!(icon.getDrawable() instanceof TurnPathHelper.RouteDrawable)){
				icon.setImageDrawable(new TurnPathHelper.RouteDrawable());
			}
			((TurnPathHelper.RouteDrawable) icon.getDrawable()).setRouteType(model.turnType);
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, ShowRouteInfoActivity.this));
			label.setText(model.descriptionRoute);
			int seconds = model.expectedTime % 60;
			int min = (model.expectedTime / 60) % 60;
			int hours = (model.expectedTime / 3600);
			if (hours == 0) {
				timeLabel.setText(String.format("%02d:%02d", min, seconds)); //$NON-NLS-1$
			} else {
				timeLabel.setText(String.format("%d:%02d:%02d", hours, min, seconds)); //$NON-NLS-1$ 
			}
			return row;
		}
	}
}

