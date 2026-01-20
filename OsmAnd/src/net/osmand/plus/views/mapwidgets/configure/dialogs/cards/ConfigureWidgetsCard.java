package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.LEFT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.RIGHT;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment.SCREEN_LAYOUT_MODE;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.util.CollectionUtils;

import java.util.Collections;

public class ConfigureWidgetsCard extends MapBaseCard {

	private final MapWidgetRegistry widgetRegistry;
	private final ScreenLayoutMode[] layoutMode;

	private final int profileColor;
	private final int defaultIconColor;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_card;
	}

	public ConfigureWidgetsCard(@NonNull MapActivity mapActivity, @NonNull ScreenLayoutMode[] layoutMode) {
		super(mapActivity, false);
		this.layoutMode = layoutMode;
		this.widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		this.profileColor = appMode.getProfileColor(nightMode);
		this.defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
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
	}

	private void setupWidgetGroupView(@NonNull View view, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		boolean rtl = AndroidUtils.isLayoutRtl(app);
		int count = getWidgetsCount(panel, appMode);

		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.items_count_descr);

		title.setText(panel.getTitleId(rtl));
		description.setText(String.valueOf(count));

		int iconColor = count > 0 ? profileColor : defaultIconColor;
		icon.setImageDrawable(getPaintedIcon(panel.getIconId(rtl, layoutMode[0]), iconColor));

		view.findViewById(R.id.button_container).setOnClickListener(v -> {
			Bundle args = new Bundle();
			args.putSerializable(SCREEN_LAYOUT_MODE, layoutMode[0]);
			ConfigureWidgetsFragment.showInstance(getMapActivity(), panel, appMode, args);
		});

		view.setTag(panel.name());
		setupListItemBackground(view, appMode);
		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), CollectionUtils.equalsToAny(panel, RIGHT, BOTTOM));
	}

	private int getWidgetsCount(@NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		return widgetRegistry.getWidgetsForPanel(mapActivity, appMode, layoutMode[0], filter, Collections.singletonList(panel)).size();
	}

	private void setupTransparentWidgetsButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.transparent_widgets_button);

		boolean enabled = settings.getTransparentMapThemePreference(layoutMode[0]).getModeValue(appMode);
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

			boolean transparent = settings.getTransparentMapThemePreference(layoutMode[0]).get();
			settings.getTransparentMapThemePreference(layoutMode[0]).setModeValue(appMode, !transparent);
			mapActivity.updateApplicationModeSettings();
		});
		AndroidUiHelper.updateVisibility(button.findViewById(R.id.short_divider), true);
	}

	private void setupListItemBackground(@NonNull View view, @NonNull ApplicationMode appMode) {
		int color = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.button_container), background);
	}
}