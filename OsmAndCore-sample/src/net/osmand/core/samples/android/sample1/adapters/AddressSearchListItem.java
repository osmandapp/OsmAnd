package net.osmand.core.samples.android.sample1.adapters;

import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.search.AddressSearchItem;
import net.osmand.util.Algorithms;

public class AddressSearchListItem extends SearchListItem {
	private String nameStr;
	private String typeStr;

	public AddressSearchListItem(SampleApplication app, AddressSearchItem searchItem) {
		super(app, searchItem);

		StringBuilder sb = new StringBuilder();
		String localizedName = searchItem.getLocalizedNames().get(MapUtils.LANGUAGE);
		if (Algorithms.isEmpty(localizedName)) {
			localizedName = searchItem.getNativeName();
		}
		if (!Algorithms.isEmpty(searchItem.getNamePrefix())) {
			sb.append(searchItem.getNamePrefix());
			sb.append(" ");
		}
		sb.append(localizedName);
		if (!Algorithms.isEmpty(searchItem.getNameSuffix())) {
			sb.append(" ");
			sb.append(searchItem.getNameSuffix());
		}

		nameStr = sb.toString();
		typeStr = searchItem.getType();
	}

	@Override
	public String getName() {
		return nameStr;
	}

	@Override
	public String getType() {
		return typeStr;
	}
}
