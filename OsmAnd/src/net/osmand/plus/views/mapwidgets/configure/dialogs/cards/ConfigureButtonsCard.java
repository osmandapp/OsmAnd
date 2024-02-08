package net.osmand.plus.views.mapwidgets.configure.dialogs.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet;
import net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet;

public class ConfigureButtonsCard extends MapBaseCard {

	private final Fragment target;

	@Override
	public int getCardLayoutId() {
		return R.layout.configure_buttons_card;
	}

	public ConfigureButtonsCard(@NonNull MapActivity activity, @NonNull Fragment target) {
		super(activity, false);
		this.target = target;
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.shared_string_buttons);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);

		ApplicationMode appMode = settings.getApplicationMode();

		setupCompassVisibilityButton(appMode);
		setupMap3DModeVisibilityButton(appMode);
		setupQuickActionsButton(appMode);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);
	}

	private void setupQuickActionsButton(@NonNull ApplicationMode appMode) {
		String actions = getString(R.string.shared_string_actions);
		int actionsCount = app.getQuickActionRegistry().getQuickActions().size();

		View button = view.findViewById(R.id.quick_actions_button);
		button.setOnClickListener(v -> QuickActionListFragment.showInstance(getMapActivity()));
		setupButton(button, getString(R.string.configure_screen_quick_action), getString(R.string.ltr_or_rtl_combine_via_colon, actions, String.valueOf(actionsCount)), R.drawable.ic_quick_action, settings.QUICK_ACTION.getModeValue(appMode), nightMode);
	}

	private void setupMap3DModeVisibilityButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.map_3d_mode_button);
		button.setOnClickListener(v -> {
			FragmentManager fragmentManager = getMapActivity().getSupportFragmentManager();
			Map3DModeBottomSheet.showInstance(fragmentManager, target, appMode);
		});
		Map3DModeVisibility visibility = settings.MAP_3D_MODE_VISIBILITY.getModeValue(appMode);
		setupButton(button, getString(R.string.map_3d_mode_action), getString(visibility.getTitleId()), visibility.iconId, true, nightMode);

		AndroidUiHelper.updateVisibility(button, app.useOpenGlRenderer());
		AndroidUiHelper.updateVisibility(button.findViewById(R.id.short_divider), true);
	}

	private void setupCompassVisibilityButton(@NonNull ApplicationMode appMode) {
		View button = view.findViewById(R.id.compass_button);
		button.setOnClickListener(v -> {
			FragmentManager fragmentManager = getMapActivity().getSupportFragmentManager();
			CompassVisibilityBottomSheet.showInstance(fragmentManager, target, appMode);
		});
		CompassVisibility visibility = settings.COMPASS_VISIBILITY.getModeValue(appMode);
		setupButton(button, getString(R.string.map_widget_compass), getString(visibility.getTitleId()), visibility.iconId, true, nightMode);

		AndroidUiHelper.updateVisibility(button.findViewById(R.id.short_divider), true);
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