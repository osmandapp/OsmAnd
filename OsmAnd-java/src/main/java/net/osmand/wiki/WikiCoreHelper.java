package net.osmand.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class WikiCoreHelper {

	public static final boolean USE_OSMAND_WIKI_API = true;
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
	public static final String OSMAND_API_ENDPOINT = "https://osmand.net/api/";
	private static final String OSMAND_API_WIKIDATA_ARTICLE_ACTION = "wiki_place?article=";
	private static final String OSMAND_API_WIKIDATA_CATEGORY_ACTION = "wiki_place?category=";

	public static List<WikiImage> getWikimediaImageList(String wikiMediaTagContent, List<WikiImage> wikiImages) {

		if (wikiMediaTagContent.startsWith(WIKIMEDIA_FILE)) {
			String imageFileName = wikiMediaTagContent.replace(WIKIMEDIA_FILE, "");
			WikiImage wikiImage = getImageData(imageFileName);
			if (wikiImage != null) {
				wikiImages.add(wikiImage);
			}
		} else if (wikiMediaTagContent.startsWith(WIKIMEDIA_CATEGORY)) {
			if (USE_OSMAND_WIKI_API) {
				String categoryName = wikiMediaTagContent.replace(WIKIMEDIA_CATEGORY, "");
				wikiImages.addAll(getOsmandAPIWikidataImageListByCategory(categoryName));
			} else {
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
			}
		} else {
			LOG.error("Wrong Wikimedia category member");
		}
		return wikiImages;
	}

	public static List<WikiImage> getOsmandAPIWikidataImageListByCategory(String categoryName) {
		String url = OSMAND_API_ENDPOINT + OSMAND_API_WIKIDATA_CATEGORY_ACTION + categoryName;
		return getImagesOsmandAPIRequest(url);
	}

	public static List<WikiImage> getOsmandAPIWikidataImageList(String wikidataId) {
		String url = OSMAND_API_ENDPOINT + OSMAND_API_WIKIDATA_ARTICLE_ACTION + wikidataId;
		return getImagesOsmandAPIRequest(url);
	}

	@SuppressWarnings("ConstantConditions")
	private static List<WikiImage> getImagesOsmandAPIRequest(String url) {
		List<WikiImage> wikiImages = new ArrayList<>();
		OsmandAPIResponse response = sendWikipediaApiRequest(url, OsmandAPIResponse.class);
		if (response != null && !Algorithms.isEmpty(response.images)) {
			for (String imageUrl : response.images) {
				if (imageUrl != null) {
					WikiImage wikiImage = getOsmandApiImageData(imageUrl);
					if (wikiImage != null) {
						wikiImages.add(wikiImage);
					}
				}
			}
		}
		return wikiImages;
	}

	@SuppressWarnings("ConstantConditions")
	public static List<WikiImage> getWikidataImageList(String wikidataId) {
		List<WikiImage> wikiImages = new ArrayList<>();
		if (USE_OSMAND_WIKI_API) {
			wikiImages.addAll(getOsmandAPIWikidataImageList(wikidataId));
		} else {
			String url = WIKIDATA_API_ENDPOINT + WIKIDATA_ACTION + wikidataId + FORMAT_JSON;
			WikidataResponse response = sendWikipediaApiRequest(url, WikidataResponse.class);
			if (response != null && response.claims != null && response.claims.p18 != null) {
				for (P18 p18 : response.claims.p18) {
					String imageFileName = p18.mainsnak.datavalue.value;
					if (imageFileName != null) {
						WikiImage wikiImage = getImageData(imageFileName);
						if (wikiImage != null) {
							wikiImages.add(wikiImage);
						}
					}
				}
			}
		}
		return wikiImages;
	}

	public static WikiImage getOsmandApiImageData(String imageUrl) {
		try {
			imageUrl = URLDecoder.decode(imageUrl, "UTF-8");
			String imageHiResUrl = imageUrl.replace(" ", "_");
			String imageFileName = Algorithms.getFileWithoutDirs(imageUrl);
			String imageName = Algorithms.getFileNameWithoutExtension(imageUrl);
			String imageStubUrl = imageHiResUrl + "?width=" + THUMB_SIZE;
			return new WikiImage(imageFileName, imageName, imageStubUrl, imageHiResUrl);
		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
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

	public static class OsmandAPIResponse {
		@SerializedName("features")
		@Expose
		private final List<String> images = null;
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
