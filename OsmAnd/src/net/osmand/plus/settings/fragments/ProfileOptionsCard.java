package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class ProfileOptionsCard extends BaseCard {

	private ProfileOptionsController optionsController;

	public ProfileOptionsCard(@NonNull MapActivity activity) {
		super(activity);
	}

	public void setOptionsController(@NonNull ProfileOptionsController optionsController) {
		this.optionsController = optionsController;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.profile_appearance_options_card;
	}

	@Override
	protected void updateContent() {
		TextView titleView = view.findViewById(R.id.title_view);
		titleView.setText(R.string.shared_string_options);

		View angleView = view.findViewById(R.id.view_angle);
		View locationRadiusView = view.findViewById(R.id.location_radius);

		setupOptionItem(angleView, settings.VIEW_ANGLE_VISIBILITY, R.string.view_angle, R.string.view_angle_description, R.drawable.ic_action_location_view_angle);
		setupOptionItem(locationRadiusView, settings.LOCATION_RADIUS_VISIBILITY, R.string.location_radius, R.string.location_radius_description, R.drawable.ic_action_location_radius);
	}

	private void setupOptionItem(@NonNull View itemView, CommonPreference<MarkerDisplayOption> preference, @StringRes int itemNameRes, @StringRes int itemDescriptionRes, @DrawableRes int iconRes) {
		TextView title = itemView.findViewById(R.id.title);
		title.setText(itemNameRes);

		TextView description = itemView.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(description, true);
		description.setText(optionsController.getSelectedItem(preference).getNameRes());

		int color = settings.getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(itemView.findViewById(R.id.button_container), background);

		ImageView iconView = itemView.findViewById(R.id.icon);
		Drawable icon = app.getUIUtilities().getPaintedIcon(iconRes, ColorUtilities.getActiveColor(app, nightMode));
		iconView.setImageDrawable(icon);

		if (activity instanceof MapActivity) {
			itemView.setOnClickListener(v -> optionsController.showDialog((MapActivity) activity,
					app.getString(itemNameRes), app.getString(itemDescriptionRes), preference));
		}
	}
}
