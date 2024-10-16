package net.osmand.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private static final String IMAGE_BASE_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/";
	public static final String WIKIMEDIA_FILE = "File:";
	public static final String WIKIMEDIA_CATEGORY = "Category:";
	private static final int THUMB_SIZE = 480;
	public static final String OSMAND_API_ENDPOINT = "https://osmand.net/api/wiki_place?";
	private static final int DEPT_CAT_LIMIT = 1;
	private static final List<String> IMAGE_EXTENSIONS = new ArrayList<>(Arrays.asList(".jpeg", ".jpg", ".png", ".gif"));


	public static List<WikiImage> getWikiImageList(Map<String, String> tags) {
		List<WikiImage> wikiImages = new ArrayList<WikiImage>();
		String wikidataId = tags.getOrDefault(Amenity.WIKIDATA, "");
		String wikimediaCommons = tags.get(Amenity.WIKIMEDIA_COMMONS);
		String wikiTitle = tags.get(Amenity.WIKIPEDIA);
		String wikiCategory = "";
		int urlInd = wikiTitle == null ? 0 : wikiTitle.indexOf(".wikipedia.org/wiki/");
		if (urlInd > 0) {
			String prefix = wikiTitle.substring(0, urlInd);
			String lang = prefix.substring(prefix.lastIndexOf("/") + 1, prefix.length());
			String title = wikiTitle.substring(urlInd + ".wikipedia.org/wiki/".length());
			wikiTitle = lang + ":" + title;
		}
		if (!Algorithms.isEmpty(wikimediaCommons)) {
			if (wikimediaCommons.startsWith(WIKIMEDIA_FILE)) {
				addFile(wikiImages, wikimediaCommons);
			} else if (wikimediaCommons.startsWith(WIKIMEDIA_CATEGORY)) {
				wikiCategory = wikimediaCommons.replace(WIKIMEDIA_CATEGORY, "");
			}
		}
		if (Algorithms.isEmpty(wikiTitle)) {
			for (String tag : tags.keySet()) {
				if (tag.startsWith(Amenity.WIKIPEDIA + ":")) {
					wikiTitle = tag.substring((Amenity.WIKIPEDIA + ":").length()) + ":" + tags.get(tag);
				}
			}
		}
		if (USE_OSMAND_WIKI_API) {
			// article // category
			String url = "";
			try {
				if (!Algorithms.isEmpty(wikidataId)) {
					url += (url.length() == 0 ? OSMAND_API_ENDPOINT : "&") + "article=" + URLEncoder.encode(wikidataId, "UTF-8");
				}
				if (!Algorithms.isEmpty(wikiCategory)) {
					url += (url.length() == 0 ? OSMAND_API_ENDPOINT : "&") + "category=" + URLEncoder.encode(wikiCategory, "UTF-8");
				}
				if (!Algorithms.isEmpty(wikiTitle)) {
					url += (url.length() == 0 ? OSMAND_API_ENDPOINT : "&") + "wiki=" + URLEncoder.encode(wikiTitle, "UTF-8");
				}
				if (!Algorithms.isEmpty(wikidataId)) {
					url += (url.length() == 0 ? OSMAND_API_ENDPOINT : "&") + "addMetaData=" + URLEncoder.encode("true", "UTF-8");
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			if (url.length() > 0) {
				getImagesOsmAndAPIRequestV2(url, wikiImages);
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

	private static void addFile(List<WikiImage> wikiImages, String wikimediaCommons) {
		String imageFileName = wikimediaCommons.replace(WIKIMEDIA_FILE, "");
		WikiImage wikiImage = getImageData(imageFileName);
		wikiImages.add(wikiImage);
	}

	private static List<WikiImage> getWikimediaImageCategory(String categoryName, List<WikiImage> wikiImages, int depth) {
		String url = WIKIMEDIA_API_ENDPOINT + WIKIMEDIA_ACTION + WIKIMEDIA_CATEGORY + categoryName + CM_LIMIT
				+ FORMAT_JSON;
		WikimediaResponse response = sendWikipediaApiRequest(url, WikimediaResponse.class);
		if (response != null) {
			List<String> subCategories = new ArrayList<>();
			for (Categorymember cm : response.query.categorymembers) {
				String memberTitle = cm.title;
				if (memberTitle != null) {
					if (memberTitle.startsWith(WIKIMEDIA_CATEGORY)) {
						subCategories.add(memberTitle);
					} else if (memberTitle.startsWith(WIKIMEDIA_FILE)) {
						addFile(wikiImages, memberTitle);
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
		return wikiImages;
	}

	private static List<WikiImage> getImagesOsmAndAPIRequestV2(String url, List<WikiImage> wikiImages) {
		OsmandAPIResponseV2 response = sendWikipediaApiRequest(url, OsmandAPIResponseV2.class);
		if (response != null && !Algorithms.isEmpty(response.images)) {
			for (Map<String, Object> image : response.images) {
				WikiImage wikiImage = parseImageDataWithMetaData(image);
				if (wikiImage != null && isUrlFileImage(wikiImage)) {
					wikiImages.add(wikiImage);
				}
			}
		}
		return wikiImages;
	}

	private static boolean isUrlFileImage(WikiImage wikiImage) {
		String path = wikiImage.getImageHiResUrl();
		int lastIndexOfDot = path.lastIndexOf('.');
		if (lastIndexOfDot != -1) {
			return IMAGE_EXTENSIONS.contains(path.substring(lastIndexOfDot).toLowerCase());
		}
		return false;
	}

	private static List<WikiImage> getImagesOsmAndAPIRequest(String url, List<WikiImage> wikiImages) {
		OsmandAPIResponse response = sendWikipediaApiRequest(url, OsmandAPIResponse.class);
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

	private static WikiImage parseImageDataWithMetaData(Map<String, Object> image) {
		String imageUrl = (String) image.get("image");
		if (!Algorithms.isEmpty(image)) {
			WikiImage wikiImage = parseImageDataFromFile(imageUrl);
			if (wikiImage != null) {
				Metadata metadata = wikiImage.getMetadata();

				String date = (String) image.get("date");
				if (date != null) {
					metadata.setDate(date);
				}
				String author = (String) image.get("author");
				if (date != null) {
					metadata.setAuthor(author);
				}
				String license = (String) image.get("license");
				if (date != null) {
					metadata.setLicense(license);
				}
				return wikiImage;
			}
		}
		return null;
	}

	private static WikiImage parseImageDataFromFile(String imageUrl) {
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

	public static class OsmandAPIResponseV2 {
		@SerializedName("features-v2")
		@Expose
		private final Set<Map<String, Object>> images = null;
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
