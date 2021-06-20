package net.osmand.plus.download;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.OTHER_GROUP;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_REC;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_HEADER_TTS;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_REC;
import static net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType.VOICE_TTS;

public class VoiceIndexes {

	private final OsmandApplication app;

	private boolean isDownloadedFromInternet = false;
	private boolean downloadFromInternetFailed = false;

	private final List<DownloadItem> voicePromptsTTS = new ArrayList<>();
	private final List<DownloadItem> voicePromptsRec = new ArrayList<>();

	public VoiceIndexes(OsmandApplication app) {
		this.app = app;
	}

	public void listVoicePrompts(@NonNull List<IndexItem> resources) {
		voicePromptsTTS.clear();
		voicePromptsRec.clear();

		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		DateFormat dateFormat = app.getResourceManager().getDateFormat();

		for (IndexItem ii : resources) {
			if (ii.getType() == DownloadActivityType.VOICE_FILE) {
				if (DownloadActivityType.isVoiceTTS(ii)) {
					voicePromptsTTS.add(ii);
					DownloadResources.checkIfItemOutdated(app, indexFileNames, indexFileNames, ii, dateFormat);
				} else if (DownloadActivityType.isVoiceRec(ii)) {
					voicePromptsRec.add(ii);
					DownloadResources.checkIfItemOutdated(app, indexFileNames, indexFileNames, ii, dateFormat);
				}
			}
		}

		Collections.sort(voicePromptsTTS, createComparator());
		Collections.sort(voicePromptsRec, createComparator());
	}

	@NonNull
	public List<DownloadItem> getVoicePrompts(@NonNull VoiceIndexType indexType) {
		DownloadResources indexes = app.getDownloadThread().getIndexes();
		if (indexes.isDownloadedFromInternet) {
			return indexes.getDownloadItemsForGroup(indexType.groupId);
		}
		return indexType == VoiceIndexType.TTS ? voicePromptsTTS : voicePromptsRec;
	}


	public boolean isDownloadedFromInternet() {
		if (app.getDownloadThread().getIndexes().isDownloadedFromInternet) {
			return true;
		} else {
			return isDownloadedFromInternet;
		}
	}

	public boolean downloadFromInternetFailed() {
		DownloadResources indexes = app.getDownloadThread().getIndexes();
		if (indexes.isDownloadedFromInternet && !indexes.downloadFromInternetFailed) {
			return false;
		} else {
			return downloadFromInternetFailed;
		}
	}

	public void setDownloadedFromInternet(boolean downloaded) {
		this.isDownloadedFromInternet = downloaded;
	}

	public void setDownloadFromInternetFailed(boolean failed) {
		this.downloadFromInternetFailed = failed;
	}

	private Comparator<DownloadItem> createComparator() {
		Collator collator = OsmAndCollator.primaryCollator();
		OsmandRegions regions = app.getRegions();
		return (first, second) -> collator.compare(first.getVisibleName(app, regions), second.getVisibleName(app, regions));
	}

	public enum VoiceIndexType {

		TTS(OTHER_GROUP.getDefaultId() + "#" + VOICE_TTS.getDefaultId() + "#" + VOICE_HEADER_TTS.getDefaultId()),
		REC(OTHER_GROUP.getDefaultId() + "#" + VOICE_REC.getDefaultId() + "#" + VOICE_HEADER_REC.getDefaultId());

		private String groupId;

		VoiceIndexType(String groupId) {
			this.groupId = groupId;
		}
	}
}