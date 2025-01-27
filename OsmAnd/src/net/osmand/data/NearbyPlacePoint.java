package net.osmand.data;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;

import java.io.Serializable;


public class NearbyPlacePoint implements Serializable, LocationPoint {

	private static final long serialVersionUID = 829654300829771466L;

	public static final BackgroundType DEFAULT_BACKGROUND_TYPE = BackgroundType.CIRCLE;
	@Nullable
	public Bitmap imageBitmap;
	public String photoTitle;
	public String wikiTitle;
	public String poitype;
	public String poisubtype;
	public String wikiDesc;

	private double latitude;
	private double longitude;
	private double altitude = Double.NaN;

	private int color;
	private BackgroundType backgroundType;

	public NearbyPlacePoint(OsmandApiFeatureData featureData) {
		this.latitude = featureData.geometry.coordinates[1];
		this.longitude = featureData.geometry.coordinates[0];
		this.photoTitle = featureData.properties.photoTitle;
		this.poisubtype = featureData.properties.poisubtype;
		this.poitype = featureData.properties.poitype;
		this.wikiTitle = featureData.properties.wikiTitle;
		this.wikiDesc = featureData.properties.wikiDesc;
	}

	public int getColor() {
		return color;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	public String getKey() {
		return photoTitle;
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_NEARBY_PLACE, wikiDesc);
	}

	public void setColor(int color) {
		this.color = color;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public String getDisplayName(@NonNull Context ctx) {
		return wikiTitle;
	}

	public String getName() {
		return wikiTitle;
	}

	public String getDescription() {
		return wikiDesc;
	}

	public BackgroundType getBackgroundType() {
		return backgroundType == null ? DEFAULT_BACKGROUND_TYPE : backgroundType;
	}

	public void setBackgroundType(BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	@NonNull
	@Override
	public String toString() {
		return "NearbyPlace " + getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (getClass() != o.getClass()) {
			return false;
		}

		NearbyPlacePoint point = (NearbyPlacePoint) o;
		if(point.imageBitmap != imageBitmap) {
			return false;
		}

		if (!Algorithms.stringsEqual(photoTitle, point.photoTitle)) {
			return false;
		}
		if (!Algorithms.stringsEqual(wikiTitle, point.wikiTitle)) {
			return false;
		}
		if (!Algorithms.stringsEqual(poitype, point.poitype)) {
			return false;
		}
		if (!Algorithms.stringsEqual(poisubtype, point.poisubtype)) {
			return false;
		}

		if (!Algorithms.stringsEqual(wikiDesc, point.wikiDesc)) {
			return false;
		}

		return Double.compare(this.latitude, point.latitude) == 0
				&& Double.compare(this.longitude, point.longitude) == 0
				&& Double.compare(this.altitude, point.altitude) == 0
				&& (this.color == point.color)
				&& (this.backgroundType == point.backgroundType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) Math.floor(latitude * 10000);
		result = prime * result + (int) Math.floor(longitude * 10000);
		result = prime * result + (int) Math.floor(altitude * 10000);
		result = prime * result + ((photoTitle == null) ? 0 : photoTitle.hashCode());
		result = prime * result + ((wikiTitle == null) ? 0 : wikiTitle.hashCode());
		result = prime * result + ((poitype == null) ? 0 : poitype.hashCode());
		result = prime * result + ((poisubtype == null) ? 0 : poisubtype.hashCode());
		result = prime * result + ((wikiDesc == null) ? 0 : wikiDesc.hashCode());
		return result;
	}
}
