package net.osmand.plus.mapcontextmenu.other;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;

public class TrackDetailsMenu {

	private MapActivity mapActivity;
	private OsmandMapTileView mapView;
	private MapControlsLayer mapControlsLayer;

	private static boolean VISIBLE;
	private boolean nightMode;

	public TrackDetailsMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.mapControlsLayer = mapControlsLayer;
		mapView = mapActivity.getMapView();
	}

	public void show() {
		if (!VISIBLE) {
			VISIBLE = true;
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
			}

			mapActivity.refreshMap();

			TrackDetailsMenuFragment.showInstance(mapActivity);

			if (!AndroidUiHelper.isXLargeDevice(mapActivity)) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			}
			if (!portrait) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
			}
		}
	}

	public void hide() {
		WeakReference<TrackDetailsMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		} else {
			VISIBLE = false;
		}
	}

	public WeakReference<TrackDetailsMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(TrackDetailsMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((TrackDetailsMenuFragment) fragment);
		} else {
			return null;
		}
	}

	public void onDismiss() {
		VISIBLE = false;
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
	}

	public void updateInfo(final View main) {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		updateView(main);
	}

	private void updateView(final View parentView) {
		/*
		String via = generateViaDescription();
		View viaLayout = parentView.findViewById(R.id.ViaLayout);
		if (via.length() == 0) {
			viaLayout.setVisibility(View.GONE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.GONE);
		} else {
			viaLayout.setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.VISIBLE);
			((TextView) parentView.findViewById(R.id.ViaView)).setText(via);
		}

		viaLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargets().checkPointToNavigateShort()) {
					mapActivity.getMapActions().openIntermediatePointsDialog();
				}
			}
		});

		ImageView viaIcon = (ImageView) parentView.findViewById(R.id.viaIcon);
		viaIcon.setImageDrawable(getIconOrig(R.drawable.list_intermediate));
		*/
	}
}
