package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.objects.VillageSearchObject;

public class VillageSearchListItem extends SearchListPositionItem{

	private String nameStr;
	private String typeStr;

	public VillageSearchListItem(SampleApplication app, VillageSearchObject villageObject) {
		super(app, villageObject);

		nameStr = villageObject.getName(MapUtils.LANGUAGE);
		typeStr = "Village";
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
