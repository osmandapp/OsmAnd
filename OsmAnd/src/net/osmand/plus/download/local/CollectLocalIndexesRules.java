package net.osmand.plus.download.local;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CollectLocalIndexesRules {

	private final OsmandApplication app;
	private final Map<File, Boolean> directories;
	private final Set<File> forcedAddUnknownDirectories;
	private final Set<LocalItemType> typesToCalculateSizeSeparately;

	private CollectLocalIndexesRules(@NonNull OsmandApplication app,
	                                 @NonNull Map<File, Boolean> directories,
	                                 @NonNull Set<File> forcedAddUnknownDirectories,
	                                 @NonNull Set<LocalItemType> typesToCalculateSizeSeparately) {
		this.app = app;
		this.directories = directories;
		this.forcedAddUnknownDirectories = forcedAddUnknownDirectories;
		this.typesToCalculateSizeSeparately = typesToCalculateSizeSeparately;
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public Collection<File> getDirectories() {
		return directories.keySet();
	}

	public boolean shouldAddUnknown(@NonNull File directory) {
		Boolean addUnknown = directories.get(directory);
		return addUnknown != null ? addUnknown : shouldAddUnknown(directory, false);
	}

	public boolean shouldAddUnknown(@NonNull File directory, boolean addUnknown) {
		if (!addUnknown && !Algorithms.isEmpty(forcedAddUnknownDirectories)) {
			return forcedAddUnknownDirectories.contains(directory);
		}
		return addUnknown;
	}

	public boolean shouldCalculateSizeSeparately(@NonNull LocalItemType type) {
		return typesToCalculateSizeSeparately.contains(type);
	}

	public static class Builder {

		private final OsmandApplication app;
		private final Map<File, Boolean> directories = new LinkedHashMap<>();
		private final Set<File> forcedAddUnknownDirectories = new HashSet<>();
		private final Set<LocalItemType> typesToCalculateSizeSeparately = new HashSet<>();

		public Builder(@NonNull OsmandApplication app) {
			this.app = app;
		}

		public Builder addDirectoryIfNotPresent(@NonNull File directory, boolean addUnknown) {
			if (!directories.containsKey(directory)) {
				directories.put(directory, addUnknown);
			}
			return this;
		}

		public Builder addForcedAddUnknownDirectory(@NonNull File directory) {
			forcedAddUnknownDirectories.add(directory);
			return this;
		}

		public Builder addTypeToCalculateSizeSeparately(@NonNull LocalItemType type) {
			typesToCalculateSizeSeparately.add(type);
			return this;
		}

		public CollectLocalIndexesRules build() {
			return new CollectLocalIndexesRules(app, directories, forcedAddUnknownDirectories,
					typesToCalculateSizeSeparately);
		}
	}
}
