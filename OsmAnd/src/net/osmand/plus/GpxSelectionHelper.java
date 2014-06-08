package net.osmand.plus;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.activities.SavingTrackHelper;
import android.content.Context;
import android.graphics.Bitmap;

public class GpxSelectionHelper {

	private OsmandApplication app;
	// save into settings
//	public final CommonPreference<Boolean> SHOW_CURRENT_GPX_TRACK = 
//			new BooleanPreference("show_current_gpx_track", false).makeGlobal().cache();
	private List<SelectedGpxFile> selectedGPXFiles = new java.util.concurrent.CopyOnWriteArrayList<SelectedGpxFile>();
	private SavingTrackHelper savingTrackHelper;

	public GpxSelectionHelper(OsmandApplication osmandApplication) {
		this.app = osmandApplication;
		savingTrackHelper = this.app.getSavingTrackHelper();
	}
	
	public void clearAllGpxFileToShow() {
		selectedGPXFiles.clear();
	}
	
	public boolean isShowingAnyGpxFiles() {
		return !selectedGPXFiles.isEmpty();
	}
	
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}
	
	public final String getString(int resId, Object... formatArgs) {
		return app.getString(resId, formatArgs);
	}
	
	public List<GpxDisplayGroup> getDisplayGroups() {
		List<GpxDisplayGroup> dg = new ArrayList<GpxSelectionHelper.GpxDisplayGroup>();
		for(SelectedGpxFile s : selectedGPXFiles) {
			GPXFile g = s.getGpxFile();
			collectDisplayGroups(dg, g);
			
		}
		return dg;
	}

	private void collectDisplayGroups(List<GpxDisplayGroup> dg, GPXFile g) {
		String name = g.path;
		if(g.showCurrentTrack){
			name =  getString(R.string.gpx_selection_current_track);
		} else {
			int i = name.indexOf('/');
			if(i >= 0) {
				name = name.substring(i + 1);
			}
			if(name.endsWith(".gpx")) {
				name = name.substring(0, name.length() - 4);
			}
		}
		if (g.tracks.size() > 0) {
			int k = 0;
			for (Track t : g.tracks) {
				GpxDisplayGroup group = new GpxDisplayGroup(g);
				group.setType(GpxDisplayItemType.TRACK_POINTS);
				group.setTrack(t);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_track, name, g.tracks.size() == 1 ? "" : ks));
				String d = "";
				if(t.name != null && t.name.length() > 0) {
					d = t.name + " " + d;
				}
				group.setDescription(d);
				dg.add(group);
			}
		}
		if (g.routes.size() > 0) {
			int k = 0;
			for (Route r : g.routes) {
				GpxDisplayGroup group = new GpxDisplayGroup(g);
				group.setType(GpxDisplayItemType.TRACK_ROUTE_POINTS);
				String d = getString(R.string.gpx_selection_number_of_points, r.points.size());
				if(r.name != null && r.name.length() > 0) {
					d = r.name + " " + d;
				}
				group.setDescription(d);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_route_points, name, g.routes.size() == 1 ? "" : ks));
				dg.add(group);
			}
		}
		
		if (g.points.size() > 0) {
			GpxDisplayGroup group = new GpxDisplayGroup(g);
			group.setType(GpxDisplayItemType.TRACK_POINTS);
			group.setDescription(getString(R.string.gpx_selection_number_of_points, g.points.size()));
			group.setName(getString(R.string.gpx_selection_points));
			dg.add(group);
			List<GpxDisplayItem> list = group.getModifiableList();
			for (WptPt r : g.points) {
				GpxDisplayItem item = new GpxDisplayItem();
				item.description = r.desc;
				item.name = r.name;
				item.locationStart = r;
				item.locationEnd = r;
				list.add(item);
			}
		}
	}
	
	public SelectedGpxFile getSelectedFileByPath(String path) {
		for(SelectedGpxFile s : selectedGPXFiles) {
			if(s.getGpxFile().path.equals(path)) {
				return s;
			}
		}
		return null;
	}
	
	public void setGpxFileToDisplay(GPXFile... gpxs) {
		// special case for gpx current route
		for(GPXFile gpx : gpxs) {
			boolean show = true;
			selectGpxFileImpl(gpx, show);
		}
		saveCurrentSelections();
	}

	private void saveCurrentSelections() {
//		TODO;
	}

	private void selectGpxFileImpl(GPXFile gpx, boolean show) {
		boolean displayed = false;
		SelectedGpxFile sf ;
		if(gpx.showCurrentTrack) {
			sf = savingTrackHelper.getCurrentTrack();
			displayed = selectedGPXFiles.contains(sf);
		} else {
			sf = getSelectedFileByPath(gpx.path);
			displayed = sf != null;
			if(show && sf == null) {
				sf = new SelectedGpxFile();
				sf.setGpxFile(gpx);
			}
		}
		if(displayed != show) {
			if(show) {
				selectedGPXFiles.add(sf);
			} else {
				selectedGPXFiles.remove(sf);
			}
		}
	}
	
	public void selectGpxFile(GPXFile gpx, boolean show) {
		selectGpxFileImpl(gpx, show);
		saveCurrentSelections();
	}
	
	public static class SelectedGpxFile {
		private boolean showCurrentTrack;
		private GPXFile gpxFile;
		private int color;
		private List<List<WptPt>> processedPointsToDisplay = new ArrayList<List<WptPt>>();
		
		public void setGpxFile(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
			this.processedPointsToDisplay = gpxFile.proccessPoints();
		}
		
		public List<List<WptPt>> getPointsToDisplay() {
			return processedPointsToDisplay;
		}
		
		public List<List<WptPt>> getModifiablePointsToDisplay() {
			return processedPointsToDisplay;
		}
		
		public GPXFile getGpxFile() {
			return gpxFile;
		}
		
		public boolean isShowCurrentTrack() {
			return showCurrentTrack;
		}
		
		public void setShowCurrentTrack(boolean showCurrentTrack) {
			this.showCurrentTrack = showCurrentTrack;
		}

		public int getColor() {
			return color;
		}
	}
	
	public enum GpxDisplayItemType {
		TRACK_SEGMENT,
		TRACK_POINTS,
		TRACK_ROUTE_POINTS
	}
	
	public static class GpxDisplayGroup {
		
		private GpxDisplayItemType type = GpxDisplayItemType.TRACK_SEGMENT;
		private List<GpxDisplayItem> list = new ArrayList<GpxDisplayItem>();
		private GPXFile gpx;
		private Track track;
		private String name;
		private String description;
		
		public GpxDisplayGroup(GPXFile gpx) {
			this.gpx = gpx;
		}

		public void setTrack(Track track) {
			this.track = track;
		}
		
		public Track getTrack() {
			return track;
		}

		public GpxDisplayGroup cloneInstance() {
			GpxDisplayGroup group = new GpxDisplayGroup(gpx);
			group.type = type;
			group.name = name;
			group.description = description;
			group.track = track;
			group.list = new ArrayList<GpxSelectionHelper.GpxDisplayItem>(list);
			return group;
		}
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public List<GpxDisplayItem> getModifiableList() {
			return list;
		}
		
		public GpxDisplayItemType getType() {
			return type;
		}
		
		public void setType(GpxDisplayItemType type) {
			this.type = type;
		}


		public String getGroupName(Context ctx) {
			return name;
		}
	}
	
	public static class GpxDisplayItem {
		
		public WptPt locationStart;
		public WptPt locationEnd;
		public String name;
		public String description;
		public String url;
		public Bitmap image;
		
		
	}


}
