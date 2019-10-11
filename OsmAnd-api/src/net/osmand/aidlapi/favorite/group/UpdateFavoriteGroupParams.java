package net.osmand.aidlapi.favorite.group;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateFavoriteGroupParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("favoriteGroupPrev", favoriteGroupPrev);
		bundle.putParcelable("favoriteGroupNew", favoriteGroupNew);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AFavoriteGroup.class.getClassLoader());
		favoriteGroupPrev = bundle.getParcelable("favoriteGroupPrev");
		favoriteGroupNew = bundle.getParcelable("favoriteGroupNew");
	}
}