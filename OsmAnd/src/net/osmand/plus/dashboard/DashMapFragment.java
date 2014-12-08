package net.osmand.plus.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import net.osmand.data.LatLon;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MainMenuActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashMapFragment extends DashBaseFragment  implements IMapDownloaderCallback {

	public static final String TAG = "DASH_MAP_FRAGMENT";
	private OsmandMapTileView osmandMapTileView;

	@Override
	public void onDestroy() {
		super.onDestroy();
		getMyApplication().getResourceManager().getMapTileDownloader().removeDownloaderCallback(this);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getMyApplication().getResourceManager().getMapTileDownloader().addDownloaderCallback(this);
	}

	protected void startMapActivity() {
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_map_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.map_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_map)).setTypeface(typeface);
		setupMapView(view);
		(view.findViewById(R.id.show_map)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startMapActivity();
			}

			
		});
		return view;
	}

	private void setupMapView(View view){
		OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) view.findViewById(R.id.MapView);
		surf.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startMapActivity();
			}
		});
		osmandMapTileView = surf.getMapView();
		osmandMapTileView.getView().setVisibility(View.VISIBLE);
		osmandMapTileView.removeAllLayers();
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null, true);
		MapTextLayer mapTextLayer = new MapTextLayer();
		mapTextLayer.setAlwaysVisible(true);
		// 5.95 all labels
		osmandMapTileView.addLayer(mapTextLayer, 5.95f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		osmandMapTileView.setShowMapPosition(false);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		LatLon lm = getMyApplication().getSettings().getLastKnownMapLocation();
		int zm = getMyApplication().getSettings().getLastKnownMapZoom();
		osmandMapTileView.setLatLon(lm.getLatitude(), lm.getLongitude());
		osmandMapTileView.setComplexZoom(zm, osmandMapTileView.getSettingsMapDensity());
		osmandMapTileView.refreshMap(true);
	}

	@Override
	public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.MapView).setVisibility(View.GONE);
		if(getMyApplication().isApplicationInitializing()) {
			getMyApplication().checkApplicationIsBeingInitialized(getActivity(), (TextView) view.findViewById(R.id.ProgressMessage),
					(ProgressBar) view.findViewById(R.id.ProgressBar), new Runnable() {
						@Override
						public void run() {
							applicationInitialized(view);
						}
					});
		} else {
			applicationInitialized(view);
		}
	}

	private void applicationInitialized(View view) {
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		MainMenuActivity dashboardActivity =((MainMenuActivity)getSherlockActivity());
		if (dashboardActivity != null){
			dashboardActivity.updateDownloads();
			view.findViewById(R.id.MapView).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		if(request != null && !request.error && request.fileToSave != null){
			ResourceManager mgr = getMyApplication().getResourceManager();
			mgr.tileDownloaded(request);
		}
		if(request == null || !request.error){
			if(osmandMapTileView != null) {
				osmandMapTileView.tileDownloaded(request);
			}
		}		
	}
}
