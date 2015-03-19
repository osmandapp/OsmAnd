package net.osmand.plus.sherpafy;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavoriteGroup;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;


public class SherpafyStageFragment extends Fragment {
	public static final String STAGE_PARAM = "STAGE";
	public static final String TOUR_PARAM = "TOUR";
	private static final int START = 8;
	private static final int NEXT_STAGE = 9;
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
		app = (OsmandApplication) getActivity().getApplication();
		customization = (SherpafyCustomization) app.getAppCustomization();

		setHasOptionsMenu(true);
		String id = getArguments().getString(TOUR_PARAM);
		for(TourInformation ti : customization.getTourInformations()) {
			if(ti.getId().equals(id)) {
				tour = ti;
				((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(tour.getName());
				break;
			}
		}
		int k = getArguments().getInt(STAGE_PARAM);
		if(tour != null && tour.getStageInformation().size() > k) {
			stage = tour.getStageInformation().get(k);
		}
		if (stage != null){
			((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(stage.getName());
		}
	}
	



	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// createMenuItem(menu, ACTION_GO_TO_MAP, R.string.start_tour, 0, 0,/* R.drawable.ic_action_marker_light, */
		// MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		if (tour != null) {
			boolean current = customization.getSelectedStage() == stage;
			int text = current ? R.string.continue_stage
					: R.string.start_stage;
			if(customization.isStageVisited(stage.getOrder())) {
				text = R.string.stage_is_completed;
			}
			((TourViewActivity) getActivity()).createMenuItem(menu, START, text, 0,
					MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT,
					new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							return onOptionsItemSelected(item);
						}
					});
			if (customization.isStageVisited(stage.getOrder()) && customization.getNextAvailableStage(tour) != null) {
				((TourViewActivity) getActivity()).createMenuItem(menu, NEXT_STAGE, R.string.next_stage, 0,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT,
						new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								return onOptionsItemSelected(item);
							}
						});
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			((TourViewActivity) getActivity()).selectMenu(tour);
			return true;
		} else if(item.getItemId() == START) {
			((TourViewActivity) getActivity()).startStage(stage);
			return true;
		} else if(item.getItemId() == NEXT_STAGE) {
			((TourViewActivity) getActivity()).selectMenu(customization.getNextAvailableStage(tour));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.download, container, false);
		tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
		tabHost.setup();

		ViewPager mViewPager = (ViewPager) view.findViewById(R.id.pager);
		
		mTabsAdapter = new TabsAdapter(getChildFragmentManager(), getActivity(), tabHost, mViewPager, stage);
		if (stage != null) {
			mTabsAdapter.addTab(tabHost.newTabSpec("INFO").setIndicator(getString(R.string.sherpafy_stage_tab_info)),
					SherpafyStageInfoFragment.class);
			if (!stage.getItinerary().equals("") && 
					(stage.getGpx() != null || stage.getStartPoint() != null)) {
				mTabsAdapter.addTab(
						tabHost.newTabSpec("ROUTE").setIndicator(getString(R.string.sherpafy_stage_tab_route)),
						SherpafyStageItineraryFragment.class);
			}
			if (stage.getFavorites().size() > 0) {
				mTabsAdapter.addTab(tabHost.newTabSpec("FAV").setIndicator(getString(R.string.sherpafy_stage_tab_fav)),
						SherpafyFavoritesListFragment.class);
			}
			StageFavoriteGroup group = stage.getGroupById("destination");
			if (group != null && group.getFavorites().size() > 0) {
				int o = group.getFavorites().get(0).getOrder();
				Bundle bl = new Bundle();
				bl.putInt(SherpafyFavoriteFragment.FAV_PARAM, o);
				mTabsAdapter.addTab(
						tabHost.newTabSpec("TARGET").setIndicator(getString(R.string.sherpafy_stage_tab_target)),
						SherpafyFavoriteFragment.class, bl);
			}
		}
		return view;
	}
	
	@Override
	public void onDetach() {
	    super.onDetach();

	    try {
	        Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
	        childFragmentManager.setAccessible(true);
	        childFragmentManager.set(this, null);

	    } catch (NoSuchFieldException e) {
	    	e.printStackTrace();
	    } catch (IllegalAccessException e) {
	    	e.printStackTrace();
	    }
	}

	public void onBackPressed() {
		((TourViewActivity) getActivity()).selectMenu(tour);
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
		private StageInformation stage;

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

        public TabsAdapter(FragmentManager fm, Context ui,  TabHost tabHost, ViewPager pager,
        		StageInformation stage) {
            super(fm);
            mContext = ui;
            mTabHost = tabHost;
            mViewPager = pager;
			this.stage = stage;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public TabSpec addTab(TabHost.TabSpec tabSpec, Class<?> clss) {
        	return addTab(tabSpec, clss, new Bundle());
        }
        
        public TabSpec addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();
            args.putInt(STAGE_PARAM, stage.getOrder());
            args.putString(TOUR_PARAM, stage.getTour().getId());

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