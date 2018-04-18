package net.osmand.plus.wikivoyage;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient {

	private static final String TAG = WikivoyageWebViewClient.class.getSimpleName();

	private OsmandApplication app;
	private FragmentManager fragmentManager;
	private Context context;
	private GPXUtilities.GPXFile gpxFile;

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WEB_DOMAIN = ".wikivoyage.com/wiki/";

	public WikivoyageWebViewClient(FragmentActivity context, FragmentManager fm) {
		app = (OsmandApplication) context.getApplication();
		fragmentManager = fm;
		this.context = context;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.contains(WEB_DOMAIN)) {
			String lang = url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
			String articleName = url.replace(PAGE_PREFIX_HTTPS + lang + WEB_DOMAIN, "")
					.replaceAll("_", " ");
			try {
				articleName = URLDecoder.decode(articleName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			long articleId = app.getTravelDbHelper().getArticleId(articleName, lang);
			if (articleId != 0) {
				WikivoyageArticleDialogFragment.showInstance(app, fragmentManager, articleId, lang);
			} else {
				warnAboutExternalLoad(url);
			}
			return true;
		} else if (url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS)) {
			warnAboutExternalLoad(url);
		} else if (url.startsWith(PREFIX_GEO)) {
			if (gpxFile != null) {
				List<GPXUtilities.WptPt> points = gpxFile.getPoints();
				GPXUtilities.WptPt gpxPoint = null;
				String coordinates = url.replace(PREFIX_GEO, "");
				double lat;
				double lon;
				try {
					lat = Double.valueOf(coordinates.substring(0, coordinates.indexOf(",")));
					lon = Double.valueOf(coordinates.substring(coordinates.indexOf(",") + 1,
							coordinates.length()));
				} catch (NumberFormatException e) {
					Log.w(TAG, e.getMessage(), e);
					return true;
				}
				for (GPXUtilities.WptPt point : points) {
					if (point.getLatitude() == lat && point.getLongitude() == lon) {
						gpxPoint = point;
						break;
					}
				}
				if (gpxPoint != null) {
					final OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(lat, lon,	settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxPoint.name),
							false,
							gpxPoint);
					fragmentManager.popBackStackImmediate(WikivoyageExploreDialogFragment.TAG,
							POP_BACK_STACK_INCLUSIVE);
					MapActivity.launchMapActivityMoveToTop(context);
				}
			}
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(i);
		}
		return true;
	}

	private void warnAboutExternalLoad(final String url) {
		new AlertDialog.Builder(context)
				.setTitle(url)
				.setMessage(R.string.online_webpage_warning)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						context.startActivity(i);
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	public void setGpxFile(GPXUtilities.GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}
}
