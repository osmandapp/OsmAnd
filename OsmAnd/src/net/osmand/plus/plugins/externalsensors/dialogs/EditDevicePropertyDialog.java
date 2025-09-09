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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class EditDevicePropertyDialog extends BaseFullScreenDialogFragment {

	public static final String TAG = "EditDeviceNameDialog";

	private static final String PROPERTY_ID_KEY = "name_key";
	private static final String DEVICE_ID_KEY = "content_key";

	private OsmandTextFieldBoxes propertyValueView;
	private ExtendedEditText textInput;
	private TextView propertyName;
	private String propertyOldValueValue;
	private DeviceChangeableProperty property;
	private String deviceId;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.dialog_edit_device_name, container, false);

		propertyName = view.findViewById(R.id.property_name);
		textInput = view.findViewById(R.id.description);
		textInput.requestFocus();

		propertyValueView = view.findViewById(R.id.property_value);
		propertyValueView.setClearButton(getContentIcon(R.drawable.ic_action_cancel));

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
			int propertyId = args.getInt(PROPERTY_ID_KEY);
			deviceId = args.getString(DEVICE_ID_KEY);
			property = DeviceChangeableProperty.values()[propertyId];
			textInput.setInputType(property.getInputType());
			propertyName.setText(property.getDisplayNameResId());
			propertyValueView.setHasClearButton(property == DeviceChangeableProperty.NAME);
			ExternalSensorsPlugin plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
			if (plugin != null) {
				int unitsId = property.getUnitsResId(app, false);
				if (unitsId != 0) {
					if (AndroidUtils.isLayoutRtl(view.getContext())) {
						textInput.setPrefix(app.getString(unitsId));
					} else {
						textInput.setSuffix(app.getString(unitsId));
					}

				}
				AbstractDevice<?> device = plugin.getDevice(deviceId);
				if (device != null) {
					propertyOldValueValue = plugin.getFormattedDevicePropertyValue(device, property).replace(",", ".");
					textInput.setText(propertyOldValueValue);
				}
			}
		}

		return view;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@Override
	protected int getStatusBarColorId() {
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	private boolean shouldClose() {
		Editable editable = textInput.getText();
		if (propertyOldValueValue == null || editable == null) {
			return true;
		}
		return propertyOldValueValue.equals(editable.toString());
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
		AlertDialogData dialogData = new AlertDialogData(requireActivity(), nightMode)
				.setTitle(R.string.shared_string_dismiss)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		CustomAlert.showSimpleMessage(dialogData, R.string.exit_without_saving);
	}

	private void onSaveEditedText(@NonNull String newName) {
		Fragment target = getTargetFragment();
		if (target instanceof OnSaveSensorPropertyCallback callback && property != null) {
			callback.changeSensorPropertyValue(deviceId, property, newName);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                @NonNull AbstractDevice<?> device,
	                                @NonNull DeviceChangeableProperty property) {
		if (!(target instanceof OnSaveSensorPropertyCallback)) {
			throw new IllegalArgumentException("target fragment should implement OnSaveSensorNameCallback");
		}
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			EditDevicePropertyDialog fragment = new EditDevicePropertyDialog();
			Bundle args = new Bundle();
			ExternalSensorsPlugin plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
			if (plugin != null) {
				args.putInt(PROPERTY_ID_KEY, property.ordinal());
				args.putString(DEVICE_ID_KEY, device.getDeviceId());
				fragment.setArguments(args);
				fragment.setTargetFragment(target, 0);
				fragment.show(fragmentManager, TAG);
			}
		}
	}

	public interface OnSaveSensorPropertyCallback {
		void changeSensorPropertyValue(@NonNull String sensorId, @NonNull DeviceChangeableProperty property, @NonNull String newValue);
	}
}
