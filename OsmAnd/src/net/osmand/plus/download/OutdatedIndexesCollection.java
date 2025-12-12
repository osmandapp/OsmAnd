package net.osmand.plus.download;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public record OutdatedIndexesCollection(@NonNull List<IndexItem> all,
                                        @NonNull List<IndexItem> activated,
                                        @NonNull List<DownloadItem> groupedAll,
                                        @NonNull List<DownloadItem> groupedActivated,
                                        @NonNull List<IndexItem> deprecated) {
	public static OutdatedIndexesCollection emptyInstance() {
		return new OutdatedIndexesCollection(Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList());
	}
}
