package net.osmand.plus.plugins.development;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

/** The three things that pretend the app has just been installed, in one sheet. */
public class SimulateUiBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SimulateUiBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.simulate_ui)));

		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.simulate_initial_startup))
				.setIconHidden(true)
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_pad_32dp)
				.setOnClickListener(v -> {
					resetFirstStart();
					dismiss();
				})
				.create());

		items.add(new DividerHalfItem(getContext()));

		addSwitch(R.string.show_free_version_banner, settings.SHOULD_SHOW_FREE_VERSION_BANNER);
		addSwitch(R.string.show_discount_bottom_sheet, settings.SHOULD_SHOW_DISCOUNT_BOTTOM_SHEET);
	}

	private void addSwitch(@StringRes int title, @NonNull OsmandPreference<Boolean> preference) {
		BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
		item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(preference.get())
				.setTitle(getString(title))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(v -> {
					boolean checked = !item[0].isChecked();
					preference.set(checked);
					item[0].setChecked(checked);
				})
				.create();
		items.add(item[0]);
	}

	private void resetFirstStart() {
		app.getAppInitializer().resetFirstTimeRun();
		settings.FIRST_MAP_IS_DOWNLOADED.resetToDefault();
		settings.WEBGL_SUPPORTED.resetToDefault();
		settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.resetToDefault();

		MapillaryPlugin mapillaryPlugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
		if (mapillaryPlugin != null) {
			mapillaryPlugin.MAPILLARY_FIRST_DIALOG_SHOWN.resetToDefault();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (!manager.isStateSaved() && manager.findFragmentByTag(TAG) == null) {
			SimulateUiBottomSheet fragment = new SimulateUiBottomSheet();
			fragment.show(manager, TAG);
		}
	}
}
