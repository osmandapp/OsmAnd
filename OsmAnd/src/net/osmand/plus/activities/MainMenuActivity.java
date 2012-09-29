package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.Random;

import net.osmand.Version;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.render.MapRenderRepositories;
import android.app.Activity;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

public class MainMenuActivity extends Activity {

	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static final String VECTOR_INDEXES_CHECK = "VECTOR_INDEXES_CHECK"; //$NON-NLS-1$
	private static final String TIPS_SHOW = "TIPS_SHOW"; //$NON-NLS-1$
	private static final String VERSION_INSTALLED = "VERSION_INSTALLED"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = ResourceManager.APP_DIR + "exception.log"; //$NON-NLS-1$
	
	private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	
	public static final int APP_EXIT_CODE = 4;
	public static final String APP_EXIT_KEY = "APP_EXIT_KEY";
	
	private ProgressDialog startProgressDialog;
	
	public void checkPreviousRunsForExceptions(boolean firstTime) {
		long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
		final File file = ((OsmandApplication) getApplication()).getSettings().extendOsmandPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !firstTime) {
				String msg = MessageFormat.format(getString(R.string.previous_run_crashed), OsmandApplication.EXCEPTION_PATH);
				Builder builder = new AccessibleAlertBuilder(MainMenuActivity.this);
				builder.setMessage(msg).setNeutralButton(getString(R.string.close), null);
				builder.setPositiveButton(R.string.send_report, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "osmand.app@gmail.com" }); //$NON-NLS-1$
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
						text.append("\nApp Version : ").append(Version.getAppName(MainMenuActivity.this)); //$NON-NLS-1$
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == APP_EXIT_CODE){
			finish();
		}
	}
	
	public static Animation getAnimation(int left, int top){
		Animation anim = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, left, 
				TranslateAnimation.RELATIVE_TO_SELF, 0, TranslateAnimation.RELATIVE_TO_SELF, top, TranslateAnimation.RELATIVE_TO_SELF, 0);
		anim.setDuration(700);
		anim.setInterpolator(new AccelerateInterpolator());
		return anim;
	}
	
	public static void onCreateMainMenu(Window window, final Activity activity){
		View head = (View) window.findViewById(R.id.Headliner);
		head.startAnimation(getAnimation(0, -1));
		
		View leftview = (View) window.findViewById(R.id.MapButton);
		leftview.startAnimation(getAnimation(-1, 0));
		leftview = (View) window.findViewById(R.id.FavoritesButton);
		leftview.startAnimation(getAnimation(-1, 0));
		
		View rightview = (View) window.findViewById(R.id.SettingsButton);
		rightview.startAnimation(getAnimation(1, 0));
		rightview = (View) window.findViewById(R.id.SearchButton);
		rightview.startAnimation(getAnimation(1, 0));
		
		String textVersion = Version.getAppVersion(activity);
		final TextView textVersionView = (TextView) window.findViewById(R.id.TextVersion);
		textVersionView.setText(textVersion);
		SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
		
		// only one commit should be with contribution version flag
//		 prefs.edit().putBoolean(CONTRIBUTION_VERSION_FLAG, true).commit();
		if (prefs.contains(CONTRIBUTION_VERSION_FLAG)) {
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
		View helpButton = window.findViewById(R.id.HelpButton);
		helpButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TipsAndTricksActivity tactivity = new TipsAndTricksActivity(activity);
				Dialog dlg = tactivity.getDialogToShowTips(false, true);
				dlg.show();
			}
		});
	}
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean exit = false;
		if(getIntent() != null){
			Intent intent = getIntent();
			if(intent.getExtras() != null && intent.getExtras().containsKey(APP_EXIT_KEY)){
				exit = true;
				finish();
			}
		}
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);
		
		onCreateMainMenu(getWindow(), this);

		Window window = getWindow();
		final Activity activity = this;
		View showMap = window.findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent mapIndent = new Intent(activity, OsmandIntents.getMapActivity());
				activity.startActivityForResult(mapIndent, 0);
			}
		});
		View settingsButton = window.findViewById(R.id.SettingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(activity, OsmandIntents.getSettingsActivity());
				activity.startActivity(settings);
			}
		});

		View favouritesButton = window.findViewById(R.id.FavoritesButton);
		favouritesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent favorites = new Intent(activity, OsmandIntents.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});

		final View closeButton = window.findViewById(R.id.CloseButton);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getMyApplication().closeApplication();
				//moveTaskToBack(true);
				activity.finish();
				// this is different from MapActivity close...
				if (getMyApplication().getNavigationService() == null) {
					//http://stackoverflow.com/questions/2092951/how-to-close-android-application
					System.runFinalizersOnExit(true);
					System.exit(0);
				}
			}
		});
		View searchButton = window.findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent search = new Intent(activity, OsmandIntents.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(search);
			}
		});
		if(exit){
			return;
		}
		OsmandApplication app = ((OsmandApplication) getApplication());
		// restore follow route mode
		if(app.getSettings().FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()){
			final Intent mapIndent = new Intent(this, OsmandIntents.getMapActivity());
			startActivityForResult(mapIndent, 0);
			return;
		}
		startProgressDialog = new ProgressDialog(this);
		getMyApplication().checkApplicationIsBeingInitialized(this, startProgressDialog);
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		boolean firstTime = false;
		if(!pref.contains(FIRST_TIME_APP_RUN)){
			firstTime = true;
			pref.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(activity)).commit();
			
			applicationInstalledFirstTime();
		} else {
			int i = pref.getInt(TIPS_SHOW, 0);
			if (i < 7){
				pref.edit().putInt(TIPS_SHOW, ++i).commit();
			}
			boolean appVersionChanged = false;
			if(!Version.getFullVersion(activity).equals(pref.getString(VERSION_INSTALLED, ""))){
				pref.edit().putString(VERSION_INSTALLED, Version.getFullVersion(activity)).commit();
				appVersionChanged = true;
			}
						
			if (i == 1 || i == 5 || appVersionChanged) {
				TipsAndTricksActivity tipsActivity = new TipsAndTricksActivity(this);
				Dialog dlg = tipsActivity.getDialogToShowTips(!appVersionChanged, false);
				dlg.show();
			} else {
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
		}
		checkPreviousRunsForExceptions(firstTime);
	}

	private void applicationInstalledFirstTime() {
		boolean netOsmandWasInstalled = false;
		try {
			ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("net.osmand", PackageManager.GET_META_DATA);
			netOsmandWasInstalled = applicationInfo != null;
		} catch (NameNotFoundException e) {
			netOsmandWasInstalled = false;
		}
		
		if(netOsmandWasInstalled){
			Builder builder = new AccessibleAlertBuilder(this);
			builder.setMessage(R.string.osmand_net_previously_installed);
			builder.setPositiveButton(R.string.default_buttons_ok, null);
			builder.show();
		} else {
			Builder builder = new AccessibleAlertBuilder(this);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(MainMenuActivity.this, DownloadIndexActivity.class));
				}

			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			builder.show();
		}
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
					startActivity(new Intent(MainMenuActivity.this, DownloadIndexActivity.class));
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

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}
	

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH
                && event.getRepeatCount() == 0) {
			final Intent search = new Intent(MainMenuActivity.this, SearchActivity.class);
			search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(search);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
	
	
}
