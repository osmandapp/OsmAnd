package net.osmand.plus.wikipedia;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiImage;

public class WikiImageCard extends ImageCard {

	private final WikiImage wikiImage;

	private boolean metadataDownloaded;

	public WikiImageCard(@NonNull MapActivity mapActivity, @NonNull WikiImage wikiImage) {
		super(mapActivity, null);
		this.wikiImage = wikiImage;
		if (topIconId == 0) {
			topIconId = R.drawable.ic_logo_wikimedia;
		}

		this.imageUrl = wikiImage.getImageStubUrl();
		this.title = wikiImage.getImageName();
		this.url = this.imageUrl;
		this.imageHiresUrl = wikiImage.getImageHiResUrl();

		View.OnClickListener listener = v -> openUrl(mapActivity, app, getTitle(),
				wikiImage.getUrlWithCommonAttributions(), false, false);

		if (!Algorithms.isEmpty(buttonText)) {
			this.onButtonClickListener = listener;
		} else {
			this.onClickListener = listener;
		}
	}

	@NonNull
	public WikiImage getWikiImage() {
		return wikiImage;
	}

	public boolean isMetaDataDownloaded() {
		return metadataDownloaded;
	}

	public void setMetaDataDownloaded(boolean metadataDownloaded) {
		this.metadataDownloaded = metadataDownloaded;
	}
}
