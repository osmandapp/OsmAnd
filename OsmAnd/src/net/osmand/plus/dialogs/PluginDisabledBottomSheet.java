package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import static net.osmand.plus.OsmandPlugin.PLUGIN_ID_KEY;

public class PluginDisabledBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = PluginDisabledBottomSheet.class.getName();

	private static final Log LOG = PlatformUtil.getLog(PluginDisabledBottomSheet.class);

	private String pluginId;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		Context context = getContext();
		if (context == null || app == null) {
			return;
		}

		if (savedInstanceState != null) {
			pluginId = savedInstanceState.getString(PLUGIN_ID_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				pluginId = args.getString(PLUGIN_ID_KEY);
			}
		}

		OsmandPlugin plugin = OsmandPlugin.getPlugin(pluginId);
		if (plugin == null) {
			return;
		}

		BaseBottomSheetItem titleItem = new TitleItem.Builder()
				.setTitle(getString(R.string.plugin_disabled))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create();
		items.add(titleItem);

		Typeface typeface = FontCache.getRobotoMedium(getContext());
		SpannableString pluginTitleSpan = new SpannableString(plugin.getName());
		pluginTitleSpan.setSpan(new CustomTypefaceSpan(typeface), 0, pluginTitleSpan.length(), 0);

		BaseBottomSheetItem pluginTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(pluginTitleSpan)
				.setTitleColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setIcon(getContentIcon(R.drawable.ic_extension_dark))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.create();
		items.add(pluginTitle);

		BaseBottomSheetItem descrItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.plugin_disabled_descr))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(descrItem);

	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.open_settings;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		Intent intent = getPluginSettingsIntent();
		if (app != null && AndroidUtils.isIntentSafe(app, intent)) {
			startActivity(intent);
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PLUGIN_ID_KEY, pluginId);
	}

	private Intent getPluginSettingsIntent() {
		Intent intent = null;

		OsmandApplication app = getMyApplication();
		OsmandPlugin plugin = OsmandPlugin.getPlugin(pluginId);
		if (plugin != null && app != null) {
			String installedPackage = null;
			if (OsmandPlugin.isPackageInstalled(plugin.getComponentId1(), app)) {
				installedPackage = plugin.getComponentId1();
			}
			if (OsmandPlugin.isPackageInstalled(plugin.getComponentId2(), app)) {
				installedPackage = plugin.getComponentId2();
			}
			if (installedPackage != null) {
				intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Uri uri = Uri.fromParts("package", installedPackage, null);
				intent.setData(uri);
			}
		}
		return intent;
	}

	public static void showInstance(@NonNull FragmentManager fm, String pluginId, Boolean usedOnMap) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PLUGIN_ID_KEY, pluginId);

				PluginDisabledBottomSheet dialog = new PluginDisabledBottomSheet();
				dialog.setArguments(args);
				dialog.setUsedOnMap(usedOnMap);
				dialog.show(fm, PluginDisabledBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}