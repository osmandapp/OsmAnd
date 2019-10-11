package net.osmand.aidlapi.favorite;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddFavoriteParams extends AidlParams {

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
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("favorite", favorite);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AFavorite.class.getClassLoader());
		favorite = bundle.getParcelable("favorite");
	}
}