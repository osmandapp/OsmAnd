package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;

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
		this.basePath = app.getAppPath(null).getAbsolutePath();
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
		return map.get(getRelativePath(file));
	}

	@NonNull
	private String getRelativePath(@NonNull File file) {
		String path = file.getAbsolutePath();
		return path.startsWith(basePath) ? path.substring(basePath.length() + 1) : path;
	}
}
