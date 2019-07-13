package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WptPtMenuBuilder extends MenuBuilder {
	//todo extract
	final String KEY_PHONE = "Phone: ";
	final String KEY_EMAIL = "Email: ";
	final String PHONE_REGEX = "(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*";
	final String EMAIL_REGEX = "([0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*@([0-9a-zA-Z][-\\w]*[0-9a-zA-Z]\\.)+[a-zA-Z]{2,9})";


	private final WptPt wpt;

	public WptPtMenuBuilder(@NonNull MapActivity mapActivity, final @NonNull WptPt wpt) {
		super(mapActivity);
		this.wpt = wpt;
		setShowNearestWiki(true);
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	protected void buildTopInternal(View view) {
		super.buildTopInternal(view);
		buildWaypointsView(view);
	}

	@Override
	public void buildInternal(View view) {
		if (wpt.time > 0) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(wpt.time);
			buildRow(view, R.drawable.ic_action_data,
					null, dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, null, false, 0, false, null, false);
		}
		if (wpt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					null, OsmAndFormatter.getFormattedSpeed((float)wpt.speed, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					null, OsmAndFormatter.getFormattedDistance((float) wpt.ele, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					null, Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int)wpt.hdop, 0,
					false, null, false, 0, false, null, false);
		}
		
		String phoneToken = getDescriptionToken(wpt.desc, KEY_PHONE, PHONE_REGEX);
		String emailToken = getDescriptionToken(wpt.desc, KEY_EMAIL, EMAIL_REGEX);
		
		final ArrayList<String> phones = findAllElementsInLine(phoneToken, PHONE_REGEX);
		final ArrayList<String> emails = findAllElementsInLine(emailToken, EMAIL_REGEX);
		
		final String desc = deleteAllElementsOccurrence(wpt.desc, phoneToken, emailToken);
		if (!Algorithms.isEmpty(desc)) {
			final View row = buildRow(view, R.drawable.ic_action_note_dark, null, desc, 0, false, null, true, 10, false, null, false);
			//todo maybe delete
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(row.getContext(), app, desc,
							row.getResources().getString(R.string.shared_string_description));
				}
			});
		}
		if (phones != null) {
			String phonesCommaLine = prepareCommaLine(phones);
			if (!Algorithms.isEmpty(phonesCommaLine)) {
				buildRow(view, R.drawable.ic_action_call_dark,
						null, phonesCommaLine, 0,
						false, null, false, 0, false, true, false, null, false);
			}
		}
		if (!Algorithms.isEmpty(wpt.link)) {
			buildRow(view, R.drawable.ic_world_globe_dark,
					null, wpt.link, 0,
					false, null, false, 0, true, null, false);
		}
		if (emails != null) {
			String emailsCommaLine = prepareCommaLine(emails);
			if (!Algorithms.isEmpty(emailsCommaLine)) {
				buildRow(view, R.drawable.ic_action_message,
						null, emailsCommaLine, 0,
						false, null, false, 0, false, false, true, null, false);
			}
		}
		if (!Algorithms.isEmpty(wpt.comment)) {
			final View rowc = buildRow(view, R.drawable.ic_action_note_dark, null, wpt.comment, 0,
					false, null, true, 10, false, null, false);
			rowc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(rowc.getContext(), app, wpt.comment,
							rowc.getResources().getString(R.string.poi_dialog_comment));
				}
			});
		}

		buildPlainMenuItems(view);
	}
	
	//todo extract somewhere / improve algorithm
	private String getDescriptionToken(String text, String key, String ... allowedElementsRegEx) {
		final String END_TOKEN_REGEX = "( +)?[.,]";
		final String SPACE_REGEX = "\\s+";
		
		if (!Algorithms.isEmpty(text)) {
			int startId, endId;
			if (!Algorithms.isEmpty(key) && text.contains(key)) {
				startId = text.indexOf(key);
				endId = 0;

				int finalIndex = text.indexOf(':', startId + key.length());
				if (finalIndex > 0) {
					text = text.substring(0, finalIndex);
				}
				
				for (String regEx : allowedElementsRegEx) {
					ArrayList<String> items = findAllElementsInLine(text, regEx);
					for (String item : items) {
						int currentEnd = text.indexOf(item) + item.length();
						if (endId < currentEnd) {
							endId = currentEnd;
						}
					}
					
					String endedText = text.substring(endId);
					
					if (endedText.startsWith(SPACE_REGEX)) {
						endId += findAllElementsInLine(endedText, SPACE_REGEX).get(0).length();
					}
				}
				
				return text.substring(startId, endId);
			}
		}
		return null;
	}
	
	//todo extract somewhere, maybe to Algorithms
	private ArrayList<String> findAllElementsInLine(String text, String ... regExArgs) {
		ArrayList<String> foundItems = new ArrayList<>();
		if (!Algorithms.isEmpty(text)) {
			for (String regEx : regExArgs) {
				Pattern p = Pattern.compile(regEx);
				Matcher m = p.matcher(text);
				while (m.find()) {
					String item = m.group().trim();
					if (!Algorithms.isEmpty(item)) {
						foundItems.add(item);
					}
				}
			}
			return foundItems;
		}
		return null;
	}
	
	//todo extract somewhere, maybe to Algorithms / improve algorithm
	private String deleteAllElementsOccurrence(String text, String ... args) {
		if (text != null) {
			for (String s : args) {
				if (s != null) {
					text = text.replace(s, "");
				}
			}
			while (text.startsWith(".") && text.length() > 1) {
				text = text.substring(1);
				text = text.trim();
			}
			text = text.trim();
		}
		return text;
	}
	
	//todo extract somewhere, maybe to Algorithms
	private String prepareCommaLine(List<String> items) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);
			if (!Algorithms.isEmpty(item)) {
				sb.append(item);
				if (i < items.size() - 1) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	private void buildWaypointsView(View view) {
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			List<WptPt> points = selectedGpxFile.getGpxFile().getPoints();
			GPXUtilities.GPXFile gpx = selectedGpxFile.getGpxFile();
			if (points.size() > 0) {
				String title = view.getContext().getString(R.string.context_menu_points_of_group);
				File file = new File(gpx.path);
				String gpxName = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
				int color = getPointColor(wpt, getFileColor(selectedGpxFile));
				buildRow(view, app.getUIUtilities().getPaintedIcon(R.drawable.ic_type_waypoints_group, color), null, title, 0, gpxName,
						true, getCollapsableWaypointsView(view.getContext(), true, gpx, wpt),
						false, 0, false, null, false);
			}
		}
	}

	private int getFileColor(@NonNull SelectedGpxFile g) {
		return g.getColor() == 0 ? ContextCompat.getColor(app, R.color.gpx_color_point) : g.getColor();
	}

	@ColorInt
	private int getPointColor(WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? ContextCompat.getColor(app, R.color.color_ok) : o.getColor(fileColor);
	}

	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if (visited != null && !visited.equals("0")) {
			visit = true;
		}
		return visit;
	}

	private CollapsableView getCollapsableWaypointsView(final Context context, boolean collapsed, @NonNull final GPXUtilities.GPXFile gpxFile, WptPt selectedPoint) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		List<WptPt> points = gpxFile.getPoints();
		String selectedCategory = selectedPoint != null && selectedPoint.category != null ? selectedPoint.category : "";
		int showedCount = 0;
		for (final WptPt point : points) {
			String currentCategory = point != null ? point.category : null;
			if (selectedCategory.equals(currentCategory)) {
				showedCount++;
				boolean selected = selectedPoint != null && selectedPoint.equals(point);
				TextViewEx button = buildButtonInCollapsableView(context, selected, false);
				button.setText(point.name);

				if (!selected) {
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
							PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, point.name);
							mapActivity.getContextMenu().setCenterMarker(true);
							mapActivity.getContextMenu().show(latLon, pointDescription, point);
						}
					});
				}
				view.addView(button);
			}
			if (showedCount >= 10) {
				break;
			}
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmAndAppCustomization appCustomization = app.getAppCustomization();
					final Intent intent = new Intent(context, appCustomization.getTrackActivity());
					intent.putExtra(TrackActivity.TRACK_FILE_NAME, gpxFile.path);
					intent.putExtra(TrackActivity.OPEN_POINTS_TAB, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}
