package net.osmand.plus.plugins.externalsensors.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.EditTextEx;
import net.osmand.util.Algorithms;

public class EditDeviceNameDialog extends BaseOsmAndDialogFragment {

	public static final String TAG = "EditDeviceNameDialog";

	private static final String NAME_KEY = "name_key";
	private static final String SENSOR_ID_KEY = "content_key";

	private EditTextEx textInput;
	private String name;
	private String sensorId;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.dialog_edit_device_name, container, false);

		textInput = view.findViewById(R.id.description);
		textInput.requestFocus();

		view.findViewById(R.id.btn_close).setOnClickListener(v -> {
			if (shouldClose()) {
				dismiss();
			} else {
				showDismissDialog();
			}
		});

		setupSaveButton(view);

		Bundle args = getArguments();
		if (args != null) {
			name = args.getString(NAME_KEY);
			if (name != null) {
				textInput.append(name);
			}
			sensorId = args.getString(SENSOR_ID_KEY);
		}

		return view;
	}

	@Override
	protected boolean useMapNightMode() {
		return true;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			int statusBarColor = ColorUtilities.getActivityBgColor(ctx, nightMode);
			window.setStatusBarColor(statusBarColor);
		}
		return dialog;
	}

	private boolean shouldClose() {
		Editable editable = textInput.getText();
		if (name == null || editable == null) {
			return true;
		}
		return name.equals(editable.toString());
	}

	private void setupSaveButton(View view) {
		View btnSaveContainer = view.findViewById(R.id.btn_save_container);
		btnSaveContainer.setOnClickListener(v -> {
			Editable editable = textInput.getText();
			if (!Algorithms.isEmpty(editable)) {
				onSaveEditedText(editable.toString());
				dismiss();
			}
		});

		Context ctx = btnSaveContainer.getContext();
		AndroidUtils.setBackground(ctx, btnSaveContainer, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		View btnSave = view.findViewById(R.id.btn_save);
		int drawableRes = nightMode ? R.drawable.btn_solid_border_dark : R.drawable.btn_solid_border_light;
		AndroidUtils.setBackground(btnSave, getIcon(drawableRes));
	}

	private void showDismissDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode(false));
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		dismissDialog.show();
	}

	private void onSaveEditedText(@NonNull String newName) {
		Fragment target = getTargetFragment();
		if (target instanceof OnSaveSensorNameCallback) {
			OnSaveSensorNameCallback callback = (OnSaveSensorNameCallback) target;
			callback.changeSensorName(sensorId, newName);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable Fragment target, @NonNull AbstractDevice<?> device) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			EditDeviceNameDialog fragment = new EditDeviceNameDialog();
			Bundle args = new Bundle();
			ExternalSensorsPlugin plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
			if (plugin != null) {
				args.putString(NAME_KEY, plugin.getDeviceName(device));
				args.putString(SENSOR_ID_KEY, device.getDeviceId());
				fragment.setArguments(args);
				fragment.setTargetFragment(target, 0);
				fragment.show(fragmentManager, TAG);
			}
		}
	}

	public interface OnSaveSensorNameCallback {
		void changeSensorName(@NonNull String sensorId, @NonNull String newName);
	}
}
