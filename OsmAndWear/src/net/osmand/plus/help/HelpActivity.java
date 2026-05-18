package net.osmand.plus.help;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.development.BaseLogcatActivity;


public class HelpActivity extends BaseLogcatActivity {

	private static final int LOGCAT_READ_MS = 5 * 1000;

	private OsmandApplication app;
	private HelpArticlesHelper articlesHelper;

	@NonNull
	public HelpArticlesHelper getArticlesHelper() {
		return articlesHelper;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		articlesHelper = new HelpArticlesHelper(this);
		articlesHelper.loadArticles();

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_help);
			actionBar.setElevation(5.0f);
		}
		setContentView(R.layout.help_activity);

		if (savedInstanceState == null) {
			HelpMainFragment.showInstance(getSupportFragmentManager());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLogcatAsyncTask();
	}

	public void updateContent() {
		FragmentManager manager = getSupportFragmentManager();
		HelpMainFragment fragment = (HelpMainFragment) manager.findFragmentByTag(HelpMainFragment.TAG);
		if (fragment != null && fragment.isAdded()) {
			fragment.updateContent();
		}
	}

	protected void readAndSaveLogs() {
		logs.clear();

		startLogcatAsyncTask();
		setSupportProgressBarIndeterminateVisibility(true);

		app.runInUIThread(() -> {
			stopLogcatAsyncTask();
			startSaveLogsAsyncTask();
			setSupportProgressBarIndeterminateVisibility(false);
		}, LOGCAT_READ_MS);
	}

	@NonNull
	@Override
	protected String getFilterLevel() {
		return "";
	}
}