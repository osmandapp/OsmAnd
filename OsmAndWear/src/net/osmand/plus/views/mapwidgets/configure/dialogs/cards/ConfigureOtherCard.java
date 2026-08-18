package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.controllers.MapFocusDialogController;
import net.osmand.plus.settings.enums.MapFocus;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.SpeedometerSettingsFragment;

public class ConfigureOtherCard extends MapBaseCard {

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_widgets_other_card;
	}

	public ConfigureOtherCard(@NonNull MapActivity mapActivity) {
		super(mapActivity, false);
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.shared_string_other);

		ApplicationMode appMode = settings.getApplicationMode();
		setupDisplayPositionButton(appMode);
		setupDistanceRulerButton(appMode);
		setupSpeedometerButton(appMode);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);
	}

	private void setupDisplayPositionButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.map_display_position_button);
		button.setOnClickListener(v -> MapFocusDialogController.showDialog(getMapActivity(), appMode));

		int value = settings.POSITION_PLACEMENT_ON_MAP.getModeValue(appMode);
		MapFocus mapFocus = MapFocus.valueOf(value);
		ConfigureButtonsCard.setupButton(button, getString(R.string.display_position),
				getString(mapFocus.getTitleId()), mapFocus.getIconId(), true, nightMode);

		AndroidUiHelper.updateVisibility(button, app.useOpenGlRenderer());
		AndroidUiHelper.updateVisibility(button.findViewById(R.id.short_divider), true);
	}

	private void setupDistanceRulerButton(@NonNull ApplicationMode appMode) {
		boolean enabled = settings.SHOW_DISTANCE_RULER.getModeValue(appMode);

		View button = view.findViewById(R.id.distance_by_tap_button);
		button.setOnClickListener(v -> DistanceByTapFragment.showInstance(getMapActivity()));
		ConfigureButtonsCard.setupButton(button, getString(R.string.map_widget_distance_by_tap), null, R.drawable.ic_action_ruler_line, enabled, nightMode);

		TextView description = button.findViewById(R.id.items_count_descr);
		description.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);
		description.setTextSize(COMPLEX_UNIT_PX, app.getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));

		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(button.findViewById(R.id.short_divider), true);
	}

	private void setupSpeedometerButton(@NonNull ApplicationMode appMode) {
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
	}
}