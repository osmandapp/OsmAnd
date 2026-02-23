package net.osmand.plus.myplaces;

import static net.osmand.plus.backup.ui.BackupAuthorizationFragment.OPEN_BACKUP_AUTH;
import static net.osmand.plus.helpers.AndroidUiHelper.ANIMATION_DURATION;
import static net.osmand.plus.helpers.IntentHelper.REQUEST_CODE_CREATE_FILE;
import static net.osmand.plus.helpers.MapFragmentsHelper.CLOSE_ALL_FRAGMENTS;
import static net.osmand.plus.mapcontextmenu.other.ShareItem.COPY_LIST;
import static net.osmand.plus.mapcontextmenu.other.ShareItem.SAVE_AS_FILE;
import static net.osmand.plus.mapcontextmenu.other.ShareMenu.KEY_SAVE_FILE_NAME;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_ACTION_ID;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_LIST;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesSearchFragment.FAV_SEARCH_QUERY_KEY;
import static net.osmand.plus.myplaces.favorites.dialogs.SearchFavoriteFragment.FAVORITE_SEARCH_GROUP_KEY;
import static net.osmand.plus.myplaces.favorites.dialogs.SearchFavoriteFragment.FAVORITE_SEARCH_QUERY_KEY;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.data.PointDescription;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FavoritesSearchFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.favorites.dialogs.SearchFavoriteFragment;
import net.osmand.plus.myplaces.tracks.dialogs.AvailableTracksFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyPlacesActivity extends TabActivity {


	public static final String TAB_ID = "selected_tab_id";

	public static final int GPX_TAB = R.string.shared_string_tracks;
	public static final int FAV_TAB = R.string.shared_string_my_favorites;

	private LockableViewPager viewPager;
	private PagerSlidingTabStrip mSlidingTabLayout;
	private AppBarLayout appBar;
	@Nullable private ValueAnimator tabsHeightAnimator;

	private final List<WeakReference<FragmentStateHolder>> fragmentsStateList = new ArrayList<>();
	private int tabSize;

	private Bundle intentParams;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		app.logEvent("myplaces_open");
		app.getImportHelper().setUiActivity(this);

		updateToolbar();
		setContentView(R.layout.my_places);
		viewPager = findViewById(R.id.pager);

		List<TabItem> tabItems = getTabItems();
		setTabs(tabItems);

		if (savedInstanceState == null) {
			Intent intent = getIntent();

			if (intent != null) {
				Bundle bundle = intent.getExtras();
				if (bundle != null && bundle.containsKey(FAV_SEARCH_QUERY_KEY)) {
					String searchQuery = bundle.getString(FAV_SEARCH_QUERY_KEY, "");
					FavoritesSearchFragment.showInstance(this, searchQuery);
				}

				if (bundle != null && bundle.containsKey(FAVORITE_SEARCH_QUERY_KEY) && bundle.containsKey(FAVORITE_SEARCH_GROUP_KEY)) {
					String searchQuery = bundle.getString(FAVORITE_SEARCH_QUERY_KEY, "");
					String groupKey = bundle.getString(FAVORITE_SEARCH_GROUP_KEY, "");
					FragmentManager manager = getSupportFragmentManager();

					SearchFavoriteFragment.showInstance(manager, null, new ArrayList<>(), groupKey, searchQuery);
				}

				if (intent.hasExtra(MapActivity.INTENT_PARAMS)) {
					intentParams = intent.getBundleExtra(MapActivity.INTENT_PARAMS);
					int tabId = intentParams.getInt(TAB_ID, FAV_TAB);
					int pagerItem = 0;
					for (int n = 0; n < tabItems.size(); n++) {
						if (tabItems.get(n).resId == tabId) {
							pagerItem = n;
							break;
						}
					}
					viewPager.setCurrentItem(pagerItem, false);
				}
			}
		}
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createBottomContainer(R.id.bottomControls));
		collection.replace(InsetTarget.createHorizontalLandscape(R.id.appbar));
		return collection;
	}

	public void updateToolbar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_my_places);
			actionBar.setElevation(0);
		}
	}

	private void setTabs(@NonNull List<TabItem> tabItems) {
		appBar = findViewById(R.id.appbar);

		mSlidingTabLayout = findViewById(R.id.sliding_tabs);
		mSlidingTabLayout.setBackgroundColor(Color.TRANSPARENT);
		appBar.setBackgroundColor(Color.TRANSPARENT);
		appBar.setElevation(0);
		appBar.setOutlineProvider(null);

		Integer tabId = settings.FAVORITES_TAB.get();
		int tab = 0;
		for (int i = 0; i < tabItems.size(); i++) {
			if (tabItems.get(i).resId == tabId) {
				tab = i;
			}
		}
		tabSize = tabItems.size();
		setViewPagerAdapter(viewPager, tabItems);
		mSlidingTabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(tab);
	}

	private void animateHeight(View v, int from, int to, @Nullable Runnable endAction) {
		if (tabsHeightAnimator != null) tabsHeightAnimator.cancel();

		tabsHeightAnimator = ValueAnimator.ofInt(from, to);
		tabsHeightAnimator.setDuration(ANIMATION_DURATION);
		tabsHeightAnimator.addUpdateListener(anim -> {
			int h = (int) anim.getAnimatedValue();
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			lp.height = h;
			v.setLayoutParams(lp);
		});
		tabsHeightAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
			@Override public void onAnimationEnd(android.animation.Animator animation) {
				if (endAction != null) endAction.run();
			}
		});
		tabsHeightAnimator.start();
	}

	public void animateShowHideTabs(boolean hideTabs) {
		updateScreen(hideTabs);
		viewPager.setSwipeLocked(hideTabs);

		final int tabsH = getResources().getDimensionPixelSize(R.dimen.list_item_height);

		mSlidingTabLayout.animate().cancel();

		if (hideTabs) {
			if (isDisableAnimations()) {

				ViewGroup.LayoutParams lp = mSlidingTabLayout.getLayoutParams();
				lp.height = 0;
				mSlidingTabLayout.setLayoutParams(lp);
				mSlidingTabLayout.setVisibility(View.GONE);
				return;
			}

			mSlidingTabLayout.setVisibility(View.VISIBLE);
			ensureTabsHeight(tabsH);

			mSlidingTabLayout.setTranslationY(0f);
			mSlidingTabLayout.setAlpha(1f);

			mSlidingTabLayout.animate()
					.translationY(-tabsH)
					.alpha(0f)
					.setDuration(ANIMATION_DURATION)
					.start();

			animateHeight(mSlidingTabLayout, tabsH, 0, () -> {
				mSlidingTabLayout.setVisibility(View.GONE);
				mSlidingTabLayout.setTranslationY(0f);
				mSlidingTabLayout.setAlpha(1f);
			});

		} else {
			if (isDisableAnimations()) {

				mSlidingTabLayout.setVisibility(View.VISIBLE);
				ViewGroup.LayoutParams lp = mSlidingTabLayout.getLayoutParams();
				lp.height = tabsH;
				mSlidingTabLayout.setLayoutParams(lp);

				mSlidingTabLayout.setTranslationY(0f);
				mSlidingTabLayout.setAlpha(1f);
				return;
			}

			mSlidingTabLayout.setVisibility(View.VISIBLE);
			ViewGroup.LayoutParams lp = mSlidingTabLayout.getLayoutParams();
			lp.height = 0;
			mSlidingTabLayout.setLayoutParams(lp);

			mSlidingTabLayout.setTranslationY(-tabsH);
			mSlidingTabLayout.setAlpha(0f);

			animateHeight(mSlidingTabLayout, 0, tabsH, null);

			mSlidingTabLayout.animate()
					.translationY(0f)
					.alpha(1f)
					.setDuration(ANIMATION_DURATION)
					.start();
		}
	}

	private void ensureTabsHeight(int tabsH) {
		ViewGroup.LayoutParams lp = mSlidingTabLayout.getLayoutParams();
		if (lp.height != tabsH) {
			lp.height = tabsH;
			mSlidingTabLayout.setLayoutParams(lp);
		}
	}

	private void updateScreen(boolean hideTabs) {
		appBar.setExpanded(true, true);

		mSlidingTabLayout.setClickable(!hideTabs);
		mSlidingTabLayout.setFocusable(!hideTabs);

		updateToolbar();
	}

	private boolean isDisableAnimations() {
		return app.getSettings().DO_NOT_USE_ANIMATIONS.getModeValue(app.getSettings().getApplicationMode());
	}

	@NonNull
	private List<TabItem> getTabItems() {
		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(FAV_TAB, FavoriteFoldersFragment.class));
		mTabs.add(getTabIndicator(GPX_TAB, AvailableTracksFragment.class));
		PluginsHelper.addMyPlacesTabPlugins(this, mTabs, getIntent());
		return mTabs;
	}

	@Nullable
	public Bundle storeCurrentState() {
		int currentItem = viewPager.getCurrentItem();
		if (currentItem >= 0 && currentItem < fragmentsStateList.size()) {
			FragmentStateHolder stateHolder = fragmentsStateList.get(currentItem).get();
			if (stateHolder != null) {
				return stateHolder.storeState();
			}
		}
		return null;
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		if (fragment instanceof FragmentStateHolder) {
			fragment.setArguments(intentParams);
			fragmentsStateList.add(new WeakReference<>((FragmentStateHolder) fragment));
		}
	}

	@Override
	protected void onNewIntent(@NonNull Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		int actionId = intent.getIntExtra(KEY_SHARE_ACTION_ID, -1);

		if (actionId == COPY_LIST.ordinal()) {
			String text = intent.getStringExtra(KEY_SHARE_LIST);
			if (Algorithms.isEmpty(text)) {
				return;
			}
			ShareMenu.copyToClipboard(this, text);
			return;
		}

		if (actionId == SAVE_AS_FILE.ordinal() || intent.hasExtra(KEY_SAVE_FILE_NAME)) {
			String filePath = intent.getStringExtra("file_path");
			if (Algorithms.isEmpty(filePath)) {
				return;
			}
			File fileToSave = new File(filePath);

			Intent createFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			createFileIntent.setType("*/*");
			createFileIntent.putExtra(Intent.EXTRA_TITLE, fileToSave.getName());

			AndroidUtils.startActivityForResultIfSafe(this, createFileIntent, REQUEST_CODE_CREATE_FILE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<TabItem> tabItems = getTabItems();
		if (tabItems.size() != tabSize) {
			setTabs(tabItems);
		}
		app.getImportHelper().setUiActivity(this);
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				settings.FAVORITES_TAB.set(tabItems.get(position).resId);
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		viewPager.clearOnPageChangeListeners();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ImportHelper importHelper = app.getImportHelper();
		importHelper.resetUIActivity(this);
		removeActivityResultListener(importHelper.getSaveFileResultListener());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		List<Fragment> fragmentsWithoutTabs = new ArrayList<>();
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			boolean isTabFragment = false;
			for (TabItem tabItem : getTabItems()) {
				if (fragment.getClass() == tabItem.fragment) {
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
		View pagerContent = findViewById(R.id.pager_content);
		View slidingTabs = findViewById(R.id.sliding_tabs);
		int accessibility = getActiveTalkbackFragments().isEmpty() ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
		pagerContent.setImportantForAccessibility(accessibility);
		slidingTabs.setImportantForAccessibility(accessibility);
	}

	public void showOnMap(@Nullable FragmentStateHolder fragment, double latitude, double longitude,
	                      int zoom, PointDescription pointDescription, boolean addToHistory, Object toShow) {
		settings.setMapLocationToShow(latitude, longitude, zoom, pointDescription, addToHistory, toShow);

		Bundle args = new Bundle();
		args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

		Bundle bundle = fragment != null ? fragment.storeState() : null;
		MapActivity.launchMapActivityMoveToTop(this, bundle, null, args);
	}

	public void showOsmAndCloud(@Nullable FragmentStateHolder fragment) {
		Bundle args = new Bundle();
		args.putBoolean(OPEN_BACKUP_AUTH, true);
		args.putBoolean(CLOSE_ALL_FRAGMENTS, true);

		Bundle bundle = fragment != null ? fragment.storeState() : null;
		MapActivity.launchMapActivityMoveToTop(this, bundle, null, args);
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
	}
}