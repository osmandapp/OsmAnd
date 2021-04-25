package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
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
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.TrackSelectSegmentBottomSheet;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.OTHER_GROUP;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_REC;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_TTS;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_REC;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_TTS;
import static net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet.PREFERENCE_ID;

public class VoiceLanguageBottomSheetFragment extends MenuBottomSheetDialogFragment implements DownloadIndexesThread.DownloadEvents {

	private static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(VoiceLanguageBottomSheetFragment.class);

	private static final String VOICE_REC_KEY = OTHER_GROUP.getDefaultId() + "#" + VOICE_REC.getDefaultId() + "#" + VOICE_HEADER_REC.getDefaultId();
	private static final String VOICE_TTS_KEY = OTHER_GROUP.getDefaultId() + "#" + VOICE_TTS.getDefaultId() + "#" + VOICE_HEADER_TTS.getDefaultId();

	private static final int DEFAULT_LANGUAGE_POSITION = 6;

	private OsmandApplication app;
	private OsmandSettings settings;
	private DownloadIndexesThread downloadThread;
	List<DownloadItem> voiceItems;
	List<DownloadItem> voiceItemsRec;

	private InfoType selectedVoiceType = InfoType.TTS;

	public static void showInstance(@NonNull FragmentManager fm, Fragment targetFragment, String key, boolean usedOnMap) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, key);
				VoiceLanguageBottomSheetFragment fragment = new VoiceLanguageBottomSheetFragment();
				fragment.setRetainInstance(true);
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		downloadThread = app.getDownloadThread();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		settings = app.getSettings();
		int padding = getDimen(R.dimen.content_padding_small);

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

		items.add(new DividerSpaceItem(context, padding));
		BaseBottomSheetItem switchStartAndEndItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(selectedVoiceType == InfoType.TTS ? R.string.tts_description : R.string.recorded_description))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(switchStartAndEndItem);

		items.add(createDividerItem());

		createVoiceView();
	}

	@Override
	public void newDownloadIndexes() {
		updateItems();
	}

	private void updateItems() {
		Activity activity = getActivity();
		View mainView = getView();
		if (activity != null && mainView != null) {
			Context context = requireContext();
			LinearLayout itemsContainer = mainView.findViewById(useScrollableItemsContainer()
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

	private void setupTypeRadioGroup(LinearLayout buttonsContainer) {
		TextRadioItem tts = createRadioButton(InfoType.TTS, R.string.tts_title);
		TextRadioItem recorded = createRadioButton(InfoType.RECORDED, R.string.shared_string_recorded);

		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(tts, recorded);

		if (selectedVoiceType == InfoType.TTS) {
			radioGroup.setSelectedItem(tts);
		} else {
			radioGroup.setSelectedItem(recorded);
		}
	}

	private TextRadioItem createRadioButton(final InfoType voiceType, int titleId) {
		String title = app.getString(titleId);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener(new OnRadioItemClickListener() {
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
		if (selectedVoiceType == InfoType.TTS && voiceItems == null || voiceItems.isEmpty()) {
			voiceItems = getVoiceList(VOICE_TTS_KEY);
		} else if (selectedVoiceType == InfoType.RECORDED && voiceItemsRec == null) {
			voiceItemsRec = getVoiceList(VOICE_REC_KEY);
		}
		createSuggestedVoiceItemsView(selectedVoiceType == InfoType.TTS ? voiceItems : voiceItemsRec);
	}

	private void createSuggestedVoiceItemsView(List<DownloadItem> suggestedMaps) {
		final Context context = requireContext();
		final OsmandSettings settings = app.getSettings();
		final ApplicationMode applicationMode = settings.getApplicationMode();
		final LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);

		for (final DownloadItem indexItem : suggestedMaps) {
			View container = inflater.inflate(R.layout.list_item_icon_and_download, null);
			AndroidUtils.setBackground(container, UiUtilities.getSelectableDrawable(context));
			container.findViewById(R.id.divider).setVisibility(View.GONE);

			String systemLanguage = app.getLanguage();
			DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.DEFAULT);

			String indexName = indexItem.getBasename();
			String indexFormattedName = indexName.replaceAll("-tts", "");
			boolean isDefault = indexItem.isDownloaded() && indexFormattedName.equals(systemLanguage) && indexItem.getRelatedGroup().getType().equals(VOICE_HEADER_TTS);
			String title = isDefault ? getString(R.string.use_system_language) : indexItem.getVisibleName(app, app.getRegions(), false);
			String dateUpdate = ((IndexItem) indexItem).getDate(df);
			String description = isDefault ? indexItem.getVisibleName(app, app.getRegions(), false) : indexItem.getSizeDescription(app) + " • " + dateUpdate;
			int position = isDefault ? DEFAULT_LANGUAGE_POSITION : -1;

			final ImageView secondaryIcon = container.findViewById(R.id.secondary_icon);
			int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;

			@SuppressWarnings("ConstantConditions")
			Drawable downloadIcon =  getContentIcon(R.drawable.ic_action_gsave_dark).getConstantState().newDrawable();
			
			@SuppressWarnings("ConstantConditions")
			Drawable undoDownloadIcon =  getContentIcon(R.drawable.ic_action_remove_dark).getConstantState().newDrawable();

			final Drawable tintedDownloadIcon = UiUtilities.tintDrawable(downloadIcon, ContextCompat.getColor(
					context, activeColorResId));
			final Drawable tintedUndoDownloadIcon = UiUtilities.tintDrawable(undoDownloadIcon, ContextCompat.getColor(
					context, activeColorResId));

			final ProgressBar progressBar = container.findViewById(R.id.ProgressBar);
			final TextView textDescription = container.findViewById(R.id.description);
			RadioButton radioButton = container.findViewById(R.id.compound_button);
			UiUtilities.setupCompoundButton(radioButton, nightMode, PROFILE_DEPENDENT);

			AndroidUiHelper.updateVisibility(secondaryIcon, true);
			AndroidUiHelper.updateVisibility(progressBar, downloadThread.isDownloading((IndexItem) indexItem));

			if (indexItem == downloadThread.getCurrentDownloadingItem()) {
				progressBar.setProgress(downloadThread.getCurrentDownloadingItemProgress());
				progressBar.setIndeterminate(false);
				secondaryIcon.setImageDrawable(tintedUndoDownloadIcon);
			} else {
				progressBar.setIndeterminate(downloadThread.isDownloading());
				secondaryIcon.setImageDrawable(tintedDownloadIcon);
			}

			if (indexItem.isDownloaded()) {
				AndroidUiHelper.updateVisibility(radioButton, true);
				AndroidUiHelper.updateVisibility(secondaryIcon, false);
			} else {
				AndroidUiHelper.updateVisibility(radioButton, false);
				AndroidUiHelper.updateVisibility(secondaryIcon, true);
			}
			boolean selected = indexItem.getBasename().equals(settings.VOICE_PROVIDER.getModeValue(applicationMode));
			final BottomSheetItemWithCompoundButton[] voiceDownloadedItem = new BottomSheetItemWithCompoundButton[1];
			voiceDownloadedItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setCompoundButtonColorId(activeColorResId)
					.setChecked(selected)
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
								dismiss();
							} else {
								if (downloadThread.isDownloading((IndexItem) indexItem)) {
									downloadThread.cancelDownload(indexItem);
									AndroidUiHelper.updateVisibility(progressBar, false);
									AndroidUiHelper.updateVisibility(textDescription, true);
									secondaryIcon.setImageDrawable(tintedDownloadIcon);
								} else {
									AndroidUiHelper.updateVisibility(progressBar, true);
									AndroidUiHelper.updateVisibility(textDescription, false);
									progressBar.setIndeterminate(downloadThread.isDownloading());
									secondaryIcon.setImageDrawable(tintedUndoDownloadIcon);
									new DownloadValidationManager(app).startDownload(getActivity(), (IndexItem) indexItem);
								}
							}
						}
					})
					.setCustomView(container)
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
		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		boolean downloadIndexes = settings.isInternetConnectionAvailable()
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		List<DownloadItem> suggestedVoice = new ArrayList<>();
		if (!downloadIndexes) {
			suggestedVoice.addAll(downloadThread.getIndexes().getDownloadItemsForGroup(type));
		}

		return suggestedVoice;
	}

	private enum InfoType {
		TTS,
		RECORDED
	}
}
