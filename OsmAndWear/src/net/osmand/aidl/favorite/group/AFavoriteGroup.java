package net.osmand.aidl.favorite.group;

import android.os.Parcel;
import android.os.Parcelable;

public class AFavoriteGroup implements Parcelable {

    private String name;
    private String color;
    private boolean visible;

    public AFavoriteGroup(String name, String color, boolean visible) {
        this.name = name;
        this.color = color;
        this.visible = visible;
    }

    public AFavoriteGroup(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<AFavoriteGroup> CREATOR = new Creator<AFavoriteGroup>() {
        @Override
        public AFavoriteGroup createFromParcel(Parcel in) {
            return new AFavoriteGroup(in);
        }

        @Override
        public AFavoriteGroup[] newArray(int size) {
            return new AFavoriteGroup[size];
        }
    };

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(color);
        out.writeByte((byte) (visible ? 1 : 0));
    }

    private void readFromParcel(Parcel in) {
        name = in.readString();
        color = in.readString();
        visible = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}