package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.CustomMapButtonsFragment;
import net.osmand.plus.views.mapwidgets.configure.buttons.DefaultMapButtonsFragment;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.ArrayList;
import java.util.List;

public class ConfigureButtonsCard extends MapBaseCard {

	private final Fragment target;
	private final MapButtonsHelper mapButtonsHelper;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_buttons_card;
	}

	public ConfigureButtonsCard(@NonNull MapActivity activity, @NonNull Fragment target) {
		super(activity, false);
		this.target = target;
		this.mapButtonsHelper = app.getMapButtonsHelper();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.shared_string_buttons);

		setupCustomWidgetsButton();
		setupDefaultWidgetsButton();

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);
	}

	private void setupCustomWidgetsButton() {
		List<QuickActionButtonState> buttons = mapButtonsHelper.getButtonsStates();
		List<QuickActionButtonState> enabledButtons = mapButtonsHelper.getEnabledButtonsStates();

		boolean enabled = !enabledButtons.isEmpty();
		String title = getString(R.string.custom_buttons);

		View button = view.findViewById(R.id.custom_buttons);
		setupButton(button, title, null, R.drawable.ic_quick_action, enabled, nightMode);

		TextView count = button.findViewById(R.id.items_count_descr);
		count.setText(getString(R.string.ltr_or_rtl_combine_via_slash, enabledButtons.size(), buttons.size()));

		button.setOnClickListener(v -> CustomMapButtonsFragment.showInstance(mapActivity.getSupportFragmentManager(), target));

		AndroidUiHelper.updateVisibility(count, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), true);
	}

	private void setupDefaultWidgetsButton() {
		int enabledButtons = 0;
		List<MapButtonState> buttonStates = getDefaultButtonsStates();
		for (MapButtonState buttonState : buttonStates) {
			if (buttonState.isEnabled()) {
				enabledButtons++;
			}
		}
		boolean enabled = enabledButtons > 0;
		String title = getString(R.string.default_buttons);

		View button = view.findViewById(R.id.default_buttons);
		setupButton(button, title, null, R.drawable.ic_action_button_default, enabled, nightMode);

		TextView count = button.findViewById(R.id.items_count_descr);
		count.setText(getString(R.string.ltr_or_rtl_combine_via_slash, enabledButtons, buttonStates.size()));

		button.setOnClickListener(v -> DefaultMapButtonsFragment.showInstance(mapActivity.getSupportFragmentManager(), target));

		AndroidUiHelper.updateVisibility(count, true);
	}

	@NonNull
	public List<MapButtonState> getDefaultButtonsStates() {
		List<MapButtonState> list = new ArrayList<>();

		list.add(mapButtonsHelper.getMap3DButtonState());
		list.add(mapButtonsHelper.getCompassButtonState());

		return list;
	}

	public static void setupButton(@NonNull View view, @NonNull String title, @Nullable String description,
	                               @DrawableRes int iconId, boolean enabled, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		ApplicationMode appMode = app.getSettings().getApplicationMode();

		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(title);

		int iconColor = enabled ? appMode.getProfileColor(nightMode) : ColorUtilities.getDefaultIconColor(app, nightMode);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconId, iconColor));

		TextView descriptionTv = view.findViewById(R.id.description);
		descriptionTv.setText(description);
		AndroidUiHelper.updateVisibility(descriptionTv, description != null);

		setupListItemBackground(view, appMode, nightMode);
	}

	private static void setupListItemBackground(@NonNull View view, @NonNull ApplicationMode mode, boolean nightMode) {
		int color = mode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(view.getContext(), color, 0.3f);
		AndroidUtils.setBackground(view.findViewById(R.id.button_container), background);
	}
}