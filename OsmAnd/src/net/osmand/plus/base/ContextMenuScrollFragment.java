package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.LockableScrollView;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
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

	@Nullable
	private View mapBottomHudButtons;

	@Nullable
	private RulerWidget rulerWidget;

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	public boolean isShowMapBottomHudButtons() {
		return true;
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

			View bottomScrollView = getBottomScrollView();
			if (bottomScrollView instanceof LockableScrollView) {
				((LockableScrollView) bottomScrollView).setScrollingEnabled(true);
			}

			mapBottomHudButtons = view.findViewById(R.id.map_controls_container);
			if (mapBottomHudButtons != null) {
				if (isShowMapBottomHudButtons()) {
					setupControlButtons(mapBottomHudButtons);
				} else {
					AndroidUiHelper.updateVisibility(mapBottomHudButtons, false);
				}
			}
		}
		return view;
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated) {
		updateMapControlsPos(fragment, y, animated);
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState, int previousMenuState) {
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
		View zoomInButtonView = view.findViewById(R.id.map_zoom_in_button);
		View zoomOutButtonView = view.findViewById(R.id.map_zoom_out_button);
		View myLocButtonView = view.findViewById(R.id.map_my_location_button);
		View mapRulerView = view.findViewById(R.id.map_ruler_layout);

		MapActivityLayers mapLayers = mapActivity.getMapLayers();

		OsmandMapTileView mapTileView = mapActivity.getMapView();
		View.OnLongClickListener longClickListener = MapControlsLayer.getOnClickMagnifierListener(mapTileView);

		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		mapControlsLayer.setupZoomInButton(zoomInButtonView, longClickListener, ZOOM_IN_BUTTON_ID);
		mapControlsLayer.setupZoomOutButton(zoomOutButtonView, longClickListener, ZOOM_OUT_BUTTON_ID);
		mapControlsLayer.setupBackToLocationButton(myLocButtonView, false, BACK_TO_LOC_BUTTON_ID);

		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		rulerWidget = mapInfoLayer.setupRulerWidget(mapRulerView);
	}

	public void updateMapControlsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		View mapControlsView = this.mapBottomHudButtons;
		if (mapControlsView != null) {
			int zoomY = y - getMapControlsHeight();
			if (animated) {
				fragment.animateView(mapControlsView, zoomY, null);
			} else {
				mapControlsView.setY(zoomY);
			}
		}
	}

	private int getMapControlsHeight() {
		View mapControlsContainer = this.mapBottomHudButtons;
		return mapControlsContainer != null ? mapControlsContainer.getHeight() : 0;
	}

	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY;
	}

	private void updateMapControlsVisibility(int menuState) {
		if (mapBottomHudButtons != null) {
			if (shouldShowMapControls(menuState)) {
				if (mapBottomHudButtons.getVisibility() != View.VISIBLE) {
					mapBottomHudButtons.setVisibility(View.VISIBLE);
				}
			} else {
				if (mapBottomHudButtons.getVisibility() == View.VISIBLE) {
					mapBottomHudButtons.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}