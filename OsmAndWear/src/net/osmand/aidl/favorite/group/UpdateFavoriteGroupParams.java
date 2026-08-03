package net.osmand.aidl.favorite.group;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateFavoriteGroupParams implements Parcelable {

    private AFavoriteGroup favoriteGroupPrev;
    private AFavoriteGroup favoriteGroupNew;

    public UpdateFavoriteGroupParams(AFavoriteGroup favoriteGroup, AFavoriteGroup favoriteGroupNew) {
        this.favoriteGroupPrev = favoriteGroup;
        this.favoriteGroupNew = favoriteGroupNew;
    }

    public UpdateFavoriteGroupParams(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<UpdateFavoriteGroupParams> CREATOR = new Creator<UpdateFavoriteGroupParams>() {
        @Override
        public UpdateFavoriteGroupParams createFromParcel(Parcel in) {
            return new UpdateFavoriteGroupParams(in);
        }

        @Override
        public UpdateFavoriteGroupParams[] newArray(int size) {
            return new UpdateFavoriteGroupParams[size];
        }
    };

    public AFavoriteGroup getFavoriteGroupPrev() {
        return favoriteGroupPrev;
    }

    public AFavoriteGroup getFavoriteGroupNew() {
        return favoriteGroupNew;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(favoriteGroupPrev, flags);
        out.writeParcelable(favoriteGroupNew, flags);
    }

    private void readFromParcel(Parcel in) {
        favoriteGroupPrev = in.readParcelable(AFavoriteGroup.class.getClassLoader());
        favoriteGroupNew = in.readParcelable(AFavoriteGroup.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}