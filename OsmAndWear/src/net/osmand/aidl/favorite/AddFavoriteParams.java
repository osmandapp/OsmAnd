package net.osmand.aidl.favorite;

import android.os.Parcel;
import android.os.Parcelable;

public class AddFavoriteParams implements Parcelable {

    private AFavorite favorite;

    public AddFavoriteParams(AFavorite favorite) {
        this.favorite = favorite;
    }

    public AddFavoriteParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<AddFavoriteParams> CREATOR = new Creator<AddFavoriteParams>() {
        @Override
        public AddFavoriteParams createFromParcel(Parcel in) {
            return new AddFavoriteParams(in);
        }

        @Override
        public AddFavoriteParams[] newArray(int size) {
            return new AddFavoriteParams[size];
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
