package net.osmand.plus.download;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public record ItemsToUpdateCollection(@NonNull List<IndexItem> all,
                                      @NonNull List<IndexItem> activated,
                                      @NonNull List<DownloadItem> groupedAll,
                                      @NonNull List<DownloadItem> groupedActivated) {
	public static ItemsToUpdateCollection emptyInstance() {
		return new ItemsToUpdateCollection(Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}
}
