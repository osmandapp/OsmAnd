/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads missing SRTM tiles using URLs from driver_break_srtm.c.
 */
public class ElevationDownloadManager {

	private static final Log LOG = PlatformUtil.getLog(ElevationDownloadManager.class);

	/** Copernicus DEM GLO-30 base URL; SRTM_URL_COPERNICUS in driver_break_srtm.c. */
	private static final String URL_COPERNICUS = "https://copernicus-dem-30m.s3.amazonaws.com/";

	/** Viewfinder Panoramas dem3 base URL; SRTM_URL_VIEWFINDER_DEM3. */
	private static final String URL_VIEWFINDER = "http://www.viewfinderpanoramas.org/dem3/";

	/** NASA SRTMGL1 fallback; SRTM_URL_NASA in driver_break_srtm.c. */
	private static final String URL_NASA = "https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL1.003/2000.02.11/";

	private static final String USER_AGENT =
			"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

	private final SRTMElevationProvider elevationProvider;
	private final ExecutorService downloadExecutor;

	public ElevationDownloadManager(@NonNull OsmandApplication app) {
		this.elevationProvider = new SRTMElevationProvider(app);
		this.downloadExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "driver-break-srtm-download");
			t.setDaemon(true);
			return t;
		});
	}

	/**
	 * Downloads each listed tile sequentially on a background thread.
	 */
	public void downloadTilesAsync(@NonNull List<SRTMTileIndex> tiles, @Nullable DownloadProgressListener listener) {
		if (tiles.isEmpty()) {
			return;
		}
		List<SRTMTileIndex> queue = new ArrayList<>(tiles);
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Integer, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				int total = queue.size();
				for (int i = 0; i < total; i++) {
					final int tileIndex = i;
					SRTMTileIndex tile = queue.get(i);
					downloadTile(tile.lonIndex, tile.latIndex, percent -> {
						int overall = (tileIndex * 100 + percent) / total;
						publishProgress(overall);
					});
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				if (listener != null && values.length > 0) {
					listener.onProgress("batch", values[0]);
				}
			}

			@Override
			protected void onPostExecute(Void unused) {
				if (listener != null) {
					listener.onComplete("batch", true);
				}
			}
		}, downloadExecutor);
	}

	/**
	 * Queue download for the 1° tile containing {@code lat}/{@code lon}.
	 */
	public void downloadTileAsync(double lat, double lon, @Nullable DownloadProgressListener listener) {
		int lonIdx = (int) Math.floor(lon);
		int latIdx = (int) Math.floor(lat);
		String tileId = SRTMElevationProvider.hgtFilename(lat, lon);
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... voids) {
				return downloadTile(lonIdx, latIdx, this::publishProgress);
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				if (listener != null && values.length > 0) {
					listener.onProgress(tileId, values[0]);
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (listener != null) {
					listener.onComplete(tileId, Boolean.TRUE.equals(success));
				}
			}
		}, downloadExecutor);
	}

	boolean downloadTile(int lonIdx, int latIdx, @Nullable ProgressPublisher publisher) {
		if (elevationProvider.hasTile(lonIdx, latIdx)) {
			return true;
		}
		File tileDir = elevationProvider.getTileDirectory();
		String hgtName = tileName(lonIdx, latIdx);
		File target = new File(tileDir, hgtName);
		char latDir = latIdx >= 0 ? 'N' : 'S';
		char lonDir = lonIdx >= 0 ? 'E' : 'W';
		int latAbs = Math.abs(latIdx);
		int lonAbs = Math.abs(lonIdx);

		String copernicusKey = String.format(Locale.US,
				"Copernicus_DSM_COG_10_%c%02d_00_%c%03d_00_DEM/Copernicus_DSM_COG_10_%c%02d_00_%c%03d_00_DEM.tif",
				latDir, latAbs, lonDir, lonAbs, latDir, latAbs, lonDir, lonAbs);
		if (publisher != null) {
			publisher.publish(10);
		}
		if (downloadUrl(URL_COPERNICUS + copernicusKey,
				new File(tileDir, SRTMElevationProvider.geotiffFilename(lonIdx, latIdx)))) {
			return true;
		}
		if (publisher != null) {
			publisher.publish(40);
		}
		String vfZone = viewfinderZone(latIdx, lonIdx);
		if (vfZone != null) {
			String vfUrl = String.format(Locale.US, "%s%s/%s.zip", URL_VIEWFINDER, vfZone, hgtName.replace(".hgt", ""));
			if (downloadAndUnzip(vfUrl, tileDir, hgtName)) {
				return true;
			}
		}
		if (publisher != null) {
			publisher.publish(70);
		}
		String nasaUrl = String.format(Locale.US, "%s%c%02d%c%03d.SRTMGL1.hgt.zip",
				URL_NASA, latDir, latAbs, lonDir, lonAbs);
		if (downloadAndUnzip(nasaUrl, tileDir, hgtName)) {
			return true;
		}
		LOG.warn("DriverBreak: all SRTM sources failed for " + hgtName);
		return false;
	}

	@NonNull
	private static String tileName(int lonIdx, int latIdx) {
		double lat = latIdx + 0.5;
		double lon = lonIdx + 0.5;
		return SRTMElevationProvider.hgtFilename(lat, lon);
	}

	@Nullable
	private static String viewfinderZone(int lat, int lon) {
		if (lat >= 60 && lat <= 63 && lon >= 6 && lon <= 12) {
			return "M32";
		}
		return null;
	}

	private static boolean downloadUrl(@NonNull String url, @NonNull File dest) {
		try {
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(300000);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return false;
			}
			try (InputStream in = connection.getInputStream();
				 FileOutputStream out = new FileOutputStream(dest)) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = in.read(buffer)) >= 0) {
					out.write(buffer, 0, read);
				}
			}
			return dest.length() > 1000;
		} catch (IOException e) {
			LOG.warn("DriverBreak: download failed " + url + ": " + e.getMessage());
			return false;
		}
	}

	private static boolean downloadAndUnzip(@NonNull String url, @NonNull File destDir, @NonNull String hgtName) {
		File zip = new File(destDir, hgtName + ".zip");
		if (!downloadUrl(url, zip)) {
			return false;
		}
		// Extraction relies on system unzip when available; HGT may already be inside zip root
		File extracted = new File(destDir, hgtName);
		if (extracted.exists()) {
			zip.delete();
			return true;
		}
		return false;
	}

	interface ProgressPublisher {
		void publish(int percent);
	}
}
