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
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
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


	private RoutingHelper helper;
	private TextView header;
	private DisplayMetrics dm;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		header = new TextView(this);
		helper = RoutingHelper.getInstance(this);
		
		lv.addHeaderView(header);
		setContentView(lv);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		int dist = helper.getLeftDistance();
		int hours = helper.getLeftTime() / (60 * 60);
		int minutes = (helper.getLeftTime() / 60) % 60;
		header.setText(MessageFormat.format(getString(R.string.route_general_information), MapUtils.getFormattedDistance(dist),
				hours, minutes));
		float f = Math.min(dm.widthPixels/(dm.density*160),dm.heightPixels/(dm.density*160));
		if (f >= 3) {
			// large screen
			header.setTextSize(dm.scaledDensity * 23);
		}
		setListAdapter(new RouteInfoAdapter(RoutingHelper.getInstance(this).getRouteDirections()));
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		if(position == 0){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			OsmandSettings.setMapLocationToShow(this, loc.getLatitude(),loc.getLongitude());
			startActivity(new Intent(this, MapActivity.class));
		}
	}
	

	class RouteDrawable extends Drawable {
		Paint paintRouteDirection;
		Path p = new Path();
		Path dp = new Path();
		
		public RouteDrawable(){
			paintRouteDirection = new Paint();
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(Color.rgb(100, 0, 255));
			paintRouteDirection.setAntiAlias(true);
		}
		

		@Override
		protected void onBoundsChange(Rect bounds) {
			Matrix m = new Matrix();
			m.setScale(bounds.width()/96f, bounds.height()/96f);
			p.transform(m, dp);
		}
		
		public void setRouteType(TurnType t){
			MapInfoLayer.calcTurnPath(p, t, null);
			onBoundsChange(getBounds());
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawPath(dp, paintRouteDirection);
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

