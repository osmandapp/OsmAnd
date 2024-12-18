package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet;
import net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet;

public class MapButtonVisibilityCard extends MapBaseCard {

	private final Fragment fragment;
	private final ApplicationMode appMode;
	private final MapButtonState buttonState;

	public MapButtonVisibilityCard(@NonNull MapActivity activity, @NonNull MapButtonState buttonState, @NonNull Fragment fragment) {
		super(activity, false);
		this.fragment = fragment;
		this.buttonState = buttonState;
		this.appMode = settings.getApplicationMode();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.map_button_visibility_card;
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		title.setText(buttonState.getDescription());

		setupVisibilityButton(view.findViewById(R.id.button_container));
		view.setBackgroundColor(ColorUtilities.getCardAndListBackgroundColor(app, nightMode));
	}

	public void setupVisibilityButton(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.gpx_visibility_txt);

		ImageView icon = view.findViewById(R.id.icon);
		if (buttonState.isEnabled()) {
			icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_show_16));
		} else {
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_show_16));
		}
		if (buttonState instanceof Map3DButtonState) {
			setup3DVisibility(view, (Map3DButtonState) buttonState);
		} else if (buttonState instanceof CompassButtonState) {
			setupCompassVisibility(view, (CompassButtonState) buttonState);
		} else {
			setupBooleanVisibility(view);
		}
		UiUtilities.setupListItemBackground(view.getContext(), view, appMode.getProfileColor(nightMode));
	}

	public void setup3DVisibility(@NonNull View view, @NonNull Map3DButtonState buttonState) {
		TextView description = view.findViewById(R.id.description);
		description.setText(buttonState.getVisibility(appMode).getTitleId());

		view.setOnClickListener(v -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			Map3DModeBottomSheet.showInstance(manager, fragment, appMode);
		});
		AndroidUiHelper.updateVisibility(description, true);
	}

	public void setupCompassVisibility(@NonNull View view, @NonNull CompassButtonState buttonState) {
		TextView description = view.findViewById(R.id.description);
		description.setText(buttonState.getModeVisibility(appMode).getTitleId());

		view.setOnClickListener(v -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			CompassVisibilityBottomSheet.showInstance(manager, fragment, appMode);
		});
		AndroidUiHelper.updateVisibility(description, true);
	}

	public void setupBooleanVisibility(@NonNull View view) {
		CommonPreference<Boolean> preference = (CommonPreference<Boolean>) buttonState.getVisibilityPref();

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setClickable(false);
		compoundButton.setFocusable(false);
		compoundButton.setChecked(preference.getModeValue(appMode));
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		view.setOnClickListener(v -> {
			boolean visible = !compoundButton.isChecked();
			compoundButton.setChecked(visible);
			preference.setModeValue(appMode, visible);
			notifyCardPressed();
		});
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.compound_button), true);
	}
}