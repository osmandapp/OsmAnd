package net.osmand.plus.track;

import androidx.annotation.NonNull;

public class GpxSelectionParams {

	private boolean showOnMap;
	private boolean addToMarkers;
	private boolean syncGroup;
	private boolean notShowNavigationDialog;
	private boolean selectedByUser;
	private boolean addToHistory;
	private boolean saveSelection;
	private boolean updateVisibilityOnMap;
	private boolean selectedByUserChanged;

	private GpxSelectionParams() {
	}

	public boolean isShowOnMap() {
		return showOnMap;
	}

	public boolean shouldUpdateSelected() {
		return updateVisibilityOnMap;
	}

	public boolean isSelectedByUserChanged() {
		return selectedByUserChanged;
	}

	public boolean isAddToMarkers() {
		return addToMarkers;
	}

	public boolean isSyncGroup() {
		return syncGroup;
	}

	public boolean isNotShowNavigationDialog() {
		return notShowNavigationDialog;
	}

	public boolean isSelectedByUser() {
		return selectedByUser;
	}

	public boolean isAddToHistory() {
		return addToHistory;
	}

	public boolean isSaveSelection() {
		return saveSelection;
	}


	public GpxSelectionParams showOnMap() {
		showOnMap = true;
		updateVisibilityOnMap = true;
		return this;
	}

	public GpxSelectionParams hideFromMap() {
		showOnMap = false;
		updateVisibilityOnMap = true;
		return this;
	}

	public GpxSelectionParams addToMarkers() {
		addToMarkers = true;
		return this;
	}

	public GpxSelectionParams syncGroup() {
		syncGroup = true;
		return this;
	}

	public GpxSelectionParams notShowNavigationDialog() {
		notShowNavigationDialog = true;
		return this;
	}

	public GpxSelectionParams selectedByUser() {
		return setSelectedByUser(true);
	}

	public GpxSelectionParams selectedAutomatically() {
		return setSelectedByUser(false);
	}

	public GpxSelectionParams setSelectedByUser(boolean selectedByUser) {
		this.selectedByUser = selectedByUser;
		selectedByUserChanged = true;
		return this;
	}

	public GpxSelectionParams addToHistory() {
		addToHistory = true;
		return this;
	}

	public GpxSelectionParams saveSelection() {
		saveSelection = true;
		return this;
	}

	@NonNull
	public static GpxSelectionParams newInstance() {
		return new GpxSelectionParams();
	}

	@NonNull
	public static GpxSelectionParams getDefaultSelectionParams() {
		return newInstance().showOnMap().selectedByUser().syncGroup().addToHistory().addToMarkers().saveSelection();
	}
}
