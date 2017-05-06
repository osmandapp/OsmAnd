package net.osmand.plus.mapillary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarController;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarControllerType;

import org.json.JSONObject;

public class MapillaryImageCard extends ImageCard {

	private int prevMapPosition = OsmandSettings.CENTER_CONSTANT;

	public MapillaryImageCard(final MapActivity mapActivity, final JSONObject imageObject) {
		super(mapActivity, imageObject);
		this.icon = getMyApplication().getIconsCache().getIcon(R.drawable.ic_logo_mapillary);
		this.onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final MapillaryImageViewerController toolbarController = new MapillaryImageViewerController();
				toolbarController.setTitle(getMyApplication().getString(R.string.mapillary));
				toolbarController.setCloseBtnVisible(false);
				toolbarController.setBottomView(getWebView(getUrl()));
				toolbarController.setOnBackButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						getMapActivity().hideTopToolbar(toolbarController);
					}
				});
				toolbarController.setOnCloseToolbarListener(new Runnable() {
					@Override
					public void run() {
						restoreMapPosition();
						setSelectedImageLocation(null, Double.NaN);
					}
				});
				getMapActivity().getContextMenu().hideMenues();
				getMapActivity().showTopToolbar(toolbarController);
				setSelectedImageLocation(getLocation(), getCa());

			}
		};
	}

	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	private WebView getWebView(String url) {
		final WebView webView = new WebView(view.getContext());
		webView.setBackgroundColor(Color.BLACK);
		//webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		webView.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new WebAppInterface(view.getContext()), "Android");
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getMapActivity());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				portrait ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMyApplication(), 360f),
				AndroidUtils.dpToPx(getMyApplication(), 200f));
		webView.setLayoutParams(lp);
		webView.loadUrl(url);
		return webView;
	}

	private void shiftMapPosition() {
		OsmandMapTileView mapView = getMapActivity().getMapView();
		if (AndroidUiHelper.isOrientationPortrait(getMapActivity())) {
			if (mapView.getMapPosition() != OsmandSettings.MIDDLE_CONSTANT) {
				prevMapPosition = mapView.getMapPosition();
				mapView.setMapPosition(OsmandSettings.MIDDLE_CONSTANT);
			}
		} else {
			mapView.setMapPositionX(1);
		}
	}

	private void restoreMapPosition() {
		if (AndroidUiHelper.isOrientationPortrait(getMapActivity())) {
			getMapActivity().getMapView().setMapPosition(prevMapPosition);
		} else {
			getMapActivity().getMapView().setMapPositionX(0);
		}
	}

	private void setSelectedImageLocation(LatLon latLon, double ca) {
		MapillaryLayer layer = getMapActivity().getMapView().getLayerByClass(MapillaryLayer.class);
		if (layer != null) {
			layer.setSelectedImageLocation(latLon);
			if (!Double.isNaN(ca)) {
				layer.setSelectedImageCameraAngle((float) ca);
			} else {
				layer.setSelectedImageCameraAngle(null);
			}
			if (latLon != null) {
				shiftMapPosition();
				getMapActivity().setMapLocation(latLon.getLatitude(), latLon.getLongitude());
			} else {
				getMapActivity().refreshMap();
			}
		}
	}

	private class WebAppInterface {
		Context mContext;

		WebAppInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void onNodeChanged(double latitude, double longitude, double ca) {
			LatLon latLon = null;
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
				latLon = new LatLon(latitude, longitude);
			}
			setSelectedImageLocation(latLon, ca);
		}
	}

	private static class MapillaryImageViewerController extends TopToolbarController {

		MapillaryImageViewerController() {
			super(TopToolbarControllerType.ONLINE_IMAGE);
			setBackBtnIconIds(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_dark);
			setBackBtnIconClrIds(0, 0);
			setTitleTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setDescrTextClrIds(R.color.primary_text_dark, R.color.primary_text_dark);
			setBgIds(R.color.osmand_orange, R.color.osmand_orange,
					R.color.osmand_orange, R.color.osmand_orange);
		}
	}
}
