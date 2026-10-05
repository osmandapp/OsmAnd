package net.osmand.plus.plugins.development;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

/** The four switches that draw the map button grid, in one sheet instead of four rows. */
public class MapButtonGridBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = MapButtonGridBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.visualizing_button_grid)));
		addSwitch(R.string.efficient_grid, OsmandSettings.DEV_GRID_LAYOUT_DRAW_CELLS,
				checked -> OsmandSettings.DEV_GRID_LAYOUT_DRAW_CELLS = checked);
		addSwitch(R.string.slots, OsmandSettings.DEV_GRID_LAYOUT_DRAW_SLOTS,
				checked -> OsmandSettings.DEV_GRID_LAYOUT_DRAW_SLOTS = checked);
		addSwitch(R.string.button_frames, OsmandSettings.DEV_GRID_LAYOUT_DRAW_BUTTON_FRAMES,
				checked -> OsmandSettings.DEV_GRID_LAYOUT_DRAW_BUTTON_FRAMES = checked);
		addSwitch(R.string.show_layout_grid_logs, OsmandSettings.DEV_GRID_LAYOUT_SHOW_LOGS,
				checked -> OsmandSettings.DEV_GRID_LAYOUT_SHOW_LOGS = checked);
	}

	private void addSwitch(@StringRes int title, boolean checked, @NonNull OnChecked onChecked) {
		BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
		item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setTitle(getString(title))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(v -> {
					boolean value = !item[0].isChecked();
					item[0].setChecked(value);
					onChecked.onChecked(value);
					app.getOsmandMap().refreshMap();
				})
				.create();
		items.add(item[0]);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private interface OnChecked {
		void onChecked(boolean checked);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (!manager.isStateSaved() && manager.findFragmentByTag(TAG) == null) {
			MapButtonGridBottomSheet fragment = new MapButtonGridBottomSheet();
			fragment.show(manager, TAG);
		}
	}
}
