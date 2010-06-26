/**
 * 
 */
package com.osmand.activities;

import java.text.MessageFormat;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.activities.RoutingHelper.RouteDirectionInfo;
import com.osmand.activities.RoutingHelper.TurnType;
import com.osmand.osm.MapUtils;
import com.osmand.views.MapInfoLayer;

/**
 * 
 */
public class ShowRouteInfoActivity extends ListActivity {

	

	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		TextView header = new TextView(this);
		RoutingHelper helper = RoutingHelper.getInstance(this);
		int time = helper.getLeftTime()* 1000;
		int dist = helper.getLeftDistance();
		header.setText(MessageFormat.format(getString(R.string.route_general_information), MapUtils.getFormattedDistance(dist),
				DateFormat.format("kk:mm", time))); //$NON-NLS-1$
		lv.addHeaderView(header);
		setContentView(lv);
		setListAdapter(new RouteInfoAdapter(RoutingHelper.getInstance(this).getRouteDirections()));
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		RoutingHelper inst = RoutingHelper.getInstance(this);
		Location loc = inst.getLocationFromRouteDirection(item);
		if(loc != null){
			OsmandSettings.setMapLocationToShow(this, loc.getLatitude(),loc.getLongitude());
			startActivity(new Intent(this, MapActivity.class));
		}
	}
	

	class RouteDrawable extends Drawable {
		Paint paintRouteDirection;
		Path p = new Path();
		Matrix m = new Matrix();
		public RouteDrawable(){
			m.setScale(0.33f, 0.33f);
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(Color.rgb(100, 0, 255));
			paintRouteDirection.setAntiAlias(true);
		}
		
		
		public void setRouteType(TurnType t){
			MapInfoLayer.calcTurnPath(p, t, m);
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawPath(p, paintRouteDirection);
		}

		@Override
		public int getOpacity() {
			return 0;
		}

		@Override
		public void setAlpha(int alpha) {
			paintRouteDirection.setAlpha(alpha);
			
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			paintRouteDirection.setColorFilter(cf);
		}
		
	}
	
	class RouteInfoAdapter extends ArrayAdapter<RouteDirectionInfo> {
		RouteInfoAdapter(List<RouteDirectionInfo> list) {
			super(ShowRouteInfoActivity.this, R.layout.route_info_list_item, list);
			this.setNotifyOnChange(false);
		}


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
			
			if(!(icon.getDrawable() instanceof RouteDrawable)){
				icon.setImageDrawable(new RouteDrawable());
			}
			((RouteDrawable) icon.getDrawable()).setRouteType(model.turnType);
			distanceLabel.setText(MapUtils.getFormattedDistance(model.distance));
			label.setText(model.descriptionRoute);
			if(model.expectedTime < 3600){
				timeLabel.setText(DateFormat.format("mm:ss", model.expectedTime * 1000)); //$NON-NLS-1$
			} else {
				timeLabel.setText(DateFormat.format("k:mm:ss", model.expectedTime * 1000)); //$NON-NLS-1$
			}
			return row;
		}
	}

}

