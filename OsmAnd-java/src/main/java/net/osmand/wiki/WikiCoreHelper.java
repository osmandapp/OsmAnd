package net.osmand.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.wiki.WikiHelper;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.shared.wiki.WikiMetadata;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WikiCoreHelper {

	public static final boolean USE_OSMAND_WIKI_API = true;
	private static final Log LOG = PlatformUtil.getLog(WikiCoreHelper.class);
	private static final String WIKIMEDIA_API_ENDPOINT = "https://commons.wikimedia.org/w/api.php";
	private static final String WIKIDATA_API_ENDPOINT = "https://www.wikidata.org/w/api.php";
	private static final String WIKIDATA_ACTION = "?action=wbgetclaims&property=P18&entity=";
	private static final String WIKIMEDIA_ACTION = "?action=query&list=categorymembers&cmtitle=";
	private static final String CM_LIMIT = "&cmlimit=100";
	private static final String FORMAT_JSON = "&format=json";
	public static final String WIKIMEDIA_FILE = "File:";
	public static final String WIKIMEDIA_CATEGORY = "Category:";
	private static final int THUMB_SIZE = 480;
	private static final int ICON_SIZE = 64;
	public static final String OSMAND_API_ENDPOINT = "https://osmand.net/api/";
	public static final String OSMAND_SEARCH_ENDPOINT = "https://osmand.net/search/";
	private static final String WIKI_PLACE_ACTION = "wiki_place?";
	private static final String GET_WIKI_DATA_ACTION = "get-wiki-data?";
	private static final int DEPT_CAT_LIMIT = 1;
	private static final List<String> IMAGE_EXTENSIONS = new ArrayList<>(Arrays.asList(".jpeg", ".jpg", ".png", ".gif"));


	public static List<OsmandApiFeatureData> getExploreImageList(KQuadRect mapRect, int zoom, String langs) {
		List<OsmandApiFeatureData> wikiImages = new ArrayList<>();
		StringBuilder url = new StringBuilder();
		String baseApiActionUrl = OSMAND_SEARCH_ENDPOINT + GET_WIKI_DATA_ACTION;
		String northWest = String.format(Locale.US, "%f,%f", mapRect.getTop(), mapRect.getLeft());
		String southEast = String.format(Locale.US, "%f,%f", mapRect.getBottom(), mapRect.getRight());
		url.append(baseApiActionUrl);
		try {
			url.append(String.format(Locale.US, "northWest=%s", URLEncoder.encode(northWest, "UTF-8")));
			url.append("&");
			url.append(String.format(Locale.US, "southEast=%s", URLEncoder.encode(southEast, "UTF-8")));
			url.append("&");
			url.append(String.format(Locale.US, "zoom=%d", zoom));
			url.append("&");
			url.append(String.format(Locale.US, "lang=%s", langs));
			url.append("&");
			url.append(String.format(Locale.US, "filters="));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		LOG.debug("Download images " + url + " {" + Thread.currentThread().getName() + "}");
		getNearbyImagesOsmAndAPIRequest(url.toString(), wikiImages);
		return wikiImages;
	}

	// wiki_place api call
	public static List<WikiImage> getWikiImageList(Map<String, String> tags, NetworkResponseListener listener) {
		WikiHelper.WikiTagData wikiTagData = WikiHelper.INSTANCE.extractWikiTagData(tags);
		String wikidataId = wikiTagData.getWikidataId();
		String wikiCategory = wikiTagData.getWikiCategory();
		String wikiTitle = wikiTagData.getWikiTitle();
		List<WikiImage> wikiImages = wikiTagData.getWikiImages();
		if (USE_OSMAND_WIKI_API) {
			// article // category
			String url = "";
			String baseApiActionUrl = OSMAND_API_ENDPOINT + WIKI_PLACE_ACTION;
			try {
				if (!Algorithms.isEmpty(wikidataId)) {
					url += baseApiActionUrl + "article=" + URLEncoder.encode(wikidataId, "UTF-8");
				}
				if (!Algorithms.isEmpty(wikiCategory)) {
					url += (url.isEmpty() ? baseApiActionUrl : "&") + "category=" + URLEncoder.encode(wikiCategory, "UTF-8");
				}
				if (!Algorithms.isEmpty(wikiTitle)) {
					url += (url.isEmpty() ? baseApiActionUrl : "&") + "wiki=" + URLEncoder.encode(wikiTitle, "UTF-8");
				}
				if (!url.isEmpty()) {
					url += "&" + "addMetaData=" + URLEncoder.encode("true", "UTF-8");
					getImagesOsmAndAPIRequestV2(url, wikiImages, listener);
				}
			} catch (UnsupportedEncodingException e) {
				LOG.error(e);
			}
		} else {
			if (!Algorithms.isEmpty(wikidataId)) {
				getWikidataImageWikidata(wikidataId, wikiImages);
			}
			if (!Algorithms.isEmpty(wikiCategory)) {
				getWikimediaImageCategory(wikiCategory, wikiImages, 0);
			}
		}

		return wikiImages;
	}

	private static List<WikiImage> getWikimediaImageCategory(String categoryName, List<WikiImage> wikiImages, int depth) {
		String url = WIKIMEDIA_API_ENDPOINT + WIKIMEDIA_ACTION + WIKIMEDIA_CATEGORY + categoryName + CM_LIMIT
				+ FORMAT_JSON;
		WikimediaResponse response = sendWikipediaApiRequest(url, WikimediaResponse.class, false);
		if (response != null) {
			List<String> subCategories = new ArrayList<>();
			for (Categorymember cm : response.query.categorymembers) {
				String memberTitle = cm.title;
				if (memberTitle != null) {
					if (memberTitle.startsWith(WIKIMEDIA_CATEGORY)) {
						subCategories.add(memberTitle);
					} else if (memberTitle.startsWith(WIKIMEDIA_FILE)) {
						WikiHelper.INSTANCE.addFile(wikiImages, memberTitle);
					}
				}
			}
			if (depth < DEPT_CAT_LIMIT) {
				for (String subCategory : subCategories) {
					getWikimediaImageCategory(subCategory, wikiImages, depth + 1);
				}
			}
		}
		return wikiImages;
	}


	protected static List<WikiImage> getWikidataImageWikidata(String wikidataId, List<WikiImage> wikiImages) {
		String url = WIKIDATA_API_ENDPOINT + WIKIDATA_ACTION + wikidataId + FORMAT_JSON;
		WikidataResponse response = sendWikipediaApiRequest(url, WikidataResponse.class, false);
		if (response != null && response.claims != null && response.claims.p18 != null) {
			for (P18 p18 : response.claims.p18) {
				String imageFileName = p18.mainsnak.datavalue.value;
				if (imageFileName != null) {
					WikiImage wikiImage = WikiHelper.INSTANCE.getImageData(imageFileName);
					if (wikiImage != null) {
						wikiImages.add(wikiImage);
					}
				}
			}
		}
		return wikiImages;
	}

	private static OsmandAPIResponseV2 getImagesOsmAndAPIRequestV2(String jsonString) {
		try {
			return new Gson().fromJson(jsonString, OsmandAPIResponseV2.class);
		} catch (JsonSyntaxException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
	}

	private static List<WikiImage> createWikiImages(OsmandAPIResponseV2 response, List<WikiImage> wikiImages) {
		if (response != null && !Algorithms.isEmpty(response.images)) {
			for (Map<String, String> image : response.images) {
				WikiImage wikiImage = parseImageDataWithMetaData(image);
				if (wikiImage != null && isUrlFileImage(wikiImage)) {
					wikiImages.add(wikiImage);
				}
			}
		}
		return wikiImages;
	}

	public static List<WikiImage> getImagesFromJson(String json, List<WikiImage> wikiImages) {
		OsmandAPIResponseV2 response = getImagesOsmAndAPIRequestV2(json);
		return createWikiImages(response, wikiImages);
	}

	private static List<WikiImage> getImagesOsmAndAPIRequestV2(String url, List<WikiImage> wikiImages, NetworkResponseListener listener) {
		OsmandAPIResponseV2 response = sendWikipediaApiRequest(url, OsmandAPIResponseV2.class, listener, false);
		return createWikiImages(response, wikiImages);
	}

	private static boolean isUrlFileImage(WikiImage wikiImage) {
		String path = wikiImage.getImageHiResUrl();
		int lastIndexOfDot = path.lastIndexOf('.');
		if (lastIndexOfDot != -1) {
			return IMAGE_EXTENSIONS.contains(path.substring(lastIndexOfDot).toLowerCase());
		}
		return false;
	}

	private static void getNearbyImagesOsmAndAPIRequest(String url, List<OsmandApiFeatureData> wikiImages) {
		OsmandAPIFeaturesResponse response = sendWikipediaApiRequest(url, OsmandAPIFeaturesResponse.class, true);
		if (response != null && !Algorithms.isEmpty(response.features)) {
			wikiImages.addAll(response.features);
		}
	}

	private static List<WikiImage> getImagesOsmAndAPIRequest(String url, List<WikiImage> wikiImages) {
		OsmandAPIResponse response = sendWikipediaApiRequest(url, OsmandAPIResponse.class, false);
		if (response != null && !Algorithms.isEmpty(response.images)) {
			for (String imageUrl : response.images) {
				if (imageUrl != null) {
					WikiImage wikiImage = parseImageDataFromFile(imageUrl);
					if (wikiImage != null) {
						wikiImages.add(wikiImage);
					}
				}
			}
		}
		return wikiImages;
	}

	private static WikiImage parseImageDataWithMetaData(Map<String, String> image) {
		String imageUrl = image.get("image");
		if (!Algorithms.isEmpty(image)) {
			WikiImage wikiImage = parseImageDataFromFile(imageUrl);
			if (wikiImage != null) {
				WikiMetadata.Metadata metadata = wikiImage.getMetadata();

				String date = image.get("date");
				if (date != null) {
					metadata.setDate(date);
				}
				String author = image.get("author");
				if (date != null) {
					metadata.setAuthor(author);
				}
				String license = image.get("license");
				if (date != null) {
					metadata.setLicense(license);
				}
				long mediaId = Algorithms.parseLongSilently(image.get("mediaId"), -1);
				wikiImage.setMediaId(mediaId);
				return wikiImage;
			}
		}
		return null;
	}

	private static WikiImage parseImageDataFromFile(String imageUrl) {
		String imageHiResUrl = imageUrl.replace("%20", " ").replace(" ", "_");
		try {
			imageUrl = URLDecoder.decode(imageUrl, "UTF-8");
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			LOG.error(e.getLocalizedMessage());
		}
		String imageFileName = Algorithms.getFileWithoutDirs(imageUrl);
		String imageName = Algorithms.getFileNameWithoutExtension(imageUrl);
		String imageStubUrl = imageHiResUrl + "?width=" + THUMB_SIZE;
		String imageIconUrl = imageHiResUrl + "?width=" + ICON_SIZE;
		return new WikiImage(imageFileName, imageName, imageStubUrl, imageHiResUrl, imageIconUrl);
	}

	private static <T> T sendWikipediaApiRequest(String url, Class<T> responseClass, boolean useGzip) {
		return sendWikipediaApiRequest(url, responseClass, null, useGzip);
	}

	private static <T> T sendWikipediaApiRequest(String url, Class<T> responseClass, NetworkResponseListener listener, boolean useGzip) {
		StringBuilder rawResponse = new StringBuilder();
		try {
			// Send the GET request with GZIP support
			String errorMessage = NetworkUtils.sendGetRequest(url, null, rawResponse, useGzip);
			if (errorMessage == null) {
				try {
					// Parse the JSON response
					String stringResponse = rawResponse.toString();
					if (listener != null) {
						listener.onGetRawResponse(stringResponse);
					}
					return new Gson().fromJson(stringResponse, responseClass);
				} catch (JsonSyntaxException e) {
					LOG.error(e.getLocalizedMessage());
				}
			} else {
				LOG.error(errorMessage);
			}
		} catch (Exception e) {
			LOG.error(e.getLocalizedMessage());
		}
		return null;
	}

	public static class OsmandAPIFeaturesResponse {
		@SerializedName("features")
		@Expose
		private final List<OsmandApiFeatureData> features = null;
	}

	public static class OsmandAPIResponseV2 {
		@SerializedName("features-v2")
		@Expose
		private final Set<Map<String, String>> images = null;
	}

	public static class OsmandApiFeatureData {
		@Expose
		public WikiDataProperties properties;
		@Expose
		public WikiDataGeometry geometry;
	}

	public static class WikiDataGeometry {
		@Expose
		public double[] coordinates;
	}

	public static class WikiDataProperties {
		public String id;
		public String photoId;
		public String photoTitle;
		public String wikiTitle;
		public String poitype;
		public String poisubtype;
		public String catId;
		public String catTitle;
		public String depId;
		public String depTitle;
		public String wikiLang;
		public String wikiDesc;
		public String wikiLangs;
		public String wikiLangViews;
		public Long osmid;

		public Double elo;
		public int osmtype;
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

	public interface NetworkResponseListener {
		void onGetRawResponse(String response);
	}
}
