package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;
import net.osmand.plus.voice.JsTtsCommandPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssetsCollection {

	private final String basePath;
	private final Map<String, AssetEntry> map = new LinkedHashMap<>();

	public AssetsCollection(@NonNull OsmandApplication app, @NonNull List<AssetEntry> assets) {
		assets.forEach(asset -> map.put(asset.destination, asset));
		this.basePath = app.getAppPath(null).getAbsolutePath() + "/";
	}

	@NonNull
	public Collection<AssetEntry> getEntries() {
		return map.values();
	}

	public boolean isFileDerivedFromAssets(@NonNull File file) {
		return getAssetEntry(file) != null;
	}

	@Nullable
	public Long getVersionTime(@NonNull File file) {
		AssetEntry assetEntry = getAssetEntry(file);
		return assetEntry != null ? assetEntry.getVersionTime() : null;
	}

	@NonNull
	public List<AssetEntry> getFilteredEntries(@NonNull CallbackWithObject<AssetEntry> condition) {
		List<AssetEntry> result = new ArrayList<>();
		for (AssetEntry entry : getEntries()) {
			if (condition.processResult(entry)) {
				result.add(entry);
			}
		}
		return result;
	}

	@Nullable
	private AssetEntry getAssetEntry(@NonNull File file) {
		String path = getRelativePath(file);
		AssetEntry entry = map.get(path);

		if (entry == null && JsTtsCommandPlayer.isMyData(file)) {
			File langFile = JsTtsCommandPlayer.getLangFile(file);
			if (langFile != null) {
				path = getRelativePath(langFile);
				entry = map.get(path);
			}
		}
		return entry;
	}

	@NonNull
	private String getRelativePath(@NonNull File file) {
		String path = file.getAbsolutePath();
		return path.replace(basePath, "");
	}
}
