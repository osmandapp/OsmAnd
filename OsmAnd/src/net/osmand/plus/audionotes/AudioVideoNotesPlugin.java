package net.osmand.plus.audionotes;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import net.osmand.LogUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.parkingpoint.ParkingPositionLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;

import org.apache.commons.logging.Log;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.Toast;

public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final String ID = "osmand.audionotes";
	private static final Log log = LogUtil.getLog(AudioVideoNotesPlugin.class);
	private OsmandApplication app;
	private TextInfoControl recordControl;
	private MapActivity mapActivity;
	
	private DataTileManager<Recording> indexedFiles = new DataTileManager<AudioVideoNotesPlugin.Recording>(14);
	private AudioNotesLayer audioNotesLayer;
	
	public static class Recording {
		public Recording(File f) {
			this.file = f;
		}
		public File file;
		public int x31;
		public int y31;
	}
	
	@Override
	public String getId() {
		return ID;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		this.app = app;

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.audionotes_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.audionotes_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		if(audioNotesLayer != null) {
			activity.getMapView().removeLayer(audioNotesLayer);
		}
		audioNotesLayer = new AudioNotesLayer(activity);
		registerWidget(activity);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
	}
	
	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		mapActivity = activity;
		if (mapInfoLayer != null ) {
			recordControl = createRecordControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(recordControl,
					-1/*R.drawable.widget_parking*/, R.string.map_widget_appearance, "audionotes", false,
					EnumSet.allOf(ApplicationMode.class),
					EnumSet.noneOf(ApplicationMode.class), 22);
			mapInfoLayer.recreateControls();
		}
	}
	private TextInfoControl createRecordControl(MapActivity activity, Paint paintText, Paint paintSubText) {
		TextInfoControl recordPlaceControl = new TextInfoControl(activity, 0, paintText, paintSubText);
		setRecordListener(recordPlaceControl);
		return recordPlaceControl;
	}

	private void setRecordListener(final TextInfoControl recordPlaceControl) {
		recordPlaceControl.setText("", app.getString(R.string.monitoring_control_start));
		recordPlaceControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				recordAudioVideo(recordPlaceControl);
			}
		});
	}
	
	private void recordAudioVideo(final TextInfoControl recordPlaceControl) {
		Location loc = mapActivity.getLastKnownLocation();
		if(loc == null) {
			AccessibleToast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
		}
		String basename = MapUtils.createShortLocString(loc.getLatitude(), loc.getLongitude(), 15);
		File f = app.getSettings().extendOsmandPath(ResourceManager.AV_PATH);
		f.mkdirs();
		final MediaRecorder mr = new MediaRecorder();
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		int k = 1;
		File fl;
		do {
			fl = new File(f, basename + "_" + (k++) + ".3gpp");
		} while(fl.exists());
		mr.setOutputFile(fl.getAbsolutePath());
		mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		recordPlaceControl.setText("", app.getString(R.string.monitoring_control_stop));
		try {
			mr.prepare();
			mr.start();
			recordPlaceControl.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					mr.stop();
					mr.release();
					setRecordListener(recordPlaceControl);
				}
			});
			
		} catch (Exception e) {
			AccessibleToast.makeText(app, app.getString(R.string.error_io_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void indexFile(File f){
		Recording r = new Recording(f);
		indexedFiles.registerObject(r.x31, r.y31, r);
	}

	@Override
	public void disable(OsmandApplication app) {
	}
	
	
	public class AudioNotesLayer extends OsmandMapLayer {

		private MapActivity activity;

		public AudioNotesLayer(MapActivity activity) {
			this.activity = activity;
		}

		@Override
		public void initLayer(OsmandMapTileView view) {
		}

		@Override
		public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings) {
			
		}

		@Override
		public void destroyLayer() {
		}

		@Override
		public boolean drawInScreenPixels() {
			return false;
		}
		
	}

}
