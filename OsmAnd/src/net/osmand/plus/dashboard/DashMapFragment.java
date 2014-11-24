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
import android.widget.TextView;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.R;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashMapFragment extends DashBaseFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_map_fragment, container, false);
		setupMapView(view);
		Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Medium.ttf");
		((TextView) view.findViewById(R.id.map_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_map)).setTypeface(typeface);

		(view.findViewById(R.id.show_map)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent mapIndent = new Intent(activity, appCustomization.getMapActivity());
				activity.startActivityForResult(mapIndent, 0);
			}
		});

		return view;
	}

	private void setupMapView(View view){
		OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) view.findViewById(R.id.MapView);
		OsmandMapTileView osmandMapTileView = surf.getMapView();
		osmandMapTileView.getView().setVisibility(View.VISIBLE);
		osmandMapTileView.removeAllLayers();
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null);
		MapTextLayer mapTextLayer = new MapTextLayer();
		mapTextLayer.setAlwaysVisible(true);
		// 5.95 all labels
		osmandMapTileView.addLayer(mapTextLayer, 5.95f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		net.osmand.Location location = getMyApplication().getLocationProvider().getFirstTimeRunDefaultLocation();
		if(location != null){
			osmandMapTileView.setLatLon(location.getLatitude(), location.getLongitude());
			osmandMapTileView.setIntZoom(14);
		}
		osmandMapTileView.refreshMap(true);
	}
}
