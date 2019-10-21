package net.osmand.aidlapi.favorite.group;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddFavoriteGroupParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("favoriteGroup", favoriteGroup);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AFavoriteGroup.class.getClassLoader());
		favoriteGroup = bundle.getParcelable("favoriteGroup");
	}
}