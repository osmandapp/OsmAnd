package net.osmand.plus.itinerary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.mapmarkers.CategoriesSubHeader;
import net.osmand.plus.mapmarkers.GroupHeader;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.ShowHideHistoryButton;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItineraryGroup {

	public static final int ANY_TYPE = -1;
	public static final int FAVORITES_TYPE = 0;
	public static final int GPX_TYPE = 1;

	public static final String MARKERS_SYNC_GROUP_ID = "markers_sync_group_id";

	private String id;
	private String name;
	private int type = ANY_TYPE;
	private Set<String> wptCategories;
	private long creationDate;
	private boolean disabled;
	private boolean visible = true;
	private boolean wasShown = false;
	private boolean visibleUntilRestart;
	private List<MapMarker> markers = new ArrayList<>();
	private TravelArticle wikivoyageArticle;
	// TODO should be removed from this class:
	private GroupHeader header;
	private CategoriesSubHeader categoriesSubHeader;
	private ShowHideHistoryButton showHideHistoryButton;

	public ItineraryGroup() {

	}

	public ItineraryGroup(@NonNull String id, @NonNull String name, int type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getGpxPath() {
		return id;
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

	public void setHeader(GroupHeader header) {
		this.header = header;
	}

	public void setCategoriesSubHeader(CategoriesSubHeader categoriesSubHeader) {
		this.categoriesSubHeader = categoriesSubHeader;
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

	public int getType() {
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

	public GroupHeader getGroupHeader() {
		return header;
	}

	public CategoriesSubHeader getCategoriesSubHeader() {
		return categoriesSubHeader;
	}

	public ShowHideHistoryButton getShowHideHistoryButton() {
		return showHideHistoryButton;
	}

	@Nullable
	public String getWptCategoriesString() {
		if (wptCategories != null) {
			return Algorithms.encodeStringSet(wptCategories);
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
}
