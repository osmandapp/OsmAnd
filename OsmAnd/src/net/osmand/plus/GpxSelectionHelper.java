package net.osmand.plus;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import android.content.Context;
import android.graphics.Bitmap;

public class GpxSelectionHelper {

	private OsmandApplication app;
	// save into settings
//	public final CommonPreference<Boolean> SHOW_CURRENT_GPX_TRACK = 
//			new BooleanPreference("show_current_gpx_track", false).makeGlobal().cache();
	private List<SelectedGpxFile> selectedGPXFiles = new java.util.concurrent.CopyOnWriteArrayList<SelectedGpxFile>();

	public GpxSelectionHelper(OsmandApplication osmandApplication) {
		this.app = osmandApplication;
	}
	
	public void clearAllGpxFileToShow() {
		selectedGPXFiles.clear();
	}
	
	public boolean isShowingCurrentTrack() {
		return getCurrentTrack() != null;
	}
	
	public boolean isShowingAnyGpxFiles() {
		return !selectedGPXFiles.isEmpty();
	}
	
	public SelectedGpxFile getCurrentTrack() {
		for(SelectedGpxFile s : selectedGPXFiles) {
			if(s.isShowCurrentTrack()) {
				return s;
			}
		}
		return null;
	}
	
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}
	
	public void setToDisplayCurrentGpxFile(boolean show) {
		if(show != isShowingCurrentTrack()) {
			if(show) {
				TODO_LOAD_CURRENT_BUFFER ;
				SelectedGpxFile sg = new SelectedGpxFile();
				sg.setGpxFile(new GPXFile());
				sg.setShowCurrentTrack(true);
				selectedGPXFiles.add(sg);
			} else {
				Iterator<SelectedGpxFile> it = selectedGPXFiles.iterator();
				while(it.hasNext()) {
					if(it.next().isShowCurrentTrack()) {
						it.remove();
						break;
					}
				}
			}
		}
		TODO_SAVE_IN_SETTINGS;
	}
	public List<GpxDisplayGroup> getDisplayGroups() {
		TODO;
	}
	
	public SelectedGpxFile getSelectedFileByPath(String path) {
		for(SelectedGpxFile s : selectedGPXFiles) {
			if(s.getGpxFile().path.equals(path)) {
				return s;
			}
		}
		return null;
	}
	
	public SelectedGpxFile setGpxFileToDisplay(GPXFile... gpx) {
		// special case for gpx current route
		SelectedGpxFile sf = getSelectedFileByPath(gpx.path);
		boolean displayed = sf != null;
		if(displayed != show) {
			if(show) {
				sf = new SelectedGpxFile();
				sf.setGpxFile(gpx);
				selectedGPXFiles.add(sf);
			} else {
				selectedGPXFiles.remove(sf);
			}
		}
		return sf;
	}
	
	public SelectedGpxFile setGpxFileToDisplay(String path, boolean show) {
		SelectedGpxFile sf = getSelectedFileByPath(path);
		boolean displayed = sf != null;
		if(displayed != show) {
			if(show) {
				TODO_ASYNC;
				GPXFile r = GPXUtilities.loadGPXFile(app, new File(path));
				sf = new SelectedGpxFile();
				sf.setGpxFile(r);
				selectedGPXFiles.add(sf);
			} else {
				selectedGPXFiles.remove(sf);
			}
		}
		return sf;
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
		
		public GpxDisplayGroup cloneInstance() {
			GpxDisplayGroup group = new GpxDisplayGroup();
			group.type = type;
			group.list = new ArrayList<GpxSelectionHelper.GpxDisplayItem>(list);
			return group;
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
			return TODO;
		}
	}
	
	public static class GpxDisplayItem {
		
		private boolean filter;
		private String description;
		private Bitmap image;
		private String url;
		
		public boolean isFilter() {
			return filter;
		}
		
		public void setFilter(boolean filter) {
			this.filter = filter;
		}
	}


}
