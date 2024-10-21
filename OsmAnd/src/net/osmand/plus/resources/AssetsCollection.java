package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetEntry;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssetsCollection {

	private final Map<String, AssetEntry> map = new LinkedHashMap<>();
	private final String basePath;

	public AssetsCollection(@NonNull OsmandApplication app, @NonNull List<AssetEntry> assets) {
		assets.forEach(asset -> map.put(asset.destination, asset));
		this.basePath = app.getAppPath(null).getAbsolutePath();
	}

	@NonNull
	public Collection<AssetEntry> getEntrys() {
		return map.values();
	}

	@Nullable
	public Long getVersionTime(@NonNull File file) {
		String destination = getRelativePath(file);
		return getVersionTime(destination);
	}

	@Nullable
	public Long getVersionTime(@NonNull String destination) {
		AssetEntry assetEntry = map.get(destination);
		return assetEntry != null ? assetEntry.getVersionTime() : null;
	}

	@NonNull
	private String getRelativePath(@NonNull File file) {
		String path = file.getAbsolutePath();
		return path.startsWith(basePath) ? path.substring(basePath.length() + 1) : path;
	}
}
