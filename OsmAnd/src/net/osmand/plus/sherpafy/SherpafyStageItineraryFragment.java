package net.osmand.plus.sherpafy;

import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.GPXLayer;
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
		osmandMapTileView.setVisibility(View.VISIBLE);
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		osmandMapTileView.addLayer(new GPXLayer(), 0.9f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		osmandMapTileView.setLatLon(stage.getStartPoint().getLatitude(), stage.getStartPoint().getLongitude());
		osmandMapTileView.setIntZoom(14);
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