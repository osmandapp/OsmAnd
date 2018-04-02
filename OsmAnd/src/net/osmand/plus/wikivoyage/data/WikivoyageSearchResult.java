package net.osmand.plus.wikivoyage.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult implements Parcelable {

	List<String> searchTerms = new ArrayList<>();
	long cityId;
	List<String> articleTitles = new ArrayList<>();
	List<String> langs = new ArrayList<>();

	WikivoyageSearchResult() {

	}

	private WikivoyageSearchResult(Parcel in) {
		searchTerms = in.createStringArrayList();
		cityId = in.readLong();
		articleTitles = in.createStringArrayList();
		langs = in.createStringArrayList();
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
