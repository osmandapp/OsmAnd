package net.osmand.aidl;

import java.util.ArrayList;
import java.util.List;

public class AidlSearchResultWrapper {

	private final double latitude;
	private final double longitude;

	private final String localName;
	private final String localTypeName;

	private final String alternateName;
	private final List<String> otherNames = new ArrayList<>();

	public AidlSearchResultWrapper(double latitude, double longitude, String localName, String localTypeName,
	                               String alternateName, List<String> otherNames) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.localName = localName;
		this.localTypeName = localTypeName;
		this.alternateName = alternateName;
		if (otherNames != null) {
			this.otherNames.addAll(otherNames);
		}
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getLocalName() {
		return localName;
	}

	public String getLocalTypeName() {
		return localTypeName;
	}

	public String getAlternateName() {
		return alternateName;
	}

	public List<String> getOtherNames() {
		return otherNames;
	}
}