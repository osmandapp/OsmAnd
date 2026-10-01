package net.osmand.plus.download;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record OutdatedIndexesCollection(@NonNull List<IndexItem> all,
                                        @NonNull List<IndexItem> activated,
                                        @NonNull List<DownloadItem> groupedAll,
                                        @NonNull List<DownloadItem> groupedActivated,
                                        @NonNull List<IndexItem> deprecated,
                                        @NonNull Set<String> allFileNames
) {
	public static OutdatedIndexesCollection emptyInstance() {
		return new OutdatedIndexesCollection(Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(),
				Collections.emptySet());
	}
}
