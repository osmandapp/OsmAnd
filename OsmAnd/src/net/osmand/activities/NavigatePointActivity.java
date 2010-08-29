package net.osmand.activities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Locale;

import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.views.OsmandMapTileView;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class NavigatePointActivity extends Activity {
	Dialog dlg;
	MapActivity activity; 
	int currentFormat = Location.FORMAT_DEGREES;
	
	// dialog constructor
	public NavigatePointActivity(MapActivity activity){
		this.activity = activity;
		dlg =  new Dialog(activity);
	}
	// activity constructor
	public NavigatePointActivity() {
	}
	
	public void showDialog(){
		dlg.setContentView(R.layout.navigate_point);
		dlg.setTitle(R.string.map_specify_point);
		LatLon loc = activity.getMapLocation();
		initUI(loc.getLatitude(), loc.getLongitude());
		dlg.show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LatLon loc = OsmandSettings.getLastKnownMapLocation(this);
		setContentView(R.layout.navigate_point);
		setTitle(R.string.map_specify_point);
		initUI(loc.getLatitude(), loc.getLongitude());
		((Button) findViewById(R.id.Cancel)).setText(getString(R.string.navigate_to));
	}
	
	@Override
	public View findViewById(int id) {
		if(dlg != null){
			return dlg.findViewById(id);
		}
		return super.findViewById(id);
	}
	
	public void close(){
		if(dlg != null){
			dlg.dismiss();
		} else {
			Intent newIntent = new Intent(this, MapActivity.class);
			startActivity(newIntent);
		}
	}
	
	public void initUI(double latitude, double longitude){
		latitude = MapUtils.checkLatitude(latitude);
		longitude = MapUtils.checkLongitude(longitude);
		((TextView)findViewById(R.id.LatitudeEdit)).setText(convert(latitude, Location.FORMAT_DEGREES));
		((TextView)findViewById(R.id.LongitudeEdit)).setText(convert(longitude, Location.FORMAT_DEGREES));
		((RadioButton)findViewById(R.id.Format1)).setChecked(true);
		((RadioGroup)findViewById(R.id.RadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				int newFormat = currentFormat;
				if(checkedId == R.id.Format1){
					newFormat = Location.FORMAT_DEGREES;
				} else if(checkedId == R.id.Format2){
					newFormat = Location.FORMAT_MINUTES;
				} else if(checkedId == R.id.Format3){
					newFormat = Location.FORMAT_SECONDS;
				}
				try { 
					double lat = Location.convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
					double lon = Location.convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
					((TextView)findViewById(R.id.LatitudeEdit)).setText(convert(lat, newFormat));
					((TextView)findViewById(R.id.LongitudeEdit)).setText(convert(lon, newFormat));
				} catch (RuntimeException e) {
					((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.VISIBLE);
					((TextView) findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
					Log.w(LogUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
				}
			}
			
		});
		((Button) findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dlg == null){
					showOnMap(true);
				} else {
					close();
				}
			}
		});
		((Button) findViewById(R.id.ShowOnMap)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(false);
			}
		});
	}
	
	public void showOnMap(boolean navigate){
		try {
			// TODO there is a bug in android implementation : doesn't convert if min = 59.23 or sec = 59.32 or deg=179.3
			double lat = Location.convert(((TextView) findViewById(R.id.LatitudeEdit)).getText().toString());
			double lon = Location.convert(((TextView) findViewById(R.id.LongitudeEdit)).getText().toString());
			
			if(navigate){
				OsmandSettings.setPointToNavigate(this, lat, lon);
			} else {
				// in case when it is dialog
				if(activity != null) {
					OsmandMapTileView v = activity.getMapView();
					activity.getMapView().getAnimatedDraggingThread().startMoving(v.getLatitude(), v.getLongitude(),
							lat, lon, v.getZoom(), v.getZoom(), v.getSourceTileSize(), v.getRotate(), true);
				} else {
					OsmandSettings.setMapLocationToShow(this, lat, lon, MessageFormat.format(getString(R.string.search_history_navigate_to), lat, lon));
				}
			}
			close();
		} catch (RuntimeException e) {
			((TextView) findViewById(R.id.ValidateTextView)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.ValidateTextView)).setText(R.string.invalid_locations);
			Log.w(LogUtil.TAG, "Convertion failed", e); //$NON-NLS-1$
		}
	}
	
	
	// THIS code is copied from Location.convert() in order to change locale
 	public static final int FORMAT_DEGREES = 0;
	public static final int FORMAT_MINUTES = 1;
	public static final int FORMAT_SECONDS = 2;

	public static String convert(double coordinate, int outputType) {
		if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate)) {
			throw new IllegalArgumentException("coordinate=" + coordinate); //$NON-NLS-1$
		}
		if ((outputType != FORMAT_DEGREES) && (outputType != FORMAT_MINUTES) && (outputType != FORMAT_SECONDS)) {
			throw new IllegalArgumentException("outputType=" + outputType); //$NON-NLS-1$
		}

		StringBuilder sb = new StringBuilder();

		// Handle negative values
		if (coordinate < 0) {
			sb.append('-');
			coordinate = -coordinate;
		}

		DecimalFormat df = new DecimalFormat("###.#####", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
		if (outputType == FORMAT_MINUTES || outputType == FORMAT_SECONDS) {
			int degrees = (int) Math.floor(coordinate);
			sb.append(degrees);
			sb.append(':');
			coordinate -= degrees;
			coordinate *= 60.0;
			if (outputType == FORMAT_SECONDS) {
				int minutes = (int) Math.floor(coordinate);
				sb.append(minutes);
				sb.append(':');
				coordinate -= minutes;
				coordinate *= 60.0;
			}
		}
		sb.append(df.format(coordinate));
		return sb.toString();
	}
	
	

}
