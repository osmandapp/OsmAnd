package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.CitySearchObject;
import net.osmand.core.samples.android.sample1.search.objects.PoiSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.PostcodeSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.StreetSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.VillageSearchObject;

public class SearchListItem {

	protected SampleApplication app;
	private SearchObject searchObject;

	public SearchListItem(SampleApplication app, SearchObject searchObject) {
		this.app = app;
		this.searchObject = searchObject;
	}

	public static SearchListItem buildListItem(SampleApplication app, SearchObject item) {
		switch (item.getType()) {
			case POI:
				return new PoiSearchListItem(app, (PoiSearchObject) item);
			case STREET:
				return new StreetSearchListItem(app, (StreetSearchObject) item);
			case CITY:
				return new CitySearchListItem(app, (CitySearchObject) item);
			case VILLAGE:
				return new VillageSearchListItem(app, (VillageSearchObject) item);
			case POSTCODE:
				return new PostcodeSearchListItem(app, (PostcodeSearchObject) item);
		}
		return null;
	}

	protected SearchObject getSearchObject() {
		return searchObject;
	}

	public String getName() {
		return searchObject.getNativeName();
	}

	public String getTypeName() {
		return searchObject.getType().name();
	}

	public Drawable getIcon() {
		return null;
	}

}
