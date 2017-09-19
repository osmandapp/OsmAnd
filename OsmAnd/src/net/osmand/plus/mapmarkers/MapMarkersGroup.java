package net.osmand.plus.mapmarkers;

import net.osmand.plus.MapMarkersHelper;

import java.util.ArrayList;
import java.util.List;

public class MapMarkersGroup {
	private String name;
	private List<MapMarkersHelper.MapMarker> mapMarkers = new ArrayList<>();
	private List<MapMarkersHelper.MapMarker> historyMarkers = new ArrayList<>();
	private long creationDate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<MapMarkersHelper.MapMarker> getMapMarkers() {
		return mapMarkers;
	}

	public void setMapMarkers(List<MapMarkersHelper.MapMarker> mapMarkers) {
		this.mapMarkers = mapMarkers;
	}

	public List<MapMarkersHelper.MapMarker> getHistoryMarkers() {
		return historyMarkers;
	}

	public void setHistoryMarkers(List<MapMarkersHelper.MapMarker> historyMarkers) {
		this.historyMarkers = historyMarkers;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
}
