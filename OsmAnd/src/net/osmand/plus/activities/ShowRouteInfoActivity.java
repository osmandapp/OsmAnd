/**
 *
 */

package net.osmand.plus.activities;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.util.Algorithms;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


/**
 *
 */
public class ShowRouteInfoActivity extends OsmandListActivity {


	private static final int SAVE = 0;
	private static final int SHARE = 1;
	private static final int PRINT = 2;
	private RoutingHelper helper;
	private TextView header;
	private DisplayMetrics dm;

	public static final String START_NAVIGATION = "START_NAVIGATION";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.default_list_view);
		ListView lv = (ListView) findViewById(android.R.id.list);
		View headerView = getLayoutInflater().inflate(R.layout.route_details_header, null);
		header = (TextView) headerView.findViewById(R.id.header);
		helper = ((OsmandApplication)getApplication()).getRoutingHelper();
		((ImageView)
				headerView.findViewById(R.id.start_navigation)).setImageDrawable(
						getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_start_navigation, R.color.color_myloc_distance));
		headerView.findViewById(R.id.start_navigation).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ShowRouteInfoActivity.this, MapActivity.class);
				i.putExtra(START_NAVIGATION, START_NAVIGATION);
				startActivity(i);
			}
		});
		lv.addHeaderView(headerView);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SAVE) {
			MapActivityActions.createSaveDirections(ShowRouteInfoActivity.this, helper).show();
			return true;
		}
        if (item.getItemId() == SHARE) {
              final GPXFile gpx = helper.generateGPXFileWithRoute();

              final Intent sendIntent = new Intent();
              sendIntent.setAction(Intent.ACTION_SEND);
              sendIntent.putExtra(Intent.EXTRA_TEXT, GPXUtilities.asString(gpx, getMyApplication()));
              sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_route_subject));
              sendIntent.setType("application/gpx+xml");
              startActivity(sendIntent);
            return true;
        }
        if (item.getItemId() == PRINT) {
        	print();
          return true;
      }

		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onResume() {
		super.onResume();
		header.setText(helper.getGeneralRouteInformation());
		float f = Math.min(dm.widthPixels/(dm.density*160),dm.heightPixels/(dm.density*160));
		if (f >= 3) {
			// large screen
			header.setTextSize(dm.scaledDensity * 23);
		}
		setListAdapter(new RouteInfoAdapter(helper.getRouteDirections()));
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		createMenuItem(menu, PRINT, R.string.print_route,
				R.drawable.ic_action_gprint_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SAVE, R.string.shared_string_save_as_gpx,
				R.drawable.ic_action_gsave_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SHARE, R.string.share_route_as_gpx,
				R.drawable.ic_action_gshare_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(position < 1){
			return;
		}
		RouteDirectionInfo item = ((RouteInfoAdapter)getListAdapter()).getItem(position - 1);
		Location loc = helper.getLocationFromRouteDirection(item);
		if(loc != null){
			MapRouteInfoMenu.directionInfo = position - 1;
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			settings.setMapLocationToShow(loc.getLatitude(),loc.getLongitude(),
					Math.max(13, settings.getLastKnownMapZoom()), 
					new PointDescription(PointDescription.POINT_TYPE_MARKER, item.getDescriptionRoutePart() + " " + getTimeDescription(item)),
					false, null);
			MapActivity.launchMapActivityMoveToTop(this);
		}
	}

	class RouteInfoAdapter extends ArrayAdapter<RouteDirectionInfo> {
		public class CumulativeInfo {
			public int distance;
			public int time;

			public CumulativeInfo() {
				distance = 0;
				time = 0;
			}
		}

		RouteInfoAdapter(List<RouteDirectionInfo> list) {
			super(ShowRouteInfoActivity.this, R.layout.route_info_list_item, list);
			this.setNotifyOnChange(false);
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.route_info_list_item, parent, false);
			}
			RouteDirectionInfo model = (RouteDirectionInfo) getItem(position);
			TextView label = (TextView) row.findViewById(R.id.description);
			TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
			TextView timeLabel = (TextView) row.findViewById(R.id.time);
			TextView cumulativeDistanceLabel = (TextView) row.findViewById(R.id.cumulative_distance);
			TextView cumulativeTimeLabel = (TextView) row.findViewById(R.id.cumulative_time);
			ImageView icon = (ImageView) row.findViewById(R.id.direction);

			TurnPathHelper.RouteDrawable drawable = new TurnPathHelper.RouteDrawable(getResources());
			drawable.setRouteType(model.getTurnType());
			icon.setImageDrawable(drawable);

			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(
					model.distance, getMyApplication()));
			timeLabel.setText(getTimeDescription(model));
			label.setText(model.getDescriptionRoutePart());
			CumulativeInfo cumulativeInfo = getRouteDirectionCumulativeInfo(position);
			cumulativeDistanceLabel.setText(OsmAndFormatter.getFormattedDistance(
					cumulativeInfo.distance, getMyApplication()));
			cumulativeTimeLabel.setText(Algorithms.formatDuration(cumulativeInfo.time));
			return row;
		}

		public CumulativeInfo getRouteDirectionCumulativeInfo(int position) {
			CumulativeInfo cumulativeInfo = new CumulativeInfo();
			for (int i = 0; i < position; i++) {
				RouteDirectionInfo routeDirectionInfo = (RouteDirectionInfo) getItem(i);
				cumulativeInfo.time += routeDirectionInfo.getExpectedTime();
				cumulativeInfo.distance += routeDirectionInfo.distance;
			}
			return cumulativeInfo;
		}
	}

	private String getTimeDescription(RouteDirectionInfo model) {
		final int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds);
	}

	void print() {
		File file = generateRouteInfoHtml((RouteInfoAdapter)getListAdapter(),
				helper.getGeneralRouteInformation());
		if (file.exists()) {
			Uri uri = Uri.fromFile(file);
			Intent browserIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // use Android Print Framework
				browserIntent = new Intent(this, PrintDialogActivity.class)
						.setDataAndType(uri, "text/html");
			} else { // just open html document
				browserIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(
						uri, "text/html");
			}
			startActivity(browserIntent);
		}
	}

	private File generateRouteInfoHtml(RouteInfoAdapter routeInfo, String title) {
		File file = null;
		if (routeInfo == null) {
			return file;
		}

		final String FILE_NAME = "route_info.html";
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		html.append("<head>");
		html.append("<title>Route info</title>");
		html.append("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />");
		html.append("<style>");
		html.append("table, th, td {");
		html.append("border: 1px solid black;");
		html.append("border-collapse: collapse;}");
		html.append("th, td {");
		html.append("padding: 5px;}");
		html.append("</style>");
		html.append("</head>");
		html.append("<body>");

		FileOutputStream fos = null;
		try {
			if (!TextUtils.isEmpty(title)) {
				html.append("<h1>");
				html.append(title);
				html.append("</h1>");
			}
			html.append("<table style=\"width:100%\">");
			final String NBSP = "&nbsp;";
			final String BR = "<br>";
			for (int i = 0; i < routeInfo.getCount(); i++) {
				RouteDirectionInfo routeDirectionInfo = (RouteDirectionInfo) routeInfo
						.getItem(i);
				html.append("<tr>");
				StringBuilder sb = new StringBuilder();
				sb.append(OsmAndFormatter.getFormattedDistance(
						routeDirectionInfo.distance, getMyApplication()));
				sb.append(", ");
				sb.append(getTimeDescription(routeDirectionInfo));
				String distance = sb.toString().replaceAll("\\s", NBSP);
				html.append("<td>");
				html.append(distance);
				html.append("</td>");
				String description = routeDirectionInfo
						.getDescriptionRoutePart();
				html.append("<td>");
				html.append(description);
				html.append("</td>");
				RouteInfoAdapter.CumulativeInfo cumulativeInfo = routeInfo
						.getRouteDirectionCumulativeInfo(i);
				html.append("<td>");
				sb = new StringBuilder();
				sb.append(OsmAndFormatter.getFormattedDistance(
						cumulativeInfo.distance, getMyApplication()));
				sb.append(" - ");
				sb.append(OsmAndFormatter.getFormattedDistance(
						cumulativeInfo.distance + routeDirectionInfo.distance,
						getMyApplication()));
				sb.append(BR);
				sb.append(Algorithms.formatDuration(cumulativeInfo.time));
				sb.append(" - ");
				sb.append(Algorithms.formatDuration(cumulativeInfo.time
						+ routeDirectionInfo.getExpectedTime()));
				String cumulativeTimeAndDistance = sb.toString().replaceAll("\\s", NBSP);
				html.append(cumulativeTimeAndDistance);
				html.append("</td>");
				html.append("</tr>");
			}
			html.append("</table>");
			html.append("</body>");
			html.append("</html>");

			file = ((OsmandApplication) getApplication()).getAppPath(FILE_NAME);
			fos = new FileOutputStream(file);
			fos.write(html.toString().getBytes("UTF-8"));
			fos.flush();
		} catch (IOException e) {
			file = null;
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					file = null;
					e.printStackTrace();
				}
			}
		}

		return file;
	}

}

