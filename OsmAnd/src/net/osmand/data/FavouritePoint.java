package net.osmand.data;

import java.io.Serializable;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;


public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;

	private static final String HIDDEN = "hidden";
	private static final String ADDRESS_EXTENSION = "address";
	private static final String DEFAULT_ICON_NAME = "special_star";

	protected String name = "";
	protected String description;
	protected String category = "";
	protected String address = "";
	protected int iconId;
	private String originObjectName = "";
	private double latitude;
	private double longitude;
	private int color;
	private boolean visible = true;
	private SpecialPointType specialPointType = null;

	public FavouritePoint() {
	}

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		if(name == null) {
			name = "";
		}
		this.name = name;
		initPersonalType();
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
		this.address = favouritePoint.address;
		this.iconId = favouritePoint.iconId;
		initPersonalType();
	}

	private void initPersonalType() {
		if(FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY.equals(category)) {
			for(SpecialPointType p : SpecialPointType.values()) {
				if(p.typeName.equals(this.name)) {
					this.specialPointType = p;
				}
			}
		}
	}

	public SpecialPointType getSpecialPointType() {
		return specialPointType;
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

	public boolean isAddressSpecified() {
		return !Algorithms.isEmpty(address);
	}

	public int getIconId() {
		return iconId;
	}

	public String getIconEntryName(Context ctx) {
		return ctx.getResources().getResourceEntryName(getOverlayIconId());
	}

	public void setIconId(int iconId) {
		this.iconId = iconId;
	}

	public void setIconIdFromName(Context ctx, String iconName) {
		this.iconId = ctx.getResources().getIdentifier("mx_" + iconName, "drawable", ctx.getPackageName());
	}

	public boolean isSpecialPoint() {
		return specialPointType != null;
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
		if (isSpecialPoint()) {
			return specialPointType.getIconId();
		}
		return iconId;
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
		return FavouritesDbHelper.FavoriteGroup.getDisplayName(ctx, category);
	}
	
	public void setCategory(String category) {
		this.category = category;
		initPersonalType();
	}

	public String getDisplayName(@NonNull Context ctx) {
		if (isSpecialPoint()) {
			return specialPointType.getHumanString(ctx);
		}
		return name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		initPersonalType();
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


	public enum SpecialPointType {
		HOME("home", R.string.home_button, R.drawable.mx_special_house),
		WORK("work", R.string.work_button, R.drawable.mx_special_building),
		PARKING("parking", R.string.map_widget_parking, R.drawable.mx_parking);

		private String typeName;
		@StringRes
		private int resId;
		@DrawableRes
		private int iconId;

		SpecialPointType(@NonNull String typeName, @StringRes int resId, @DrawableRes int iconId) {
			this.typeName = typeName;
			this.resId = resId;
			this.iconId = iconId;
		}

		public String getCategory() { return FavouritesDbHelper.FavoriteGroup.PERSONAL_CATEGORY; }

		public String getName() {
			return typeName;
		}

		public int getIconId() {
			return iconId;
		}

		public String getHumanString(@NonNull Context ctx) {
			return ctx.getString(resId);
		}
	}


	public static FavouritePoint fromWpt(@NonNull WptPt pt, @NonNull Context ctx) {
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
		fp.setAddress(pt.getExtensionsToRead().get(ADDRESS_EXTENSION));
		String iconName = pt.getIconName();
		if (iconName != null) {
			fp.setIconIdFromName(ctx, iconName);
		}
		return fp;
	}

	public WptPt toWpt(@NonNull Context ctx) {
		WptPt pt = new WptPt();
		pt.lat = getLatitude();
		pt.lon = getLongitude();
		if (!isVisible()) {
			pt.getExtensionsToWrite().put(HIDDEN, "true");
		}
		if (isAddressSpecified()) {
			pt.getExtensionsToWrite().put(ADDRESS_EXTENSION, getAddress());
		}
		if (iconId != 0) {
			pt.setIconName(getIconEntryName(ctx).substring(3));
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