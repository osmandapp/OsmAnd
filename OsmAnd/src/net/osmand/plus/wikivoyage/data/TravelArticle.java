package net.osmand.plus.wikivoyage.data;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import static net.osmand.gpx.GPXUtilities.WptPt;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class TravelArticle {

	private static final Log LOG = PlatformUtil.getLog(TravelArticle.class);
	private static final String IMAGE_ROOT_URL = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String THUMB_PREFIX = "320px-";
	private static final String REGULAR_PREFIX = "1280px-";//1280, 1024, 800


	File file;
	String title;
	String content;
	String isPartOf;
	String isParentOf = "";
	double lat = Double.NaN;
	double lon = Double.NaN;
	String imageTitle;
	GPXFile gpxFile;
	String routeId;
	int routeRadius = -1;
	public String ref;
	String routeSource = "";
	long originalId;
	String lang;
	String contentsJson;
	String aggregatedPartOf;
	String description;

	long lastModified;
	boolean gpxFileReading;
	boolean gpxFileRead;

	@NonNull
	public TravelArticleIdentifier generateIdentifier() {
		return new TravelArticleIdentifier(this);
	}

	@NonNull
	public static String getTravelBook(@NonNull OsmandApplication app, @NonNull File file) {
		return file.getPath().replace(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).getPath() + "/", "");
	}

	@Nullable
	public String getTravelBook(@NonNull OsmandApplication app) {
		return file != null ? getTravelBook(app, file) : null;
	}

	public File getFile() {
		return file;
	}

	public long getLastModified() {
		if (lastModified > 0) {
			return lastModified;
		}
		return file != null ? file.lastModified() : 0;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getContent() {
		return content;
	}

	public String getIsPartOf() {
		return isPartOf;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public String getImageTitle() {
		return imageTitle;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public String getRouteId() {
		return routeId;
	}

	public String getRouteSource() {
		return routeSource;
	}

	public long getOriginalId() {
		return originalId;
	}

	public String getLang() {
		return lang;
	}

	public String getContentsJson() {
		return contentsJson;
	}

	public String getAggregatedPartOf() {
		return aggregatedPartOf;
	}

	@Nullable
	public String getGeoDescription() {
		if (TextUtils.isEmpty(aggregatedPartOf)) {
			return null;
		}

		String[] parts = aggregatedPartOf.split(",");
		if (parts.length > 0) {
			StringBuilder res = new StringBuilder();
			res.append(parts[parts.length - 1]);
			if (parts.length > 1) {
				res.append(" \u2022 ").append(parts[0]);
			}
			return res.toString();
		}

		return null;
	}

	@NonNull
	public static String getImageUrl(@NonNull String imageTitle, boolean thumbnail) {
		imageTitle = imageTitle.replace(" ", "_");
		try {
			imageTitle = URLDecoder.decode(imageTitle, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		String[] hash = getHash(imageTitle);
		try {
			imageTitle = URLEncoder.encode(imageTitle, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		String prefix = thumbnail ? THUMB_PREFIX : REGULAR_PREFIX;
		String suffix = imageTitle.endsWith(".svg") ? ".png" : "";
		return IMAGE_ROOT_URL + "thumb/" + hash[0] + "/" + hash[1] + "/" + imageTitle + "/" + prefix + imageTitle + suffix;
	}

	@NonNull
	public String getPointFilterString(){
		return ROUTE_ARTICLE_POINT;
	}

	@NonNull
	public WptPt createWptPt(@NonNull Amenity amenity, @Nullable String lang) {
		WptPt wptPt = new WptPt();
		wptPt.name = amenity.getName();
		wptPt.lat = amenity.getLocation().getLatitude();
		wptPt.lon = amenity.getLocation().getLongitude();
		wptPt.desc = amenity.getDescription(lang);
		wptPt.link = amenity.getSite();
		String colorId = amenity.getColor();
		if (colorId != null) {
			wptPt.setColor(DefaultColors.valueOf(colorId));
		}
		String iconName = amenity.getGpxIcon();
		if (iconName != null) {
			wptPt.setIconName(iconName);
		}
		String category = amenity.getTagSuffix("category_");
		if (category != null) {
			wptPt.category = capitalizeFirstLetter(category);
		}
		return wptPt;
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return null;
	}

	@NonNull
	public BinaryMapIndexReader.SearchPoiTypeFilter getSearchFilter(String filterSubcategory) {
		return new BinaryMapIndexReader.SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return subcategory.equals(filterSubcategory);
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	@Size(2)
	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s)));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TravelArticle that = (TravelArticle) o;
		return TravelArticleIdentifier.areLatLonEqual(that.lat, that.lon, lat, lon) &&
				Algorithms.objectEquals(file, that.file) &&
				Algorithms.stringsEqual(routeId, that.routeId) &&
				Algorithms.stringsEqual(routeSource, that.routeSource);
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(file, lat, lon, routeId, routeSource);
	}

	public static class TravelArticleIdentifier implements Parcelable {
		@Nullable File file;
		double lat;
		double lon;
		@Nullable String title;
		@Nullable String routeId;
		@Nullable String routeSource;

		public static final Creator<TravelArticleIdentifier> CREATOR = new Creator<TravelArticleIdentifier>() {
			@Override
			public TravelArticleIdentifier createFromParcel(Parcel in) {
				return new TravelArticleIdentifier(in);
			}

			@Override
			public TravelArticleIdentifier[] newArray(int size) {
				return new TravelArticleIdentifier[size];
			}
		};

		private TravelArticleIdentifier(@NonNull Parcel in) {
			readFromParcel(in);
		}

		private TravelArticleIdentifier(@NonNull TravelArticle article) {
			file = article.file;
			lat = article.lat;
			lon = article.lon;
			title = article.title;
			routeId = article.routeId;
			routeSource = article.routeSource;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeDouble(lat);
			out.writeDouble(lon);
			out.writeString(title);
			out.writeString(routeId);
			out.writeString(routeSource);
			out.writeString(file != null ? file.getAbsolutePath() : null);
		}

		private void readFromParcel(Parcel in) {
			lat = in.readDouble();
			lon = in.readDouble();
			title = in.readString();
			routeId = in.readString();
			routeSource = in.readString();
			String filePath = in.readString();
			if (!Algorithms.isEmpty(filePath)) {
				file = new File(filePath);
			}
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TravelArticleIdentifier that = (TravelArticleIdentifier) o;
			return areLatLonEqual(that.lat, that.lon, lat, lon) &&
					Algorithms.objectEquals(file, that.file) &&
					Algorithms.stringsEqual(routeId, that.routeId) &&
					Algorithms.stringsEqual(routeSource, that.routeSource);
		}

		@Override
		public int hashCode() {
			return Algorithms.hash(file, lat, lon, routeId, routeSource);
		}

		public static boolean areLatLonEqual(double lat1, double lon1, double lat2, double lon2) {
			boolean latEqual = (Double.isNaN(lat1) && Double.isNaN(lat2)) || Math.abs(lat1 - lat2) < 0.00001;
			boolean lonEqual = (Double.isNaN(lon1) && Double.isNaN(lon2)) || Math.abs(lon1 - lon2) < 0.00001;
			return latEqual && lonEqual;
		}
	}
}