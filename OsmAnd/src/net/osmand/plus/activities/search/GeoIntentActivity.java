package net.osmand.plus.activities.search;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmandListActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.GeoPointParserUtil;

import org.apache.commons.logging.Log;

import java.util.List;

public class GeoIntentActivity extends OsmandListActivity {

	private static final Log LOG = PlatformUtil.getLog(GeoIntentActivity.class);

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_address_offline);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.search_osm_offline);
		}

		Intent intent = getIntent();
		if (intent != null) {
			ProgressDialog progress = ProgressDialog.show(this, getString(R.string.searching),
					getString(R.string.searching_address));
			GeoIntentTask task = new GeoIntentTask(intent, progress);

			progress.setOnCancelListener(dialog -> task.cancel(true));
			progress.setCancelable(true);

			OsmAndTaskManager.executeTask(task);
			setIntent(null);
		}
	}

	private class GeoIntentTask extends AsyncTask<Void, Void, List<GeoParsedPoint>> {

		private final Intent intent;
		private final ProgressDialog progress;

		private GeoIntentTask(@NonNull Intent intent, @Nullable ProgressDialog progress) {
			this.intent = intent;
			this.progress = progress;
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
		protected List<GeoParsedPoint> doInBackground(Void... nothing) {
			try {
				while (app.isApplicationInitializing()) {
					Thread.sleep(200);
				}
				Uri uri = intent.getData();
				if (uri != null) {
					return GeoPointParserUtil.parsePoints(uri.toString());
				}
			} catch (Exception e) {
				LOG.error(e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<GeoParsedPoint> points) {
			if (progress != null && progress.isShowing()) {
				try {
					progress.dismiss();
				} catch (Exception e) {
					LOG.error(e);
				}
			}
			if (Algorithms.isEmpty(points)) {
				return;
			}
			try {
				GeoParsedPoint point = points.get(0);
				if (point != null && point.isGeoPoint()) {
					PointDescription pd = new PointDescription(point.getLatitude(), point.getLongitude());
					if (!Algorithms.isEmpty(point.getLabel())) {
						pd.setName(point.getLabel());
					}
					settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							settings.getLastKnownMapZoom(), pd);
				} else {
					Uri uri = intent.getData();
					String searchString = point != null && point.isGeoAddress() ? point.getQuery() : uri.toString();
					settings.setSearchRequestToShow(searchString);
				}
				MapActivity.launchMapActivityMoveToTop(GeoIntentActivity.this);
				GeoIntentActivity.this.finish();
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}

	@Override
	protected void onStop() {
		dismiss();
		super.onStop();
	}

	private void dismiss() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}
}
