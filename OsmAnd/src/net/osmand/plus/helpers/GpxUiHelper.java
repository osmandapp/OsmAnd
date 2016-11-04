package net.osmand.plus.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.dialogs.ConfigureMapMenu.AppearanceListItem;
import net.osmand.plus.dialogs.ConfigureMapMenu.GpxAppearanceAdapter;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.dialogs.ConfigureMapMenu.refreshMapComplete;
import static net.osmand.plus.download.DownloadActivity.formatMb;

public class GpxUiHelper {

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1005;

	public static String getDescription(OsmandApplication app, GPXFile result, File f, boolean html) {
		GPXTrackAnalysis analysis = result.getAnalysis(f == null ? 0 : f.lastModified());
		return getDescription(app, analysis, html);
	}

	public static String getDescription(OsmandApplication app, TrkSegment t, boolean html) {
		return getDescription(app, GPXTrackAnalysis.segment(0, t), html);
	}


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
						OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app), html),
				getColorValue(distanceClr, analysis.points + "", html)));
		if (analysis.totalTracks > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.totalTracks + "", html)));
		}
		if (analysis.wptPoints > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.wptPoints + "", html)));
		}
		if (analysis.isTimeSpecified()) {
			description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.startTime));
			description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.endTime));
		}

		// 2. Time span
		if (analysis.timeSpan > 0 && analysis.timeSpan / 1000 != analysis.timeMoving / 1000) {
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if (analysis.isTimeMoving()) {
				//Next few lines for Issue 3222 heuristic testing only
				//final String formatDuration0 = Algorithms.formatDuration((int) (analysis.timeMoving0 / 1000), app.accessibilityEnabled());
				//description.append(nl).append(app.getString(R.string.gpx_timemoving,
				//		getColorValue(timeSpanClr, formatDuration0, html)));
				//description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving0, app), html) + ")");
			final String formatDuration = Algorithms.formatDuration((int) (analysis.timeMoving / 1000), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
			description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving, app), html) + ")");
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
			description.append(app.getString(R.string.gpx_info_diff_altitude, min + " - " + max));
			description.append(nl);
			description.append(app.getString(R.string.gpx_info_asc_altitude, "\u2193 " + desc + "   \u2191 " + asc + ""));
		}


		if (analysis.isSpeedSpecified()) {
			String avg = getColorValue(speedClr, OsmAndFormatter.getFormattedSpeed(analysis.avgSpeed, app), html);
			String max = getColorValue(ascClr, OsmAndFormatter.getFormattedSpeed(analysis.maxSpeed, app), html);
			description.append(nl).append(app.getString(R.string.gpx_info_average_speed, avg));
			description.append(nl).append(app.getString(R.string.gpx_info_maximum_speed, max));
		}
		return description.toString();
	}

	public static AlertDialog selectGPXFiles(List<String> selectedGpxList, final Activity activity,
											 final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<GPXInfo> allGpxList = getSortedGPXFilesInfo(dir, selectedGpxList, false);
		if (allGpxList.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		allGpxList.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
		final ContextMenuAdapter adapter = createGpxContextMenuAdapter(allGpxList, selectedGpxList, true);

		return createDialog(activity, true, true, true, callbackWithObject, allGpxList, adapter);
	}

	public static AlertDialog selectGPXFile(final Activity activity,
											final boolean showCurrentGpx, final boolean multipleChoice, final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<GPXInfo> list = getSortedGPXFilesInfo(dir, null, false);
		if (list.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if (!list.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(list, null, showCurrentGpx);
			return createDialog(activity, showCurrentGpx, multipleChoice, false, callbackWithObject, list, adapter);
		}
		return null;
	}

	public static AlertDialog selectSingleGPXFile(final Activity activity,
											final boolean showCurrentGpx, final CallbackWithObject<GPXFile[]> callbackWithObject) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		int gpxDirLength = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath().length();
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		final List<GPXInfo> list = new ArrayList<>(selectedGpxFiles.size() + 1);
		if (!selectedGpxFiles.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(new GPXInfo(activity.getString(R.string.shared_string_currently_recording_track), 0, 0));
			}

			for (SelectedGpxFile selectedGpx : selectedGpxFiles) {
				if (!selectedGpx.getGpxFile().showCurrentTrack) {
					list.add(new GPXInfo(selectedGpx.getGpxFile().path.substring(gpxDirLength + 1), selectedGpx.getGpxFile().modifiedTime, 0));
				}
			}

			final ContextMenuAdapter adapter = createGpxContextMenuAdapter(list, null, showCurrentGpx);
			return createSingleChoiceDialog(activity, showCurrentGpx, callbackWithObject, list, adapter);
		}
		return null;
	}

	private static ContextMenuAdapter createGpxContextMenuAdapter(List<GPXInfo> allGpxList,
																  List<String> selectedGpxList,
																  boolean showCurrentTrack) {
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		//element position in adapter
		int i = 0;
		for (GPXInfo gpxInfo : allGpxList) {
			String s = gpxInfo.getFileName();
			String fileName = s;
			if (s.endsWith(".gpx")) {
				s = s.substring(0, s.length() - ".gpx".length());
			}
			s = s.replace('_', ' ');

			adapter.addItem(ContextMenuItem.createBuilder(s).setSelected(false)
					.setIcon(R.drawable.ic_action_polygom_dark).createItem());

			//if there's some selected files - need to mark them as selected
			if (selectedGpxList != null) {
				updateSelection(selectedGpxList, showCurrentTrack, adapter, i, fileName);
			}
			i++;
		}
		return adapter;
	}

	protected static void updateSelection(List<String> selectedGpxList, boolean showCurrentTrack,
										  final ContextMenuAdapter adapter, int position, String fileName) {
		ContextMenuItem item = adapter.getItem(position);
		if (position == 0 && showCurrentTrack) {
			if (selectedGpxList.contains("")) {
				item.setSelected(true);
			}
		} else {
			for (String file : selectedGpxList) {
				if (file.endsWith(fileName)) {
					item.setSelected(true);
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
				ContextMenuItem item = cmAdapter.getItem(position);
				item.setTitle(item.getTitle() + "\n" + getDescription((OsmandApplication) app, result[0], f, false));
				adapter.notifyDataSetInvalidated();
				return true;
			}
		}, dir, null, filename);
	}

	private static AlertDialog createSingleChoiceDialog(final Activity activity,
											final boolean showCurrentGpx,
											final CallbackWithObject<GPXFile[]> callbackWithObject,
											final List<GPXInfo> list,
											final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final IconsCache iconsCache = app.getIconsCache();
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final int layout = R.layout.list_menu_item_native_singlechoice;

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.text1,
				adapter.getItemNames()) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = activity.getLayoutInflater().inflate(layout, null);
				}
				final ContextMenuItem item = adapter.getItem(position);
				TextView tv = (TextView) v.findViewById(R.id.text1);
				Drawable icon;
				if (showCurrentGpx && position == 0) {
					icon = null;
				} else {
					icon = iconsCache.getThemedIcon(item.getIcon());
				}
				tv.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity, 10f));
				tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				tv.setText(item.getTitle());
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

				return v;
			}
		};

		int selectedIndex = 0;
		String prevSelectedGpx = app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.get();
		if (prevSelectedGpx != null) {
			selectedIndex = list.indexOf(prevSelectedGpx);
		}
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		final int[] selectedPosition = {selectedIndex};
		builder.setSingleChoiceItems(listAdapter, selectedIndex, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				selectedPosition[0] = position;
			}
		});
		builder.setTitle(R.string.select_gpx)
				.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						int position = selectedPosition[0];
						if (position != -1) {
							if (showCurrentGpx && position == 0) {
								callbackWithObject.processResult(null);
								app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(null);
							} else {
								String fileName = list.get(position).getFileName();
								app.getSettings().LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT.set(fileName);
								SelectedGpxFile selectedGpxFile =
										app.getSelectedGpxHelper().getSelectedFileByName(fileName);
								if (selectedGpxFile != null) {
									callbackWithObject.processResult(new GPXFile[]{selectedGpxFile.getGpxFile()});
								} else {
									loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
								}
							}
						}
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);

		final AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	private static AlertDialog createDialog(final Activity activity,
											final boolean showCurrentGpx,
											final boolean multipleChoice,
											final boolean showAppearanceSetting,
											final CallbackWithObject<GPXFile[]> callbackWithObject,
											final List<GPXInfo> list,
											final ContextMenuAdapter adapter) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(activity);
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final int layout = R.layout.list_item_with_checkbox;
		final int switchLayout = R.layout.list_item_with_switch;
		final Map<String, String> gpxAppearanceParams = new HashMap<>();

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layout, R.id.title,
				adapter.getItemNames()) {

			@Override
			public int getItemViewType(int position) {
				return showCurrentGpx && position == 0 ? 1 : 0;
			}

			@Override
			public int getViewTypeCount() {
				return 2;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					if (getItemViewType(position) == 0) {
						v = activity.getLayoutInflater().inflate(layout, null);
					} else {
						v = activity.getLayoutInflater().inflate(switchLayout, null);
					}
				}

				TextView tv = (TextView) v.findViewById(R.id.title);
				TextView dv = (TextView) v.findViewById(R.id.description);
				final ContextMenuItem item = adapter.getItem(position);

				if (showCurrentGpx && position == 0) {
					tv.setText(item.getTitle());
					dv.setText(OsmAndFormatter.getFormattedDistance(app.getSavingTrackHelper().getDistance(), app));
					final SwitchCompat ch = ((SwitchCompat) v.findViewById(R.id.toggle_item));
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(item.getSelected());
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							item.setSelected(isChecked);
						}
					});
					return v;
				}

				tv.setText(item.getTitle().replace("/", " • "));
				GPXInfo info = list.get(position);
				StringBuilder sb = new StringBuilder();
				if (info.getLastModified() > 0) {
					sb.append(dateFormat.format(info.getLastModified()));
				}
				if (info.getFileSize() >= 0) {
					if (sb.length() > 0) {
						sb.append(" • ");
					}
					long fileSizeKB = info.getFileSize() / 1000;
					if (info.getFileSize() < 5000) {
						sb.append(info.getFileSize()).append(" B");
					} else if (fileSizeKB > 100) {
						sb.append(formatMb.format(new Object[]{(float) fileSizeKB / (1 << 10)}));
					} else {
						sb.append(fileSizeKB).append(" kB");
					}
				}
				dv.setText(sb.toString());

				/*
				final ArrayAdapter<String> arrayAdapter = this;
				iconView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int nline = item.getTitle().indexOf('\n');
						if (nline == -1) {
							String fileName = list.get(position).getFileName();
							setDescripionInDialog(arrayAdapter, adapter, activity, dir, fileName, position);
						} else {
							item.setTitle(item.getTitle().substring(0, nline));
							arrayAdapter.notifyDataSetInvalidated();
						}
					}

				});
				*/


				final CheckBox ch = ((CheckBox) v.findViewById(R.id.toggle_item));
				if (item.getSelected() == null) {
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(item.getSelected());
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							item.setSelected(isChecked);
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
		builder.setAdapter(listAdapter, onClickListener);
		if (multipleChoice) {
			if (showAppearanceSetting) {
				final RenderingRuleProperty trackWidthProp;
				final RenderingRuleProperty trackColorProp;
				final RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (renderer != null) {
					trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
					trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
				} else {
					trackWidthProp = null;
					trackColorProp = null;
				}
				if (trackWidthProp == null || trackColorProp == null) {
					builder.setTitle(R.string.show_gpx);
				} else {
					final View apprTitleView = activity.getLayoutInflater().inflate(R.layout.select_gpx_appearance_title, null);

					final OsmandSettings.CommonPreference<String> prefWidth
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
					final OsmandSettings.CommonPreference<String> prefColor
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);

					updateAppearanceTitle(activity, app, trackWidthProp, renderer, apprTitleView, prefWidth.get(), prefColor.get());

					apprTitleView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							final ListPopupWindow popup = new ListPopupWindow(activity);
							popup.setAnchorView(apprTitleView);
							popup.setContentWidth(AndroidUtils.dpToPx(activity, 200f));
							popup.setModal(true);
							popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
							popup.setVerticalOffset(AndroidUtils.dpToPx(activity, -48f));
							popup.setHorizontalOffset(AndroidUtils.dpToPx(activity, -6f));
							final GpxAppearanceAdapter gpxApprAdapter = new GpxAppearanceAdapter(activity,
									gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get());
							popup.setAdapter(gpxApprAdapter);
							popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									AppearanceListItem item = gpxApprAdapter.getItem(position);
									if (item != null) {
										if (item.getAttrName() == CURRENT_TRACK_WIDTH_ATTR) {
											gpxAppearanceParams.put(CURRENT_TRACK_WIDTH_ATTR, item.getValue());
										} else if (item.getAttrName() == CURRENT_TRACK_COLOR_ATTR) {
											gpxAppearanceParams.put(CURRENT_TRACK_COLOR_ATTR, item.getValue());
										}
									}
									popup.dismiss();
									updateAppearanceTitle(activity, app, trackWidthProp, renderer,
											apprTitleView,
											gpxAppearanceParams.containsKey(CURRENT_TRACK_WIDTH_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_WIDTH_ATTR) : prefWidth.get(),
											gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get());
								}
							});
							popup.show();
						}
					});
					builder.setCustomTitle(apprTitleView);
				}
			} else {
				builder.setTitle(R.string.show_gpx);
			}
			builder.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (gpxAppearanceParams.size() > 0) {
						for (Map.Entry<String, String> entry : gpxAppearanceParams.entrySet()) {
							final OsmandSettings.CommonPreference<String> pref
									= app.getSettings().getCustomRenderProperty(entry.getKey());
							pref.set(entry.getValue());
						}
						if (activity instanceof MapActivity) {
							ConfigureMapMenu.refreshMapComplete((MapActivity) activity);
						}
					}
					GPXFile currentGPX = null;
					//clear all previously selected files before adding new one
					OsmandApplication app = (OsmandApplication) activity.getApplication();
					if (app != null && app.getSelectedGpxHelper() != null) {
						app.getSelectedGpxHelper().clearAllGpxFileToShow();
					}
					if (app != null && showCurrentGpx && adapter.getItem(0).getSelected()) {
						currentGPX = app.getSavingTrackHelper().getCurrentGpx();
					}
					List<String> s = new ArrayList<>();
					for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
						if (adapter.getItem(i).getSelected()) {
							s.add(list.get(i).getFileName());
						}
					}
					dialog.dismiss();
					loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
							s.toArray(new String[s.size()]));
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
					&& list.size() > 1 || !showCurrentGpx && list.size() > 0) {
				builder.setNeutralButton(R.string.gpx_add_track, null);
			}
		}

		final AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(true);
		if (list.size() == 0 || showCurrentGpx && list.size() == 1) {
			final View footerView = activity.getLayoutInflater().inflate(R.layout.no_gpx_files_list_footer, null);
			TextView descTextView = (TextView)footerView.findViewById(R.id.descFolder);
			String descPrefix = app.getString(R.string.gpx_no_tracks_title_folder);
			SpannableString spannableDesc = new SpannableString(descPrefix + ": " + dir.getAbsolutePath());
			spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
					descPrefix.length() + 1, spannableDesc.length(), 0);
			descTextView.setText(spannableDesc);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				footerView.findViewById(R.id.button).setVisibility(View.GONE);
			} else {
				footerView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addTrack(activity, dlg);
					}
				});
			}
			dlg.getListView().addFooterView(footerView);
		}
		dlg.getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (multipleChoice) {
					ContextMenuItem item = adapter.getItem(position);
					item.setSelected(!item.getSelected());
					listAdapter.notifyDataSetInvalidated();
				} else {
					dlg.dismiss();
					if (showCurrentGpx && position == 0) {
						callbackWithObject.processResult(null);
					} else {
						String fileName = list.get(position).getFileName();
						SelectedGpxFile selectedGpxFile =
								app.getSelectedGpxHelper().getSelectedFileByName(fileName);
						if (selectedGpxFile != null) {
							callbackWithObject.processResult(new GPXFile[]{selectedGpxFile.getGpxFile()});
						} else {
							loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
						}
					}
				}
			}
		});
		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button addTrackButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
				if (addTrackButton != null) {
					addTrackButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							addTrack(activity, dlg);
						}
					});
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

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static void addTrack(final Activity activity, final AlertDialog dialog) {
		if (activity instanceof MapActivity) {
			final MapActivity mapActivity = (MapActivity) activity;
			ActivityResultListener listener = new ActivityResultListener(OPEN_GPX_DOCUMENT_REQUEST, new OnActivityResultListener() {
				@Override
				public void onResult(int resultCode, Intent resultData) {
					if (resultCode == Activity.RESULT_OK) {
						if (resultData != null) {
							Uri uri = resultData.getData();
							if (mapActivity.getGpxImportHelper().handleGpxImport(uri, false)) {
								dialog.dismiss();
							}
						}
					}
				}
			});

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			//intent.addCategory(Intent.CATEGORY_OPENABLE);
			//intent.setType("application/gpx+xml");
			//intent.setType("text/plain");
			//intent.setType("text/xml");
			intent.setType("*/*");
			mapActivity.registerActivityResultListener(listener);
			activity.startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
		}
	}

	private static void updateAppearanceTitle(Activity activity, OsmandApplication app,
											  RenderingRuleProperty trackWidthProp,
											  RenderingRulesStorage renderer,
											  View apprTitleView,
											  String prefWidthValue,
											  String prefColorValue) {
		TextView widthTextView = (TextView) apprTitleView.findViewById(R.id.widthTitle);
		ImageView colorImageView = (ImageView) apprTitleView.findViewById(R.id.colorImage);
		if (Algorithms.isEmpty(prefWidthValue)) {
			widthTextView.setText(SettingsActivity.getStringPropertyValue(activity, trackWidthProp.getDefaultValueDescription()));
		} else {
			widthTextView.setText(SettingsActivity.getStringPropertyValue(activity, prefWidthValue));
		}
		int color = GpxAppearanceAdapter.parseTrackColor(renderer, prefColorValue);
		if (color == -1) {
			colorImageView.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getIconsCache().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	public static List<GPXInfo> getSortedGPXFilesInfoByDate(File dir, boolean absolutePath) {
		final List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo object1, GPXInfo object2) {
				long lhs = object1.getLastModified();
				long rhs = object2.getLastModified();
				return lhs < rhs ? 1 : (lhs == rhs ? 0 : -1);
			}
		});
		return list;
	}


	public static List<GPXInfo> getSortedGPXFilesInfo(File dir, final List<String> selectedGpxList, boolean absolutePath) {
		final List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		if (selectedGpxList != null) {
			for (GPXInfo info : list) {
				for (String fileName : selectedGpxList) {
					if (fileName.endsWith(info.getFileName())) {
						info.setSelected(true);
						break;
					}
				}
			}
		}
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				int res = i1.isSelected() == i2.isSelected() ? 0 : i1.isSelected() ? -1 : 1;
				if (res != 0) {
					return res;
				}
				return -i1.getFileName().compareTo(i2.getFileName());
			}
		});
		return list;
	}

	private static void readGpxDirectory(File dir, final List<GPXInfo> list, String parent,
										 boolean absolutePath) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(".gpx")) { //$NON-NLS-1$
						list.add(new GPXInfo(absolutePath ? f.getAbsolutePath() :
								parent + f.getName(), f.lastModified(), f.length()));
					} else if (f.isDirectory()) {
						readGpxDirectory(f, list, parent + f.getName() + "/", absolutePath);
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
							Toast.makeText(activity, warn, Toast.LENGTH_LONG).show();
						} else {
							callbackWithObject.processResult(result);
						}
					}
				});
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}

	public static class GPXInfo {
		private String fileName;
		private long lastModified;
		private long fileSize;
		private boolean selected;

		public GPXInfo(String fileName, long lastModified, long fileSize) {
			this.fileName = fileName;
			this.lastModified = lastModified;
			this.fileSize = fileSize;
		}

		public String getFileName() {
			return fileName;
		}

		public long getLastModified() {
			return lastModified;
		}

		public long getFileSize() {
			return fileSize;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}
}
