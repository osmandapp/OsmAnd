package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;

import java.util.Arrays;
import java.util.Collections;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;

public abstract class ContextMenuScrollFragment extends ContextMenuFragment implements ContextMenuFragmentListener {

	public static final String TAG = ContextMenuScrollFragment.class.getSimpleName();

	private static final String ZOOM_IN_BUTTON_ID = ZOOM_IN_HUD_ID + TAG;
	private static final String ZOOM_OUT_BUTTON_ID = ZOOM_OUT_HUD_ID + TAG;
	private static final String BACK_TO_LOC_BUTTON_ID = BACK_TO_LOC_HUD_ID + TAG;

	private View mapControlsView;

	private RulerWidget rulerWidget;

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setListener(this);
			getBottomScrollView().setScrollingEnabled(true);
			setupControlButtons(view);
		}
		return view;
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated) {
		updateMapControlsPos(fragment, y, animated);
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState) {
		updateMapControlsVisibility(menuState);
	}

	@Override
	public void onContextMenuDismiss(@NonNull ContextMenuFragment fragment) {

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapActivityLayers mapLayers = mapActivity.getMapLayers();

			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.removeHudButtons(Arrays.asList(ZOOM_IN_BUTTON_ID, ZOOM_OUT_BUTTON_ID, BACK_TO_LOC_BUTTON_ID));

			MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
			mapInfoLayer.removeRulerWidgets(Collections.singletonList(rulerWidget));
		}
	}

	private void setupControlButtons(@NonNull View view) {
		MapActivity mapActivity = requireMapActivity();
		mapControlsView = view.findViewById(R.id.map_controls_container);

		View zoomInButtonView = mapControlsView.findViewById(R.id.map_zoom_in_button);
		View zoomOutButtonView = mapControlsView.findViewById(R.id.map_zoom_out_button);
		View myLocButtonView = mapControlsView.findViewById(R.id.map_my_location_button);
		View mapRulerView = mapControlsView.findViewById(R.id.map_ruler_layout);

		MapActivityLayers mapLayers = mapActivity.getMapLayers();

		OsmandMapTileView mapTileView = mapActivity.getMapView();
		View.OnLongClickListener longClickListener = MapControlsLayer.getOnClickMagnifierListener(mapTileView);

		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		mapControlsLayer.setupZoomInButton(zoomInButtonView, longClickListener, ZOOM_IN_BUTTON_ID);
		mapControlsLayer.setupZoomOutButton(zoomOutButtonView, longClickListener, ZOOM_OUT_BUTTON_ID);
		mapControlsLayer.setupBackToLocationButton(myLocButtonView, BACK_TO_LOC_BUTTON_ID);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(mapRulerView);
	}

	public void updateMapControlsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		View mapControlsView = this.mapControlsView;
		if (mapControlsView != null) {
			int zoomY = y - getMapControlsHeight();
			if (animated) {
				fragment.animateView(mapControlsView, zoomY);
			} else {
				mapControlsView.setY(zoomY);
			}
		}
	}

	private int getMapControlsHeight() {
		View mapControlsContainer = this.mapControlsView;
		return mapControlsContainer != null ? mapControlsContainer.getHeight() : 0;
	}

	private void updateMapControlsVisibility(int menuState) {
		if (mapControlsView != null) {
			if (menuState == MenuState.HEADER_ONLY) {
				if (mapControlsView.getVisibility() != View.VISIBLE) {
					mapControlsView.setVisibility(View.VISIBLE);
				}
			} else {
				if (mapControlsView.getVisibility() == View.VISIBLE) {
					mapControlsView.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}