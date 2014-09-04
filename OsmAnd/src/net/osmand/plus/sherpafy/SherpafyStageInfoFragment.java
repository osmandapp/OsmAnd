package net.osmand.plus.sherpafy;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class SherpafyStageInfoFragment extends SherlockFragment {
	public static final String STAGE_PARAM = "STAGE";
	public static final String TOUR_PARAM = "TOUR";
	OsmandApplication app;
	private SherpafyCustomization customization;
	protected StageInformation stage;
	protected TourInformation tour;
	private View view;
	OsmandMapTileView osmandMapTileView;

	public SherpafyStageInfoFragment() {
	}
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getSherlockActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();

		setHasOptionsMenu(true);
		extractArguments(getArguments());
	}


	protected void extractArguments(Bundle args) {
		String id = args.getString(TOUR_PARAM);
		for(TourInformation ti : customization.getTourInformations()) {
			if(ti.getId().equals(id)) {
				tour = ti;
				break;
			}
		}
		int k = args.getInt(STAGE_PARAM);
		if(tour != null && tour.getStageInformation().size() > k) {
			stage = tour.getStageInformation().get(k);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.sherpafy_stage_info, container, false);
		WebView description = (WebView) view.findViewById(R.id.Description);
		WebSettings settings = description.getSettings();
		settings.setDefaultTextEncodingName("utf-8");
		ImageView icon = (ImageView) view.findViewById(R.id.Icon);
		TextView additional = (TextView) view.findViewById(R.id.AdditionalText);
		TextView text = (TextView) view.findViewById(R.id.Text);
		TextView header = (TextView) view.findViewById(R.id.HeaderText);
		osmandMapTileView = (OsmandMapTileView) view.findViewById(R.id.MapView);
		MapTileLayer mapTileLayer = new MapTileLayer(true);
		MapVectorLayer mapVectorLayer = new MapVectorLayer(mapTileLayer);

		osmandMapTileView.addLayer(mapTileLayer,0.0f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		//osmandMapTileView.addLayer(new GPXLayer(), 0.9f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		osmandMapTileView.setLatLon(stage.getStartPoint().getLatitude(), stage.getStartPoint().getLongitude());
		osmandMapTileView.setIntZoom(14);

		updateView(description, icon, additional, text, header);
		return view;
	}


	protected void updateView(WebView description, ImageView icon, TextView additional, TextView text, TextView header) {
		if (stage.getImageBitmap() != null) {
			icon.setImageBitmap(stage.getImageBitmap());
		} else {
			icon.setVisibility(View.GONE);
		}
		if (stage.getDistance() > 0) {
			additional.setText(OsmAndFormatter.getFormattedDistance((float) stage.getDistance(), getMyApplication()));
		} else {
			additional.setText("");
		}
		header.setText(stage.getName());
		text.setText(stage.getShortDescription());
		description.loadData("<html><body>" + stage.getFullDescription() + "</body></html", "text/html; charset=utf-8",
				"utf-8");
	}

	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
}