package net.osmand.aidlapi.favorite;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveFavoriteParams extends AidlParams {

	private AFavorite favorite;

	public RemoveFavoriteParams(AFavorite favorite) {
		this.favorite = favorite;
	}

	public RemoveFavoriteParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveFavoriteParams> CREATOR = new Creator<RemoveFavoriteParams>() {
		@Override
		public RemoveFavoriteParams createFromParcel(Parcel in) {
			return new RemoveFavoriteParams(in);
		}

		@Override
		public RemoveFavoriteParams[] newArray(int size) {
			return new RemoveFavoriteParams[size];
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