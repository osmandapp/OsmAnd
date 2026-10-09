package net.osmand.plus.download;

import net.osmand.data.Amenity;

public class CityItem {
	private final String name;
	private final Amenity amenity;
	private IndexItem indexItem;

	public CityItem(String name, Amenity amenity, IndexItem indexItem) {
		this.name = name;
		this.amenity = amenity;
		this.indexItem = indexItem;
	}

	public String getName() {
		return name;
	}

	public Amenity getAmenity() {
		return amenity;
	}

	public IndexItem getIndexItem() {
		return indexItem;
	}

	public void setIndexItem(IndexItem indexItem) {
		this.indexItem = indexItem;
	}
}
