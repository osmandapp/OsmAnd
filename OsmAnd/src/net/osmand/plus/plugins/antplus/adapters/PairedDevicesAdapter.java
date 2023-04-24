package net.osmand.plus.plugins.antplus.adapters;

import android.content.res.ColorStateList;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.antplus.ExternalDevice;
import net.osmand.plus.plugins.antplus.viewholders.FoundDeviceViewHolder;

public class PairedDevicesAdapter extends FoundDevicesAdapter {

	private FoundDevicesMenuListener deviceMenuClickListener;

	public PairedDevicesAdapter(@NonNull OsmandApplication app, boolean nightMode, FoundDevicesMenuListener deviceMenuClickListener) {
		super(app, nightMode, null);
		this.deviceMenuClickListener = deviceMenuClickListener;
	}

	@Override
	public void onBindViewHolder(@NonNull FoundDeviceViewHolder holder, int position) {
		super.onBindViewHolder(holder, position);
		ExternalDevice device = items.get(position);
		holder.menuIcon.setOnClickListener(v -> showOptionsMenu(v, device));

	}

	private void showOptionsMenu(View view, ExternalDevice device) {
		PopupMenu optionsMenu = new PopupMenu(view.getContext(), view);
		((MenuBuilder) optionsMenu.getMenu()).setOptionalIconsVisible(true);
		optionsMenu.getMenu().setGroupDividerEnabled(true);
		MenuItem enableDisableItem = optionsMenu.getMenu().add(1, 1, Menu.NONE,
				device.isConnected() ?
						R.string.external_device_details_disconnect :
						R.string.external_device_details_connect);
		enableDisableItem.setIcon(device.isConnected() ? R.drawable.ic_action_sensor_off : R.drawable.ic_action_sensor);
		enableDisableItem.setIconTintList(ColorStateList.valueOf(getMenuIconColor()));
		enableDisableItem.setOnMenuItemClickListener(item -> {
			if (device.isConnected()) {
				deviceMenuClickListener.onDisconnect(device);
			} else {
				deviceMenuClickListener.onConnect(device);
			}
			optionsMenu.dismiss();
			return true;
		});

		MenuItem settingsItem = optionsMenu.getMenu().add(1, 2, Menu.NONE,
				R.string.shared_string_settings);
		settingsItem.setIcon(R.drawable.ic_action_settings_outlined);
		settingsItem.setIconTintList(ColorStateList.valueOf(getMenuIconColor()));
		settingsItem.setOnMenuItemClickListener(item -> {
			deviceMenuClickListener.onSettings(device);
			optionsMenu.dismiss();
			return true;
		});

		MenuItem renameItem = optionsMenu.getMenu().add(1, 3, Menu.NONE,
				R.string.shared_string_rename);
		renameItem.setIcon(R.drawable.ic_action_edit_outlined);
		renameItem.setIconTintList(ColorStateList.valueOf(getMenuIconColor()));
		renameItem.setOnMenuItemClickListener(item -> {
			deviceMenuClickListener.onRename(device);
			optionsMenu.dismiss();
			return true;
		});

		MenuItem forgetItem = optionsMenu.getMenu().add(2, 4, Menu.NONE,
				R.string.external_device_menu_forget);
		forgetItem.setIcon(R.drawable.ic_action_sensor_remove);
		forgetItem.setIconTintList(ColorStateList.valueOf(getMenuIconColor()));
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
		void onDisconnect(ExternalDevice device);

		void onConnect(ExternalDevice device);

		void onSettings(ExternalDevice device);

		void onRename(ExternalDevice device);

		void onForget(ExternalDevice device);
	}

}