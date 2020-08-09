package net.osmand.aidlapi.map;

import android.os.Parcel;
import android.os.Parcelable;

public enum ALocationType implements Parcelable {
    CURRENT(0),
    PROJECTION(1),
    ROUTE_END(2),
    ;

    final int value;

    ALocationType(int value) {
        this.value = value;
    }

    public static final Creator<ALocationType> CREATOR = new Creator<ALocationType>() {
        @Override
        public ALocationType createFromParcel(Parcel in) {
            return ALocationType.valueOf(in.readString());
        }

        @Override
        public ALocationType[] newArray(int size) {
            return new ALocationType[size];
        }
    };

    public int getValue() {
        return value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }
}
