package net.osmand.plus.wikivoyage.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult implements Parcelable {

	List<String> searchTerm = new ArrayList<>();
	long cityId;
	List<String> articleTitle = new ArrayList<>();
	List<String> langs = new ArrayList<>();

	WikivoyageSearchResult() {

	}

	private WikivoyageSearchResult(Parcel in) {
		searchTerm = in.createStringArrayList();
		cityId = in.readLong();
		articleTitle = in.createStringArrayList();
		langs = in.createStringArrayList();
	}

	public List<String> getSearchTerm() {
		return searchTerm;
	}

	public long getCityId() {
		return cityId;
	}

	public List<String> getArticleTitle() {
		return articleTitle;
	}

	public List<String> getLang() {
		return langs;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringList(searchTerm);
		dest.writeLong(cityId);
		dest.writeStringList(articleTitle);
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
