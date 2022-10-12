package net.osmand.plus.configmap;

public abstract class TrackTreeView {
	final public TrackTreeViewType viewType;

	TrackTreeView(TrackTreeViewType viewType){
		this.viewType = viewType;
	}
}

class SortView extends TrackTreeView {

	SortView() {
		super(TrackTreeViewType.SortView);
	}
}

class EmptyOnMapTrackView extends TrackTreeView{

	EmptyOnMapTrackView() {
		super(TrackTreeViewType.EmptyOnMapTrackView);
	}
}

class TrackView extends TrackTreeView{
	Track track;

	TrackView(Track track) {
		super(TrackTreeViewType.TrackView);
		this.track = track;
	}
}

class TrackGroupView extends TrackTreeView{
	TrackGroup group;
	TrackGroupView(TrackGroup group) {
		super(TrackTreeViewType.TrackGroupView);
		this.group = group;
	}
}

enum TrackTreeViewType {
	TrackGroupView,
	EmptyOnMapTrackView,
	SortView,
	TrackView
}