package net.osmand.plus.resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CachedAssetsVersion {

	private String basePath = null;
	private Map<String, Long> map = new HashMap<>();

	public void setBasePath(@NonNull String basePath) {
		this.basePath = basePath;
	}

	public void putVersion(@NonNull String destination, @Nullable Date version) {
		if (version != null) {
			map.put(destination, version.getTime());
		}
	}

	@Nullable
	public Long getVersionTime(@NonNull File file) {
		String destination = getRelativePath(file);
		return getVersionTime(destination);
	}

	@Nullable
	public Long getVersionTime(@NonNull String destination) {
		return map.get(destination);
	}

	private String getRelativePath(@NonNull File file) {
		String path = file.getAbsolutePath();
		return path.startsWith(basePath) ? path.substring(basePath.length() + 1) : path;
	}

	public void clear() {
		map = new HashMap<>();
	}
}
