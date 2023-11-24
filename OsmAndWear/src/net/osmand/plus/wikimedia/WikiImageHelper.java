package net.osmand.plus.wikimedia;

import static net.osmand.wiki.WikiCoreHelper.WIKIMEDIA_CATEGORY;
import static net.osmand.wiki.WikiCoreHelper.WIKIMEDIA_FILE;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardType;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.wiki.WikiImage;
import net.osmand.wiki.WikiCoreHelper;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class WikiImageHelper {
	private static final String WIKIDATA_PREFIX = "Q";

	private static final Log LOG = PlatformUtil.getLog(WikiImageHelper.class);

	public static void addWikidataImageCards(@NonNull MapActivity mapActivity, @NonNull String wikidataId,
	                                         @NonNull ImageCardsHolder holder) {

		if (wikidataId.startsWith(WIKIDATA_PREFIX)) {
			List<WikiImage> wikiImageList = WikiCoreHelper.getWikidataImageList(wikidataId);
			for (WikiImage wikiImage : wikiImageList) {
				addImageCard(mapActivity, holder, ImageCardType.WIKIDATA, wikiImage);
			}
		} else {
			LOG.error("Wrong Wikidata ID");
		}
	}

	public static void addWikimediaImageCards(@NonNull MapActivity mapActivity, @NonNull String wikiMediaTagContent,
	                                          @NonNull ImageCardsHolder holder) {
		if (wikiMediaTagContent.startsWith(WIKIMEDIA_FILE) || wikiMediaTagContent.startsWith(WIKIMEDIA_CATEGORY)) {
			List<WikiImage> wikiImages = new ArrayList<>();
			List<WikiImage> wikimediaImageList = WikiCoreHelper.getWikimediaImageList(wikiMediaTagContent, wikiImages);
			for (WikiImage wikiImage : wikimediaImageList) {
				addImageCard(mapActivity, holder, ImageCardType.WIKIMEDIA, wikiImage);
			}
		} else {
			LOG.error("Wrong Wikimedia category member");
		}
	}

	private static void addImageCard(@NonNull MapActivity mapActivity,
	                                 @NonNull ImageCardsHolder holder,
	                                 @NonNull ImageCardType type,
	                                 @NonNull WikiImage wikiImage) {
		holder.add(type, new WikiImageCard(mapActivity, wikiImage));
	}
}
