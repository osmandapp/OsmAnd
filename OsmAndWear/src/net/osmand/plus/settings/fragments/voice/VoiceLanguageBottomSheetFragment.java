package net.osmand.plus.settings.fragments.voice;

import static net.osmand.IndexConstants.VOICE_PROVIDER_SUFFIX;
import static net.osmand.plus.download.DownloadOsmandIndexesHelper.listLocalRecordedVoiceIndexes;
import static net.osmand.plus.download.DownloadOsmandIndexesHelper.listTtsVoiceIndexes;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.fragments.voice.VoiceItemsAdapter.VoiceItemsListener;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class VoiceLanguageBottomSheetFragment extends BasePreferenceBottomSheet implements DownloadEvents, VoiceItemsListener {

	private static final String TAG = TrackSelectSegmentBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private DownloadIndexesThread downloadThread;
	private DownloadValidationManager validationManager;

	private List<IndexItem> ttsItems;
	private List<IndexItem> recordedItems;

	private VoiceItemsAdapter adapter;
	private VoiceType selectedVoiceType;
	private IndexItem indexToSelectAfterDownload;

	@Override
	protected int getActiveColorId() {
		return ColorUtilities.getActiveColorId(nightMode);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		settings = app.getSettings();
		downloadThread = app.getDownloadThread();
		validationManager = new DownloadValidationManager(app);
		selectedVoiceType = defineSelectedVoiceType();
		loadVoiceItems();
	}

	@NonNull
	private VoiceType defineSelectedVoiceType() {
		ApplicationMode mode = getAppMode();
		String voiceProvider = settings.VOICE_PROVIDER.getModeValue(mode);
		boolean tts = voiceProvider.endsWith(VOICE_PROVIDER_SUFFIX);
		return settings.isVoiceProviderNotSelected(mode) || tts ? VoiceType.TTS : VoiceType.RECORDED;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.recyclerview, null);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());

		adapter = new VoiceItemsAdapter(this, nightMode);
		updateAdapter();

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		recyclerView.setAdapter(adapter);
	}

	private void updateAdapter() {
		adapter.setVoiceType(selectedVoiceType);
		adapter.setVoiceItems(selectedVoiceType == VoiceType.TTS ? ttsItems : recordedItems);
	}

	private void updateVoiceProvider(IndexItem indexItem, boolean forceDismiss) {
		Activity activity = getActivity();
		if (activity != null) {
			ApplicationMode appMode = getAppMode();
			if (settings.isVoiceProviderNotSelected(appMode)) {
				app.getRoutingHelper().getVoiceRouter().setMuteForMode(appMode, false);
			}
			settings.VOICE_PROVIDER.setModeValue(appMode, indexItem.getBasename());
			onVoiceProviderChanged();
			app.initVoiceCommandPlayer(activity, appMode, null, false, false, false, false);
		}
		if (DownloadActivityType.isVoiceTTS(indexItem) || forceDismiss) {
			dismiss();
		}
		indexToSelectAfterDownload = null;
	}

	private void onVoiceProviderChanged() {
		Fragment target = getTargetFragment();
		if (target instanceof OnPreferenceChanged) {
			((OnPreferenceChanged) target).onPreferenceChanged(settings.VOICE_PROVIDER.getId());
		}
	}

	@Override
	public boolean isItemSelected(@NonNull IndexItem item) {
		return Algorithms.stringsEqual(item.getBasename(), settings.VOICE_PROVIDER.getModeValue(getAppMode()));
	}

	@Override
	public void onItemClicked(@NonNull IndexItem item) {
		if (item.isDownloaded()) {
			updateVoiceProvider(item, true);
		} else if (DownloadActivityType.isVoiceTTS(item)) {
			if (!downloadThread.isDownloading(item)) {
				downloadIndexItem(item);
			}
		} else if (downloadThread.isDownloading(item)) {
			downloadThread.cancelDownload(item);
			if (item.equals(indexToSelectAfterDownload)) {
				indexToSelectAfterDownload = null;
			}
		} else {
			downloadIndexItem(item);
		}
	}

	@Override
	public void onVoiceTypeSelected(@NonNull VoiceType voiceType) {
		selectedVoiceType = voiceType;
		updateAdapter();
	}

	private void downloadIndexItem(@NonNull IndexItem item) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			validationManager.startDownload(activity, item);
			indexToSelectAfterDownload = item;
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		loadVoiceItems();
		if (isAdded()) {
			updateAdapter();
		}
	}

	@Override
	public void downloadInProgress() {
		IndexItem item = downloadThread.getCurrentDownloadingItem();
		if (item != null && (DownloadActivityType.isVoiceTTS(item) || DownloadActivityType.isVoiceRec(item))) {
			adapter.updateItem(item);
		}
	}

	@Override
	public void downloadHasFinished() {
		if (indexToSelectAfterDownload != null && indexToSelectAfterDownload.isDownloaded()) {
			updateVoiceProvider(indexToSelectAfterDownload, false);
		}
		adapter.notifyDataSetChanged();
	}

	private void loadVoiceItems() {
		if (Algorithms.isEmpty(ttsItems)) {
			ttsItems = getVoiceList(VoiceType.TTS);
		}
		DownloadResources indexes = downloadThread.getIndexes();
		boolean successfulDownload = indexes.isDownloadedFromInternet && !indexes.downloadFromInternetFailed;
		boolean shouldReloadList = successfulDownload || downloadThread.shouldDownloadIndexes();
		if (Algorithms.isEmpty(recordedItems) || shouldReloadList) {
			recordedItems = getVoiceList(VoiceType.RECORDED);
		}
	}

	@NonNull
	private List<IndexItem> getVoiceList(@NonNull VoiceType voiceType) {
		List<IndexItem> items = getVoiceItemsFromInternet(voiceType);
		addLocalVoiceItems(items, voiceType);
		return items;
	}

	@NonNull
	private List<IndexItem> getVoiceItemsFromInternet(@NonNull VoiceType voiceType) {
		List<IndexItem> items = new ArrayList<>();
		DownloadResources indexes = downloadThread.getIndexes();
		if (!indexes.isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}
		if (indexes.isDownloadedFromInternet && !indexes.downloadFromInternetFailed) {
			List<DownloadItem> itemsForGroup = indexes.getDownloadItemsForGroup(voiceType.indexGroupName);
			for (DownloadItem item : itemsForGroup) {
				if (item instanceof IndexItem) {
					addVoiceItem(items, voiceType, (IndexItem) item);
				}
			}
		}
		return items;
	}

	private void addLocalVoiceItems(@NonNull List<IndexItem> items, @NonNull VoiceType voiceType) {
		List<IndexItem> localItems = voiceType == VoiceType.TTS ? listTtsVoiceIndexes(app) : listLocalRecordedVoiceIndexes(app);
		for (IndexItem item : localItems) {
			boolean contains = false;
			for (DownloadItem suggestedItem : items) {
				if (Algorithms.stringsEqual(item.getFileName(), suggestedItem.getFileName())) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				addVoiceItem(items, voiceType, item);
			}
		}
	}

	private void addVoiceItem(@NonNull List<IndexItem> items, @NonNull VoiceType voiceType, @NonNull IndexItem item) {
		if (voiceType == VoiceType.TTS && DownloadActivityType.isDefaultVoiceTTS(app, item)) {
			items.add(0, item);
		} else {
			items.add(item);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target,
	                                @Nullable ApplicationMode appMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			VoiceLanguageBottomSheetFragment fragment = new VoiceLanguageBottomSheetFragment();
			fragment.setRetainInstance(true);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}