package net.osmand.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class WikiCoreHelper {
	private static final Log LOG = PlatformUtil.getLog(WikiCoreHelper.class);
	private static final String WIKIMEDIA_API_ENDPOINT = "https://commons.wikimedia.org/w/api.php";
	private static final String WIKIDATA_API_ENDPOINT = "https://www.wikidata.org/w/api.php";
	private static final String WIKIDATA_ACTION = "?action=wbgetclaims&property=P18&entity=";
	private static final String WIKIMEDIA_ACTION = "?action=query&list=categorymembers&cmtitle=";
	private static final String CM_LIMIT = "&cmlimit=500";
	private static final String FORMAT_JSON = "&format=json";
	private static final String IMAGE_BASE_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/";
	public static final String WIKIMEDIA_FILE = "File:";
	public static final String WIKIMEDIA_CATEGORY = "Category:";
	private static final int THUMB_SIZE = 500;

	public static List<WikiImage> getWikimediaImageList(String wikiMediaTagContent, List<WikiImage> wikiImages) {

		if (wikiMediaTagContent.startsWith(WIKIMEDIA_FILE)) {
			String imageFileName = wikiMediaTagContent.replace(WIKIMEDIA_FILE, "");
			wikiImages.add(getImageData(imageFileName));
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
							getWikimediaImageList(memberTitle, wikiImages);
						}
					}
				}
				for (String subCategory : subCategories) {
					getWikimediaImageList(subCategory, wikiImages);
				}
			}
		} else {
			LOG.error("Wrong Wikimedia category member");
		}
		return wikiImages;
	}

	public static List<WikiImage> getWikidataImageList(String wikidataId) {
		List<WikiImage> wikiImages = new ArrayList<>();
		String url = WIKIDATA_API_ENDPOINT + WIKIDATA_ACTION + wikidataId + FORMAT_JSON;
		WikidataResponse response = sendWikipediaApiRequest(url, WikidataResponse.class);
		if (response != null && response.claims != null && response.claims.p18 != null) {
			for (P18 p18 : response.claims.p18) {
				String imageFileName = p18.mainsnak.datavalue.value;
				if (imageFileName != null) {
					wikiImages.add(getImageData(imageFileName));
				}
			}
		}
		return wikiImages;
	}

	public static WikiImage getImageData(String imageFileName) {
		try {
			String imageName = URLDecoder.decode(imageFileName, "UTF-8");
			imageFileName = imageName.replace(" ", "_");
			imageName = imageName.substring(0, imageName.lastIndexOf("."));

			String imageHiResUrl = IMAGE_BASE_URL + imageFileName;

			String imageStubUrl = IMAGE_BASE_URL + imageFileName + "?width=" + THUMB_SIZE;

			return new WikiImage(imageFileName, imageName, imageStubUrl, imageHiResUrl);

		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
	}

	private static <T> T sendWikipediaApiRequest(String url, Class<T> responseClass) {
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

	// Wikidata response classes
	public static class WikidataResponse {
		@SerializedName("claims")
		@Expose
		private Claims claims;
	}

	public static class Claims {
		@SerializedName("P18")
		@Expose
		private final List<P18> p18 = null;
	}

	public static class P18 {
		@SerializedName("mainsnak")
		@Expose
		private Mainsnak mainsnak;
	}

	public static class Mainsnak {
		@SerializedName("datavalue")
		@Expose
		private Datavalue datavalue;
		@SerializedName("datatype")
		@Expose
		private String datatype;
	}

	public static class Datavalue {
		@SerializedName("value")
		@Expose
		private String value;
		@SerializedName("type")
		@Expose
		private String type;
	}

	// Wikimedia response classes
	public static class WikimediaResponse {
		@SerializedName("query")
		@Expose
		private Query query;
	}

	public static class Query {
		@SerializedName("categorymembers")
		@Expose
		private List<Categorymember> categorymembers;
	}

	public static class Categorymember {
		@SerializedName("title")
		@Expose
		private String title;
	}
}
