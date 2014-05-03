package net.osmand.plus.sherpafy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class TourCommonActivity extends SherlockFragmentActivity {

	public static final String TOUR_SELECTION = "TOUR_SELECTION";
	public static final String TOUR_STAGE = "TOUR_STAGE";
	public static final String TOUR_INFO = "TOUR_INFO";
	private TabsAdapter mTabsAdapter;
	List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		// getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		setContentView(R.layout.tour_main);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.tour_activity_title);
		
		// TabWidget tabs = (TabWidget) findViewById(android.R.id.tabs);
		
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        tabHost.setup();

        ViewPager mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, tabHost,  mViewPager);
		mTabsAdapter.addTab(tabHost.newTabSpec(TOUR_INFO).setIndicator(getString(R.string.tab_current_tour)), 
				TourInformationFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec(TOUR_STAGE).setIndicator(getString(R.string.tab_stages)), 
				TourStageFragment.class, null);
		mTabsAdapter.addTab(tabHost.newTabSpec(TOUR_SELECTION).setIndicator(getString(R.string.tab_tours)), 
				TourSelectionFragment.class, null);
        if (savedInstanceState != null) {
            tabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }
	
	public void selectTour(TourInformation ti){
		final SherpafyCustomization c = (SherpafyCustomization) ((OsmandApplication) getApplication()).getAppCustomization();
		
		new AsyncTask<TourInformation, Void, Void> (){
			private ProgressDialogImplementation dlg;
			
			protected void onPreExecute() {
				dlg = ProgressDialogImplementation.createProgressDialog(TourCommonActivity.this, "", getString(R.string.indexing_tour, ""),
						ProgressDialog.STYLE_SPINNER);
				
			};
			
			@Override
			protected Void doInBackground(TourInformation... params) {
				c.selectTour(params[0], dlg);
				return null;
			}
			
			protected void onPostExecute(Void result) {
				dlg.getDialog().dismiss();
				for(WeakReference<Fragment> ref : fragList) {
			        Fragment f = ref.get();
			        if(f instanceof TourFragment) {
			            if(!f.isDetached()) {
			            	((TourFragment) f).refreshTour();
			            }
			        }
			    }
			};
		}.execute(ti);
	}
	
	public interface TourFragment {
		
		public void refreshTour();
	}
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabsAdapter.mTabHost.getCurrentTabTag());
    }

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public void onAttachFragment (Fragment fragment) {
	    fragList.add(new WeakReference<Fragment>(fragment));
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

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
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
            if (TOUR_INFO.equals(tabId)) {
			} else	if (TOUR_STAGE.equals(tabId)) {
			} else	if (TOUR_SELECTION.equals(tabId)) {
			}
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
