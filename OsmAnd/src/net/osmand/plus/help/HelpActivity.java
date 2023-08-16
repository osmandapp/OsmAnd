package net.osmand.plus.help;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.FeedbackHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.plugins.development.BaseLogcatActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class HelpActivity extends BaseLogcatActivity {

	private static final int MENU_MORE_ID = 0;
	private static final int LOGCAT_READ_MS = 5 * 1000;

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private HelpArticlesHelper articlesHelper;
	private boolean nightMode;

	@NonNull
	public HelpArticlesHelper getArticlesHelper() {
		return articlesHelper;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		uiUtilities = app.getUIUtilities();
		nightMode = !app.getSettings().isLightContent();

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

	public void updateContent() {
		FragmentManager manager = getSupportFragmentManager();
		HelpMainFragment fragment = (HelpMainFragment) manager.findFragmentByTag(HelpMainFragment.TAG);
		if (fragment != null && fragment.isAdded()) {
			fragment.updateContent();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(0, MENU_MORE_ID, 0, R.string.shared_string_more);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setContentDescription(getString(R.string.shared_string_more));

		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		item.setIcon(uiUtilities.getIcon(R.drawable.ic_overflow_menu_white, colorId));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			onBackPressed();
			return true;
		} else if (id == MENU_MORE_ID) {
			showOptionsMenu(findViewById(MENU_MORE_ID));
		}
		return super.onOptionsItemSelected(item);
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		File exceptionLog = app.getAppPath(FeedbackHelper.EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(new PopUpMenuItem.Builder(this)
					.setTitleId(R.string.send_crash_log)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_bug_outlined_send))
					.setOnClickListener(v -> app.getFeedbackHelper().sendCrashLog()).create());
		}
		items.add(new PopUpMenuItem.Builder(this)
				.setTitleId(R.string.send_logcat_log)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_file_report_outlined_send))
				.setOnClickListener(v -> readAndSaveLogs()).create());

		items.add(new PopUpMenuItem.Builder(this)
				.setTitleId(R.string.copy_build_version)
				.showTopDivider(true)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_osmand_logo))
				.setOnClickListener(v -> ShareMenu.copyToClipboardWithToast(this,
						Version.getFullVersionWithReleaseDate(app), Toast.LENGTH_SHORT)).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLogcatAsyncTask();
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