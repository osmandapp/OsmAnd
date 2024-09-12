package net.osmand.plus.views.mapwidgets.configure.buttons;

import static android.view.Gravity.CENTER;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

public class MapButtonCard extends MapBaseCard {

	private final MapButtonState buttonState;
	private final ButtonAppearanceParams customAppearanceParams;

	private MapButton mapButton;

	public MapButtonCard(@NonNull MapActivity mapActivity, @NonNull MapButtonState buttonState,
	                     @Nullable ButtonAppearanceParams customAppearanceParams) {
		super(mapActivity, false);
		this.buttonState = buttonState;
		this.customAppearanceParams = customAppearanceParams;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.map_button_card;
	}

	@Override
	protected void updateContent() {
		ViewGroup container = view.findViewById(R.id.map_button_container);
		container.removeAllViews();

		setupButton(container);
		setupButtonBackground(container);
	}

	public void setupButton(@NonNull ViewGroup container) {
		mapButton = (MapButton) themedInflater.inflate(buttonState.getDefaultLayoutId(), container, false);
		mapButton.setAlwaysVisible(true);
		mapButton.setNightMode(nightMode);
		mapButton.setMapActivity(mapActivity);
		mapButton.setOnTouchListener(null);
		mapButton.setOnClickListener(null);
		mapButton.setOnLongClickListener(null);
		mapButton.setCustomAppearanceParams(customAppearanceParams);
		if (mapButton instanceof QuickActionButton actionButton) {
			actionButton.setButtonState((QuickActionButtonState) buttonState);
		}
		container.addView(mapButton, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, CENTER));
	}

	public void updateButton() {
		if (mapButton != null) {
			mapButton.update();
		}
	}

	private void setupButtonBackground(@NonNull View view) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer != null) {
			MapRenderRepositories maps = app.getResourceManager().getRenderer();
			RenderingRuleSearchRequest request = maps.getSearchRequestWithAppliedCustomRules(renderer, nightMode);
			if (request.searchRenderingAttribute("waterColor")) {
				int color = request.getIntPropertyValue(renderer.PROPS.R_ATTR_COLOR_VALUE);
				if (color != -1) {
					view.setBackgroundColor(color);
				}
			}
		}
	}
}