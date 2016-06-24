package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.OsmAndCoreJNI;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;

public class SearchObjectsHelper {

	public static SearchPositionObject getAddressObject(Address address) {

		switch (address.getAddressType()) {
			case Street:
				StreetInternal street = new StreetInternal(address);
				return new StreetSearchObject(street);

			case StreetGroup:
				StreetGroupInternal streetGroup = new StreetGroupInternal(address);
				switch (streetGroup.getType()) {
					case CityOrTown:
						return new CitySearchObject(streetGroup);
					case Village:
						return new VillageSearchObject(streetGroup);
					case Postcode:
						return new PostcodeSearchObject(streetGroup);
				}
				break;
		}
		return null;
	}

	private static class StreetInternal extends Street {
		public StreetInternal(Address address) {
			super(OsmAndCoreJNI.Street_SWIGSmartPtrUpcast(Address.getCPtr(address)), false);
		}
	}

	private static class StreetGroupInternal extends StreetGroup {
		public StreetGroupInternal(Address address) {
			super(OsmAndCoreJNI.StreetGroup_SWIGSmartPtrUpcast(Address.getCPtr(address)), false);
		}
	}
}
