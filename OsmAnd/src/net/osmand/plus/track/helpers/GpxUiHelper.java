package net.osmand.plus.track.helpers;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.GPX_RECORDED_INDEX_DIR;
import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.gpx.GpxParameter.SPLIT_TYPE;
import static net.osmand.gpx.GpxParameter.WIDTH;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;
import static net.osmand.util.Algorithms.formatDuration;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.SelectGpxTrackBottomSheet;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.SplitTrackAsyncTask;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilter;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class GpxUiHelper {

	private static final Log LOG = PlatformUtil.getLog(GpxUiHelper.class);


	public static String getColorValue(String clr, String value, boolean html) {
		if (!html) {
			return value;
		}
		return "<font color=\"" + clr + "\">" + value + "</font>";
	}

	public static String getColorValue(String clr, String value) {
		return getColorValue(clr, value, true);
	}

	public static String getDescription(OsmandApplication app, GPXTrackAnalysis analysis, boolean html) {
		StringBuilder description = new StringBuilder();
		String nl = html ? "<br/>" : "\n";
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		// OUTPUT:
		// 1. Total distance, Start time, End time
		description.append(app.getString(R.string.gpx_info_distance, getColorValue(distanceClr,
						OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app), html),
				getColorValue(distanceClr, analysis.getPoints() + "", html)));
		if (analysis.getTotalTracks() > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.getTotalTracks() + "", html)));
		}
		if (analysis.getWptPoints() > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.getWptPoints() + "", html)));
		}
		if (analysis.isTimeSpecified()) {
			description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.getStartTime()));
			description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.getEndTime()));
		}

		// 2. Time span
		if (analysis.getDurationInMs() > 0 && analysis.getDurationInMs() != analysis.getTimeMoving()) {
			String formatDuration = Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if (analysis.isTimeMoving()) {
			//Next few lines for Issue 3222 heuristic testing only
			//final String formatDuration0 = Algorithms.formatDuration((int) (analysis.timeMoving0 / 1000.0f + 0.5), app.accessibilityEnabled());
			//description.append(nl).append(app.getString(R.string.gpx_timemoving,
			//		getColorValue(timeSpanClr, formatDuration0, html)));
			//description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving0, app), html) + ")");
			String formatDuration = Algorithms.formatDuration((int) (analysis.getTimeMoving() / 1000.0f + 0.5), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
			description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.getTotalDistanceMoving(), app), html) + ")");
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if (analysis.isElevationSpecified()) {
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_avg_altitude,
					getColorValue(speedClr, OsmAndFormatter.getFormattedAlt(analysis.getAvgElevation(), app), html)));
			description.append(nl);
			String min = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.getMinElevation(), app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.getMaxElevation(), app), html);
			String asc = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationUp(), app), html);
			String desc = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.getDiffElevationDown(), app), html);
			description.append(app.getString(R.string.gpx_info_diff_altitude, min + " - " + max));
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_asc_altitude, "\u2193 " + desc + "   \u2191 " + asc + ""));
		}


		if (analysis.isSpeedSpecified()) {
			String avg = getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.getAvgSpeed(), app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedSpeed(analysis.getMaxSpeed(), app), html);
			description.append(nl).append(app.getString(R.string.gpx_info_average_speed, avg));
			description.append(nl).append(app.getString(R.string.gpx_info_maximum_speed, max));
		}
		return description.toString();
	}

	public static void selectSingleGPXFile(FragmentActivity activity, boolean showCurrentGpx,
	                                       CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		int gpxDirLength = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath().length();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		List<GPXInfo> list = new ArrayList<>(selectedGpxFiles.size() + 1);
		if (!PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			showCurrentGpx = false;
		}
		if (!selectedGpxFiles.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(new GPXInfo(activity.getString(R.string.shared_string_currently_recording_track), null));
			}

			for (SelectedGpxFile selectedGpx : selectedGpxFiles) {
				GPXFile gpxFile = selectedGpx.getGpxFile();
				if (!gpxFile.showCurrentTrack && gpxFile.path.length() > gpxDirLength + 1) {
					list.add(new GPXInfo(gpxFile.path.substring(gpxDirLength + 1), new File(gpxFile.path)));
				}
			}
			SelectGpxTrackBottomSheet.showInstance(activity.getSupportFragmentManager(), showCurrentGpx, callbackWithObject, list);
		}
	}

	@NonNull
	public static String getFolderName(@NonNull Context context, @NonNull File dir, boolean includeParentDir) {
		String name = dir.getName();
		if (GPX_INDEX_DIR.equals(name + File.separator)) {
			return context.getString(R.string.shared_string_tracks);
		}
		String dirPath = dir.getPath() + File.separator;
		if (dirPath.endsWith(GPX_IMPORT_DIR) || dirPath.endsWith(GPX_RECORDED_INDEX_DIR)) {
			return Algorithms.capitalizeFirstLetter(name);
		}
		if (includeParentDir) {
			File parent = dir.getParentFile();
			String parentName = parent != null ? parent.getName() : "";
			if (!Algorithms.isEmpty(parentName) && !GPX_INDEX_DIR.equals(parentName + File.separator)) {
				name = parentName + File.separator + name;
			}
			return name;
		}
		return name;
	}

	@NonNull
	public static String getFolderDescription(@NonNull OsmandApplication app, @NonNull TrackFolder folder) {
		long lastModified = folder.getLastModified();
		int tracksCount = folder.getFlattenedTrackItems().size();

		String empty = app.getString(R.string.shared_string_empty);
		String numberOfTracks = tracksCount > 0 ? app.getString(R.string.number_of_tracks, String.valueOf(tracksCount)) : empty;
		if (lastModified > 0) {
			String formattedDate = OsmAndFormatter.getFormattedDate(app, lastModified);
			return app.getString(R.string.ltr_or_rtl_combine_via_comma, formattedDate, numberOfTracks);
		}
		return numberOfTracks;
	}

	@NonNull
	public static String getGpxTitle(@Nullable String name) {
		return name != null ? Algorithms.getFileNameWithoutExtension(name) : "";
	}

	@NonNull
	public static String getGpxDirTitle(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return "";
		}
		return Algorithms.capitalizeFirstLetter(Algorithms.getFileNameWithoutExtension(name));
	}

	public static void updateGpxInfoView(@NonNull OsmandApplication app,
	                                     @NonNull View view,
	                                     @NonNull String itemTitle,
	                                     @Nullable Drawable iconDrawable,
	                                     @Nullable GPXInfo info) {
		if (info != null) {
			GpxDataItem item = getDataItem(app, info, dataItem -> updateGpxInfoView(app, view, itemTitle, iconDrawable, info, dataItem));
			if (item != null) {
				updateGpxInfoView(app, view, itemTitle, iconDrawable, info, item);
			}
		} else {
			updateGpxInfoView(view, itemTitle, null, null, app);
			if (iconDrawable != null) {
				ImageView icon = view.findViewById(R.id.icon);
				icon.setImageDrawable(iconDrawable);
				icon.setVisibility(View.VISIBLE);
			}
		}
	}

	private static void updateGpxInfoView(@NonNull OsmandApplication app,
	                                      @NonNull View view,
	                                      @NonNull String itemTitle,
	                                      @Nullable Drawable iconDrawable,
	                                      @NonNull GPXInfo info,
	                                      @NonNull GpxDataItem dataItem) {
		updateGpxInfoView(view, itemTitle, info, dataItem.getAnalysis(), app);
		if (iconDrawable != null) {
			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(iconDrawable);
			icon.setVisibility(View.VISIBLE);
		}
	}

	public static void updateGpxInfoView(@NonNull View v,
	                                     @NonNull String itemTitle,
	                                     @Nullable GPXInfo gpxInfo,
	                                     @Nullable GPXTrackAnalysis analysis,
	                                     @NonNull OsmandApplication app) {
		TextView viewName = v.findViewById(R.id.name);
		viewName.setText(itemTitle.replace("/", " • ").trim());
		viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		ImageView icon = v.findViewById(R.id.icon);
		icon.setVisibility(View.GONE);

		boolean hasGPXInfo = gpxInfo != null;
		boolean hasAnalysis = analysis != null;
		if (hasAnalysis) {
			ImageView distanceI = v.findViewById(R.id.distance_icon);
			distanceI.setVisibility(View.VISIBLE);
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsI = v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeI = v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_16));
			TextView time = v.findViewById(R.id.time);
			TextView distance = v.findViewById(R.id.distance);
			TextView pointsCount = v.findViewById(R.id.points_count);
			pointsCount.setText(String.valueOf(analysis.getWptPoints()));
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

			if (analysis.isTimeSpecified()) {
				time.setText(Algorithms.formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
			} else {
				time.setText("");
			}
		} else if (hasGPXInfo) {
			String date = "";
			String size = "";
			if (gpxInfo.getFileSize() >= 0) {
				size = AndroidUtils.formatSize(v.getContext(), gpxInfo.getFileSize());
			}
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			long lastModified = gpxInfo.getLastModified();
			if (lastModified > 0) {
				date = (format.format(new Date(lastModified)));
			}
			TextView sizeText = v.findViewById(R.id.date_and_size_details);
			sizeText.setText(date + " • " + size);
		}
		AndroidUiHelper.updateVisibility(v.findViewById(R.id.check_item), false);
		AndroidUiHelper.updateVisibility(v.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(v.findViewById(R.id.read_section), hasAnalysis);
		AndroidUiHelper.updateVisibility(v.findViewById(R.id.unknown_section), !hasAnalysis && hasGPXInfo);
	}

	private static GpxDataItem getDataItem(@NonNull OsmandApplication app,
	                                       @NonNull GPXInfo info,
	                                       @Nullable GpxDataItemCallback callback) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		String fileName = info.getFileName();
		File file = new File(dir, fileName);
		return app.getGpxDbHelper().getItem(file, callback);
	}

	@NonNull
	public static List<String> getSelectedTrackPaths(OsmandApplication app) {
		List<String> trackNames = new ArrayList<>();
		for (SelectedGpxFile file : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			trackNames.add(file.getGpxFile().path);
		}
		return trackNames;
	}

	public static List<GPXInfo> getSortedGPXFilesInfoByDate(File dir, boolean absolutePath) {
		List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		Collections.sort(list, (object1, object2) -> {
			long lhs = object1.getLastModified();
			long rhs = object2.getLastModified();
			return Long.compare(rhs, lhs);
		});
		return list;
	}

	@Nullable
	public static GPXInfo getGpxInfoByFileName(@NonNull OsmandApplication app, @NonNull String fileName) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		File file = new File(dir, fileName);
		if (file.exists() && isGpxFile(file)) {
			return new GPXInfo(fileName, file);
		}
		return null;
	}

	@NonNull
	public static List<GPXInfo> getGPXFiles(@NonNull File dir, boolean absolutePath) {
		return getGPXFiles(dir, absolutePath, true);
	}

	@NonNull
	public static List<GPXInfo> getGPXFiles(@NonNull File dir, boolean absolutePath, boolean includeSubFolders) {
		List<GPXInfo> gpxInfos = new ArrayList<>();
		readGpxDirectory(dir, gpxInfos, "", absolutePath, includeSubFolders);
		return gpxInfos;
	}

	@NonNull
	public static List<GPXInfo> getSortedGPXFilesInfo(File dir, List<String> selectedGpxList, boolean absolutePath) {
		List<GPXInfo> allGpxFiles = new ArrayList<>();
		readGpxDirectory(dir, allGpxFiles, "", absolutePath);
		if (selectedGpxList != null) {
			for (GPXInfo gpxInfo : allGpxFiles) {
				for (String selectedGpxName : selectedGpxList) {
					if (selectedGpxName.endsWith(gpxInfo.getFileName())) {
						gpxInfo.setSelected(true);
						break;
					}
				}
			}
		}
		Collections.sort(allGpxFiles, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				int res = i1.isSelected() == i2.isSelected() ? 0 : i1.isSelected() ? -1 : 1;
				if (res != 0) {
					return res;
				}

				String name1 = i1.getFileName();
				String name2 = i2.getFileName();
				int d1 = depth(name1);
				int d2 = depth(name2);
				if (d1 != d2) {
					return d1 - d2;
				}
				int lastSame = 0;
				for (int i = 0; i < name1.length() && i < name2.length(); i++) {
					if (name1.charAt(i) != name2.charAt(i)) {
						break;
					}
					if (name1.charAt(i) == '/') {
						lastSame = i + 1;
					}
				}

				boolean isDigitStarts1 = isLastSameStartsWithDigit(name1, lastSame);
				boolean isDigitStarts2 = isLastSameStartsWithDigit(name2, lastSame);
				res = isDigitStarts1 == isDigitStarts2 ? 0 : isDigitStarts1 ? -1 : 1;
				if (res != 0) {
					return res;
				}
				if (isDigitStarts1) {
					return -name1.compareToIgnoreCase(name2);
				}
				return name1.compareToIgnoreCase(name2);
			}

			private int depth(String name1) {
				int d = 0;
				for (int i = 0; i < name1.length(); i++) {
					if (name1.charAt(i) == '/') {
						d++;
					}
				}
				return d;
			}

			private boolean isLastSameStartsWithDigit(String name1, int lastSame) {
				if (name1.length() > lastSame) {
					return Character.isDigit(name1.charAt(lastSame));
				}

				return false;
			}
		});
		return allGpxFiles;
	}

	public static void readGpxDirectory(@Nullable File dir, @NonNull List<GPXInfo> list,
	                                    @NonNull String parent, boolean absolutePath) {
		readGpxDirectory(dir, list, parent, absolutePath, true);
	}

	public static void readGpxDirectory(@Nullable File dir, @NonNull List<GPXInfo> list,
	                                    @NonNull String parent, boolean absolutePath, boolean includeSubFolders) {//
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					if (isGpxFile(file)) {
						String fileName = absolutePath ? file.getAbsolutePath() : parent + name;
						list.add(new GPXInfo(fileName, file));
					} else if (file.isDirectory() && includeSubFolders) {
						readGpxDirectory(file, list, parent + name + "/", absolutePath);
					}
				}
			}
		}
	}

	public static void loadGPXFileInDifferentThread(Activity activity, CallbackWithObject<GPXFile[]> callback,
	                                                File dir, GPXFile currentFile, String... filename) {
		ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading_smth, ""),
				activity.getString(R.string.loading_data));
		new Thread(() -> {
			GPXFile[] result = new GPXFile[filename.length + (currentFile == null ? 0 : 1)];
			int k = 0;
			StringBuilder builder = new StringBuilder();
			if (currentFile != null) {
				result[k++] = currentFile;
			}
			for (String name : filename) {
				File file = new File(dir, name);
				GPXFile gpxFile = GPXUtilities.loadGPXFile(file);
				if (gpxFile.error != null && !Algorithms.isEmpty(gpxFile.error.getMessage())) {
					builder.append(gpxFile.error.getMessage()).append("\n");
				} else {
					gpxFile.addGeneralTrack();
				}
				result[k++] = gpxFile;
			}
			dlg.dismiss();
			String warn = builder.toString();
			activity.runOnUiThread(() -> {
				if (warn.length() > 0) {
					Toast.makeText(activity, warn, Toast.LENGTH_LONG).show();
				} else {
					callback.processResult(result);
				}
			});
		}, "Loading gpx").start();
	}

	@NonNull
	public static GPXFile makeGpxFromRoute(RouteCalculationResult route, OsmandApplication app) {
		return makeGpxFromLocations(route.getRouteLocations(), app);
	}

	@NonNull
	public static GPXFile makeGpxFromLocations(List<Location> locations, OsmandApplication app) {
		double lastHeight = HEIGHT_UNDEFINED;
		double lastValidHeight = Double.NaN;
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		if (locations != null) {
			Track track = new Track();
			TrkSegment seg = new TrkSegment();
			List<WptPt> pts = seg.points;
			for (Location l : locations) {
				WptPt point = new WptPt();
				point.lat = l.getLatitude();
				point.lon = l.getLongitude();
				if (l.hasAltitude()) {
					gpx.hasAltitude = true;
					float h = (float) l.getAltitude();
					point.ele = h;
					lastValidHeight = h;
					if (lastHeight == HEIGHT_UNDEFINED && pts.size() > 0) {
						for (WptPt pt : pts) {
							if (Double.isNaN(pt.ele)) {
								pt.ele = h;
							}
						}
					}
					lastHeight = h;
				} else {
					lastHeight = HEIGHT_UNDEFINED;
				}
				if (pts.size() == 0) {
					if (l.hasSpeed() && l.getSpeed() > 0) {
						point.speed = l.getSpeed();
					}
					point.time = System.currentTimeMillis();
				} else {
					GPXUtilities.WptPt prevPoint = pts.get(pts.size() - 1);
					if (l.hasSpeed() && l.getSpeed() > 0) {
						point.speed = l.getSpeed();
						double dist = MapUtils.getDistance(prevPoint.lat, prevPoint.lon, point.lat, point.lon);
						point.time = prevPoint.time + (long) (dist / point.speed * SECOND_IN_MILLIS);
					} else {
						point.time = prevPoint.time;
					}
				}
				pts.add(point);
			}
			if (!Double.isNaN(lastValidHeight) && lastHeight == HEIGHT_UNDEFINED) {
				for (ListIterator<WptPt> iterator = pts.listIterator(pts.size()); iterator.hasPrevious(); ) {
					WptPt point = iterator.previous();
					if (!Double.isNaN(point.ele)) {
						break;
					}
					point.ele = lastValidHeight;
				}
			}
			track.segments.add(seg);
			gpx.tracks.add(track);
		}
		return gpx;
	}

	@Nullable
	public static GpxDisplayItem makeGpxDisplayItem(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                                @NonNull ChartPointLayer chartPointLayer, @Nullable GPXTrackAnalysis analysis) {
		TrackDisplayGroup group;
		if (!Algorithms.isEmpty(gpxFile.tracks)) {
			group = app.getGpxDisplayHelper().buildTrackDisplayGroup(gpxFile);
			if (analysis == null) {
				SplitTrackAsyncTask.processGroupTrack(app, group, null, false);
				if (!Algorithms.isEmpty(group.getDisplayItems())) {
					GpxDisplayItem gpxItem = group.getDisplayItems().get(0);
					if (gpxItem != null) {
						gpxItem.chartPointLayer = chartPointLayer;
					}
					return gpxItem;
				}
			} else {
				List<TrkSegment> segments = gpxFile.getSegments(true);
				if (!Algorithms.isEmpty(segments)) {
					GpxDisplayItem gpxItem = SplitTrackAsyncTask.createGpxDisplayItem(app, group, segments.get(0), analysis);
					gpxItem.chartPointLayer = chartPointLayer;
					return gpxItem;
				}
			}
		}
		return null;
	}

	public static void saveAndShareGpx(@NonNull Context context, @NonNull GPXFile gpxFile) {
		File file = getGpxTempFile(context, gpxFile);
		SaveGpxHelper.saveGpx(file, gpxFile, errorMessage -> {
			if (errorMessage == null) {
				shareGpx(context, file);
			}
		});
	}

	@NonNull
	public static File getGpxTempFile(@NonNull Context context, @NonNull GPXFile gpxFile) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		String fileName = Algorithms.getFileWithoutDirs(gpxFile.path);
		return new File(FileUtils.getTempDir(app), fileName);
	}

	public static void saveAndShareCurrentGpx(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		SaveGpxHelper.saveCurrentTrack(app, gpxFile, errorMessage -> {
			if (errorMessage == null) {
				shareGpx(app, new File(gpxFile.path));
			}
		});
	}

	public static void saveAndShareGpxWithAppearance(@NonNull Context context, @NonNull GPXFile gpxFile) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		GpxDataItem dataItem = getDataItem(app, gpxFile);
		if (dataItem != null) {
			addAppearanceToGpx(gpxFile, dataItem);
			saveAndShareGpx(app, gpxFile);
		}
	}

	public static void saveAndOpenGpx(@NonNull MapActivity mapActivity,
	                                  @NonNull File file,
	                                  @NonNull GPXFile gpxFile,
	                                  @NonNull WptPt selectedPoint,
	                                  @Nullable GPXTrackAnalysis analyses,
	                                  @Nullable RouteKey routeKey) {
		SaveGpxHelper.saveGpx(file, gpxFile, errorMessage -> {
			if (errorMessage == null) {
				OsmandApplication app = mapActivity.getMyApplication();
				GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
				GPXTrackAnalysis trackAnalysis = analyses != null ? analyses : selectedGpxFile.getTrackAnalysis(app);
				SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(selectedGpxFile, selectedPoint);
				Bundle bundle = new Bundle();
				bundle.putBoolean(TrackMenuFragment.ADJUST_MAP_POSITION, false);
				TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint,
						trackAnalysis, routeKey, bundle);
			} else {
				LOG.error(errorMessage);
			}
		});
	}

	private static GpxDataItem getDataItem(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		GpxDataItemCallback callback = item -> {
			addAppearanceToGpx(gpxFile, item);
			saveAndShareGpx(app, gpxFile);
		};
		return app.getGpxDbHelper().getItem(new File(gpxFile.path), callback);
	}

	private static void addAppearanceToGpx(@NonNull GPXFile gpxFile, @NonNull GpxDataItem dataItem) {
		gpxFile.setShowArrows(dataItem.getParameter(SHOW_ARROWS));
		gpxFile.setShowStartFinish(dataItem.getParameter(SHOW_START_FINISH));
		gpxFile.setSplitInterval(dataItem.getParameter(SPLIT_INTERVAL));
		gpxFile.setSplitType(GpxSplitType.getSplitTypeByTypeId(dataItem.getParameter(SPLIT_TYPE)).getTypeName());

		int color = dataItem.getParameter(COLOR);
		if (color != 0) {
			gpxFile.setColor(color);
		}
		String width = dataItem.getParameter(WIDTH);
		if (width != null) {
			gpxFile.setWidth(width);
		}
		String coloringType = dataItem.getParameter(COLORING_TYPE);
		if (coloringType != null) {
			gpxFile.setColoringType(coloringType);
		}
		GpsFilter.writeValidFilterValuesToExtensions(gpxFile.getExtensionsToWrite(), dataItem);
	}

	public static void shareGpx(@NonNull Context context, @NonNull File file) {
		Uri fileUri = AndroidUtils.getUriForFile(context, file);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_STREAM, fileUri);
		intent.setType("application/gpx+xml");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (context instanceof OsmandApplication) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		Intent chooserIntent = Intent.createChooser(intent, context.getString(R.string.shared_string_share));
		AndroidUtils.startActivityIfSafe(context, chooserIntent);
	}

	@NonNull
	public static String getGpxFileRelativePath(@NonNull OsmandApplication app, @NonNull String fullPath) {
		String rootGpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath() + '/';
		return fullPath.replace(rootGpxDir, "");
	}

	public static boolean isGpxFile(@NonNull File file) {
		return file.isFile() && file.getName().toLowerCase().endsWith(GPX_FILE_EXT);
	}

	public static void updateGpxInfoView(@NonNull View view, @NonNull TrackItem trackItem,
	                                     @NonNull OsmandApplication app, boolean isDashItem,
	                                     @Nullable GpxDataItemCallback callback) {
		TextView viewName = view.findViewById(R.id.name);
		if (isDashItem) {
			view.findViewById(R.id.divider_dash).setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider_list).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.divider_list).setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider_dash).setVisibility(View.GONE);
		}

		viewName.setText(trackItem.getName());

		ImageView icon = view.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

		if (getSelectedGpxFile(app, trackItem) != null) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_polygom_dark, R.color.color_distance));
		}
		GPXTrackAnalysis analysis = getGpxTrackAnalysis(trackItem, app, callback);
		boolean sectionRead = analysis == null;
		if (sectionRead) {
			view.findViewById(R.id.read_section).setVisibility(View.GONE);
			view.findViewById(R.id.unknown_section).setVisibility(View.VISIBLE);
			String date = "";
			String size = "";

			File file = trackItem.getFile();
			long fileSize = file != null ? file.length() : 0;
			if (fileSize > 0) {
				size = AndroidUtils.formatSize(view.getContext(), fileSize + 512);
			}
			DateFormat format = OsmAndFormatter.getDateFormat(app);
			long lastModified = trackItem.getLastModified();
			if (lastModified > 0) {
				date = (format.format(new Date(lastModified)));
			}
			TextView sizeText = view.findViewById(R.id.date_and_size_details);
			sizeText.setText(date + " • " + size);

		} else {
			view.findViewById(R.id.read_section).setVisibility(View.VISIBLE);
			view.findViewById(R.id.unknown_section).setVisibility(View.GONE);
			ImageView distanceI = view.findViewById(R.id.distance_icon);
			distanceI.setVisibility(View.VISIBLE);
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsI = view.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeI = view.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_16));
			TextView time = view.findViewById(R.id.time);
			TextView distance = view.findViewById(R.id.distance);
			TextView pointsCount = view.findViewById(R.id.points_count);
			pointsCount.setText(String.valueOf(analysis.getWptPoints()));
			distance.setText(OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));

			if (analysis.isTimeSpecified()) {
				time.setText(formatDuration(analysis.getDurationInSeconds(), app.accessibilityEnabled()));
			} else {
				time.setText("");
			}
		}
		view.findViewById(R.id.description).setVisibility(View.GONE);
		view.findViewById(R.id.check_item).setVisibility(View.GONE);
	}

	private static SelectedGpxFile getSelectedGpxFile(@NonNull OsmandApplication app, @NonNull TrackItem trackItem) {
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		return trackItem.isShowCurrentTrack() ? selectedGpxHelper.getSelectedCurrentRecordingTrack() :
				selectedGpxHelper.getSelectedFileByPath(trackItem.getPath());
	}

	@Nullable
	public static GPXTrackAnalysis getGpxTrackAnalysis(@NonNull TrackItem trackItem,
	                                                   @NonNull OsmandApplication app,
	                                                   @Nullable GpxDataItemCallback callback) {
		SelectedGpxFile selectedGpxFile = getSelectedGpxFile(app, trackItem);
		GPXTrackAnalysis analysis = null;
		if (selectedGpxFile != null && selectedGpxFile.isLoaded()) {
			analysis = selectedGpxFile.getTrackAnalysis(app);
		} else if (trackItem.isShowCurrentTrack()) {
			analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
		} else if (trackItem.getFile() != null) {
			GpxDataItem dataItem = app.getGpxDbHelper().getItem(trackItem.getFile(), callback);
			if (dataItem != null) {
				analysis = dataItem.getAnalysis();
			}
		}
		return analysis;
	}
}