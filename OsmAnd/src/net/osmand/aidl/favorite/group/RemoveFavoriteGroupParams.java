package net.osmand.aidl.favorite.group;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveFavoriteGroupParams implements Parcelable {

    private AFavoriteGroup favoriteGroup;

    public RemoveFavoriteGroupParams(AFavoriteGroup favoriteGroup) {
        this.favoriteGroup = favoriteGroup;
    }

    public RemoveFavoriteGroupParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<RemoveFavoriteGroupParams> CREATOR = new Creator<RemoveFavoriteGroupParams>() {
        @Override
        public RemoveFavoriteGroupParams createFromParcel(Parcel in) {
            return new RemoveFavoriteGroupParams(in);
        }

        @Override
        public RemoveFavoriteGroupParams[] newArray(int size) {
            return new RemoveFavoriteGroupParams[size];
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