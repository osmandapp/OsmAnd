package net.osmand.plus.voice;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class VoiceProviderDialog {

	public static final String MORE_VALUE = "MORE_VALUE";

	public static void showVoiceProviderDialog(@NonNull MapActivity activity, ApplicationMode appMode, boolean applyAllModes) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		RoutingOptionsHelper routingOptionsHelper = app.getRoutingOptionsHelper();
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		String[] firstSelectedVoiceProvider = new String[1];

		View view = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.select_voice_first, null);

		((ImageView) view.findViewById(R.id.icon))
				.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_volume_up, settings.isLightContent()));

		view.findViewById(R.id.spinner).setOnClickListener(v -> routingOptionsHelper.selectVoiceGuidance(activity, result -> {
			boolean acceptableValue = MORE_VALUE.equals(firstSelectedVoiceProvider[0]);
			if (acceptableValue) {
				((TextView) v.findViewById(R.id.selectText))
						.setText(routingOptionsHelper.getVoiceProviderName(v.getContext(), result));
				firstSelectedVoiceProvider[0] = result;
			}
			return acceptableValue;
		}, appMode));

		((ImageView) view.findViewById(R.id.dropDownIcon))
				.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, settings.isLightContent()));

		builder.setCancelable(true);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			if (!Algorithms.isEmpty(firstSelectedVoiceProvider[0])) {
				routingOptionsHelper.applyVoiceProvider(activity, firstSelectedVoiceProvider[0], applyAllModes);
				if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(firstSelectedVoiceProvider[0])) {
					settings.VOICE_MUTE.setModeValue(appMode, true);
				} else {
					settings.VOICE_MUTE.setModeValue(appMode, false);
				}
			}
		});
		builder.setNeutralButton(R.string.shared_string_do_not_use, (dialogInterface, i) -> {
			if (applyAllModes) {
				for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
					//if (!settings.VOICE_PROVIDER.isSetForMode(mode)) {
					settings.VOICE_PROVIDER.setModeValue(mode, OsmandSettings.VOICE_PROVIDER_NOT_USE);
					settings.VOICE_MUTE.setModeValue(mode, true);
					//}
				}
			}
			settings.VOICE_PROVIDER.setModeValue(appMode, OsmandSettings.VOICE_PROVIDER_NOT_USE);
			settings.VOICE_MUTE.setModeValue(appMode, true);
		});

		builder.setView(view);
		builder.show();
	}
}
