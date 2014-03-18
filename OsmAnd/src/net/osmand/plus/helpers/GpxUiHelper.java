package net.osmand.plus.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ClientContext;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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

	public static String getDescription(OsmandApplication app, GPXFile result, File f) {
		StringBuilder description = new StringBuilder();
		float totalDistance = 0;
		int totalTracks = 0;
		long startTime = Long.MAX_VALUE;
		long endTime = Long.MIN_VALUE;
		long timeSpan = 0;
		long timeMoving = 0;
		float totalDistanceMoving = 0;

		double diffElevationUp = 0;
		double diffElevationDown = 0;
		double totalElevation = 0;
		double minElevation = 99999;
		double maxElevation = 0;
		
		float maxSpeed = 0;
		int speedCount = 0;
		double totalSpeedSum = 0;

		float[] calculations = new float[1];

		int points = 0;
		for(int i = 0; i< result.tracks.size() ; i++){
			Track subtrack = result.tracks.get(i);
			for(TrkSegment segment : subtrack.segments){
				totalTracks++;
				points += segment.points.size();
				for (int j = 0; j < segment.points.size(); j++) {
					WptPt point = segment.points.get(j);
					long time = point.time;
					if(time != 0){
						startTime = Math.min(startTime, time);
						endTime = Math.max(startTime, time);
					}

					double elevation = point.ele;
					if (!Double.isNaN(elevation)) {
						totalElevation += elevation;
						minElevation = Math.min(elevation, minElevation);
						maxElevation = Math.max(elevation, maxElevation);
					}

					float speed = (float) point.speed;
					if(speed > 0){
						totalSpeedSum += speed;
						maxSpeed = Math.max(speed, maxSpeed);
						speedCount ++;
					}
					
					if (j > 0) {
						WptPt prev = segment.points.get(j - 1);

						if (!Double.isNaN(point.ele) && !Double.isNaN(prev.ele)) {
							double diff = point.ele - prev.ele;
							if (diff > 0) {
								diffElevationUp += diff;
							} else {
								diffElevationDown -= diff;
							}
						}

						//totalDistance += MapUtils.getDistance(prev.lat, prev.lon, point.lat, point.lon);
						// using ellipsoidal 'distanceBetween' instead of spherical haversine (MapUtils.getDistance) is a little more exact, also seems slightly faster:
						net.osmand.Location.distanceBetween(prev.lat, prev.lon, point.lat, point.lon, calculations);
						totalDistance += calculations[0];

						// Averaging speed values is less exact than totalDistance/timeMoving
						if(speed > 0 && point.time != 0 && prev.time != 0){
							timeMoving = timeMoving + (point.time - prev.time);
							totalDistanceMoving += calculations[0];
						}
					}
				}
			}
		}
		if(startTime == Long.MAX_VALUE){
			startTime = f.lastModified();
		}
		if(endTime == Long.MIN_VALUE){
			endTime = f.lastModified();
		}

		// OUTPUT:
		// 1. Total distance, Start time, End time
		description.append(app.getString(R.string.local_index_gpx_info, totalTracks, points,
				result.points.size(), OsmAndFormatter.getFormattedDistance(totalDistance, app),
				startTime, endTime));

		// 2. Time span
		timeSpan = endTime - startTime;
		description.append(app.getString(R.string.local_index_gpx_timespan, (int) ((timeSpan / 1000) / 3600), (int) (((timeSpan / 1000) / 60) % 60), (int) ((timeSpan / 1000) % 60)));

		// 3. Time moving, if any
		if(timeMoving > 0){
			description.append(
				app.getString(R.string.local_index_gpx_timemoving, (int) ((timeMoving / 1000) / 3600), (int) (((timeMoving / 1000) / 60) % 60), (int) ((timeMoving / 1000) % 60)));
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if(totalElevation != 0 || diffElevationUp != 0 || diffElevationDown != 0){
			description.append(  
					app.getString(R.string.local_index_gpx_info_elevation,
					OsmAndFormatter.getFormattedAlt(totalElevation / points, app),
					OsmAndFormatter.getFormattedAlt(minElevation, app),
					OsmAndFormatter.getFormattedAlt(maxElevation, app),
					OsmAndFormatter.getFormattedAlt(diffElevationUp, app),
					OsmAndFormatter.getFormattedAlt(diffElevationDown, app)));
		}

		// 5. Max speed and Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
		if(speedCount > 0){
			if(timeMoving > 0){
				description.append(
					app.getString(R.string.local_index_gpx_info_speed,
					OsmAndFormatter.getFormattedSpeed((float) (totalDistanceMoving / timeMoving * 1000), app),
					OsmAndFormatter.getFormattedSpeed(maxSpeed, app)));
					// (Use totalDistanceMoving instead of totalDistance for av-speed to ignore effect of position fluctuations at rest)
			} else {
				// Averaging speed values is less exact than totalDistance/timeMoving
				description.append(
					app.getString(R.string.local_index_gpx_info_speed,
					OsmAndFormatter.getFormattedSpeed((float) (totalSpeedSum / speedCount), app),
					OsmAndFormatter.getFormattedSpeed(maxSpeed, app)));
			}
		}
		return description.toString();
	}
	
	public static void selectGPXFile(final Activity activity,
			final boolean showCurrentGpx, final boolean multipleChoice, final CallbackWithObject<GPXFile> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<String> list = getSortedGPXFilenames(dir);
		if(list.isEmpty()){
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if(!list.isEmpty() || showCurrentGpx){
			
			final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
			if(showCurrentGpx){
				list.add(0, activity.getString(R.string.show_current_gpx_title));
			}
			for(String s : list) {
				if (s.endsWith(".gpx")) {
					s = s.substring(0, s.length() - ".gpx".length());
				}
				s = s.replace('_', ' ');
				adapter.item(s).selected(multipleChoice ? 0 : -1)
						.icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light).reg();
			}
			createDialog(activity, showCurrentGpx, multipleChoice, callbackWithObject, list, adapter);
		}
	}
	
	private static void setDescripionInDialog(final ArrayAdapter<?> adapter, final ContextMenuAdapter cmAdapter, Activity activity,
			final File dir, String filename, final int position) {
		final Application app = activity.getApplication();
		final File f = new File(dir, filename);
		loadGPXFileInDifferentThread(activity, new CallbackWithObject<GPXUtilities.GPXFile>() {
			
			@Override
			public boolean processResult(GPXFile result) {
				cmAdapter.setItemName(position, getDescription((OsmandApplication) app, result, f));
				adapter.notifyDataSetInvalidated();
				return true;
			}
		}, dir, null, filename);
	}

	private static void createDialog(final Activity activity, final boolean showCurrentGpx,
			final boolean multipleChoice, final CallbackWithObject<GPXFile> callbackWithObject,
			final List<String> list, final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		Builder b = new AlertDialog.Builder(activity);
		// final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final boolean light = app.getSettings().isLightContentMenu();
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
				icon.setImageResource(adapter.getImageId(position, light));
				final ArrayAdapter<String> arrayAdapter = this;
				icon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (showCurrentGpx && position == 0) {
							return;
						}
						setDescripionInDialog(arrayAdapter, adapter, activity, dir, list.get(position), position);
					}

				});
				icon.setVisibility(View.VISIBLE);
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
			b.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					GPXFile currentGPX = null;
					if (showCurrentGpx && adapter.getSelection(0) > 0) {
						currentGPX = new GPXFile();
						currentGPX.showCurrentTrack = true;
					}
					List<String> s = new ArrayList<String>();
					for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
						if (adapter.getSelection(i) > 0) {
							s.add(list.get(i));
						}
					}
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
	}
	
	private static List<String> getSortedGPXFilenames(File dir,String sub) {
		final List<String> list = new ArrayList<String>();
		readGpxDirectory(dir, list, "");
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				if (object1.compareTo(object2) > 0) {
					return -1;
				} else if (object1.equals(object2)) {
					return 0;
				}
				return 1;
			}

		});
		return list;
	}

	private static void readGpxDirectory(File dir, final List<String> list, String parent) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(".gpx")) { //$NON-NLS-1$
						list.add(parent + f.getName());
					} else if (f.isDirectory()) {
						readGpxDirectory(f, list, parent + f.getName() + "/");
					}
				}
			}
		}
	}
	private static List<String> getSortedGPXFilenames(File dir) {
		return getSortedGPXFilenames(dir, null);
	}
	
	private static void loadGPXFileInDifferentThread(final Activity activity, final CallbackWithObject<GPXFile> callbackWithObject,
			final File dir, final GPXFile currentFile, final String... filename) {
		final ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading),
				activity.getString(R.string.loading_data));
		new Thread(new Runnable() {
			@Override
			public void run() {
				GPXFile r = currentFile; 
				for(String fname : filename) {
					final File f = new File(dir, fname);
					GPXFile res = GPXUtilities.loadGPXFile((ClientContext) activity.getApplication(), f);
					GPXUtilities.mergeGPXFileInto(res, r);
					r = res;
				}
				final GPXFile res = r;
				dlg.dismiss();
				if (res != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (res.warning != null) {
								AccessibleToast.makeText(activity, res.warning, Toast.LENGTH_LONG).show();
							} else {
								callbackWithObject.processResult(res);
							}
						}
					});
				}
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}
}
