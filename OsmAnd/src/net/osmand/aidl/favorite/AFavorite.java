package net.osmand.aidl.favorite;

import android.os.Parcel;
import android.os.Parcelable;

public class AFavorite implements Parcelable {

    private double lat;
    private double lon;
    private String name;
    private String description;
    private String category;
    private String color;
    private boolean visible;

    public AFavorite(double lat, double lon, String name, String description,
                     String category, String color, boolean visible) {
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.description = description;
        this.category = category;
        this.color = color;
        this.visible = visible;
    }

    public AFavorite(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<AFavorite> CREATOR = new Creator<AFavorite>() {
        @Override
        public AFavorite createFromParcel(Parcel in) {
            return new AFavorite(in);
        }

        @Override
        public AFavorite[] newArray(int size) {
            return new AFavorite[size];
        }
    };

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(lat);
        out.writeDouble(lon);
        out.writeString(name);
        out.writeString(description);
        out.writeString(category);
        out.writeString(color);
        out.writeByte((byte) (visible ? 1 : 0));
    }

    private void readFromParcel(Parcel in) {
        lat = in.readDouble();
        lon = in.readDouble();
        name = in.readString();
        description = in.readString();
        category = in.readString();
        color = in.readString();
        visible = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
