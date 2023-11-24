package net.osmand.aidl.favorite;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateFavoriteParams implements Parcelable {

    private AFavorite favoritePrev;
    private AFavorite favoriteNew;

    public UpdateFavoriteParams(AFavorite favoritePrev, AFavorite favoriteNew) {
        this.favoritePrev = favoritePrev;
        this.favoriteNew = favoriteNew;
    }

    public UpdateFavoriteParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<UpdateFavoriteParams> CREATOR = new Creator<UpdateFavoriteParams>() {
        @Override
        public UpdateFavoriteParams createFromParcel(Parcel in) {
            return new UpdateFavoriteParams(in);
        }

        @Override
        public UpdateFavoriteParams[] newArray(int size) {
            return new UpdateFavoriteParams[size];
        }
    };

    public AFavorite getFavoritePrev() {
        return favoritePrev;
    }

    public AFavorite getFavoriteNew() {
        return favoriteNew;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(favoritePrev, flags);
        out.writeParcelable(favoriteNew, flags);
    }

    private void readFromParcel(Parcel in) {
        favoritePrev = in.readParcelable(AFavorite.class.getClassLoader());
        favoriteNew = in.readParcelable(AFavorite.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
