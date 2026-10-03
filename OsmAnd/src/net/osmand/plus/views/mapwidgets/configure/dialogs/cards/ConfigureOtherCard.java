package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.controllers.MapFocusDialogController;
import net.osmand.plus.settings.enums.MapFocus;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.SpeedometerSettingsFragment;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigureOtherCard extends MapBaseCard {
	private final MapWidgetRegistry widgetRegistry;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_other_card;
	}

	public ConfigureOtherCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
		this.widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.shared_string_other);

		ApplicationMode appMode = settings.getApplicationMode();
		List<View> rows = Stream.of(
				setupDisplayPositionButton(appMode),
				setupDistanceRulerButton(appMode),
				setupSpeedometerButton(appMode),
				setupAndroidAutoWidgetsButton()
		).filter(Objects::nonNull).collect(Collectors.toList());
		for (int i = 0; i < rows.size(); i++) {
			AndroidUiHelper.updateVisibility(rows.get(i).findViewById(R.id.short_divider), i != rows.size() - 1);
		}

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);
	}

	private View setupDisplayPositionButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.map_display_position_button);
		button.setOnClickListener(v -> MapFocusDialogController.showDialog(getMapActivity(), appMode));

		int value = settings.POSITION_PLACEMENT_ON_MAP.getModeValue(appMode);
		MapFocus mapFocus = MapFocus.valueOf(value);
		ConfigureButtonsCard.setupButton(button, getString(R.string.display_position),
				getString(mapFocus.getTitleId()), mapFocus.getIconId(), true, nightMode);

		AndroidUiHelper.updateVisibility(button, true);

		return button;
	}

	private View setupDistanceRulerButton(@NonNull ApplicationMode appMode) {
		boolean enabled = settings.SHOW_DISTANCE_RULER.getModeValue(appMode);

		View button = view.findViewById(R.id.distance_by_tap_button);
		button.setOnClickListener(v -> DistanceByTapFragment.showInstance(getMapActivity()));
		ConfigureButtonsCard.setupButton(button, getString(R.string.map_widget_distance_by_tap), null, R.drawable.ic_action_ruler_line, enabled, nightMode);

		TextView description = button.findViewById(R.id.items_count_descr);
		description.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);
		description.setTextSize(COMPLEX_UNIT_PX, app.getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(description, true);

		return button;
	}

	private View setupSpeedometerButton(@NonNull ApplicationMode appMode) {
		boolean enabled = settings.SHOW_SPEEDOMETER.getModeValue(appMode);

		String title = getString(R.string.shared_string_speedometer);
		int iconId = enabled ? (nightMode ? R.drawable.widget_speed_night : R.drawable.widget_speed_day) : R.drawable.ic_action_speed_outlined;

		View button = view.findViewById(R.id.speedometer);
		ConfigureButtonsCard.setupButton(button, title, null, 0, enabled, nightMode);
		button.setOnClickListener(v -> SpeedometerSettingsFragment.showInstance(getMapActivity()));

		ImageView imageView = button.findViewById(R.id.icon);
		imageView.setImageDrawable(getIcon(iconId));

		TextView description = button.findViewById(R.id.items_count_descr);
		description.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);
		description.setTextSize(COMPLEX_UNIT_PX, app.getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(description, true);

		return button;
	}

	private View setupAndroidAutoWidgetsButton() {
		boolean isAndroidAutoAvailable = InAppPurchaseUtils.isAndroidAutoAvailable(getMyApplication());
		boolean modeIsCompatible = appMode.isAndroidAutoCompatible();
		boolean shouldShow = isAndroidAutoAvailable && modeIsCompatible;

		View button = view.findViewById(R.id.aa_widgets);
		AndroidUiHelper.updateVisibility(button, shouldShow);

		if (shouldShow) {
			String title = getString(R.string.android_auto_widget_settings);

			int count = getAndroidAutoWidgetsCount(appMode);
			TextView description = button.findViewById(R.id.items_count_descr);
			description.setText(String.valueOf(count));

			int iconId = nightMode ? R.drawable.ic_action_android_auto_colored_night : R.drawable.ic_action_android_auto_colored;
			ConfigureButtonsCard.setupButton(button, title, null, iconId, true, nightMode);
			button.setOnClickListener(v -> {
				ConfigureWidgetsFragment.showInstanceForAndroidAuto(getMapActivity(), WidgetsPanel.ANDROID_AUTO, appMode, null);
			});
			AndroidUiHelper.updateVisibility(description, true);
			return button;
		}
		return null;
	}

	private int getAndroidAutoWidgetsCount(@NonNull ApplicationMode appMode) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		return widgetRegistry
				.getAndroidAutoWidgetsForPanel(app, appMode, filter, Collections.singletonList(WidgetsPanel.ANDROID_AUTO))
				.size();
	}
}
