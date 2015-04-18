package net.osmand.plus.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class GpxUiHelper {

	public static String getDescription(OsmandApplication app, GPXFile result, File f, boolean html) {
		GPXTrackAnalysis analysis = result.getAnalysis(f == null ? 0 : f.lastModified());
		return getDescription(app, analysis, html);
	}
	
	public static String getDescription(OsmandApplication app, TrkSegment t, boolean html) {
		return getDescription(app, GPXTrackAnalysis.segment(0, t), html);
	}
	

	
	public static String getColorValue(String clr, String value, boolean html) {
		if(!html) {
			return value;
		}
		return "<font color=\"" + clr + "\">" + value + "</font>";
	}
	
	public static String getColorValue(String clr, String value) {
		return getColorValue(clr, value, true);
	}
	
	public static String getDescription(OsmandApplication app, GPXTrackAnalysis analysis, boolean html) {
		StringBuilder description = new StringBuilder();
		String nl = html?"<br/>":"\n";
		String timeSpanClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_time_span_color));
		String distanceClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_distance_color));
		String speedClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(app.getResources().getColor(R.color.gpx_altitude_desc));
		// OUTPUT:
		// 1. Total distance, Start time, End time
		description.append(app.getString(R.string.gpx_info_distance, getColorValue(distanceClr, 
				OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app), html), 
				getColorValue(distanceClr, analysis.points+"", html) ));
		if(analysis.totalTracks > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.totalTracks+"", html)));
		}
		if(analysis.wptPoints > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.wptPoints+"", html)));
		}
		if(analysis.isTimeSpecified()) {
				description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.startTime));
				description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.endTime));
		}

		// 2. Time span
		if(analysis.timeSpan > 0 && analysis.timeSpan / 1000 != analysis.timeMoving / 1000) {
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeSpan/1000)
							);
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if(analysis.isTimeMoving()){
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeMoving/1000)
					);
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if (analysis.isElevationSpecified()) {
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_avg_altitude,
					getColorValue(speedClr, OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app), html)));
			description.append(nl);
			String min = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.minElevation, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app), html);
			String asc = getColorValue(ascClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app), html);
			String desc = getColorValue(descClr, OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app), html);
			description.append(app.getString(R.string.gpx_info_diff_altitude,min +" - " + max ));
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_asc_altitude,"\u2193 " + desc + "   \u2191 " + asc + ""));
		}


		if(analysis.isSpeedSpecified()){
			String avg = getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app), html);
			description.append(nl).append(app.getString(R.string.gpx_info_average_speed,avg));
			description.append(nl).append(app.getString(R.string.gpx_info_maximum_speed,max));
		}
		return description.toString();
	}

	public static AlertDialog selectGPXFile(List<String> selectedGpxList, final Activity activity,
									 final boolean showCurrentGpx, final boolean multipleChoice, final CallbackWithObject<GPXFile[]> callbackWithObject){
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<String> allGpxList = getSortedGPXFilenames(dir, false);
		if(allGpxList.isEmpty()){
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}

		if(!allGpxList.isEmpty() || showCurrentGpx){
			if(showCurrentGpx){
				allGpxList.add(0, activity.getString(R.string.show_current_gpx_title));
			}
			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(activity, allGpxList, selectedGpxList, multipleChoice,
					showCurrentGpx);

			return createDialog(activity, showCurrentGpx, multipleChoice, callbackWithObject, allGpxList, adapter);
		}
		return null;
	}
	
	public static AlertDialog selectGPXFile(final Activity activity,
			final boolean showCurrentGpx, final boolean multipleChoice, final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<String> list = getSortedGPXFilenames(dir, false);
		if(list.isEmpty()){
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if(!list.isEmpty() || showCurrentGpx){
			if(showCurrentGpx){
				list.add(0, activity.getString(R.string.show_current_gpx_title));
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(activity, list, null, multipleChoice,
					showCurrentGpx);
			return createDialog(activity, showCurrentGpx, multipleChoice, callbackWithObject, list, adapter);
		}
		return null;
	}

	private static ContextMenuAdapter createGpxContextMenuAdapter(Activity activity, List<String> allGpxList,
																  List<String> selectedGpxList, boolean multipleChoice, 
																  boolean showCurrentTrack) {
		final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
		//element position in adapter
		int i = 0;
		for (String s : allGpxList) {
			String fileName = s;
			if (s.endsWith(".gpx")) {
				s = s.substring(0, s.length() - ".gpx".length());
			}
			s = s.replace('_', ' ');

			adapter.item(s).selected(multipleChoice ? 0 : -1)
					.iconColor(R.drawable.ic_action_polygom_dark).reg();

			//if there's some selected files - need to mark them as selected
			if (selectedGpxList != null) {
				updateSelection(selectedGpxList, showCurrentTrack, adapter, i, fileName);
			}
			i++;
		}
		return adapter;
	}

	protected static void updateSelection(List<String> selectedGpxList, boolean showCurrentTrack,
			final ContextMenuAdapter adapter, int i, String fileName) {
		if(i == 0 && showCurrentTrack) {
			if(selectedGpxList.contains("")) {
				adapter.setSelection(i, 1);
			}
		} else {
			for (String file : selectedGpxList) {
				if (file.endsWith(fileName)) {
					adapter.setSelection(i, 1);
					break;
				}
			}
		}
	}
	
	private static void setDescripionInDialog(final ArrayAdapter<?> adapter, final ContextMenuAdapter cmAdapter, Activity activity,
			final File dir, String filename, final int position) {
		final Application app = activity.getApplication();
		final File f = new File(dir, filename);
		loadGPXFileInDifferentThread(activity, new CallbackWithObject<GPXUtilities.GPXFile[]>() {
			
			@Override
			public boolean processResult(GPXFile[] result) {
				cmAdapter.setItemName(position, cmAdapter.getItemName(position) + "\n" + getDescription((OsmandApplication) app, result[0], f, false));
				adapter.notifyDataSetInvalidated();
				return true;
			}
		}, dir, null, filename);
	}

	private static AlertDialog createDialog(final Activity activity, final boolean showCurrentGpx,
			final boolean multipleChoice, final CallbackWithObject<GPXFile[]> callbackWithObject,
			final List<String> list, final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		Builder b = new AlertDialog.Builder(activity);
		// final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final boolean light = app.getSettings().isLightContent();
		final int layout;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			layout = R.layout.list_menu_item;
		} else {
			layout = R.layout.list_menu_item_native;
		}

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.title,
				adapter.getItemNames()) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = activity.getLayoutInflater().inflate(layout, null);
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageDrawable(adapter.getImage(app, position, light));
				final ArrayAdapter<String> arrayAdapter = this;
				icon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (showCurrentGpx && position == 0) {
							return;
						}
						int nline = adapter.getItemName(position).indexOf('\n');
						if(nline == -1) {
							setDescripionInDialog(arrayAdapter, adapter, activity, dir, list.get(position), position);
						} else {
							adapter.setItemName(position, adapter.getItemName(position).substring(0, nline));
							arrayAdapter.notifyDataSetInvalidated();
						}
					}

				});
				if(showCurrentGpx && position == 0) {
					icon.setVisibility(View.INVISIBLE);
				} else {
					icon.setVisibility(View.VISIBLE);
				}
				TextView tv = (TextView) v.findViewById(R.id.title);
				tv.setText(adapter.getItemName(position));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

				// Put the image on the TextView
				// if(adapter.getImageId(position, light) != 0) {
				// tv.setCompoundDrawablesWithIntrinsicBounds(adapter.getImageId(position, light), 0, 0, 0);
				// }
				// tv.setCompoundDrawablePadding(padding);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				if (adapter.getSelection(position) == -1) {
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(adapter.getSelection(position) > 0);
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							adapter.setSelection(position, isChecked ? 1 : 0);
						}
					});
				}
				return v;
			}
		};

		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
			}
		};
		b.setAdapter(listAdapter, onClickListener);
		if (multipleChoice) {
			b.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					GPXFile currentGPX = null;
					//clear all previously selected files before adding new one
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					if(app != null && app.getSelectedGpxHelper() != null){
						app.getSelectedGpxHelper().clearAllGpxFileToShow();
					}
					if (showCurrentGpx && adapter.getSelection(0) > 0) {
						currentGPX = app.getSavingTrackHelper().getCurrentGpx();
					}
					List<String> s = new ArrayList<String>();
					for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
						if (adapter.getSelection(i) > 0) {
							s.add(list.get(i));
						}
					}
					dialog.dismiss();
					loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
							s.toArray(new String[s.size()]));
				}
			});
		}

		final AlertDialog dlg = b.create();
		dlg.setCanceledOnTouchOutside(true);
		dlg.getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (multipleChoice) {
					adapter.setSelection(position, adapter.getSelection(position) > 0 ? 0 : 1);
					listAdapter.notifyDataSetInvalidated();
				} else {
					dlg.dismiss();
					if (showCurrentGpx && position == 0) {
						callbackWithObject.processResult(null);
					} else {
						loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, list.get(position));
					}
				}
			}
		});
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	public static List<String> getSortedGPXFilenamesByDate(File dir, boolean absolutePath) {
		final Map<String, Long> mp = new HashMap<String, Long>();
		readGpxDirectory(dir, mp, "", absolutePath);
		ArrayList<String> list = new ArrayList<String>(mp.keySet());
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				Long l1 = mp.get(object1);
				Long l2 = mp.get(object2);
				long lhs = l1 == null ? 0 : l1.longValue();
				long rhs = l2 == null ? 0 : l2.longValue();
				return lhs < rhs ? 1 : (lhs == rhs ? 0 : -1);
			}
		});
		return list;
	}

	
	public static List<String> getSortedGPXFilenames(File dir, boolean absolutePath) {
		final Map<String, Long> mp = new HashMap<String, Long>();
		readGpxDirectory(dir, mp, "", absolutePath);
		ArrayList<String> list = new ArrayList<String>(mp.keySet());
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				return -object1.compareTo(object2);
			}

		});
		return list;
	}

	private static void readGpxDirectory(File dir, final Map<String, Long> map, String parent, 
			boolean absolutePath) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(".gpx")) { //$NON-NLS-1$
						map.put(absolutePath ? f.getAbsolutePath() :
								parent + f.getName(), f.lastModified());
					} else if (f.isDirectory()) {
						readGpxDirectory(f, map, parent + f.getName() + "/", absolutePath);
					}
				}
			}
		}
	}
	
	private static void loadGPXFileInDifferentThread(final Activity activity, final CallbackWithObject<GPXFile[]> callbackWithObject,
			final File dir, final GPXFile currentFile, final String... filename) {
		final ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading_smth, ""),
				activity.getString(R.string.loading_data));
		new Thread(new Runnable() {
			@Override
			public void run() {
				final GPXFile[] result = new GPXFile[filename.length + (currentFile == null ? 0 : 1)];
				int k = 0;
				String w = "";
				if (currentFile != null) {
					result[k++] = currentFile;
				}
				for (String fname : filename) {
					final File f = new File(dir, fname);
					GPXFile res = GPXUtilities.loadGPXFile(activity.getApplication(), f);
					if (res.warning != null && res.warning.length() > 0) {
						w += res.warning + "\n";
					}
					result[k++] = res;
				}
				dlg.dismiss();
				final String warn = w;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (warn.length() > 0) {
							AccessibleToast.makeText(activity, warn, Toast.LENGTH_LONG).show();
						} else {
							callbackWithObject.processResult(result);
						}
					}
				});
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}
}
