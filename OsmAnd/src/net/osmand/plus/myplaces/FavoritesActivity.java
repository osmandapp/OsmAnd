package net.osmand.plus.myplaces;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.MenuItem;
import android.widget.ImageView;

import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesTreeFragment;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.OnSuccessfulGpxImport;
import net.osmand.plus.importfiles.ImportHelper.OnGpxImportCompleteListener;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import static net.osmand.plus.myplaces.TrackSegmentFragment.TRACK_DELETED_KEY;

/**
 *
 */
public class FavoritesActivity extends TabActivity {
	private static final Log LOG = PlatformUtil.getLog(FavoritesActivity.class);

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1006;
	private static final int IMPORT_FAVOURITES_REQUEST = 1007;
	protected static final int OPEN_GPX_REQUEST = 1008;

	public static final String TAB_ID = "selected_tab_id";

	public static final int GPX_TAB = R.string.shared_string_tracks;
	public static final int FAV_TAB = R.string.shared_string_my_favorites;

	protected List<WeakReference<FavoritesFragmentStateHolder>> fragList = new ArrayList<>();
	private int tabSize;
	private ImportHelper importHelper;

	private ViewPager viewPager;

	private Bundle intentParams = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		app.logEvent("myplaces_open");

		importHelper = new ImportHelper(this, app, null);

		//noinspection ConstantConditions
		getSupportActionBar().setTitle(R.string.shared_string_my_places);
		getSupportActionBar().setElevation(0);

		setContentView(R.layout.tab_content);
		viewPager = findViewById(R.id.pager);
		
		List<TabItem> mTabs = getTabItems();
		setTabs(mTabs);

		if (savedInstanceState == null) {
			Intent intent = getIntent();
			if (intent != null && intent.hasExtra(MapActivity.INTENT_PARAMS)) {
				intentParams = intent.getBundleExtra(MapActivity.INTENT_PARAMS);
				int tabId = intentParams.getInt(TAB_ID, FAV_TAB);
				int pagerItem = 0;
				for (int n = 0; n < mTabs.size(); n++) {
					if (mTabs.get(n).resId == tabId) {
						pagerItem = n;
						break;
					}
				}
				viewPager.setCurrentItem(pagerItem, false);
			}
		}
	}

	public void addTrack() {
		Intent intent = ImportHelper.getImportTrackIntent();
		try {
			startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
		} catch (ActivityNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public void importFavourites() {
		Intent intent = ImportHelper.getImportTrackIntent();
		try {
			startActivityForResult(intent, IMPORT_FAVOURITES_REQUEST);
		} catch (ActivityNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OPEN_GPX_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				AvailableGPXFragment gpxFragment = getGpxFragment();
				if (gpxFragment!= null) {
					gpxFragment.startImport();
				}
				importHelper.setGpxImportCompleteListener(new OnGpxImportCompleteListener() {
					@Override
					public void onImportComplete(boolean success) {
						AvailableGPXFragment gpxFragment = getGpxFragment();
						if (gpxFragment!= null) {
							gpxFragment.finishImport(success);
						}
						importHelper.setGpxImportCompleteListener(null);
					}

					@Override
					public void onSaveComplete(boolean success, GPXUtilities.GPXFile result) {

					}
				});
				if (!importHelper.handleGpxImport(uri, OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU, false)) {
					if (gpxFragment!= null) {
						gpxFragment.finishImport(false);
					}
				}
			}
		} else if (requestCode == IMPORT_FAVOURITES_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getData() != null) {
				importHelper.handleGpxOrFavouritesImport(data.getData());
			}
		} else if (requestCode == OPEN_GPX_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getBooleanExtra(TRACK_DELETED_KEY, false)) {
				AvailableGPXFragment gpxFragment = getGpxFragment();
				if (gpxFragment != null) {
					gpxFragment.resetTracksLoader();
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private AvailableGPXFragment getGpxFragment() {
		AvailableGPXFragment gpxFragment = null;
		for (WeakReference<FavoritesFragmentStateHolder> f : fragList) {
			FavoritesFragmentStateHolder frag = f.get();
			if (frag instanceof AvailableGPXFragment) {
				gpxFragment = (AvailableGPXFragment) frag;
			}
		}
		return gpxFragment;
	}

	private void setTabs(List<TabItem> mTabs) {
		PagerSlidingTabStrip mSlidingTabLayout = findViewById(R.id.sliding_tabs);
		OsmandSettings settings = getMyApplication().getSettings();
		Integer tabId = settings.FAVORITES_TAB.get();
		int tab = 0;
		for (int i = 0; i < mTabs.size(); i++) {
			if (mTabs.get(i).resId == tabId) {
				tab = i;
			}
		}
		tabSize = mTabs.size();
		setViewPagerAdapter(viewPager, mTabs);
		mSlidingTabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(tab);
	}

	private List<TabItem> getTabItems() {
		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(FAV_TAB, FavoritesTreeFragment.class));
		mTabs.add(getTabIndicator(GPX_TAB, AvailableGPXFragment.class));
		OsmandPlugin.addMyPlacesTabPlugins(this, mTabs, getIntent());
		return mTabs;
	}

	public Bundle storeCurrentState() {
		int currentItem = viewPager.getCurrentItem();
		if (currentItem >= 0 && currentItem < fragList.size()) {
			FavoritesFragmentStateHolder stateHolder = fragList.get(currentItem).get();
			if (stateHolder != null) {
				return stateHolder.storeState();
			}
		}
		return null;
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof FavoritesFragmentStateHolder) {
			fragment.setArguments(intentParams);
			fragList.add(new WeakReference<>((FavoritesFragmentStateHolder) fragment));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<TabItem> mTabs = getTabItems();
		if (mTabs.size() != tabSize) {
			setTabs(mTabs);
		}
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.getSettings().FAVORITES_TAB.set(mTabs.get(position).resId);
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		viewPager.clearOnPageChangeListeners();
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}

	public static void updateSearchView(Activity activity, SearchView searchView) {
		//do not ever do like this
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		if (!app.getSettings().isLightContent()) {
			return;
		}
		try {
			ImageView cancelIcon = (ImageView) searchView.findViewById(R.id.search_close_btn);
			cancelIcon.setImageResource(R.drawable.ic_action_gremove_dark);
			//styling search hint icon and text
			SearchView.SearchAutoComplete searchEdit = (SearchView.SearchAutoComplete) searchView.findViewById(R.id.search_src_text);
			searchEdit.setTextColor(activity.getResources().getColor(R.color.color_white));
			SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
			float rawTextSize = searchEdit.getTextSize();
			int textSize = (int) (rawTextSize * 1.25);

			//setting icon as spannable
			Drawable searchIcon = AppCompatResources.getDrawable(activity, R.drawable.ic_action_search_dark);
			if (searchIcon != null) {
				searchIcon.setBounds(0, 0, textSize, textSize);
				stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				searchEdit.setHint(stopHint);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	public static void showOnMap(@NonNull Activity activity, @Nullable FavoritesFragmentStateHolder fragment, double latitude, double longitude, int zoom, PointDescription pointDescription,
								 boolean addToHistory, Object toShow) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		app.getSettings().setMapLocationToShow(latitude, longitude, zoom, pointDescription, addToHistory, toShow);
		if (fragment != null) {
			MapActivity.launchMapActivityMoveToTop(activity, fragment.storeState());
		} else {
			MapActivity.launchMapActivityMoveToTop(activity);
		}
	}

	public static void openFavoritesGroup(Context context, String groupName) {
		OsmAndAppCustomization appCustomization = ((OsmandApplication) context.getApplicationContext()).getAppCustomization();
		Intent intent = new Intent(context, appCustomization.getFavoritesActivity());
		Bundle b = new Bundle();
		b.putInt(TAB_ID, FAV_TAB);
		b.putString(FavoritesFragmentStateHolder.GROUP_NAME_TO_SHOW, groupName);
		intent.putExtra(MapActivity.INTENT_PARAMS, b);
		context.startActivity(intent);
	}
}