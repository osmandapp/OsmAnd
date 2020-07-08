package net.osmand.plus.wikimedia;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.wikimedia.pojo.P18;
import net.osmand.plus.wikimedia.pojo.WikipediaResponse;

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
						images.add(new WikiImageCard(mapActivity, null, img));
					}
					return;
				} catch (JsonSyntaxException e) {
					error = e.getLocalizedMessage();
				}
				LOG.error(error);
			}
		} else {
			LOG.error("Wrong WikiMedia ID");
		}
	}

	public static List<WikiImage> getImageData(WikipediaResponse response) {
		List<WikiImage> images = new ArrayList<>();
		try {
			for (P18 p18 : response.getClaims().getP18()) {
				String imageFileName = p18.getMainsnak().getDatavalue().getValue();
				if (imageFileName != null) {
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
					images.add(new WikiImage(imageName, imageStubUrl,
							imageHiResUrl, p18.getMainsnak().getDatatype()));
				}
			}
		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return images;
	}

	@NonNull
	public static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s)));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}
}
