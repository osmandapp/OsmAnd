package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.Building;
import net.osmand.core.jni.OsmAndCoreJNI;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.core.jni.StreetIntersection;

public class SearchObjectsHelper {

	public static SearchPositionObject getAddressObject(Address address) {

		switch (address.getAddressType()) {
			case Building:
				BuildingInternal building = new BuildingInternal(address);
				return new BuildingSearchObject(building);

			case StreetIntersection:
				StreetIntercestionInternal streetIntersection = new StreetIntercestionInternal(address);
				return new StreetIntersectionSearchObject(streetIntersection);

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

	private static class BuildingInternal extends Building {
		public BuildingInternal(Address address) {
			super(OsmAndCoreJNI.Street_SWIGSmartPtrUpcast(Address.getCPtr(address)), false);
		}
	}

	private static class StreetIntercestionInternal extends StreetIntersection {
		public StreetIntercestionInternal(Address address) {
			super(OsmAndCoreJNI.Street_SWIGSmartPtrUpcast(Address.getCPtr(address)), false);
		}
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
