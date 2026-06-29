/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

/**
 * Asks whether to download SRTM elevation tiles for a county or country.
 */
public class ElevationDownloadPromptBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ElevationDownloadPromptBottomSheet.class.getName();

	private static final String ARG_MESSAGE = "message";
	private static final String ARG_REGION_ID = "region_id";
	private static final String ARG_TILE_COUNT = "tile_count";
	private static final String ARG_FIRST_ENABLE = "first_enable";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String message = args != null ? args.getString(ARG_MESSAGE, "") : "";

		View root = inflate(R.layout.bottom_sheet_icon_title_description);
		android.widget.TextView title = root.findViewById(R.id.title);
		android.widget.TextView description = root.findViewById(R.id.description);
		title.setText(R.string.driver_break_elevation_download_title);
		description.setText(message);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(root).create());
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_download;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.driver_break_elevation_not_now;
	}

	@Override
	protected void onDismissButtonClickAction() {
		notifyPlugin(false);
		dismiss();
	}

	@Override
	protected void onRightBottomButtonClick() {
		notifyPlugin(true);
		dismiss();
	}

	private void notifyPlugin(boolean download) {
		Bundle args = getArguments();
		if (args == null) {
			return;
		}
		String regionId = args.getString(ARG_REGION_ID);
		if (regionId == null) {
			return;
		}
		int tileCount = args.getInt(ARG_TILE_COUNT, 0);
		boolean firstEnable = args.getBoolean(ARG_FIRST_ENABLE, false);
		DriverBreakPlugin plugin = PluginsHelper.getPlugin(DriverBreakPlugin.class);
		if (plugin != null) {
			if (download) {
				plugin.onElevationDownloadAccepted(regionId, firstEnable);
			} else {
				plugin.onElevationDownloadDeclined(regionId, firstEnable);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable ApplicationMode appMode,
			boolean usedOnMap, @NonNull String regionId, @NonNull String message, int tileCount,
			boolean firstEnablePrompt) {
		if (!AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			return;
		}
		ElevationDownloadPromptBottomSheet sheet = new ElevationDownloadPromptBottomSheet();
		Bundle args = new Bundle();
		args.putString(ARG_REGION_ID, regionId);
		args.putString(ARG_MESSAGE, message);
		args.putInt(ARG_TILE_COUNT, tileCount);
		args.putBoolean(ARG_FIRST_ENABLE, firstEnablePrompt);
		sheet.setArguments(args);
		sheet.setUsedOnMap(usedOnMap);
		sheet.setAppMode(appMode);
		sheet.show(fragmentManager, TAG);
	}
}
