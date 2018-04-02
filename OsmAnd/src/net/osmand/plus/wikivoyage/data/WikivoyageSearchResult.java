package net.osmand.plus.wikivoyage.data;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult implements Parcelable {

	private static final int SHOW_LANGS = 3;

	List<String> searchTerms = new ArrayList<>();
	long cityId;
	List<String> articleTitles = new ArrayList<>();
	List<String> langs = new ArrayList<>();
	String isPartOf;

	WikivoyageSearchResult() {

	}

	private WikivoyageSearchResult(Parcel in) {
		searchTerms = in.createStringArrayList();
		cityId = in.readLong();
		articleTitles = in.createStringArrayList();
		langs = in.createStringArrayList();
		isPartOf = in.readString();
	}

	public List<String> getSearchTerms() {
		return searchTerms;
	}

	public long getCityId() {
		return cityId;
	}

	public List<String> getArticleTitles() {
		return articleTitles;
	}

	public List<String> getLangs() {
		return langs;
	}

	public String getIsPartOf() {
		return isPartOf;
	}

	public String getFirstLangsString() {
		StringBuilder res = new StringBuilder();
		int limit = Math.min(SHOW_LANGS, langs.size());
		for (int i = 0; i < limit; i++) {
			res.append(Algorithms.capitalizeFirstLetter(langs.get(i)));
			if (i != limit - 1) {
				res.append(", ");
			}
		}
		return res.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringList(searchTerms);
		dest.writeLong(cityId);
		dest.writeStringList(articleTitles);
		dest.writeStringList(langs);
		dest.writeString(isPartOf);
	}

	public static final Creator<WikivoyageSearchResult> CREATOR = new Creator<WikivoyageSearchResult>() {
		@Override
		public WikivoyageSearchResult createFromParcel(Parcel in) {
			return new WikivoyageSearchResult(in);
		}

		@Override
		public WikivoyageSearchResult[] newArray(int size) {
			return new WikivoyageSearchResult[size];
		}
	};
}
