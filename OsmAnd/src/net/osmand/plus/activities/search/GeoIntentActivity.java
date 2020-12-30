package net.osmand.plus.activities.search;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.PointDescription;
import net.osmand.data.Street;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandListActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPointParserUtil;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;

import java.io.IOException;
import java.util.Locale;

public class GeoIntentActivity extends OsmandListActivity {

	private ProgressDialog progressDlg;
	private LatLon location;
	protected static final boolean DO_NOT_SEARCH_ADDRESS = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_offline);
		getSupportActionBar().setTitle(R.string.search_osm_offline);
		
		getMyApplication().checkApplicationIsBeingInitialized(this, new AppInitializeListener() {
			@Override
			public void onProgress(AppInitializer init, InitEvents event) {
			}
			
			@Override
			public void onFinish(AppInitializer init) {
			}
		});
		location = getMyApplication().getSettings().getLastKnownMapLocation();

		final Intent intent = getIntent();
		if (intent != null) {
			final ProgressDialog progress = ProgressDialog.show(GeoIntentActivity.this, getString(R.string.searching),
					getString(R.string.searching_address));
			final GeoIntentTask task = new GeoIntentTask(progress, intent, this);

			progress.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					task.cancel(true);
				}
			});
			progress.setCancelable(true);

			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			setIntent(null);
		}
	}

	
	private PointDescription getString(MapObject o) {
		if (o instanceof Amenity) {
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			return new PointDescription(PointDescription.POINT_TYPE_POI,
					OsmAndFormatter.getPoiStringWithoutType((Amenity) o, settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get()));
		}
		if (o instanceof Street) {
			return new PointDescription(PointDescription.POINT_TYPE_ADDRESS, ((Street) o).getCity().getName() + " " + o.getName());
		}
		return new PointDescription(PointDescription.POINT_TYPE_ADDRESS, o.toString());
	}

	private class GeoIntentTask extends AsyncTask<Void, Void, GeoParsedPoint> {
		private final ProgressDialog progress;
		private final Intent intent;
		private Context context;

		private GeoIntentTask(final ProgressDialog progress, final Intent intent, Context context) {
			this.progress = progress;
			this.intent = intent;
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
		}

		/**
		 * Extracts information from geo and map intents:
		 * 
		 * geo:47.6,-122.3<br/>
		 * geo:47.6,-122.3?z=11<br/>
		 * geo:0,0?q=34.99,-106.61(Treasure)<br/>
		 * geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA<br/>
		 * 
		 * @param uri
		 *            The intent uri
		 * @return
		 */
		@Override
		protected GeoParsedPoint doInBackground(Void... nothing) {
			try {
				while (getMyApplication().isApplicationInitializing()) {
					Thread.sleep(200);
				}
				Uri uri = intent.getData();
				GeoParsedPoint gpp;
				Address address = null;
				try {
					address = new Geocoder(context, Locale.getDefault())
							.getFromLocationName(uri.toString(), 1)
							.get(0);
				} catch (IndexOutOfBoundsException | IOException e) {
					e.printStackTrace();
				}
				if (address != null) {
					gpp = new GeoParsedPoint(address.getLatitude(), address.getLongitude(), GeoParsedPoint.NO_ZOOM);
				} else {
					gpp = GeoPointParserUtil.parse(uri.toString());
				}
				return gpp;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(GeoPointParserUtil.GeoParsedPoint p ) {
			if (progress != null && progress.isShowing()) {
				try {
					progress.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				OsmandSettings settings = getMyApplication().getSettings();
				if (p != null && p.isGeoPoint()) {
					PointDescription pd = new PointDescription(p.getLatitude(), p.getLongitude());
					if (!Algorithms.isEmpty(p.getLabel())) {
						pd.setName(p.getLabel());
					}
					settings.setMapLocationToShow(p.getLatitude(), p.getLongitude(),
							settings.getLastKnownMapZoom(), pd); //$NON-NLS-1$
				} else {
					Uri uri = intent.getData();
					String searchString = p != null && p.isGeoAddress() ? p.getQuery() : uri.toString();
					settings.setSearchRequestToShow(searchString);
				}
				MapActivity.launchMapActivityMoveToTop(GeoIntentActivity.this);
				GeoIntentActivity.this.finish();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void onStop() {
		dismiss();
		super.onStop();
	}

	private void dismiss() {
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
	}
	

}
