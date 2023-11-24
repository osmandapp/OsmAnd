package net.osmand.aidl.favorite.group;

import android.os.Parcel;
import android.os.Parcelable;

public class AddFavoriteGroupParams implements Parcelable {

    private AFavoriteGroup favoriteGroup;

    public AddFavoriteGroupParams(AFavoriteGroup favoriteGroup) {
        this.favoriteGroup = favoriteGroup;
    }

    public AddFavoriteGroupParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<AddFavoriteGroupParams> CREATOR = new Creator<AddFavoriteGroupParams>() {
        @Override
        public AddFavoriteGroupParams createFromParcel(Parcel in) {
            return new AddFavoriteGroupParams(in);
        }

        @Override
        public AddFavoriteGroupParams[] newArray(int size) {
            return new AddFavoriteGroupParams[size];
        }
    };

    public AFavoriteGroup getFavoriteGroup() {
        return favoriteGroup;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(favoriteGroup, flags);
    }

    private void readFromParcel(Parcel in) {
        favoriteGroup = in.readParcelable(AFavoriteGroup.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}