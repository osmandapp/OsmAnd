package net.osmand.plus.mapmarkers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO rename after 4.0 MapMarkersGroup -> ItineraryGroup
public class MapMarkersGroup {

	public static final String MARKERS_SYNC_GROUP_ID = "markers_sync_group_id";

	private String id;
	private String name;
	private ItineraryType type = ItineraryType.MARKERS;
	private Set<String> wptCategories;
	private boolean disabled;

	private long creationDate;
	private boolean visible = true;
	private boolean wasShown;
	private boolean visibleUntilRestart;
	private List<MapMarker> markers = new ArrayList<>();
	private TravelArticle wikivoyageArticle;
	// TODO should be removed from this class:
	private ShowHideHistoryButton showHideHistoryButton;

	public MapMarkersGroup() {

	}

	public MapMarkersGroup(@NonNull String id, @NonNull String name, @NonNull ItineraryType type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getGpxPath(@NonNull OsmandApplication app) {
		return app.getAppPath(IndexConstants.GPX_INDEX_DIR + id).getAbsolutePath();
	}

	public TravelArticle getWikivoyageArticle() {
		return wikivoyageArticle;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setMarkers(List<MapMarker> markers) {
		this.markers = markers;
	}

	public void setShowHideHistoryButton(ShowHideHistoryButton showHideHistoryButton) {
		this.showHideHistoryButton = showHideHistoryButton;
	}

	public boolean isWasShown() {
		return wasShown;
	}

	public boolean isVisibleUntilRestart() {
		return visibleUntilRestart;
	}

	public void setWikivoyageArticle(TravelArticle wikivoyageArticle) {
		this.wikivoyageArticle = wikivoyageArticle;
	}

	public String getName() {
		return name;
	}

	public ItineraryType getType() {
		return type;
	}

	public void setWptCategories(Set<String> wptCategories) {
		this.wptCategories = wptCategories;
	}

	public Set<String> getWptCategories() {
		return wptCategories;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean wasShown() {
		return wasShown;
	}

	public void setWasShown(boolean wasShown) {
		this.wasShown = wasShown;
	}

	public void setVisibleUntilRestart(boolean visibleUntilRestart) {
		this.visibleUntilRestart = visibleUntilRestart;
	}

	public List<MapMarker> getMarkers() {
		return markers;
	}

	public ShowHideHistoryButton getShowHideHistoryButton() {
		return showHideHistoryButton;
	}

	@Nullable
	public String getWptCategoriesString() {
		if (wptCategories != null) {
			return Algorithms.encodeCollection(wptCategories);
		}
		return null;
	}

	public List<MapMarker> getActiveMarkers() {
		List<MapMarker> markers = new ArrayList<>(this.markers);
		List<MapMarker> activeMarkers = new ArrayList<>(markers.size());
		for (MapMarker marker : markers) {
			if (!marker.history) {
				activeMarkers.add(marker);
			}
		}
		return activeMarkers;
	}

	public List<MapMarker> getHistoryMarkers() {
		List<MapMarker> historyMarkers = new ArrayList<>();
		for (MapMarker marker : markers) {
			if (marker.history) {
				historyMarkers.add(marker);
			}
		}
		return historyMarkers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + type.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MapMarkersGroup group = (MapMarkersGroup) o;

		if (type != group.type) return false;
		return Algorithms.stringsEqual(id, group.getId());
	}
}
