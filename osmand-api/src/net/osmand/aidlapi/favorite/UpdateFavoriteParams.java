package net.osmand.aidlapi.favorite;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateFavoriteParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("favoritePrev", favoritePrev);
		bundle.putParcelable("favoriteNew", favoriteNew);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AFavorite.class.getClassLoader());
		favoritePrev = bundle.getParcelable("favoritePrev");
		favoriteNew = bundle.getParcelable("favoriteNew");
	}
}