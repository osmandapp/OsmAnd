package net.osmand.plus.base;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.LockableScrollView;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ContextMenuScrollFragment extends ContextMenuFragment implements ContextMenuFragmentListener {

	public static final String TAG = ContextMenuScrollFragment.class.getSimpleName();

	protected static final String ZOOM_IN_BUTTON_ID = ZOOM_IN_HUD_ID + TAG;
	protected static final String ZOOM_OUT_BUTTON_ID = ZOOM_OUT_HUD_ID + TAG;
	protected static final String BACK_TO_LOC_BUTTON_ID = BACK_TO_LOC_HUD_ID + TAG;

	@Nullable
	private View mapBottomHudButtons;

	@Nullable
	private RulerWidget rulerWidget;
	private List<MapButton> mapButtons = new ArrayList<>();

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
			MapLayers mapLayers = mapActivity.getMapLayers();

			MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
			mapControlsLayer.removeCustomMapButtons(mapButtons);

			if (rulerWidget != null) {
				MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
				mapInfoLayer.removeRulerWidgets(Collections.singletonList(rulerWidget));
			}
		}
	}

	@Nullable
	protected View getMapBottomHudButtons() {
		return mapBottomHudButtons;
	}

	protected void setupControlButtons(@NonNull View view) {
		MapActivity activity = requireMapActivity();
		MapLayers mapLayers = activity.getMapLayers();
		MapControlsLayer layer = mapLayers.getMapControlsLayer();

		mapButtons = new ArrayList<>();
		mapButtons.add(view.findViewById(R.id.map_zoom_in_button));
		mapButtons.add(view.findViewById(R.id.map_zoom_out_button));
		mapButtons.add(view.findViewById(R.id.map_my_location_button));
		layer.addCustomizedDefaultMapButtons(mapButtons);

		setupMapRulerWidget(view, mapLayers);
	}

	protected boolean alwaysShowButtons() {
		return true;
	}

	public String getButtonId(String defaultId) {
		return defaultId + this.getClass().getName();
	}

	protected void setupMapRulerWidget(@NonNull View view, @NonNull MapLayers mapLayers) {
		MapInfoLayer layer = mapLayers.getMapInfoLayer();
		rulerWidget = layer.setupRulerWidget(view.findViewById(R.id.map_ruler_layout));
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