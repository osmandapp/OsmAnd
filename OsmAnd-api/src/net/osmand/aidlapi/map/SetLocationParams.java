package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SetLocationParams extends AidlParams {

    private double latitude;
    private double longitude;
    private long timeToNotUseOtherGPS;

    public SetLocationParams(double latitude, double longitude, long timeToNotUseOtherGPS) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeToNotUseOtherGPS = timeToNotUseOtherGPS;
    }

    public SetLocationParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<SetLocationParams> CREATOR = new Creator<SetLocationParams>() {
        @Override
        public SetLocationParams createFromParcel(Parcel in) {
            return new SetLocationParams(in);
        }

        @Override
        public SetLocationParams[] newArray(int size) {
            return new SetLocationParams[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimeToNotUseOtherGPS() {
        return timeToNotUseOtherGPS;
    }

    @Override
    public void writeToBundle(Bundle bundle) {
        bundle.putDouble("latitude", latitude);
        bundle.putDouble("longitude", longitude);
        bundle.putLong("aidl_time_to_not_use_other_gps", timeToNotUseOtherGPS);
    }

    @Override
    protected void readFromBundle(Bundle bundle) {
        latitude = bundle.getDouble("latitude");
        longitude = bundle.getDouble("longitude");
        timeToNotUseOtherGPS = bundle.getLong("aidl_time_to_not_use_other_gps");
    }
}