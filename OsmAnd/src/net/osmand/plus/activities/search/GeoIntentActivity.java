package net.osmand.plus.activities.search;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmandListActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPointParserUtil;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;
import net.osmand.util.GeoPointParserUtil.GeoParsedDirection;

public class GeoIntentActivity extends OsmandListActivity {

	private ProgressDialog progressDlg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_offline);
		getSupportActionBar().setTitle(R.string.search_osm_offline);

		Intent intent = getIntent();
		if (intent != null) {
			ProgressDialog progress = ProgressDialog.show(this, getString(R.string.searching),
					getString(R.string.searching_address));
			GeoIntentTask task = new GeoIntentTask(progress, intent);

			progress.setOnCancelListener(dialog -> task.cancel(true));
			progress.setCancelable(true);

			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			setIntent(null);
		}
	}

	private class GeoIntentTask extends AsyncTask<Void, Void, GeoParsedPoint> {

		private final ProgressDialog progress;
		private final Intent intent;

		private GeoIntentTask(ProgressDialog progress, Intent intent) {
			this.progress = progress;
			this.intent = intent;
		}

		@Override
		protected void onPreExecute() {
		}

		/**
		 * Extracts information from geo and map intents:
		 * <p>
		 * geo:47.6,-122.3<br/>
		 * geo:47.6,-122.3?z=11<br/>
		 * geo:0,0?q=34.99,-106.61(Treasure)<br/>
		 * geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA<br/>
		 *
		 * @param uri The intent uri
		 * @return
		 */
		@Override
		protected GeoParsedPoint doInBackground(Void... nothing) {
			try {
				while (getMyApplication().isApplicationInitializing()) {
					Thread.sleep(200);
				}
				Uri uri = intent.getData();
				return GeoPointParserUtil.parse(uri.toString());
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(GeoParsedPoint p) {
			if (progress != null && progress.isShowing()) {
				try {
					progress.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				OsmandSettings settings = getMyApplication().getSettings();
				if (p instanceof GeoParsedDirection) {
					GeoParsedDirection direction = (GeoParsedDirection) p;
					settings.setPointToStart(direction.getFromLat(), direction.getFromLon(), new PointDescription(PointDescription.POINT_TYPE_LOCATION, ""));
					settings.setPointToNavigate(direction.getToLat(), direction.getToLon(), new PointDescription(PointDescription.POINT_TYPE_LOCATION, ""));
					settings.setShowRoutePreparationMenu(true);
				} else if (p != null && p.isGeoPoint()) {
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
