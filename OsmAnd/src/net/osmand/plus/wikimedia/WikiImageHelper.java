package net.osmand.plus.wikimedia;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class WikiImageHelper {
	private static final String WIKIDATA_API_ENDPOINT = "https://www.wikidata.org/w/api.php";
	private static final String WIKIMEDIA_API_ENDPOINT = "https://commons.wikimedia.org/w/api.php";
	private static final String WIKIDATA_ACTION = "?action=wbgetclaims&property=P18&entity=";
	private static final String WIKIMEDIA_ACTION = "?action=query&list=categorymembers&cmtitle=";
	private static final String CM_LIMIT = "&cmlimit=500";
	private static final String FORMAT_JSON = "&format=json";
	private static final String IMAGE_BASE_URL = "https://upload.wikimedia.org/wikipedia/commons/";

	private static final String WIKIDATA_PREFIX = "Q";
	private static final String WIKIMEDIA_FILE = "File:";
	private static final String WIKIMEDIA_CATEGORY = "Category:";

	private static final int THUMB_SIZE = 500;
	private static final Log LOG = PlatformUtil.getLog(WikiImageHelper.class);

	public static void addWikidataImageCards(@NonNull MapActivity mapActivity, @NonNull String wikidataId,
	                                         @NonNull List<ImageCard> imageCards) {
		if (wikidataId.startsWith(WIKIDATA_PREFIX)) {
			String url = WIKIDATA_API_ENDPOINT + WIKIDATA_ACTION + wikidataId + FORMAT_JSON;
			WikidataResponse response = sendWikipediaApiRequest(url, WikidataResponse.class);
			if (response != null && response.claims != null && response.claims.p18 != null) {
				for (P18 p18 : response.claims.p18) {
					String imageFileName = p18.mainsnak.datavalue.value;
					if (imageFileName != null) {
						addImageCard(mapActivity, imageCards, imageFileName);
					}
				}
			}
		} else {
			LOG.error("Wrong Wikidata ID");
		}
	}

	public static void addWikimediaImageCards(@NonNull MapActivity mapActivity, @NonNull String wikiMediaTagContent,
	                                          @NonNull List<ImageCard> imageCards) {
		if (wikiMediaTagContent.startsWith(WIKIMEDIA_FILE)) {
			String fileName = wikiMediaTagContent.replace(WIKIMEDIA_FILE, "");
			addImageCard(mapActivity, imageCards, fileName);
		} else if (wikiMediaTagContent.startsWith(WIKIMEDIA_CATEGORY)) {
			String url = WIKIMEDIA_API_ENDPOINT + WIKIMEDIA_ACTION + wikiMediaTagContent + CM_LIMIT + FORMAT_JSON;
			WikimediaResponse response = sendWikipediaApiRequest(url, WikimediaResponse.class);
			if (response != null) {
				List<String> subCategories = new ArrayList<>();
				for (Categorymember cm : response.query.categorymembers) {
					String memberTitle = cm.title;
					if (memberTitle != null) {
						if (memberTitle.startsWith(WIKIMEDIA_CATEGORY)) {
							subCategories.add(memberTitle);
						} else {
							addWikimediaImageCards(mapActivity, memberTitle, imageCards);
						}
					}
				}
				for (String subCategory : subCategories) {
					addWikimediaImageCards(mapActivity, subCategory, imageCards);
				}
			}
		} else {
			LOG.error("Wrong Wikimedia category member");
		}
	}

	private static <T> T sendWikipediaApiRequest(@NonNull String url, @NonNull Class<T> responseClass) {
		StringBuilder rawResponse = new StringBuilder();
		String errorMessage = NetworkUtils.sendGetRequest(url, null, rawResponse);
		if (errorMessage == null) {
			try {
				return new Gson().fromJson(rawResponse.toString(), responseClass);
			} catch (JsonSyntaxException e) {
				errorMessage = e.getLocalizedMessage();
			}
		}
		LOG.error(errorMessage);
		return null;
	}

	private static void addImageCard(@NonNull MapActivity mapActivity, @NonNull List<ImageCard> images,
	                                 @NonNull String fileName) {
		WikiImage img = getImageData(fileName);
		if (img != null) {
			images.add(new WikiImageCard(mapActivity, img));
		}
	}

	private static WikiImage getImageData(@NonNull String imageFileName) {
		try {
			String imageName = URLDecoder.decode(imageFileName, "UTF-8");
			imageFileName = imageName.replace(" ", "_");
			imageName = imageName.substring(0, imageName.lastIndexOf("."));
			String[] urlHashParts = getHash(imageFileName);

			String imageHiResUrl = IMAGE_BASE_URL +
					urlHashParts[0] + "/" + urlHashParts[1] + "/" +
					imageFileName;

			String imageStubUrl = IMAGE_BASE_URL + "thumb/" +
					urlHashParts[0] + "/" + urlHashParts[1] + "/" +
					imageFileName + "/" + THUMB_SIZE + "px-" +
					imageFileName;

			return new WikiImage(imageFileName, imageName, imageStubUrl, imageHiResUrl);

		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
	}

	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s)));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}

	// Wikidata response classes
	private static class WikidataResponse {
		@SerializedName("claims")
		@Expose
		private Claims claims;
	}

	private static class Claims {
		@SerializedName("P18")
		@Expose
		private List<P18> p18 = null;
	}

	private static class P18 {
		@SerializedName("mainsnak")
		@Expose
		private Mainsnak mainsnak;
	}

	private static class Mainsnak {
		@SerializedName("datavalue")
		@Expose
		private Datavalue datavalue;
		@SerializedName("datatype")
		@Expose
		private String datatype;
	}

	private static class Datavalue {
		@SerializedName("value")
		@Expose
		private String value;
		@SerializedName("type")
		@Expose
		private String type;
	}

	// Wikimedia response classes
	private static class WikimediaResponse {
		@SerializedName("query")
		@Expose
		private Query query;
	}

	private static class Query {
		@SerializedName("categorymembers")
		@Expose
		private List<Categorymember> categorymembers;
	}

	private static class Categorymember {
		@SerializedName("title")
		@Expose
		private String title;
	}
}
