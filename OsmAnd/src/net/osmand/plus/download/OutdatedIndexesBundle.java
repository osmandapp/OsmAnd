package net.osmand.plus.download;

import androidx.annotation.NonNull;

import java.util.List;

public record OutdatedIndexesBundle(@NonNull List<IndexItem> all,
                                    @NonNull List<IndexItem> activated,
                                    @NonNull List<DownloadItem> allGrouped,
                                    @NonNull List<DownloadItem> activatedGrouped) {
}
