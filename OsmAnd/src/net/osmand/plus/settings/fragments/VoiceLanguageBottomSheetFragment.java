package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.TrackSelectSegmentBottomSheet;
import net.osmand.plus.widgets.MultiStateToggleButton;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.OTHER_GROUP;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_REC;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_TTS;
import static net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet.PREFERENCE_ID;

public class VoiceLanguageBottomSheetFragment extends MenuBottomSheetDialogFragment implements DownloadIndexesThread.DownloadEvents {
	private static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();
	private static final String VOICE_REC = OTHER_GROUP.getDefaultId() + "#" + DownloadResourceGroupType.VOICE_TTS.getDefaultId() + "#" + VOICE_HEADER_TTS.getDefaultId();
	private static final String VOICE_TTS = OTHER_GROUP.getDefaultId() + "#" + DownloadResourceGroupType.VOICE_REC.getDefaultId() + "#" + VOICE_HEADER_REC.getDefaultId();
	private static final int DEFAULT_LANGUAGE_POSITION = 6;
	private static final Log LOG = PlatformUtil.getLog(VoiceLanguageBottomSheetFragment.class);
	protected OsmandSettings settings;
	private OsmandApplication app;
	private Context context;
	private InfoType selectedVoiceType = InfoType.TTS;
	private boolean isTtsDescription;
	private int padding;

	public static void showInstance(@NonNull FragmentManager fm, Fragment targetFragment, String key, boolean usedOnMap) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, key);
				VoiceLanguageBottomSheetFragment fragment = new VoiceLanguageBottomSheetFragment();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	public void newDownloadIndexes() {
		updateItems();
	}

	private void updateItems() {
		Activity activity = getActivity();
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
				item.inflate(context, itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
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
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		context = requireContext();
		settings = app.getSettings();
		padding = getDimen(R.dimen.content_padding_small);

		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.language_description))
				.setDescriptionColorId(nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light)
				.setTitle(getString(R.string.shared_string_language))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);

		items.add(new DividerSpaceItem(context, padding));

		LinearLayout voiceTypeButtons = (LinearLayout) inflater.inflate(R.layout.custom_radio_buttons, null);
		LinearLayout.MarginLayoutParams itemTimeOfDayParams = new LinearLayout.MarginLayoutParams(
				LinearLayout.MarginLayoutParams.MATCH_PARENT, LinearLayout.MarginLayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(itemTimeOfDayParams, padding, 0, padding, 0);
		voiceTypeButtons.setLayoutParams(itemTimeOfDayParams);
		setupTypeRadioGroup(voiceTypeButtons);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(voiceTypeButtons)
				.create()
		);
		createVoiceView();
	}

	private void setupTypeRadioGroup(LinearLayout buttonsContainer) {
		RadioItem tts = createRadioButton(InfoType.TTS, R.string.tts_title);
		RadioItem recorded = createRadioButton(InfoType.RECORDED, R.string.shared_string_recorded);

		MultiStateToggleButton radioGroup = new MultiStateToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(tts, recorded);

		if (selectedVoiceType == InfoType.TTS) {
			radioGroup.setSelectedItem(tts);
		} else {
			radioGroup.setSelectedItem(recorded);
		}
	}

	private RadioItem createRadioButton(final InfoType voiceType, int titleId) {
		String title = app.getString(titleId);
		RadioItem item = new RadioItem(title);
		item.setOnClickListener(new MultiStateToggleButton.OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				selectedVoiceType = voiceType;
				createVoiceView();
				updateItems();
				return true;
			}
		});
		return item;
	}

	private void createVoiceView() {
		List<DownloadItem> voiceItems;
		if (selectedVoiceType == InfoType.TTS) {
			voiceItems = getVoiceList(VOICE_TTS);
			isTtsDescription = true;
		} else {
			voiceItems = getVoiceList(VOICE_REC);
			isTtsDescription = false;
		}
		createSuggestedVoiceItems(voiceItems);
	}

	private void createSuggestedVoiceItems(List<DownloadItem> suggestedMaps) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		final ApplicationMode applicationMode = routingHelper.getAppMode();

		items.add(new DividerSpaceItem(context, padding));
		BaseBottomSheetItem switchStartAndEndItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(isTtsDescription ? R.string.tts_description : R.string.recorded_description))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(switchStartAndEndItem);

		items.add(createDividerItem());

		final DownloadIndexesThread downloadThread = app.getDownloadThread();

		for (final DownloadItem indexItem : suggestedMaps) {
			View view = UiUtilities.getInflater(context, nightMode).inflate(R.layout.list_item_icon_and_download, null);
			AndroidUtils.setBackground(view, UiUtilities.getSelectableDrawable(context));
			view.findViewById(R.id.divider).setVisibility(View.GONE);

			String systemLanguage = Resources.getSystem().getConfiguration().locale.getLanguage();
			DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.DEFAULT);
			boolean isDefault = indexItem.isDownloaded() && indexItem.getBasename().equals(systemLanguage);
			String title = isDefault ? getString(R.string.use_system_language) : indexItem.getVisibleName(app, app.getRegions(), false);
			String dateUpdate = ((IndexItem) indexItem).getDate(df);
			String description = isDefault ? indexItem.getVisibleName(app, app.getRegions(), false) : indexItem.getSizeDescription(app) + " • " + dateUpdate;
			int position = isDefault ? DEFAULT_LANGUAGE_POSITION : -1;

			final ImageView secondaryIcon = view.findViewById(R.id.secondary_icon);
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
			secondaryIcon.setColorFilter(ContextCompat.getColor(context, activeColorResId));

			final ProgressBar progressBar = view.findViewById(R.id.ProgressBar);
			final TextView textDescription = view.findViewById(R.id.description);
			final TextView compoundButton = view.findViewById(R.id.compound_button);

			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(progressBar, downloadThread.isDownloading((IndexItem) indexItem));

			if (indexItem == downloadThread.getCurrentDownloadingItem()) {
				progressBar.setProgress(downloadThread.getCurrentDownloadingItemProgress());
				progressBar.setIndeterminate(false);
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
			} else {
				progressBar.setIndeterminate(downloadThread.isDownloading());
				secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_gsave_dark));
			}

			if (indexItem.isDownloaded()) {
				AndroidUiHelper.updateVisibility(compoundButton, true);
				AndroidUiHelper.updateVisibility(secondaryIcon, false);
			} else {
				AndroidUiHelper.updateVisibility(compoundButton, false);
				AndroidUiHelper.updateVisibility(secondaryIcon, true);
			}

			final BottomSheetItemWithCompoundButton[] voiceDownloadedItem = new BottomSheetItemWithCompoundButton[1];
			voiceDownloadedItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setCompoundButtonColorId(activeColorResId)
					.setChecked(settings.VOICE_PROVIDER.getModeValue(applicationMode).contains(indexItem.getBasename()))
					.setDescription(description)
					.setIconHidden(true)
					.setTitle(title)
					.setPosition(position)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (indexItem.isDownloaded()) {
								boolean checked = !voiceDownloadedItem[0].isChecked();
								voiceDownloadedItem[0].setChecked(checked);
								settings.VOICE_PROVIDER.setModeValue(applicationMode, indexItem.getBasename());
								updateItems();
							} else {
								if (downloadThread.isDownloading((IndexItem) indexItem)) {
									downloadThread.cancelDownload(indexItem);
									AndroidUiHelper.updateVisibility(progressBar, false);
									AndroidUiHelper.updateVisibility(textDescription, true);
									secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_gsave_dark));
								} else {
									AndroidUiHelper.updateVisibility(progressBar, true);
									AndroidUiHelper.updateVisibility(textDescription, false);
									progressBar.setIndeterminate(downloadThread.isDownloading());
									secondaryIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
									new DownloadValidationManager(app).startDownload(getActivity(), (IndexItem) indexItem);
								}
							}
						}
					})
					.setCustomView(view)
					.create();
			items.add(voiceDownloadedItem[0]);
		}
	}

	public int getDimen(@DimenRes int id) {
		return getResources().getDimensionPixelSize(id);
	}

	private BaseBottomSheetItem createDividerItem() {
		DividerItem dividerItem = new DividerItem(app);
		int start = getDimen(R.dimen.content_padding);
		int vertical = getDimen(R.dimen.content_padding_small_half);
		dividerItem.setMargins(start, vertical, 0, vertical);
		return dividerItem;
	}

	public List<DownloadItem> getVoiceList(String type) {
		List<DownloadItem> suggestedVoice = new ArrayList<>();

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		boolean downloadIndexes = settings.isInternetConnectionAvailable()
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		if (!downloadIndexes) {
			suggestedVoice.addAll(getVoiceItems(type));
		}

		return suggestedVoice;
	}

	private List<DownloadItem> getVoiceItems(String type) {
		try {
			return DownloadResources.findIndexItemsAt(app, type);
		} catch (IOException e) {
			LOG.error(e);
		}
		return Collections.emptyList();
	}

	private enum InfoType {
		TTS,
		RECORDED
	}
}
