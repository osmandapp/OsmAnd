package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.ObfAddressStreetGroupSubtype;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.util.Algorithms;

public class AddressSearchItem extends SearchItem {

	private String namePrefix;
	private String nameSuffix;
	private String typeStr;

	public AddressSearchItem(Address address) {
		super();

		switch (address.getAddressType()) {
			case Street:
				StreetInternal street = new StreetInternal(address);
				setLocation(street.getPosition31());
				setNativeName(street.getNativeName());
				addLocalizedNames(street.getLocalizedNames());
				if (street.getStreetGroup() != null) {
					nameSuffix = "st.";
					typeStr = getTypeStr(street.getStreetGroup());
				} else {
					typeStr = "Street";
				}
				break;

			case StreetGroup:
				StreetGroupInternal streetGroup = new StreetGroupInternal(address);
				setLocation(streetGroup.getPosition31());
				setNativeName(streetGroup.getNativeName());
				addLocalizedNames(streetGroup.getLocalizedNames());
				typeStr = getTypeStr(streetGroup);
				break;
		}
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String getNameSuffix() {
		return nameSuffix;
	}

	@Override
	public String getName() {
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(namePrefix)) {
			sb.append(namePrefix);
			sb.append(" ");
		}
		sb.append(super.getName());
		if (!Algorithms.isEmpty(nameSuffix)) {
			sb.append(" ");
			sb.append(nameSuffix);
		}
		return sb.toString();
	}

	@Override
	public String getType() {
		return typeStr;
	}

	private String getTypeStr(StreetGroup streetGroup) {
		String typeStr;
		if (streetGroup.getSubtype() != ObfAddressStreetGroupSubtype.Unknown) {
			typeStr = streetGroup.getSubtype().name();
		} else {
			typeStr = streetGroup.getType().name();
		}
		return typeStr;
	}

	private class StreetInternal extends Street {
		public StreetInternal(Address address) {
			super(Address.getCPtr(address), false);
		}
	}

	private class StreetGroupInternal extends StreetGroup {
		public StreetGroupInternal(Address address) {
			super(Address.getCPtr(address), false);
		}
	}
}
