package net.osmand.plus.plugins;

import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;
import static net.osmand.plus.download.SrtmDownloadItem.getAbbreviationInScopes;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import java.util.List;


public class PluginInstalledBottomSheetDialog extends MenuBottomSheetDialogFragment implements DownloadEvents {

	public static final String PLUGIN_ID_KEY = "plugin_id";

	private static final String TAG = PluginInstalledBottomSheetDialog.class.getName();

	private static final Log LOG = PlatformUtil.getLog(PluginInstalledBottomSheetDialog.class);

	private static final int COLLAPSED_DESCRIPTION_LINES = 7;

	private String pluginId;

	private boolean descriptionExpanded;

	private BottomSheetItemTitleWithDescrAndButton descrItem;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) return;

		if (savedInstanceState != null) {
			pluginId = savedInstanceState.getString(PLUGIN_ID_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				pluginId = args.getString(PLUGIN_ID_KEY);
			}
		}

		OsmandPlugin plugin = PluginsHelper.getPlugin(pluginId);
		if (plugin == null) return;

		BaseBottomSheetItem titleItem = new TitleItem.Builder()
				.setTitle(getString(R.string.new_plugin_added))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create();
		items.add(titleItem);

		SpannableString pluginTitleSpan = new SpannableString(plugin.getName());
		pluginTitleSpan.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), 0, pluginTitleSpan.length(), 0);
		Drawable pluginIcon = plugin.getLogoResource();
		if (pluginIcon.getConstantState() != null) {
			pluginIcon = pluginIcon.getConstantState().newDrawable().mutate();
		}
		pluginIcon = UiUtilities.tintDrawable(pluginIcon, ColorUtilities.getDefaultIconColor(app, nightMode));

		BaseBottomSheetItem pluginTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(pluginTitleSpan)
				.setTitleColorId(ColorUtilities.getActiveColorId(nightMode))
				.setIcon(pluginIcon)
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.create();
		items.add(pluginTitle);

		descrItem = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonTitle(getString(R.string.show_full_description))
				.setOnButtonClickListener(v -> {
					descriptionExpanded = !descriptionExpanded;
					descrItem.setButtonText(getString(descriptionExpanded
							? R.string.hide_full_description : R.string.show_full_description));
					descrItem.setDescriptionMaxLines(descriptionExpanded
							? Integer.MAX_VALUE : COLLAPSED_DESCRIPTION_LINES);
					setupHeightAndBackground(getView());
				})
				.setDescriptionLinksClickable(true)
				.setDescription(plugin.getDescription(true))
				.setDescriptionMaxLines(COLLAPSED_DESCRIPTION_LINES)
				.setLayoutId(R.layout.bottom_sheet_item_with_expandable_descr)
				.create();
		items.add(descrItem);

		List<ApplicationMode> addedAppModes = plugin.getAddedAppModes();
		if (!addedAppModes.isEmpty()) {
			createAddedAppModesItems(addedAppModes);
		}

		List<IndexItem> suggestedMaps = plugin.getSuggestedMaps();
		if (!suggestedMaps.isEmpty()) {
			createSuggestedMapsItems(suggestedMaps);
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		updateMenuItems();
	}

	@Override
	public void downloadInProgress() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
		if (downloadIndexItem != null) {
			for (BaseBottomSheetItem item : items) {
				if (item instanceof BottomSheetItemWithDescription) {
					Object tag = item.getTag();
					if (tag instanceof IndexItem indexItem) {
						BottomSheetItemWithDescription mapItem = (BottomSheetItemWithDescription) item;

						ProgressBar progressBar = mapItem.getView().findViewById(R.id.ProgressBar);
						if (downloadIndexItem.equals(indexItem)) {
							progressBar.setProgress((int) downloadThread.getCurrentDownloadProgress());
							progressBar.setIndeterminate(false);
						} else if (indexItem.isDownloaded()) {
							AndroidUiHelper.updateVisibility(progressBar, false);
						}
					}
				}
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		updateMenuItems();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_turn_off;
	}

	@Override
	protected void onDismissButtonClickAction() {
		OsmandPlugin plugin = PluginsHelper.getPlugin(pluginId);
		if (plugin != null) {
			Activity activity = getActivity();
			PluginsHelper.enablePlugin(activity, app, plugin, false);

			for (Fragment fragment : getParentFragmentManager().getFragments()) {
				if (fragment instanceof PluginStateListener) {
					((PluginStateListener) fragment).onPluginStateChanged(plugin);
				}
			}
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_ok;
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PLUGIN_ID_KEY, pluginId);
	}

	private void createAddedAppModesItems(List<ApplicationMode> addedAppModes) {
		items.add(new DividerItem(getContext()));

		View categoryView = inflate(R.layout.bottom_sheet_item_with_descr_56dp);
		categoryView.findViewById(R.id.icon).setVisibility(View.GONE);

		BaseBottomSheetItem addedAppProfiles = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.added_profiles_descr))
				.setTitle(getString(R.string.added_profiles))
				.setCustomView(categoryView)
				.create();
		items.add(addedAppProfiles);

		for (ApplicationMode mode : addedAppModes) {
			BottomSheetItemWithCompoundButton[] appModeItem = new BottomSheetItemWithCompoundButton[1];
			appModeItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(ApplicationMode.values(app).contains(mode))
					.setDescription(ProfileDataUtils.getAppModeDescription(app, mode))
					.setTitle(mode.toHumanString())
					.setIcon(getActiveIcon(mode.getIconRes()))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
					.setOnClickListener(v -> {
						boolean checked = !appModeItem[0].isChecked();
						appModeItem[0].setChecked(checked);
						ApplicationMode.changeProfileAvailability(mode, checked, app);
					})
					.create();
			items.add(appModeItem[0]);
		}
	}

	private void createSuggestedMapsItems(@NonNull List<IndexItem> suggestedMaps) {
		Context themedCtx = getThemedContext();
		items.add(new DividerItem(themedCtx));

		View categoryView = inflate(R.layout.bottom_sheet_item_with_descr_56dp);
		categoryView.findViewById(R.id.icon).setVisibility(View.GONE);

		BaseBottomSheetItem addedAppProfiles = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.suggested_maps_descr))
				.setTitle(getString(R.string.suggested_maps))
				.setCustomView(categoryView)
				.create();
		items.add(addedAppProfiles);

		DownloadIndexesThread downloadThread = app.getDownloadThread();

		for (IndexItem indexItem : suggestedMaps) {
			View view = inflate(R.layout.list_item_icon_and_download);
			AndroidUtils.setBackground(view, UiUtilities.getSelectableDrawable(themedCtx));

			ImageView secondaryIcon = view.findViewById(R.id.secondary_icon);
			ProgressBar progressBar = view.findViewById(R.id.ProgressBar);

			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(progressBar, downloadThread.isDownloading(indexItem));

			if (indexItem == downloadThread.getCurrentDownloadingItem()) {
				progressBar.setProgress((int) downloadThread.getCurrentDownloadProgress());
				progressBar.setIndeterminate(false);
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
			} else {
				progressBar.setIndeterminate(downloadThread.isDownloading());
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_import));
			}

			DownloadActivityType type = indexItem.getType();
			StringBuilder builder = new StringBuilder(type.getString(app));
			if (type == SRTM_COUNTRY_FILE) {
				builder.append(" ").append(getAbbreviationInScopes(app, indexItem));
			}
			builder.append(" • ").append(indexItem.getSizeDescription(app));

			BaseBottomSheetItem mapIndexItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(builder.toString())
					.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
					.setIcon(getContentIcon(type.getIconResource()))
					.setOnClickListener(v -> {
						if (downloadThread.isDownloading(indexItem)) {
							downloadThread.cancelDownload(indexItem);
							AndroidUiHelper.updateVisibility(progressBar, false);
							secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_import));
						} else {
							AndroidUiHelper.updateVisibility(progressBar, true);
							progressBar.setIndeterminate(downloadThread.isDownloading());
							secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
							callActivity(activity -> new DownloadValidationManager(app).startDownload(activity, indexItem));
						}
					})
					.setTag(indexItem)
					.setCustomView(view)
					.create();
			items.add(mapIndexItem);
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, String pluginId, Boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(PLUGIN_ID_KEY, pluginId);

			PluginInstalledBottomSheetDialog dialog = new PluginInstalledBottomSheetDialog();
			dialog.setArguments(args);
			dialog.setUsedOnMap(usedOnMap);
			dialog.show(fm, TAG);
		}
	}

	public interface PluginStateListener {
		default void onPluginStateChanged(@NonNull OsmandPlugin plugin) {
		}

		default void onPluginInstalled(@NonNull OsmandPlugin plugin) {
		}
	}
}