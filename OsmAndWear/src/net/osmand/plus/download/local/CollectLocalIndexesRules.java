package net.osmand.plus.download.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
	private final SizeLimitCondition checkSizeLimitCondition;

	private CollectLocalIndexesRules(@NonNull OsmandApplication app,
	                                 @NonNull Map<File, Boolean> directories,
	                                 @NonNull Set<File> forcedAddUnknownDirectories,
	                                 @Nullable SizeLimitCondition sizeLimitCondition) {
		this.app = app;
		this.directories = directories;
		this.forcedAddUnknownDirectories = forcedAddUnknownDirectories;
		this.checkSizeLimitCondition = sizeLimitCondition;
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

	@Nullable
	public Long getCalculationSizeLimit(@NonNull LocalItem localItem) {
		return checkSizeLimitCondition != null ? checkSizeLimitCondition.execute(localItem) : null;
	}

	public interface SizeLimitCondition {
		@Nullable
		Long execute(@NonNull LocalItem localItem);
	}

	public static class Builder {

		private final OsmandApplication app;
		private final Map<File, Boolean> directories = new LinkedHashMap<>();
		private final Set<File> forcedAddUnknownDirectories = new HashSet<>();
		private SizeLimitCondition checkSizeLimitCondition;

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

		public Builder setSizeLimitCondition(@Nullable SizeLimitCondition checkSizeLimitCondition) {
			this.checkSizeLimitCondition = checkSizeLimitCondition;
			return this;
		}

		public CollectLocalIndexesRules build() {
			return new CollectLocalIndexesRules(app, directories, forcedAddUnknownDirectories, checkSizeLimitCondition);
		}
	}
}