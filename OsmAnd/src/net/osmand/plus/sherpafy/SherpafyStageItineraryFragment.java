package net.osmand.plus.sherpafy;

import java.util.List;

import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapTextLayer;
import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class SherpafyStageItineraryFragment extends SherpafyStageInfoFragment implements IMapDownloaderCallback {
	
	private static final boolean HIDE_ITINERARY_IMG = true;

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.getResourceManager().getMapTileDownloader().removeDownloaderCallback(this);
	}
	
	protected void updateView(WebView description, ImageView icon, TextView additional, TextView text, TextView header) {
		app.getResourceManager().getMapTileDownloader().addDownloaderCallback(this);
		osmandMapTileView.removeAllLayers();
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null, true);
		MapTextLayer mapTextLayer = new MapTextLayer();
		mapTextLayer.setAlwaysVisible(true);
		// 5.95 all labels
		osmandMapTileView.addLayer(mapTextLayer, 5.95f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		final GPXLayer gpxLayer = new GPXLayer();
		gpxLayer.setGivenGpx(stage.getGpx());
		osmandMapTileView.addLayer(gpxLayer, 0.9f);
		osmandMapTileView.addLayer(new StageFavoritesLayer(app, stage), 4.1f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		calculateLatLon(stage.getGpx());
		if (stage.getItineraryBitmap() != null && !HIDE_ITINERARY_IMG) {
			icon.setImageBitmap(stage.getItineraryBitmap());
		} else {
			icon.setVisibility(View.GONE);
		}
		additional.setVisibility(View.GONE);
		header.setVisibility(View.GONE);
		String ins = "";
		if(stage.distance > 0) {
			ins += "<h4>" + app.getString(R.string.distance) + ": "+ OsmAndFormatter.getFormattedDistance((float) stage.distance, app) + "<h4/>";
		}
		if(stage.duration > 0) {
			int min = stage.duration % 60;
			int h = stage.duration / 60;
			ins += "<h4>" + app.getString(R.string.duration) + ": "+ 
					( h == 0 ? "" : h + " " + app.getString(R.string.int_hour) + " ") + 
					( min == 0 ? "" : min + " " + app.getString(R.string.int_min))+ "<h4/>";
		}
		String content = HIDE_ITINERARY_IMG ? "" : stage.getItinerary();
		description.loadData("<html><body>" + ins + content + "</body></html", "text/html; charset=utf-8",
				"utf-8");
		
		new AsyncTask<Void, Void, Void>() {

			private GPXFile gpx;
			@Override
			protected Void doInBackground(Void... params) {
				if (stage.gpxFile != null){
					gpx = GPXUtilities.loadGPXFile(app, stage.gpxFile);
				}
				return null;
			}
			protected void onPostExecute(Void result) {
				gpxLayer.setGivenGpx(gpx);
				calculateLatLon(gpx);
				osmandMapTileView.refreshMap();
			};
		}.execute((Void)null);
	}

	protected void calculateLatLon(GPXFile gpx) {

		WptPt st = gpx == null ? null : gpx.findPointToShow();
		double llat = st == null ? stage.getStartPoint().getLatitude() : st.lat;
		double llon = st == null ? stage.getStartPoint().getLongitude() : st.lon;
		double left = llon, right = llon;
		double top = llat, bottom = llat;
		if (gpx != null) {
			for (List<WptPt> list : gpx.proccessPoints()) {
				for (WptPt l : list) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
			}
		}
		osmandMapTileView.setIntZoom(15);
		RotatedTileBox tb = new RotatedTileBox(osmandMapTileView.getCurrentRotatedTileBox());
		tb.setPixelDimensions(3 * tb.getPixWidth() / 4, 3 * tb.getPixHeight() / 4);
		double clat = bottom / 2 + top / 2;
		double clon = left / 2 + right / 2;
		tb.setLatLonCenter(clat, clon);
		while (tb.getZoom() >= 7 && (!tb.containsLatLon(top, left) || !tb.containsLatLon(bottom, right))) {
			tb.setZoom(tb.getZoom() - 1);
		}
		osmandMapTileView.setLatLon(tb.getCenterLatLon().getLatitude(), tb.getCenterLatLon().getLongitude());
		osmandMapTileView.setComplexZoom(tb.getZoom(), osmandMapTileView.getSettingsMapDensity());

	}
	
	@Override
	public void onResume() {
		super.onResume();
		osmandMapTileView.refreshMap(true);
	}
	
	@Override
	public void tileDownloaded(DownloadRequest request) {
		if(request != null && !request.error && request.fileToSave != null){
			ResourceManager mgr = app.getResourceManager();
			mgr.tileDownloaded(request);
		}
		if(request == null || !request.error){
			osmandMapTileView.tileDownloaded(request);
		}		
	}
}