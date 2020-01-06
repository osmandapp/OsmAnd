package net.osmand.data;

import java.io.Serializable;

import android.content.Context;
import android.support.annotation.NonNull;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import static net.osmand.plus.FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY;

public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;

	private static final String HIDDEN = "hidden";

	protected String name = "";
	protected String description;
	protected String category = "";
	protected String address = "";
	private String originObjectName = "";
	private double latitude;
	private double longitude;
	private int color;
	private boolean visible = true;

	public FavouritePoint(){
	}

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		if(name == null) {
			name = "";
		}
		this.name = name;
	}

	public FavouritePoint(FavouritePoint favouritePoint) {
		this.latitude = favouritePoint.latitude;
		this.longitude = favouritePoint.longitude;
		this.category = favouritePoint.category;
		this.name = favouritePoint.name;
		this.color = favouritePoint.color;
		this.description = favouritePoint.description;
		this.visible = favouritePoint.visible;
		this.originObjectName = favouritePoint.originObjectName;
	}

	public int getColor() {
		return color;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public PointDescription getPointDescription() {
		return new PointDescription(PointDescription.POINT_TYPE_FAVORITE, getName());
	}

	public boolean isAddressSpecified() {
		return !Algorithms.isEmpty(address);
	}

	public boolean isPersonalPoint() {
		// TODO: HW
		return name.equals(FavouritesDbHelper.getHomePointName())
				|| name.equals(FavouritesDbHelper.getWorkPointName())
				|| name.equals(FavouritesDbHelper.getParkingPointName());
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_FAVORITE, getDisplayName(ctx));
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getOriginObjectName() {
		return originObjectName;
	}

	public void setOriginObjectName(String originObjectName) {
		this.originObjectName = originObjectName;
	}

	public int getOverlayIconId() {
		// TODO HW
		if(isPersonalPoint()) {
			return FavouritesDbHelper.getPersonalIconId(point.getName());
		}
		return 0;
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
	
	public String getCategory() {
		return category;
	}

	public String getCategoryDisplayName(@NonNull Context ctx) {
		if (category.equals(PERSONAL_CATEGORY)) {
			return ctx.getString(R.string.personal_category_name);
		} else {
			return category;
		}
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	public String getDisplayName(@NonNull Context ctx) {
		if (isPersonalPoint()) {
			return FavouritesDbHelper.getPersonalName(this, ctx);
		}
		return name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription () {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NonNull
	@Override
	public String toString() {
		return "Favourite " + getName(); //$NON-NLS-1$
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null) return false;

		if (getClass() != o.getClass()) return false;

		FavouritePoint fp = (FavouritePoint) o;

		if (name == null) {
			if (fp.name != null)
				return false;
		} else if (!name.equals(fp.name))
			return false;

		if (category == null) {
			if (fp.category != null)
				return false;
		} else if (!category.equals(fp.category))
			return false;

		if (description == null) {
			if (fp.description != null)
				return false;
		} else if (!description.equals(fp.description))
			return false;

		if (originObjectName == null) {
			if (fp.originObjectName != null)
				return false;
		} else if (!originObjectName.equals(fp.originObjectName))
			return false;

		return (this.latitude == fp.latitude) && (this.longitude == fp.longitude);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) Math.floor(latitude * 10000);
		result = prime * result + (int) Math.floor(longitude * 10000);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((originObjectName == null) ? 0 : originObjectName.hashCode());
		return result;
	}

	public static FavouritePoint fromWpt(@NonNull WptPt pt) {
		String name = pt.name;
		String categoryName = pt.category != null ? pt.category : "";
		if (name == null) {
			name = "";
		}
		FavouritePoint fp;
			fp = new FavouritePoint(pt.lat, pt.lon, name, categoryName);
		fp.setDescription(pt.desc);
		if (pt.comment != null) {
			fp.setOriginObjectName(pt.comment);
		}
		fp.setColor(pt.getColor(0));
		fp.setVisible(!pt.getExtensionsToRead().containsKey(HIDDEN));
		return fp;
	}

	public WptPt toWpt() {
		WptPt pt = new WptPt();
		pt.lat = getLatitude();
		pt.lon = getLongitude();
		if (!isVisible()) {
			pt.getExtensionsToWrite().put(HIDDEN, "true");
		}
		if (getColor() != 0) {
			pt.setColor(getColor());
		}
		pt.name = getName();
		pt.desc = getDescription();
		if (getCategory().length() > 0)
			pt.category = getCategory();
		if (getOriginObjectName().length() > 0) {
			pt.comment = getOriginObjectName();
		}
		return pt;
	}
}