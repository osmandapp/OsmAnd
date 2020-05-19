package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class OsmAndDialogs {

	private static Map<Integer, OsmAndAction> dialogActions = new HashMap<Integer, OsmAndAction>(); 
	public static Dialog createDialog(int dialogID, Activity activity, Bundle args) {
		OsmAndAction action = dialogActions.get(dialogID);
		if(action != null) {
			return action.createDialog(activity, args);
		}
		return null;
	}
	
	public static void prepareDialog(int dialogID, Activity activity, Bundle args, Dialog dlg) {
		OsmAndAction action = dialogActions.get(dialogID);
		if(action != null) {
			action.prepareDialog(activity, args, dlg);
		}
	}


	public static void showVoiceProviderDialog(final MapActivity activity, final ApplicationMode applicationMode, final boolean applyAllModes) {
		OsmandApplication app = activity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final RoutingOptionsHelper routingOptionsHelper = app.getRoutingOptionsHelper();
		final AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		final String[] firstSelectedVoiceProvider = new String[1];

		View view = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.select_voice_first, null);

		((ImageView) view.findViewById(R.id.icon))
				.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_volume_up, settings.isLightContent()));

		view.findViewById(R.id.spinner).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				routingOptionsHelper.selectVoiceGuidance(activity, new CallbackWithObject<String>() {
					@Override
					public boolean processResult(String result) {
						boolean acceptableValue = !RoutePreferencesMenu.MORE_VALUE.equals(firstSelectedVoiceProvider[0]);
						if (acceptableValue) {
							((TextView) v.findViewById(R.id.selectText))
									.setText(routingOptionsHelper.getVoiceProviderName(v.getContext(), result));
							firstSelectedVoiceProvider[0] = result;
						}
						return acceptableValue;
					}
				}, applicationMode);
			}
		});

		((ImageView) view.findViewById(R.id.dropDownIcon))
				.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, settings.isLightContent()));

		builder.setCancelable(true);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (!Algorithms.isEmpty(firstSelectedVoiceProvider[0])) {
					routingOptionsHelper.applyVoiceProvider(activity, firstSelectedVoiceProvider[0], applyAllModes);
					if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(firstSelectedVoiceProvider[0])) {
						settings.VOICE_MUTE.setModeValue(applicationMode, true);
					} else {
						settings.VOICE_MUTE.setModeValue(applicationMode, false);
					}
				}
			}
		});
		builder.setNeutralButton(R.string.shared_string_do_not_use, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				if (applyAllModes) {
					for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
						//if (!settings.VOICE_PROVIDER.isSetForMode(mode)) {
							settings.VOICE_PROVIDER.setModeValue(mode, OsmandSettings.VOICE_PROVIDER_NOT_USE);
							settings.VOICE_MUTE.setModeValue(mode, true);
						//}
					}
				}
				settings.VOICE_PROVIDER.setModeValue(applicationMode, OsmandSettings.VOICE_PROVIDER_NOT_USE);
				settings.VOICE_MUTE.setModeValue(applicationMode, true);
			}
		});

		builder.setView(view);
		builder.show();
	}
	
	public static void registerDialogAction(OsmAndAction action) {
		if(action.getDialogID() != 0) {
			dialogActions.put(action.getDialogID(), action);
		}
	}
			
	public static final int DIALOG_ADD_FAVORITE = 200;
	public static final int DIALOG_REPLACE_FAVORITE = 201;
	public static final int DIALOG_ADD_WAYPOINT = 202;
	public static final int DIALOG_RELOAD_TITLE = 203;
	public static final int DIALOG_SHARE_LOCATION = 204;
	public static final int DIALOG_SAVE_DIRECTIONS = 206;
	public static final int DIALOG_START_GPS = 207;
	
	
	public static int getPluginDialogId(int pluginId, int dialogId) {
		return (pluginId + 3) * 100 + dialogId;
	}
}
