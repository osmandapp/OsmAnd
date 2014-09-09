package net.osmand.plus.download;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavouritesActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis on 08.09.2014.
 */
public class DownloadActivity extends SherlockFragmentActivity {

	private TabHost tabHost;
	private FavouritesActivity.TabsAdapter mTabsAdapter;
	public static DownloadIndexesThread downloadListIndexThread;
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);


		setContentView(R.layout.tab_content);
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
		mTabsAdapter = new FavouritesActivity.TabsAdapter(this, tabHost, viewPager, settings);
		mTabsAdapter.addTab(tabHost.newTabSpec("LOCAL_INDEX").setIndicator("Local maps"),
				LocalIndexesFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("DOWNLOADS").setIndicator("Downloads"),
				DownloadIndexFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("UPDATES").setIndicator("Updates"),
				UpdatesIndexFragment.class, null);
		tabHost.setCurrentTab(0);

		if(downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexesThread(this);
		}
		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			downloadListIndexThread.runCategorization(type);
		} else {
			downloadListIndexThread.runReloadIndexFiles();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		BasicProgressAsyncTask<?, ?, ?> t = downloadListIndexThread.getCurrentRunningTask();
		if(t instanceof DownloadIndexesThread.DownloadIndexesAsyncTask) {

		}
	}

	public DownloadActivityType getType() { return type;}

	public void setType(DownloadActivityType type) { this.type = type;}

	public void changeType(final DownloadActivityType tp) {
		invalidateOptionsMenu();
		if (downloadListIndexThread != null && type != tp) {
			type = tp;
			downloadListIndexThread.runCategorization(tp);
		}
	}

	public void downloadFilesPreCheckSpace() {
		double sz = 0;
		List<DownloadEntry> list = downloadListIndexThread.flattenDownloadEntries();
		for (DownloadEntry es :  list) {
			sz += es.sizeMB;
		}
		// get availabile space
		double asz = downloadListIndexThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), list.size(), sz, asz));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadListIndexThread.runDownloadFiles();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			downloadListIndexThread.runDownloadFiles();
		}

	}

	public Map<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
		if(downloadListIndexThread == null) {
			return new LinkedHashMap<IndexItem, List<DownloadEntry>>();
		}
		return downloadListIndexThread.getEntriesToDownload();
	}

	@Override
	public void onPause() {
		super.onPause();
		((OsmandApplication)getApplication()).setDownloadActivity(null);
	}

}
