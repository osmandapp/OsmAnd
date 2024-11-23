package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;

import java.util.Collections;

public class ConfigureWidgetsCard extends MapBaseCard {

	private final MapWidgetRegistry widgetRegistry;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_card;
	}

	public ConfigureWidgetsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.shared_string_widgets);
		description.setText(R.string.configure_screen_widgets_descr);

		ApplicationMode appMode = settings.getApplicationMode();

		setupWidgetGroupView(view.findViewById(R.id.left_panel), LEFT, appMode);
		setupWidgetGroupView(view.findViewById(R.id.right_panel), RIGHT, appMode);
		setupWidgetGroupView(view.findViewById(R.id.top_panel), TOP, appMode);
		setupWidgetGroupView(view.findViewById(R.id.bottom_panel), BOTTOM, appMode);

		setupTransparentWidgetsButton(appMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottomShadowView), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);
	}

	private void setupWidgetGroupView(@NonNull View view, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		boolean rtl = AndroidUtils.isLayoutRtl(app);
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		view.setTag(panel.name());
		ImageView ivIcon = view.findViewById(R.id.icon);
		TextView tvTitle = view.findViewById(R.id.title);

		int count = getWidgetsCount(panel, appMode);
		int iconColor = count > 0 ? activeColor : defColor;
		ivIcon.setImageDrawable(getPaintedIcon(panel.getIconId(rtl), iconColor));

		tvTitle.setText(panel.getTitleId(rtl));

		updateWidgetsCount(view, count);

		view.findViewById(R.id.button_container).setOnClickListener(v -> ConfigureWidgetsFragment.showInstance(getMapActivity(), panel, appMode));

		setupListItemBackground(view, appMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), panel == RIGHT);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.long_divider), panel == BOTTOM);
	}

	private int getWidgetsCount(@NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		return widgetRegistry.getWidgetsForPanel(mapActivity, appMode, filter, Collections.singletonList(panel)).size();
	}

	private void updateWidgetsCount(@NonNull View container, int count) {
		TextView countContainer = container.findViewById(R.id.items_count_descr);
		countContainer.setText(String.valueOf(count));
		AndroidUiHelper.updateVisibility(countContainer, true);
	}

	private void setupTransparentWidgetsButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.transparent_widgets_button);

		boolean enabled = settings.TRANSPARENT_MAP_THEME.getModeValue(appMode);
		ConfigureButtonsCard.setupButton(button, getString(R.string.map_widget_transparent), null, R.drawable.ic_action_appearance, enabled, nightMode);

		CompoundButton compoundButton = button.findViewById(R.id.compound_button);
		compoundButton.setChecked(enabled);
		AndroidUiHelper.updateVisibility(compoundButton, true);

		button.setOnClickListener(v -> {
			boolean newState = !compoundButton.isChecked();
			compoundButton.setChecked(newState);
		});
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		UiUtilities.setupCompoundButton(nightMode, activeColor, compoundButton);

		ImageView icon = button.findViewById(R.id.icon);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			icon.setColorFilter(isChecked ? activeColor : defColor);

			boolean transparent = settings.TRANSPARENT_MAP_THEME.get();
			settings.TRANSPARENT_MAP_THEME.setModeValue(appMode, !transparent);
			mapActivity.updateApplicationModeSettings();
		});
	}

	private void setupListItemBackground(@NonNull View view, @NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.button_container);
		int color = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
	}
}
