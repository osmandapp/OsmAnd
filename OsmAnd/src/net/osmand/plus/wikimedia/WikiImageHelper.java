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
	private static final String ACTION = "?action=wbgetclaims&property=P18&entity=";
	private static final String FORMAT_JSON = "&format=json";
	private static final String IMAGE_BASE_URL = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String WIKIDATA_PREFIX = "Q";
	private static final int THUMB_SIZE = 500;
	private static final Log LOG = PlatformUtil.getLog(WikiImageHelper.class);

	public static void fillWikiMediaCards(@NonNull MapActivity mapActivity, @NonNull String wikidata,
	                                      List<ImageCard> images) {
		if (wikidata.startsWith(WIKIDATA_PREFIX)) {
			StringBuilder rawResponse = new StringBuilder();
			String url = WIKIDATA_API_ENDPOINT + ACTION + wikidata + FORMAT_JSON;
			String error = NetworkUtils.sendGetRequest(url, null, rawResponse);
			if (error == null) {
				try {
					Gson gson = new Gson();
					WikipediaResponse response = gson.fromJson(rawResponse.toString(), WikipediaResponse.class);
					for (WikiImage img : getImageData(response)) {
						images.add(new WikiImageCard(mapActivity, img));
					}
					return;
				} catch (JsonSyntaxException e) {
					error = e.getLocalizedMessage();
				}
			}
			LOG.error(error);
		} else {
			LOG.error("Wrong WikiMedia ID");
		}
	}

	private static List<WikiImage> getImageData(WikipediaResponse response) {
		List<WikiImage> images = new ArrayList<>();
		for (P18 p18 : response.claims.p18) {
			String imageFileName = p18.mainsnak.datavalue.value;
			if (imageFileName != null) {
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
					images.add(new WikiImage(imageName, imageStubUrl, imageHiResUrl));

				} catch (UnsupportedEncodingException e) {
					LOG.error(e.getLocalizedMessage());
				}
			}
		}
		return images;
	}

	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s)));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}

	private static class Claims {
		@SerializedName("P18")
		@Expose
		private List<P18> p18 = null;
	}

	private static class Datavalue {
		@SerializedName("value")
		@Expose
		private String value;
		@SerializedName("type")
		@Expose
		private String type;
	}

	private static class Mainsnak {
		@SerializedName("datavalue")
		@Expose
		private Datavalue datavalue;
		@SerializedName("datatype")
		@Expose
		private String datatype;
	}

	private static class P18 {
		@SerializedName("mainsnak")
		@Expose
		private Mainsnak mainsnak;
	}

	private static class WikipediaResponse {
		@SerializedName("claims")
		@Expose
		private Claims claims;
	}
}
