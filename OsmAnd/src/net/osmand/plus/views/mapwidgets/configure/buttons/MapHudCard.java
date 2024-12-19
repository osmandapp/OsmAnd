package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.content.Context;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapHudCard extends MapBaseCard {

	private final List<MapButton> mapButtons = new ArrayList<>();
	private final ButtonAppearanceParams appearanceParams;

	private MapHudLayout mapHudLayout;
	private RulerWidget rulerWidget;

	public MapHudCard(@NonNull MapActivity mapActivity,
			@Nullable ButtonAppearanceParams appearanceParams) {
		super(mapActivity, false);
		this.appearanceParams = appearanceParams;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.map_hud_card;
	}

	@NonNull
	@Override
	public View inflate(@NonNull Context ctx) {
		View view = super.inflate(ctx);
		initMapButtons(view);
		return view;
	}

	public void initMapButtons(@NonNull View view) {
		mapHudLayout = view.findViewById(R.id.map_hud_layout);

		addMapButton(createMapButton(R.layout.configure_map_button));
		addMapButton(createMapButton(R.layout.map_search_button));
		addMapButton(createMapButton(R.layout.map_compass_button));
		addMapButton(createMapButton(R.layout.map_zoom_out_button));
		addMapButton(createMapButton(R.layout.map_zoom_in_button));
		addMapButton(createMapButton(R.layout.my_location_button));
		addMapButton(createMapButton(R.layout.drawer_menu_button));
		addMapButton(createMapButton(R.layout.navigation_menu_button));
		addMapButton(createMapButton(R.layout.map_3d_button));

		setupRulerWidget();
	}

	@NonNull
	private MapButton createMapButton(@LayoutRes int layoutId) {
		MapButton button = (MapButton) themedInflater.inflate(layoutId, mapHudLayout, false);
		button.setAlwaysVisible(true);
		button.setNightMode(nightMode);
		button.setMapActivity(mapActivity);
		button.setOnTouchListener(null);
		button.setOnClickListener(null);
		button.setOnLongClickListener(null);
		button.setCustomAppearanceParams(appearanceParams);

		return button;
	}

	private void addMapButton(@NonNull MapButton mapButton) {
		mapButtons.add(mapButton);
		mapHudLayout.addMapButton(mapButton);
	}

	private void setupRulerWidget() {
		rulerWidget = (RulerWidget) themedInflater.inflate(R.layout.map_ruler, mapHudLayout, false);
		mapHudLayout.addWidget(rulerWidget);

		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		mapLayers.getMapInfoLayer().setupRulerWidget(rulerWidget);
	}

	@Override
	protected void updateContent() {
		updateButtons();
		MapButtonCard.setupButtonBackground(view, nightMode);
	}

	private void updateButtons() {
		for (MapButton button : mapButtons) {
			button.setInvalidated(true);
			button.update();
		}
		mapHudLayout.updateButtons();
	}

	public void clearWidgets() {
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		mapLayers.getMapInfoLayer().removeRulerWidgets(Collections.singletonList(rulerWidget));
	}
}