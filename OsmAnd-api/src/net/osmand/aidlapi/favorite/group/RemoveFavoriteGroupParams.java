package net.osmand.aidlapi.favorite.group;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveFavoriteGroupParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("favoriteGroup", favoriteGroup);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AFavoriteGroup.class.getClassLoader());
		favoriteGroup = bundle.getParcelable("favoriteGroup");
	}
}