package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.CitySearchObject;
import net.osmand.core.samples.android.sample1.search.objects.PostcodeSearchObject;

public class PostcodeSearchListItem extends SearchListPositionItem {

	private String nameStr;
	private String typeStr;

	public PostcodeSearchListItem(SampleApplication app, PostcodeSearchObject searchItem) {
		super(app, searchItem);

		nameStr = searchItem.getNativeName();
		typeStr = "Postcode";
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
