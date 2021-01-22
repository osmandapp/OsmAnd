package net.osmand.aidlapi.map;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class SetLocationParams extends AidlParams {

    private ALocation location;
    private long timeToNotUseOtherGPS;

    public SetLocationParams(ALocation location, long timeToNotUseOtherGPS) {
        this.location = location;
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

    public ALocation getLocation() {
        return location;
    }

    public long getTimeToNotUseOtherGPS() {
        return timeToNotUseOtherGPS;
    }

    @Override
    public void writeToBundle(Bundle bundle) {
        bundle.putParcelable("location", location);
        bundle.putLong("timeToNotUseOtherGPS", timeToNotUseOtherGPS);
    }

    @Override
    protected void readFromBundle(Bundle bundle) {
        bundle.setClassLoader(ALocation.class.getClassLoader());
        location = bundle.getParcelable("location");
        timeToNotUseOtherGPS = bundle.getLong("timeToNotUseOtherGPS");
    }
}