package net.osmand.plus.dialogs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import java.util.List;

import static net.osmand.plus.OsmandPlugin.PLUGIN_ID_KEY;

public class PluginInstalledBottomSheetDialog extends MenuBottomSheetDialogFragment implements DownloadIndexesThread.DownloadEvents {

	public static final String TAG = PluginInstalledBottomSheetDialog.class.getName();

	private static final Log LOG = PlatformUtil.getLog(PluginInstalledBottomSheetDialog.class);

	private static final int COLLAPSED_DESCRIPTION_LINES = 7;

	private String pluginId;

	private boolean descriptionExpanded;

	private BottomSheetItemTitleWithDescrAndButton descrItem;

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
				.setTitle(getString(R.string.new_plugin_added))
				.setLayoutId(R.layout.bottom_sheet_item_title_big)
				.create();
		items.add(titleItem);

		Typeface typeface = FontCache.getRobotoMedium(getContext());
		SpannableString pluginTitleSpan = new SpannableString(plugin.getName());
		pluginTitleSpan.setSpan(new CustomTypefaceSpan(typeface), 0, pluginTitleSpan.length(), 0);
		Drawable pluginIcon = plugin.getLogoResource();
		if (pluginIcon.getConstantState() != null) {
			pluginIcon = pluginIcon.getConstantState().newDrawable().mutate();
		}
		pluginIcon = UiUtilities.tintDrawable(pluginIcon, ContextCompat.getColor(
				context, nightMode ? R.color.icon_color_default_light : R.color.icon_color_default_dark));

		BaseBottomSheetItem pluginTitle = new SimpleBottomSheetItem.Builder()
				.setTitle(pluginTitleSpan)
				.setTitleColorId(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light)
				.setIcon(pluginIcon)
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.create();
		items.add(pluginTitle);

		descrItem = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonTitle(getString(R.string.show_full_description))
				.setOnButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						descriptionExpanded = !descriptionExpanded;
						descrItem.setButtonText(getString(descriptionExpanded
								? R.string.hide_full_description : R.string.show_full_description));
						descrItem.setDescriptionMaxLines(descriptionExpanded
								? Integer.MAX_VALUE : COLLAPSED_DESCRIPTION_LINES);
						setupHeightAndBackground(getView());
					}
				})
				.setDescriptionLinksClickable(true)
				.setDescription(plugin.getDescription())
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
		updateItems();
	}

	@Override
	public void downloadInProgress() {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
		if (downloadIndexItem != null) {
			for (BaseBottomSheetItem item : items) {
				if (item instanceof BottomSheetItemWithDescription) {
					Object tag = item.getTag();
					if (tag instanceof IndexItem) {
						IndexItem indexItem = (IndexItem) tag;
						BottomSheetItemWithDescription mapItem = (BottomSheetItemWithDescription) item;

						ProgressBar progressBar = mapItem.getView().findViewById(R.id.ProgressBar);
						if (downloadIndexItem.equals(indexItem)) {
							progressBar.setProgress(downloadThread.getCurrentDownloadingItemProgress());
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
		updateItems();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_turn_off;
	}

	@Override
	protected void onDismissButtonClickAction() {
		OsmandApplication app = getMyApplication();
		OsmandPlugin plugin = OsmandPlugin.getPlugin(pluginId);
		if (app != null && plugin != null) {
			Activity activity = getActivity();
			OsmandPlugin.enablePlugin(activity, app, plugin, false);

			if (activity instanceof PluginStateListener) {
				((PluginStateListener) activity).onPluginStateChanged(plugin);
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PLUGIN_ID_KEY, pluginId);
	}

	private void createAddedAppModesItems(List<ApplicationMode> addedAppModes) {
		final OsmandApplication app = requiredMyApplication();

		items.add(new DividerItem(getContext()));

		View categoryView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_with_descr_56dp, null);
		categoryView.findViewById(R.id.icon).setVisibility(View.GONE);

		BaseBottomSheetItem addedAppProfiles = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.added_profiles_descr))
				.setTitle(getString(R.string.added_profiles))
				.setCustomView(categoryView)
				.create();
		items.add(addedAppProfiles);

		for (final ApplicationMode mode : addedAppModes) {
			final BottomSheetItemWithCompoundButton[] appModeItem = new BottomSheetItemWithCompoundButton[1];
			appModeItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(ApplicationMode.values(app).contains(mode))
					.setDescription(ProfileDataUtils.getAppModeDescription(app, mode))
					.setTitle(mode.toHumanString())
					.setIcon(getActiveIcon(mode.getIconRes()))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean checked = !appModeItem[0].isChecked();
							appModeItem[0].setChecked(checked);
							ApplicationMode.changeProfileAvailability(mode, checked, app);
						}
					})
					.create();
			items.add(appModeItem[0]);
		}
	}

	private void createSuggestedMapsItems(List<IndexItem> suggestedMaps) {
		final OsmandApplication app = requiredMyApplication();
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);

		items.add(new DividerItem(themedCtx));

		View categoryView = UiUtilities.getInflater(themedCtx, nightMode).inflate(R.layout.bottom_sheet_item_with_descr_56dp, null);
		categoryView.findViewById(R.id.icon).setVisibility(View.GONE);

		BaseBottomSheetItem addedAppProfiles = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.suggested_maps_descr))
				.setTitle(getString(R.string.suggested_maps))
				.setCustomView(categoryView)
				.create();
		items.add(addedAppProfiles);

		final DownloadIndexesThread downloadThread = app.getDownloadThread();

		for (final IndexItem indexItem : suggestedMaps) {
			View view = UiUtilities.getInflater(themedCtx, nightMode).inflate(R.layout.list_item_icon_and_download, null);
			AndroidUtils.setBackground(view, UiUtilities.getSelectableDrawable(themedCtx));

			final ImageView secondaryIcon = view.findViewById(R.id.secondary_icon);
			final ProgressBar progressBar = view.findViewById(R.id.ProgressBar);

			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(progressBar, downloadThread.isDownloading(indexItem));

			if (indexItem == downloadThread.getCurrentDownloadingItem()) {
				progressBar.setProgress(downloadThread.getCurrentDownloadingItemProgress());
				progressBar.setIndeterminate(false);
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
			} else {
				progressBar.setIndeterminate(downloadThread.isDownloading());
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_import));
			}

			BaseBottomSheetItem mapIndexItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(indexItem.getType().getString(app) + " • " + indexItem.getSizeDescription(app))
					.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
					.setIcon(getContentIcon(indexItem.getType().getIconResource()))
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (downloadThread.isDownloading(indexItem)) {
								downloadThread.cancelDownload(indexItem);
								AndroidUiHelper.updateVisibility(progressBar, false);
								secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_import));
							} else {
								AndroidUiHelper.updateVisibility(progressBar, true);
								progressBar.setIndeterminate(downloadThread.isDownloading());
								secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
								new DownloadValidationManager(app).startDownload(getActivity(), indexItem);
							}
						}
					})
					.setTag(indexItem)
					.setCustomView(view)
					.create();
			items.add(mapIndexItem);
		}
	}

	private void updateItems() {
		Activity activity = getActivity();
		Context themedCtx = UiUtilities.getThemedContext(activity, nightMode);
		View mainView = getView();
		if (activity != null && mainView != null) {
			LinearLayout itemsContainer = (LinearLayout) mainView.findViewById(useScrollableItemsContainer()
					? R.id.scrollable_items_container : R.id.non_scrollable_items_container);
			if (itemsContainer != null) {
				itemsContainer.removeAllViews();
			}
			items.clear();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(themedCtx, itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, String pluginId, Boolean usedOnMap) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PLUGIN_ID_KEY, pluginId);

				PluginInstalledBottomSheetDialog dialog = new PluginInstalledBottomSheetDialog();
				dialog.setArguments(args);
				dialog.setUsedOnMap(usedOnMap);
				dialog.show(fm, PluginInstalledBottomSheetDialog.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface PluginStateListener {

		void onPluginStateChanged(OsmandPlugin plugin);

	}
}