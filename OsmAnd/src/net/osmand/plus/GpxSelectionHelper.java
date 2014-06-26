package net.osmand.plus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.osmand.IProgress;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;
import android.graphics.Bitmap;

public class GpxSelectionHelper {

	private static final String CURRENT_TRACK = "currentTrack";
	private static final String FILE = "file";
	private OsmandApplication app;
	// save into settings
//	public final CommonPreference<Boolean> SHOW_CURRENT_GPX_TRACK = 
//			new BooleanPreference("show_current_gpx_track", false).makeGlobal().cache();
	private List<SelectedGpxFile> selectedGPXFiles = new java.util.concurrent.CopyOnWriteArrayList<SelectedGpxFile>();
	private SavingTrackHelper savingTrackHelper;
	private Runnable uiListener;

	public GpxSelectionHelper(OsmandApplication osmandApplication) {
		this.app = osmandApplication;
		savingTrackHelper = this.app.getSavingTrackHelper();
	}
	
	public void clearAllGpxFileToShow() {
		selectedGPXFiles.clear();
		saveCurrentSelections();
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
			if(s.displayGroups == null) {
				s.displayGroups = new ArrayList<GpxSelectionHelper.GpxDisplayGroup>();
				GPXFile g = s.getGpxFile();
				collectDisplayGroups(s.displayGroups, g);
			}
			dg.addAll(s.displayGroups);
			
		}
		return dg;
	}

	private void collectDisplayGroups(List<GpxDisplayGroup> dg, GPXFile g) {
		String name = g.path;
		if(g.showCurrentTrack){
			name =  getString(R.string.gpx_selection_current_track);
		} else {
			int i = name.lastIndexOf('/');
			if(i >= 0) {
				name = name.substring(i + 1);
			}
			i = name.lastIndexOf('\\');
			if(i >= 0) {
				name = name.substring(i + 1);
			}
			if(name.endsWith(".gpx")) {
				name = name.substring(0, name.length() - 4);
			}
			name = name.replace('_', ' ');
		}
		if (g.tracks.size() > 0) {
			int k = 1;
			for (Track t : g.tracks) {
				GpxDisplayGroup group = new GpxDisplayGroup(g);
				group.color = t.getColor(g.getColor(0));
				group.setType(GpxDisplayItemType.TRACK_SEGMENT);
				group.setTrack(t);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_track, name, g.tracks.size() == 1 ? "" : ks));
				String d = "";
				if(t.name != null && t.name.length() > 0) {
					d = t.name + " " + d;
				}
				group.setDescription(d);
				dg.add(group);
				processGroupTrack(app, group);
			}
		}
		if (g.routes.size() > 0) {
			int k = 0;
			for (Route route : g.routes) {
				GpxDisplayGroup group = new GpxDisplayGroup(g);
				group.setType(GpxDisplayItemType.TRACK_ROUTE_POINTS);
				String d = getString(R.string.gpx_selection_number_of_points, name, route.points.size());
				if(route.name != null && route.name.length() > 0) {
					d = route.name + " " + d;
				}
				group.setDescription(d);
				String ks = (k++) + "";
				group.setName(getString(R.string.gpx_selection_route_points, name, g.routes.size() == 1 ? "" : ks));
				dg.add(group);
				List<GpxDisplayItem> list = group.getModifiableList();
				int t = 0;
				for (WptPt r : route.points) {
					GpxDisplayItem item = new GpxDisplayItem();
					item.group = group;
					item.description = r.desc;
					item.expanded = true;
					item.name = r.name;
					t++;
					if (Algorithms.isEmpty(item.name)) {
						item.name = getString(R.string.gpx_selection_point, t + "");
					}
					item.locationStart = r;
					item.locationEnd = r;
					list.add(item);
				}
			}
		}
		
		if (g.points.size() > 0) {
			GpxDisplayGroup group = new GpxDisplayGroup(g);
			group.setType(GpxDisplayItemType.TRACK_POINTS);
			group.setDescription(getString(R.string.gpx_selection_number_of_points, g.points.size()));
			group.setName(getString(R.string.gpx_selection_points, name));
			dg.add(group);
			List<GpxDisplayItem> list = group.getModifiableList();
			int k = 0;
			for (WptPt r : g.points) {
				GpxDisplayItem item = new GpxDisplayItem();
				item.group = group;
				item.description = r.desc;
				item.name = r.name;
				k++;
				if (Algorithms.isEmpty(item.name)) {
					item.name = getString(R.string.gpx_selection_point, k + "");
				}
				item.expanded = true;	
				item.locationStart = r;
				item.locationEnd = r;
				list.add(item);
			}
		}
	}
	
	private static void processGroupTrack(OsmandApplication app, GpxDisplayGroup group) {
		List<GpxDisplayItem> list = group.getModifiableList();
		String timeSpanClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_time_span_color));
		String speedClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_altitude_desc));
		String distanceClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_distance_color));
		final float eleThreshold = 3;
//		int t = 1;
		for (TrkSegment r : group.track.segments) {
			if (r.points.size() == 0) {
				continue;
			}
			GPXTrackAnalysis[] as ;
			boolean split = true;
			if(group.splitDistance > 0) {
				as = r.splitByDistance(group.splitDistance).toArray(new GPXTrackAnalysis[0]);
			} else if(group.splitTime > 0) {
				as = r.splitByTime(group.splitTime).toArray(new GPXTrackAnalysis[0]);
			} else {
				split = false;
				as = new GPXTrackAnalysis[] {GPXTrackAnalysis.segment(0, r)};
			}
			for(GPXTrackAnalysis analysis : as) {
				GpxDisplayItem item = new GpxDisplayItem();
				item.group = group;
				if(split) {
					item.splitMetric = analysis.metricEnd;
				}
				
				item.description = GpxUiHelper.getDescription(app, analysis, true);
				String name = "";
//				if(group.track.segments.size() > 1) {
//					name += t++ + ". ";
//				}
				if(!group.isSplitDistance()) {
					name += GpxUiHelper.getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
				}
				if ((analysis.timeSpan > 0 || analysis.timeMoving > 0) && !group.isSplitTime()) {
					long tm = analysis.timeMoving;
					if (tm == 0) {
						tm = analysis.timeSpan;
					}
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(timeSpanClr, Algorithms.formatDuration((int) (tm / 1000)));
				}
				if (analysis.isSpeedSpecified()) {
					if (name.length() != 0)
						name += ", ";
					name += GpxUiHelper.getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app));
				}
				if (analysis.isElevationSpecified() && (analysis.diffElevationUp > eleThreshold || 
						analysis.diffElevationDown > eleThreshold) ) {
					if (name.length() != 0)
						name += ", ";
					if(analysis.diffElevationDown > eleThreshold) {
						name += GpxUiHelper.getColorValue(descClr, " \u2193 "+
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app));
					}
					if(analysis.diffElevationUp > eleThreshold) {
						name += GpxUiHelper.getColorValue(ascClr, " \u2191 "+
								OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app));
					}
				}
				item.name = name;
				item.locationStart = analysis.locationStart;
				item.locationEnd = analysis.locationEnd;
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
	
	public SelectedGpxFile getSelectedFileByName(String path) {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.getGpxFile().path.endsWith("/" + path)) {
				return s;
			}
		}
		return null;
	}
	
	public SelectedGpxFile getSelectedCurrentRecordingTrack() {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.isShowCurrentTrack()) {
				return s;
			}
		}
		return null;
	}
	
	public void setGpxFileToDisplay(boolean notShowNavigationDialog, GPXFile... gpxs) {
		// special case for gpx current route
		for(GPXFile gpx : gpxs) {
			boolean show = true;
			selectGpxFileImpl(gpx, show, notShowNavigationDialog);
		}
		saveCurrentSelections();
	}
	
	public void loadGPXTracks(IProgress p) {
		String load = app.getSettings().SELECTED_GPX.get();
		if(!Algorithms.isEmpty(load)) {
			try {
				JSONArray ar = new JSONArray(load);
				for(int i = 0; i < ar.length(); i++) {
					JSONObject obj = ar.getJSONObject(i);
					if(obj.has(FILE)) {
						File fl = new File(obj.getString(FILE));
						if(p != null) {
							p.startTask(getString(R.string.loading_smth, fl.getName()), -1);
						}
						GPXFile gpx = GPXUtilities.loadGPXFile(app, fl);
						selectGpxFile(gpx, true, false);
					} else if(obj.has(CURRENT_TRACK)) {
						selectedGPXFiles.add(savingTrackHelper.getCurrentTrack());
					}
				}
			} catch (JSONException e) {
				app.getSettings().SELECTED_GPX.set("");
				e.printStackTrace();
			}
		}
	}

	private void saveCurrentSelections() {
		JSONArray ar = new JSONArray();
		for(SelectedGpxFile s : selectedGPXFiles) {
			if(s.gpxFile != null && !s.notShowNavigationDialog) {
				JSONObject obj = new JSONObject();
				try {
					if(!Algorithms.isEmpty(s.gpxFile.path)) {
						obj.put(FILE, s.gpxFile.path);
					} else {
						obj.put(CURRENT_TRACK, true);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				ar.put(obj);
			}
		}
		app.getSettings().SELECTED_GPX.set(ar.toString());
	}

	private void selectGpxFileImpl(GPXFile gpx, boolean show, boolean notShowNavigationDialog) {
		boolean displayed = false;
		SelectedGpxFile sf ;
		if(gpx.showCurrentTrack) {
			sf = savingTrackHelper.getCurrentTrack();
			sf.notShowNavigationDialog = notShowNavigationDialog;
			displayed = selectedGPXFiles.contains(sf);
		} else {
			sf = getSelectedFileByPath(gpx.path);
			displayed = sf != null;
			if(show && sf == null) {
				sf = new SelectedGpxFile();
				sf.setGpxFile(gpx);
				sf.notShowNavigationDialog = notShowNavigationDialog;
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
	
	public void selectGpxFile(GPXFile gpx, boolean show, boolean showNavigationDialog) {
		selectGpxFileImpl(gpx, show, showNavigationDialog);
		saveCurrentSelections();
	}
	
	public void setUiListener(Runnable r) {
		this.uiListener = r;
	}
	
	public Runnable getUiListener() {
		return uiListener;
	}
	
	
	public static class SelectedGpxFile {
		public boolean notShowNavigationDialog = false;

		private boolean showCurrentTrack;
		private GPXFile gpxFile;
		private int color;
		private List<List<WptPt>> processedPointsToDisplay = new ArrayList<List<WptPt>>();
		private List<GpxDisplayGroup> displayGroups = null;
		private boolean routePoints;
		
		public void setGpxFile(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
			this.processedPointsToDisplay = gpxFile.proccessPoints();
			if(this.processedPointsToDisplay.isEmpty()) {
				this.processedPointsToDisplay = gpxFile.processRoutePoints();
				routePoints = true;
			}
		}
		
		public boolean isRoutePoints() {
			return routePoints;
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
		private String name;
		private String description;
		private Track track;
		private double splitDistance = -1;
		private int splitTime = -1;
		private int color;
		
		public GpxDisplayGroup(GPXFile gpx) {
			this.gpx = gpx;
		}

		public void setTrack(Track track) {
			this.track = track;
		}
		
		public GPXFile getGpx() {
			return gpx;
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

		public boolean isSplitDistance() {
			return splitDistance > 0;
		}
		
		public double getSplitDistance() {
			return splitDistance;
		}
		
		public boolean isSplitTime() {
			return splitTime > 0;
		}
		
		public int getSplitTime() {
			return splitTime;
		}

		public String getGroupName() {
			return name;
		}

		public void noSplit(OsmandApplication app) {
			list.clear();
			splitDistance = -1;
			splitTime = -1;
			processGroupTrack(app, this	);
		}

		public void splitByDistance(OsmandApplication app, double meters) {
			list.clear();
			splitDistance = meters;
			splitTime = -1;
			processGroupTrack(app, this	);			
		}

		public void splitByTime(OsmandApplication app, int seconds) {
			list.clear();
			splitDistance = -1;
			splitTime = seconds;
			processGroupTrack(app, this	);			
		}

		public int getColor() {
			return color;
		}
	}
	
	public static class GpxDisplayItem {
		
		public GpxDisplayGroup group;
		public WptPt locationStart;
		public WptPt locationEnd;
		public double splitMetric = -1;
		public String name;
		public String description;
		public String url;
		public Bitmap image;
		public boolean expanded;
		
		
	}



}
