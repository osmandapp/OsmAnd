package net.osmand.plus.wikivoyage.explore.travelcards;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.IndexItem;

import java.util.List;

public class TravelDownloadUpdateCard extends TravelNeededMapsCard {

	public static final int TYPE = 50;

	private final boolean download;

	public TravelDownloadUpdateCard(@NonNull OsmandApplication app, boolean nightMode, @NonNull List<IndexItem> items,
	                                boolean download) {
		super(app, nightMode, items);
		this.download = download;
	}

	public int getTitle() {
		if (isDownloading()) {
			return R.string.shared_string_downloading;
		}
		return download ? R.string.download_file : R.string.update_is_available;
	}

	@Override
	public int getDescription() {
		if (!isInternetAvailable()) {
			return R.string.no_index_file_to_download;
		}
		return download ? R.string.travel_card_download_descr : R.string.travel_card_update_descr;
	}

	@Override
	public int getIconRes() {
		return download ? R.drawable.travel_card_download_icon : R.drawable.travel_card_update_icon;
	}

	@Override
	public int getCardType() {
		return TYPE;
	}
}
