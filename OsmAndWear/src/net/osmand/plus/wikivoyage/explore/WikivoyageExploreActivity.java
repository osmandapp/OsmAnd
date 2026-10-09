package net.osmand.plus.wikivoyage.explore;

import static net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColorId;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OnDialogFragmentResultListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.search.WikivoyageSearchDialogFragment;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WikivoyageExploreActivity extends TabActivity implements DownloadEvents, OnDialogFragmentResultListener,
		TravelLocalDataHelper.Listener {

	private static final String TAB_SELECTED = "tab_selected";
	private static final String ARTICLE_ID_KEY = "article_id";
	private static final String SELECTED_LANG_KEY = "selected_lang";

	private static final int EXPLORE_POSITION = 0;
	private static final int SAVED_ARTICLES_POSITION = 1;

	private OsmandApplication app;
	private boolean nightMode;
	protected List<WeakReference<Fragment>> fragments = new ArrayList<>();

	private LockableViewPager viewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		nightMode = !settings.isLightContent();

		int themeId = nightMode ? R.style.OsmandDarkTheme_NoActionbar : R.style.OsmandLightTheme_NoActionbar_LightStatusBar;
		app.getLocaleHelper().setLanguage(this);
		setTheme(themeId);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.wikivoyage_explore);

		Window window = getWindow();
		if (window != null) {
			if (settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_NoAnimation;
			}
			window.setStatusBarColor(getResolvedColor(getStatusBarColor()));
		}

		Toolbar toolbar = findViewById(R.id.toolbar);
		Drawable icBack = getContentIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> finish());

		findViewById(R.id.options_button).setOnClickListener(v -> {
			FragmentManager manager = getSupportFragmentManager();
			WikivoyageOptionsBottomSheetDialogFragment fragment = new WikivoyageOptionsBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.show(manager, WikivoyageOptionsBottomSheetDialogFragment.TAG);
		});

		int searchColorId = ColorUtilities.getSecondaryTextColorId(nightMode);
		((TextView) findViewById(R.id.search_hint)).setTextColor(getResolvedColor(searchColorId));
		((ImageView) findViewById(R.id.search_icon))
				.setImageDrawable(getIcon(R.drawable.ic_action_search_dark, searchColorId));

		findViewById(R.id.search_button).setOnClickListener(v -> WikivoyageSearchDialogFragment.showInstance(getSupportFragmentManager()));

		viewPager = findViewById(R.id.view_pager);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setSwipeLocked(true);
		setViewPagerAdapter(viewPager, new ArrayList<>());
		OsmandFragmentPagerAdapter pagerAdapter = (OsmandFragmentPagerAdapter) viewPager.getAdapter();
		if (pagerAdapter != null) {
			pagerAdapter.addTab(getTabIndicator(R.string.shared_string_explore, ExploreTabFragment.class));
			pagerAdapter.addTab(getTabIndicator(R.string.saved_articles, SavedArticlesTabFragment.class));
		}

		ColorStateList navColorStateList = AndroidUtils.createBottomNavColorStateList(app, nightMode);
		BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
		bottomNav.setItemIconTintList(navColorStateList);
		bottomNav.setItemTextColor(navColorStateList);
		bottomNav.setOnItemSelectedListener(item -> {
			int position = -1;
			int i = item.getItemId();
			if (i == R.id.action_explore) {
				position = EXPLORE_POSITION;
			} else if (i == R.id.action_saved_articles) {
				position = SAVED_ARTICLES_POSITION;
			}
			if (position != -1 && position != viewPager.getCurrentItem()) {
				viewPager.setCurrentItem(position);
				return true;
			}
			return false;
		});

		updateSearchBarVisibility();
		populateData(true);
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		fragments.add(new WeakReference<>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			Uri data = intent.getData();
			if (data != null && ("http".equalsIgnoreCase(data.getScheme()) || "https".equalsIgnoreCase(data.getScheme()))) {
				parseLaunchIntentLink(data);
			} else {
				int currentItem = intent.getIntExtra(TAB_SELECTED, 0);
				if (currentItem == SAVED_ARTICLES_POSITION) {
					BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
					bottomNav.setSelectedItemId(R.id.action_saved_articles);
				}
				TravelArticleIdentifier articleId = intent.getParcelableExtra(ARTICLE_ID_KEY);
				String selectedLang = intent.getStringExtra(SELECTED_LANG_KEY);
				if (articleId != null) {
					WikivoyageArticleDialogFragment.showInstance(app, getSupportFragmentManager(), articleId, selectedLang);
				}
			}
			setIntent(null);
		}
		getMyApplication().getDownloadThread().setUiActivity(this);
		app.getTravelHelper().getBookmarksHelper().addListener(this);
	}

	protected void parseLaunchIntentLink(Uri data) {
		String host = data.getHost();
		String path = data.getPath();
		if (host != null && path != null && host.contains("osmand.net") && path.startsWith("/travel")) {
			String title = WikiArticleHelper.decodeTitleFromTravelUrl(data.getQueryParameter("title"));
			String selectedLang = data.getQueryParameter("lang");
			if (!Algorithms.isEmpty(title) && !Algorithms.isEmpty(selectedLang)) {
				TravelArticleIdentifier articleId = app.getTravelHelper().getArticleId(title, selectedLang);
				if (articleId != null) {
					WikivoyageArticleDialogFragment.showInstance(app, getSupportFragmentManager(), articleId, selectedLang);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		getMyApplication().getDownloadThread().resetUiActivity(this);
		app.getTravelHelper().getBookmarksHelper().removeListener(this);
	}

	@Nullable
	private ExploreTabFragment getExploreTabFragment() {
		for (WeakReference<Fragment> ref : fragments) {
			Fragment f = ref.get();
			if (f instanceof ExploreTabFragment) {
				return (ExploreTabFragment) f;
			}
		}
		return null;
	}

	@Nullable
	public SavedArticlesTabFragment getSavedArticlesTabFragment() {
		for (WeakReference<Fragment> ref : fragments) {
			Fragment f = ref.get();
			if (f instanceof SavedArticlesTabFragment) {
				return (SavedArticlesTabFragment) f;
			}
		}
		return null;
	}

	@Override
	public void onDialogFragmentResult(@NonNull String tag, int resultCode, @Nullable Bundle data) {
		if (tag.equals(WikivoyageOptionsBottomSheetDialogFragment.TAG)) {
			switch (resultCode) {
				case WikivoyageOptionsBottomSheetDialogFragment.DOWNLOAD_IMAGES_CHANGED:
				case WikivoyageOptionsBottomSheetDialogFragment.CACHE_CLEARED:
					invalidateTabAdapters();
					break;
				case WikivoyageOptionsBottomSheetDialogFragment.TRAVEL_BOOK_CHANGED:
					populateData(true);
					break;
			}
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		if (exploreTabFragment != null) {
			exploreTabFragment.onUpdatedIndexesList();
		}
	}

	@Override
	public void downloadInProgress() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		if (exploreTabFragment != null) {
			exploreTabFragment.downloadInProgress();
		}
	}

	@Override
	public void downloadHasFinished() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		if (exploreTabFragment != null) {
			exploreTabFragment.downloadHasFinished();
		}
	}

	private void applyIntentParameters(Intent intent, TravelArticle article) {
		intent.putExtra(TAB_SELECTED, viewPager.getCurrentItem());
		intent.putExtra(ARTICLE_ID_KEY, article.generateIdentifier());
		intent.putExtra(SELECTED_LANG_KEY, article.getLang());
	}

	public void setArticle(TravelArticle article) {
		Intent intent = new Intent(app, WikivoyageExploreActivity.class);
		applyIntentParameters(intent, article);
		setIntent(intent);
	}

	protected Drawable getContentIcon(int id) {
		return getIcon(id, R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int iconId) {
		return getIcon(iconId, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return app.getUIUtilities().getIcon(id, colorId);
	}

	@ColorRes
	protected int getStatusBarColor() {
		return getStatusBarSecondaryColorId(nightMode);
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(app, colorId);
	}

	public void populateData(boolean resetData) {
		switchProgressBarVisibility(true);
		if (app.isApplicationInitializing()) {
			WeakReference<WikivoyageExploreActivity> activityRef = new WeakReference<>(this);
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					init.removeListener(this);
					WikivoyageExploreActivity activity = activityRef.get();
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						new LoadWikivoyageData(activity, resetData).execute();
					}
				}
			});
		} else {
			new LoadWikivoyageData(this, resetData).execute();
		}
	}

	private void onDataLoaded() {
		switchProgressBarVisibility(false);
		updateSearchBarVisibility();
		updateFragments();
	}

	public void updateFragments() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		SavedArticlesTabFragment savedArticlesTabFragment = getSavedArticlesTabFragment();
		if (exploreTabFragment != null && savedArticlesTabFragment != null
				&& exploreTabFragment.isAdded() && savedArticlesTabFragment.isAdded()) {
			exploreTabFragment.populateData();
			savedArticlesTabFragment.savedArticlesUpdated();
		}
	}

	private void updateSearchBarVisibility() {
		boolean show = app.getTravelHelper().isAnyTravelBookPresent();
		findViewById(R.id.search_box).setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void switchProgressBarVisibility(boolean show) {
		findViewById(R.id.progress_bar).setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void invalidateTabAdapters() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		if (exploreTabFragment != null) {
			exploreTabFragment.invalidateAdapter();
		}
		SavedArticlesTabFragment savedArticlesTabFragment = getSavedArticlesTabFragment();
		if (savedArticlesTabFragment != null) {
			savedArticlesTabFragment.invalidateAdapter();
		}
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		OsmandFragmentPagerAdapter pagerAdapter = (OsmandFragmentPagerAdapter) viewPager.getAdapter();
		List<Fragment> tabFragments = new ArrayList<>();
		if (pagerAdapter != null) {
			for (int i = 0; i < pagerAdapter.getCount(); i++) {
				tabFragments.add(pagerAdapter.getItem(i));
			}
		}

		List<Fragment> fragmentsWithoutTabs = new ArrayList<>();
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			boolean isTabFragment = false;
			for (Fragment tabFragment : tabFragments) {
				if (fragment.getClass() == tabFragment.getClass()) {
					isTabFragment = true;
					break;
				}
			}
			if (!isTabFragment) {
				fragmentsWithoutTabs.add(fragment);
			}
		}
		return fragmentsWithoutTabs;
	}

	@Override
	public void setActivityAccessibility(boolean hideActivity) {
		View pagerContent = findViewById(R.id.view_pager);
		int accessibility = getActiveTalkbackFragments().isEmpty() ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
		pagerContent.setImportantForAccessibility(accessibility);
	}

	@Override
	public void savedArticlesUpdated() {
		ExploreTabFragment exploreTabFragment = getExploreTabFragment();
		SavedArticlesTabFragment savedArticlesTabFragment = getSavedArticlesTabFragment();
		if (exploreTabFragment != null && savedArticlesTabFragment != null
				&& exploreTabFragment.isAdded() && savedArticlesTabFragment.isAdded()) {
			exploreTabFragment.savedArticlesUpdated();
			savedArticlesTabFragment.savedArticlesUpdated();
		}
	}

	public static class LoadWikivoyageData extends AsyncTask<Void, Void, Void> {

		private final WeakReference<WikivoyageExploreActivity> activityRef;
		private final TravelHelper travelHelper;
		private final boolean resetData;

		LoadWikivoyageData(WikivoyageExploreActivity activity, boolean resetData) {
			travelHelper = activity.getMyApplication().getTravelHelper();
			activityRef = new WeakReference<>(activity);
			this.resetData = resetData;
		}

		@Override
		protected Void doInBackground(Void... params) {
			travelHelper.initializeDataToDisplay(resetData);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			WikivoyageExploreActivity activity = activityRef.get();
			if (AndroidUtils.isActivityNotDestroyed(activity)) {
				activity.onDataLoaded();
			}
		}
	}
}
