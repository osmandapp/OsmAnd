package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.CitySearchObject;

public class CitySearchListItem extends SearchListPositionItem{

	private String nameStr;
	private String typeStr;

	public CitySearchListItem(SampleApplication app, CitySearchObject searchItem) {
		super(app, searchItem);

		nameStr = searchItem.getName(MapUtils.LANGUAGE);
		typeStr = "City";
	}

	@Override
	public String getName() {
		return nameStr;
	}

	@Override
	public String getTypeName() {
		return typeStr;
	}
}
