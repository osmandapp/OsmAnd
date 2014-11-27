package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.sherpafy.TourViewActivity;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.ibm.icu.util.Calendar;

/**
 * Created by Denis on 05.11.2014.
 */
public class DashboardActivity extends SherlockFragmentActivity implements IMapDownloaderCallback {
	public static final boolean TIPS_AND_TRICKS = false;
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";
	
	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String TIPS_SHOW = "TIPS_SHOW"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$
	
	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	private ProgressDialog startProgressDialog;
	private OsmandMapTileView osmandMapTileView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		if(getIntent() != null){
			Intent intent = getIntent();
			if(intent.getExtras() != null && intent.getExtras().containsKey(APP_EXIT_KEY)){
				getMyApplication().closeApplication(this);
				return;
			}
		}
		if(Version.isSherpafy(getMyApplication())) {
			final Intent mapIntent = new Intent(this, TourViewActivity.class);
			startActivity(mapIntent);
			finish();
			return;
		}
		setContentView(R.layout.dashboard);
		setupContributionVersion();
		getSupportActionBar().setTitle(Version.getFullVersion(getMyApplication()));
		ColorDrawable color = new ColorDrawable(getResources().getColor(R.color.actionbar_color));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);
		setupMapView();
		setupButtons();
		setupFonts();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == APP_EXIT_CODE){
			getMyApplication().closeApplication(this);
		}
	}
	
	protected void setupContributionVersion() {
		final TextView textVersionView = (TextView) findViewById(R.id.Copyright);
		final Calendar inst = Calendar.getInstance();
		inst.setTime(new Date());
		final String textVersion = "@OsmAnd " + inst.get(Calendar.YEAR);
		textVersionView.setText(textVersion);
		final SharedPreferences prefs = getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
		textVersionView.setOnClickListener(new OnClickListener(){

			int i = 0;
			@Override
			public void onClick(View v) {
				if(i++ > 8 && Version.isDeveloperVersion(getMyApplication())) {
					prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
					enableLink(DashboardActivity.this, textVersion, textVersionView);
				}
			}
		});
		// only one commit should be with contribution version flag
//		 prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
		if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(getMyApplication())) {
			enableLink(this, textVersion, textVersionView);
		}
		final TextView about = (TextView) findViewById(R.id.About);
		final String aboutString = getString(R.string.about_settings);
		SpannableString ss = new SpannableString(aboutString);
		ClickableSpan clickableSpan = new ClickableSpan() {
		    @Override
		    public void onClick(View textView) {
		    	showAboutDialog(DashboardActivity.this, getMyApplication());
		    }
		};
		ss.setSpan(clickableSpan, 0, aboutString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		about.setText(ss);
		about.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	public static void showAboutDialog(final Activity activity, final OsmandApplication app) {
		Builder bld = new AlertDialog.Builder(activity);
		bld.setTitle(R.string.about_settings);
        ScrollView sv = new ScrollView(activity);
        TextView tv = new TextView(activity);
        sv.addView(tv);
		String version = Version.getFullVersion(app);
		String vt = activity.getString(R.string.about_version) +"\t";
		int st = vt.length();
		String edition = "";
        SharedPreferences prefs = app.getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
        if (prefs.contains(CONTRIBUTION_VERSION_FLAG) && Version.isDeveloperVersion(app)) {
            try {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
                Date date = new Date(new File(appInfo.sourceDir).lastModified());
                edition = activity.getString(R.string.local_index_installed) + " :\t" + DateFormat.getDateFormat(app).format(date);
            } catch (Exception e) {
            }
            SpannableString content = new SpannableString(vt + version +"\n" +
    				edition +"\n\n"+
    				activity.getString(R.string.about_content));
    		content.setSpan(new ClickableSpan() {
    			@Override
    			public void onClick(View widget) {
    				final Intent mapIntent = new Intent(activity, ContributionVersionActivity.class);
    				activity.startActivityForResult(mapIntent, 0);
    			}
    			
    		}, st, st + version.length(), 0);
    		tv.setText(content);
        } else {
        	tv.setText(vt + version +"\n\n" +
    				activity.getString(R.string.about_content));
        }
        
		tv.setPadding(5, 0, 5, 5);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		bld.setView(sv);
		bld.setPositiveButton(R.string.default_buttons_ok, null);
		bld.show();
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupFavorites();
	}
	
	
	
	protected void initApp(final OsmAndAppCustomization appCustomization, final Activity activity, OsmandApplication app) {
		// restore follow route mode
		if(app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()){
			final Intent mapIndent = new Intent(this, appCustomization.getMapActivity());
			startActivityForResult(mapIndent, 0);
			return;
		}
		startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
		boolean dialogShown = false;
		boolean firstTime = false;
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		boolean appVersionChanged = false;
		if (!pref.contains(FIRST_TIME_APP_RUN)) {
			firstTime = true;
			pref.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
		} else if (!Version.getFullVersion(app).equals(pref.getString(VERSION_INSTALLED, ""))) {
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			appVersionChanged = true;
		}
		if (appCustomization.showFirstTimeRunAndTips(firstTime, appVersionChanged)) {
			if (firstTime) {
				applicationInstalledFirstTime();
				dialogShown = true;
			} else {
				int i = pref.getInt(TIPS_SHOW, 0);
				if (i < 7) {
					pref.edit().putInt(TIPS_SHOW, ++i).commit();
				}
				if (i == 1 || i == 5 || appVersionChanged) {
					if(TIPS_AND_TRICKS) {
					TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(this);
					Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
					dlg.show();
					dialogShown = true;
					} else {
						if(appVersionChanged) {
							final Intent helpIntent = new Intent(activity, HelpActivity.class);
							helpIntent.putExtra(HelpActivity.TITLE, Version.getAppVersion(getMyApplication()));
							helpIntent.putExtra(HelpActivity.URL, "changes-1.9.html");
							activity.startActivity(helpIntent);
							dialogShown = true;
						}
					}
				}
			}
		}
		if(!dialogShown && appCustomization.checkBasemapDownloadedOnStart()) {
			if (startProgressDialog.isShowing()) {
				startProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						checkVectorIndexesDownloaded();
					}
				});
			} else {
				checkVectorIndexesDownloaded();
			}
		}
		if(appCustomization.checkExceptionsOnStart() && !dialogShown){
			checkPreviousRunsForExceptions(firstTime);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}
	
	private void applicationInstalledFirstTime() {
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null && !Version.isFreeVersion(getMyApplication());
		} catch (NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}
		
		if(netOsmandWasInstalled){
//			Builder builder = new AccessibleAlertBuilder(this);
//			builder.setMessage(R.string.osmand_net_previously_installed);
//			builder.setPositiveButton(R.string.default_buttons_ok, null);
//			builder.show();
		} else {
			Builder builder = new AccessibleAlertBuilder(this);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(DashboardActivity.this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
				}

			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
	}
	
	public void checkPreviousRunsForExceptions(boolean firstTime) {
		long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final OsmandApplication app = ((OsmandApplication) getApplication());
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
				Builder builder = new AccessibleAlertBuilder(DashboardActivity.this);
				builder.setMessage(msg).setNeutralButton(getString(R.string.close), null);
				builder.setPositiveButton(R.string.send_report, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "osmand.app+crash@gmail.com" }); //$NON-NLS-1$
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
						intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
						intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug"); //$NON-NLS-1$
						StringBuilder text = new StringBuilder();
						text.append("\nDevice : ").append(Build.DEVICE); //$NON-NLS-1$
						text.append("\nBrand : ").append(Build.BRAND); //$NON-NLS-1$
						text.append("\nModel : ").append(Build.MODEL); //$NON-NLS-1$
						text.append("\nProduct : ").append(Build.PRODUCT); //$NON-NLS-1$
						text.append("\nBuild : ").append(Build.DISPLAY); //$NON-NLS-1$
						text.append("\nVersion : ").append(Build.VERSION.RELEASE); //$NON-NLS-1$
						text.append("\nApp Version : ").append(Version.getAppName(app)); //$NON-NLS-1$
						try {
							PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
							if (info != null) {
								text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
							}
						} catch (NameNotFoundException e) {
						}
						intent.putExtra(Intent.EXTRA_TEXT, text.toString());
						startActivity(Intent.createChooser(intent, getString(R.string.send_report)));
					}

				});
				builder.show();
			}
			getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
		} else {
			if (size > 0) {
				getPreferences(MODE_WORLD_WRITEABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
		}
	}

	private void setupFonts() {
		Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
		((TextView) findViewById(R.id.search_for)).setTypeface(typeface);
		((TextView) findViewById(R.id.map_text)).setTypeface(typeface);
		((TextView) findViewById(R.id.fav_text)).setTypeface(typeface);
		((Button) findViewById(R.id.show_map)).setTypeface(typeface);
		((Button) findViewById(R.id.show_all)).setTypeface(typeface);
	}

	private void setupFavorites(){
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		final List<FavouritePoint> points = helper.getFavouritePoints();
		LinearLayout favorites = (LinearLayout) findViewById(R.id.favorites);
		favorites.removeAllViews();
		if (points.size() == 0){
			(findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		}

		if (points.size() > 3){
			while (points.size() != 3){
				points.remove(3);
			}
		}


		for (final FavouritePoint point : points) {
			LayoutInflater inflater = getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_fav_list, null, false);
			TextView name = (TextView) view.findViewById(R.id.name);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			name.setText(point.getName());
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(DashboardActivity.this, point.getColor()));
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			label.setText(distance, TextView.BufferType.SPANNABLE);
			label.setTypeface(Typeface.DEFAULT, point.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final Intent mapIntent = new Intent(DashboardActivity.this, getMyApplication().getAppCustomization().getMapActivity());
					getMyApplication().getSettings().setLastKnownMapLocation(point.getLatitude(), point.getLongitude());
					startActivity(mapIntent);
				}
			});
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			favorites.addView(view);
		}
	}

	private void setupButtons(){
		final Activity activity = this;
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();

		(findViewById(R.id.show_map)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent mapIndent = new Intent(activity, appCustomization.getMapActivity());
				activity.startActivityForResult(mapIndent, 0);
			}
		});

		(findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});

		(findViewById(R.id.poi)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.POI_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.address)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.ADDRESS_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.coord)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.LOCATION_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.fav_btn)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.FAVORITES_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.history)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.HISTORY_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.transport)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.TRANSPORT_TAB_INDEX);
				activity.startActivity(search);
			}
		});
	}

	private void setupMapView() {
		OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) findViewById(R.id.MapView);
		osmandMapTileView = surf.getMapView();
		osmandMapTileView.getView().setVisibility(View.VISIBLE);
		osmandMapTileView.removeAllLayers();
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null);
		MapTextLayer mapTextLayer = new MapTextLayer();
		mapTextLayer.setAlwaysVisible(true);
		// 5.95 all labels
		osmandMapTileView.addLayer(mapTextLayer, 5.95f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
		LatLon lm = getMyApplication().getSettings().getLastKnownMapLocation();
		int zm = getMyApplication().getSettings().getLastKnownMapZoom();
		osmandMapTileView.setLatLon(lm.getLatitude(), lm.getLongitude());
		osmandMapTileView.setIntZoom(zm);
		osmandMapTileView.refreshMap(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.tips_and_tricks).setIcon(R.drawable.ic_ac_help)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 1, 0, R.string.settings).setIcon(R.drawable.ic_ac_settings)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 2, 0, R.string.exit_Button).setIcon(R.drawable.ic_ac_close)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}



	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		if (item.getItemId() == 0) {
			if(TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == 1){
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == 2){
			getMyApplication().closeApplication(this);
		}
		return true;
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	protected void checkVectorIndexesDownloaded() {
		MapRenderRepositories maps = getMyApplication().getResourceManager().getRenderer();
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		boolean check = pref.getBoolean(VECTOR_INDEXES_CHECK, true);
		// do not show each time 
		if (check && new Random().nextInt() % 5 == 1) {
			Builder builder = new AccessibleAlertBuilder(this);
			if(maps.isEmpty()){
				builder.setMessage(R.string.vector_data_missing);
			} else if(!maps.basemapExists()){
				builder.setMessage(R.string.basemap_missing);
			} else {
				return;
			}
			builder.setPositiveButton(R.string.download_files, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(DashboardActivity.this, DownloadActivity.class));
				}

			});
			builder.setNeutralButton(R.string.vector_map_not_needed, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getPreferences(MODE_WORLD_WRITEABLE).edit().putBoolean(VECTOR_INDEXES_CHECK, false).commit();
				}
			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
		
	}
	
	private static void enableLink(final Activity activity, String textVersion, TextView textVersionView) {
		SpannableString content = new SpannableString(textVersion);
		content.setSpan(new ClickableSpan() {

			@Override
			public void onClick(View widget) {
				final Intent mapIntent = new Intent(activity, ContributionVersionActivity.class);
				activity.startActivityForResult(mapIntent, 0);
			}
		}, 0, content.length(), 0);
		textVersionView.setText(content);
		textVersionView.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		if(request != null && !request.error && request.fileToSave != null){
			ResourceManager mgr = getMyApplication().getResourceManager();
			mgr.tileDownloaded(request);
		}
		if(request == null || !request.error){
			if(osmandMapTileView != null) {
				osmandMapTileView.tileDownloaded(request);
			}
		}		
	}
}
