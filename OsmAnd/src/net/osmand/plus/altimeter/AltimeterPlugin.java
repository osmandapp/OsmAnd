package net.osmand.plus.altimeter;

import net.osmand.plus.OsmandPlugin;
import android.app.Activity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.plus.activities.MapActivity;
import android.graphics.Paint;
import net.osmand.plus.R;

import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.content.Context;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.view.LayoutInflater;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import java.lang.NumberFormatException;
import net.osmand.Location;

import java.util.Locale;

import android.util.Log;
import android.content.pm.FeatureInfo;

public class AltimeterPlugin extends OsmandPlugin implements SensorEventListener{
	private static final String TAG = "osmand.altimeter";

	private static final String ID = "osmand.altimeter";
	private OsmandApplication app;
	private TextInfoWidget altimeterControl;

	private SensorManager _sm;
	private float pressure;
	private double qnh = QNH.standardAtmospherePressure;

  private	EditText edittextQNH;
  private	EditText edittextAltitude;
  private	boolean promoteChange = true;

	public AltimeterPlugin(OsmandApplication app) {
		this.app = app;
		ApplicationMode.regWidgetVisibility("altimeter", ApplicationMode.DEFAULT);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_altimeter_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_altimeter_plugin_name);
	}

	@Override
        public int getAssetResourceName() {
                return 0;
        }

	@Override
	public boolean init(OsmandApplication app, Activity activity) {
		PackageManager pm = app.getPackageManager();
		if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)) {
			return false;
		}
		Log.d(TAG,"barometer found");
		return true;
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		registerWidget(activity);
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null ) {
			altimeterControl = createAltimeterControl(activity);
			mapInfoLayer.registerSideWidget(altimeterControl,
								R.drawable.widget_altitude,  R.string.map_widget_altimeter, "altimeter", false, 21);
			mapInfoLayer.recreateControls();
			updateText("0");
		}
	}

	private void updateText(String t) {
		if(altimeterControl != null){
			altimeterControl.setText(t,"");
		}
	}

	private void showDialog(final MapActivity activity) {
    Builder bld = new AlertDialog.Builder(activity);
    LayoutInflater inflater = activity.getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.altimeter_dialog, null);

    bld.setView(dialogView);
    bld.setMessage(app.getString(R.string.map_widget_altimeter));
    bld.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            try{
              qnh = Double.parseDouble(edittextQNH.getText().toString());
            }catch(NumberFormatException e){
              return;
            }
        }
      });
    bld.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
        }
      });

    edittextQNH = (EditText)dialogView.findViewById(R.id.edittext_qnh);
    edittextAltitude = (EditText)dialogView.findViewById(R.id.edittext_altitude);

    edittextQNH.addTextChangedListener(new qnhAltitudeTextWatcher(edittextAltitude){
            @Override
            String conversionMethod(double v){
                return formatAltitude(QNH.altitude(v,pressure));
            }
        });
    edittextAltitude.addTextChangedListener(new qnhAltitudeTextWatcher(edittextQNH){
            @Override
            String conversionMethod(double v){
                return formatQNH(QNH.qnh(v,pressure));
            }
        });


    edittextQNH.setText(formatQNH(qnh));
    edittextAltitude.setText(formatAltitude(QNH.altitude(qnh,pressure)));

    Button buttonUseStandardPressure = (Button) dialogView.findViewById(R.id.button_use_standard_pressure);
    buttonUseStandardPressure.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
          edittextQNH.setText(formatQNH(QNH.standardAtmospherePressure));
        }
      });

    Button buttonUseGpsAltitude = (Button) dialogView.findViewById(R.id.button_use_gps_altitude);
    final Location loc = activity.getMyApplication().getLocationProvider().getLastKnownLocation();
    if (loc != null && loc.hasAltitude()){
      final String alt = formatAltitude(loc.getAltitude());
      buttonUseGpsAltitude.setText(buttonUseGpsAltitude.getText() + " (" + alt + " m)");
      buttonUseGpsAltitude.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            edittextAltitude.setText(alt);
          }
        });
    }else buttonUseGpsAltitude.setEnabled(false);

    bld.show();
  }

  private abstract class qnhAltitudeTextWatcher implements TextWatcher {
      private EditText editTextToUpdate = null;

      public qnhAltitudeTextWatcher(EditText editTextToUpdate){
          this.editTextToUpdate = editTextToUpdate;
      }

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!promoteChange) return;
        double v;
        try{
          v = Double.parseDouble(s.toString());
        }catch(NumberFormatException e){
          return;
        }
        promoteChange = false;
        editTextToUpdate.setText(conversionMethod(v));
        promoteChange = true;
      }

      abstract String conversionMethod(double v);
    }

	private TextInfoWidget createAltimeterControl(final MapActivity activity){
		final TextInfoWidget altimeterControl = new TextInfoWidget(activity);
		altimeterControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(activity);
			}
		});
		altimeterControl.setIcons(R.drawable.widget_altitude,R.drawable.widget_altitude);
		return altimeterControl;
	}

  private SensorManager getSensorManager(MapActivity activity){
    if(_sm == null)_sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
    return _sm;
  }

	public void mapActivityResume(MapActivity activity){
    SensorManager sml = getSensorManager(activity);
		sml.registerListener(this, sml.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void mapActivityPause(MapActivity activity){
		getSensorManager(activity).unregisterListener(this);
	}

	public void mapActivityDestroy(MapActivity activity) { }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		 if (Sensor.TYPE_PRESSURE == event.sensor.getType()){
			 pressure = event.values[0];
			 updateText(formatAltitude(QNH.altitude(qnh,pressure)));
		 }
	}

	private static String formatQNH(double qnhValue){
			return String.format(Locale.ENGLISH, "%.2f", qnhValue );
	}
	private static String formatAltitude(double altValue){
			return String.format(Locale.ENGLISH, "%.1f", altValue );
	}

}
