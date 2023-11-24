package net.osmand.aidl.favorite;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveFavoriteParams implements Parcelable {

    private AFavorite favorite;

    public RemoveFavoriteParams(AFavorite favorite) {
        this.favorite = favorite;
    }

    public RemoveFavoriteParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<RemoveFavoriteParams> CREATOR = new Creator<RemoveFavoriteParams>() {
        @Override
        public RemoveFavoriteParams createFromParcel(Parcel in) {
            return new RemoveFavoriteParams(in);
        }

        @Override
        public RemoveFavoriteParams[] newArray(int size) {
            return new RemoveFavoriteParams[size];
        }
    };

    public AFavorite getFavorite() {
        return favorite;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(favorite, flags);
    }

    private void readFromParcel(Parcel in) {
        favorite = in.readParcelable(AFavorite.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
