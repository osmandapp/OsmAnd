package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SherpafyStageFragment extends SherlockFragment {
	public static final String STAGE_PARAM = "STAGE";
	public static final String TOUR_PARAM = "TOUR";
	private static final int START = 8;
	OsmandApplication app;
	private SherpafyCustomization customization;
	private StageInformation stage;
	private TourInformation tour;
	private View view;
	private TabHost tabHost;
	private TabsAdapter mTabsAdapter;

	public SherpafyStageFragment() {
	}
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		app = (OsmandApplication) getSherlockActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();

		setHasOptionsMenu(true);
		String id = getArguments().getString(TOUR_PARAM);
		for(TourInformation ti : customization.getTourInformations()) {
			if(ti.getId().equals(id)) {
				tour = ti;
				getSherlockActivity().getSupportActionBar().setTitle(tour.getName());
				break;
			}
		}
		int k = getArguments().getInt(STAGE_PARAM);
		if(tour != null && tour.getStageInformation().size() > k) {
			stage = tour.getStageInformation().get(k);
			getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.tab_stage) + " " + (k+1));
		}
	}
	



	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
		// MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (tour != null) {
			boolean current = customization.getSelectedStage() == stage;
			((TourViewActivity) getSherlockActivity()).createMenuItem(menu, START, 
					current ? R.string.continue_stage : R.string.start_stage ,
					0, 0,
					MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getSherlockActivity()).selectMenu(tour);
			return true;
		} else if(item.getItemId() == START) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.tab_content, container, false);
		tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
		tabHost.setup();

		ViewPager mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mTabsAdapter = new TabsAdapter(getSherlockActivity(), tabHost, mViewPager);
		mTabsAdapter.addTab(tabHost.newTabSpec("INFO").setIndicator(getString(R.string.sherpafy_stage_tab_info)),
				SherpafyStageInfoFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("ROUTE").setIndicator(getString(R.string.sherpafy_stage_tab_route)),
				SherpafyStageItineraryFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("FAV").setIndicator(getString(R.string.sherpafy_stage_tab_fav)),
				SherpafyStageInfoFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec("TARGET").setIndicator(getString(R.string.sherpafy_stage_tab_target)),
				SherpafyStageItineraryFragment.class, null);
		return view;
	}
	
/////////
	private ImageGetter getImageGetter(final View v) {
		return new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String s) {
				Bitmap file = customization.getSelectedTour().getImageBitmapFromPath(s);
				v.setTag(file);
				Drawable bmp = new BitmapDrawable(getResources(), file);
				// if image is thicker than screen - it may cause some problems, so we need to scale it
				int imagewidth = bmp.getIntrinsicWidth();
				// TODO
//				if (displaySize.x - 1 > imagewidth) {
//					bmp.setBounds(0, 0, bmp.getIntrinsicWidth(), bmp.getIntrinsicHeight());
//				} else {
//					double scale = (double) (displaySize.x - 1) / imagewidth;
//					bmp.setBounds(0, 0, (int) (scale * bmp.getIntrinsicWidth()),
//							(int) (scale * bmp.getIntrinsicHeight()));
//				}
				return bmp;
			}

		};
	}
	


	private void addOnClickListener(final TextView tv) {
		tv.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getTag() instanceof Bitmap) {
					final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(getActivity());
					dlg.setPositiveButton(R.string.default_buttons_ok, null);
					ScrollView sv = new ScrollView(getActivity());
					ImageView img = new ImageView(getActivity());
					img.setImageBitmap((Bitmap) tv.getTag());
					sv.addView(img);
					dlg.setView(sv);
					dlg.show();
				}
			}
		});
	}

	private void prepareBitmap(Bitmap imageBitmap) {
		ImageView img = null;
		if (imageBitmap != null) {
			img.setImageBitmap(imageBitmap);
			img.setAdjustViewBounds(true);
			img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			img.setCropToPadding(true);
			img.setVisibility(View.VISIBLE);
		} else {
			img.setVisibility(View.GONE);
		}
	}

	private void goToMap() {
		if (customization.getSelectedStage() != null) {
			GPXFile gpx = customization.getSelectedStage().getGpx();
			List<SelectedGpxFile> sgpx = getMyApplication().getSelectedGpxHelper().getSelectedGPXFiles();
			if (gpx == null && sgpx.size() > 0) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
			} else if (sgpx.size() != 1 || sgpx.get(0).getGpxFile() != gpx) {
				getMyApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				if (gpx != null && gpx.findPointToShow() != null) {
					WptPt p = gpx.findPointToShow();
					getMyApplication().getSettings().setMapLocationToShow(p.lat, p.lon, 16, null);
					getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(gpx);
				}
			}
		}
		Intent newIntent = new Intent(getActivity(), customization.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		this.startActivityForResult(newIntent, 0);
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
	
	   /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private Class<?> clss;
            private Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost,ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public TabSpec addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
            return tabSpec;
        }
        

        @Override
        public int getCount() {
            return mTabs.size();
        }
        
        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}