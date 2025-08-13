package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View.OnClickListener;

import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import org.json.JSONObject;

public class UrlImageCard extends ImageCard {

	public UrlImageCard(MapActivity mapActivity, JSONObject imageObject) {
		super(mapActivity, imageObject);
	}

	/**
	 * Returns the thumbnail URL for the image.
	 * In this implementation, it always returns `null` due to the nature of the OSM "image" tag.
	 * Since the tag can contain any URL pointing to any external service, generating a reliable
	 * thumbnail format (a highly compressed low-resolution image) is not feasible.
	 * In such cases, the best approach is to directly load the full-size image instead.
	 */
	@Nullable
	@Override
	public String getThumbnailUrl() {
		return null;
	}

	/**
	 * Returns the URL for displaying the image in the gallery.
	 * Instead of using the "hires" image URL (if available), this method provides a lower-quality
	 * "image" URL in this case to avoid potential crashes.
	 * Some high-resolution images may be too large to be properly rendered on a canvas bitmap.
	 * To prevent crashes, this implementation uses URL with lower-quality image.
	 */
	@Nullable
	@Override
	public String getGalleryFullSizeUrl() {
		return getImageUrl();
	}

	/**
	 * Returns a suitable URL for opening in a browser.
	 * This may be a high-resolution image URL (if available) or the URL stored in the OSM "image" tag.
	 * The link can either point directly to an image file or lead to a webpage hosting the image
	 * on an external service.
	 */
	@Nullable
	public String getSuitableUrl() {
		String hiresUrl = getImageHiresUrl();
		return Algorithms.isEmpty(hiresUrl) ? getUrl() : hiresUrl;
	}
}
