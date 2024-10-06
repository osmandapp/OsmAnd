package net.osmand.plus.plugins.odb.adapters;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.viewholders.FoundDeviceViewHolder;
import net.osmand.shared.data.BTDeviceInfo;
import net.osmand.util.Algorithms;

public class PairedDevicesAdapter extends FoundDevicesAdapter {

	private final FoundDevicesMenuListener deviceMenuClickListener;

	public PairedDevicesAdapter(@NonNull OsmandApplication app, boolean nightMode,
	                            @NonNull FoundDevicesMenuListener deviceMenuClickListener) {
		super(app, nightMode, null);
		this.deviceMenuClickListener = deviceMenuClickListener;
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		super.onBindViewHolder(holder, position);
		BTDeviceInfo device = (BTDeviceInfo) items.get(position);
		BTDeviceInfo connectedDevice = plugin.getConnectedDeviceInfo();
		boolean isConnected = connectedDevice != null && Algorithms.stringsEqual(device.getAddress(), connectedDevice.getAddress());
		holder.icon.setImageResource(isConnected ? R.drawable.widget_obd_car_day : R.drawable.ic_action_car_obd2);
		holder.menuIcon.setVisibility(View.VISIBLE);
		holder.menuIcon.setOnClickListener(v -> showOptionsMenu(v, device));
		holder.itemView.setOnClickListener(v -> {
			if (plugin != null/* && plugin.isDevicePaired(device)*/) {
				deviceMenuClickListener.onSettings(device);
			}
		});
		holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
	}

	private void showOptionsMenu(View view, BTDeviceInfo device) {
		PopupMenu optionsMenu = new PopupMenu(view.getContext(), view);
		((MenuBuilder) optionsMenu.getMenu()).setOptionalIconsVisible(true);
		MenuCompat.setGroupDividerEnabled(optionsMenu.getMenu(), true);
		BTDeviceInfo connectedDevice = plugin.getConnectedDeviceInfo();
		boolean isConnected = connectedDevice != null && Algorithms.stringsEqual(connectedDevice.getAddress(), device.getAddress());
		MenuItem enableDisableItem = optionsMenu.getMenu().add(1, 1, Menu.NONE,
				isConnected
						? R.string.external_device_details_disconnect
						: R.string.external_device_details_connect);
		enableDisableItem.setIcon(app.getUIUtilities().getIcon(
				isConnected ? R.drawable.ic_action_obd2_connector_disconnect : R.drawable.ic_action_obd2_connector_disable, getMenuIconColor()));
		enableDisableItem.setOnMenuItemClickListener(item -> {
			if (isConnected) {
				deviceMenuClickListener.onDisconnect(device);
			} else {
				deviceMenuClickListener.onConnect(device);
			}
			optionsMenu.dismiss();
			return true;
		});

		MenuItem settingsItem = optionsMenu.getMenu().add(1, 2, Menu.NONE,
				R.string.shared_string_settings);
		settingsItem.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_settings_outlined, getMenuIconColor()));
		settingsItem.setOnMenuItemClickListener(item -> {
			deviceMenuClickListener.onSettings(device);
			optionsMenu.dismiss();
			return true;
		});

		MenuItem renameItem = optionsMenu.getMenu().add(1, 3, Menu.NONE,
				R.string.shared_string_rename);
		renameItem.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_edit_outlined, getMenuIconColor()));
		renameItem.setOnMenuItemClickListener(item -> {
			deviceMenuClickListener.onRename(device);
			optionsMenu.dismiss();
			return true;
		});

		MenuItem forgetItem = optionsMenu.getMenu().add(2, 4, Menu.NONE,
				R.string.external_device_menu_forget);
		forgetItem.setIcon(R.drawable.ic_action_sensor_remove);
		forgetItem.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_sensor_remove, getMenuIconColor()));
		forgetItem.setOnMenuItemClickListener(item -> {
			deviceMenuClickListener.onForget(device);
			optionsMenu.dismiss();
			return true;
		});

		optionsMenu.show();
	}

	private @ColorRes int getMenuIconColor() {
		return nightMode ? R.color.icon_color_secondary_light : R.color.icon_color_secondary_dark;
	}

	public interface FoundDevicesMenuListener {
		void onDisconnect(@NonNull BTDeviceInfo device);

		void onConnect(@NonNull BTDeviceInfo device);

		void onSettings(@NonNull BTDeviceInfo device);

		void onRename(@NonNull BTDeviceInfo device);

		void onForget(@NonNull BTDeviceInfo device);
	}

}