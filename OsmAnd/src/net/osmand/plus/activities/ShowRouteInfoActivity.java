/**
 * 
 */
package net.osmand.plus.activities;

import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.TurnPathHelper;
import android.app.Dialog;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
		TextView linkSaveAs = new TextView(this);
		helper = ((OsmandApplication)getApplication()).getRoutingHelper();
		lv.addHeaderView(header);
		lv.addHeaderView(linkSaveAs);
		final CharSequence link = getText(R.string.save_route_as_gpx);
		SpannableString content = new SpannableString(link);
		content.setSpan(new ClickableSpan() {
			@Override
			public void onClick(View widget) {
				MapActivityActions.createSaveDirections(ShowRouteInfoActivity.this).show();
			}
			
			@Override
			public void updateDrawState(TextPaint ds) {
				super.updateDrawState(ds);
				ds.setColor(Color.GREEN);
			}
		}, 0, link.length(), 0);
		linkSaveAs.setText(content);
		linkSaveAs.setMovementMethod(LinkMovementMethod.getInstance());
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
		// headers are included
		if(position < 1){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 2);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			settings.setMapLocationToShow(loc.getLatitude(),loc.getLongitude(),
					Math.max(13, settings.getLastKnownMapZoom()), null, item.getDescriptionRoute() + " " + getTimeDescription(item), null);
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
			Dialog dlg = MapActivityActions.createSaveDirections(this);
			dlg.show();
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
				icon.setImageDrawable(new TurnPathHelper.RouteDrawable(getResources()));
			}
			((TurnPathHelper.RouteDrawable) icon.getDrawable()).setRouteType(model.getTurnType());
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, ShowRouteInfoActivity.this));
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

