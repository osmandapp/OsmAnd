package net.osmand.plus.dashboard;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.monitoring.DashTrackFragment;
import net.osmand.plus.views.controls.FloatingActionButton;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class DashboardOnMap {


	private static final int LIST_ID = 1;
	private static final int WAYPOINTS_ID = 2;
	private static final int CONFIGURE_SCREEN_ID = 3;
	private static final int SETTINGS_ID = 4;
	private MapActivity ma;
	FloatingActionButton fabButton;
	boolean floatingButtonVisible = true;
	private FrameLayout dashboardView;
	private boolean visible = false;


	public DashboardOnMap(MapActivity ma) {
		this.ma = ma;
	}


	public void createDashboardView() {
		dashboardView = (FrameLayout) ma.getLayoutInflater().inflate(R.layout.dashboard_over_map, null, false);
		dashboardView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDashboardVisibility(false);
			}
		};
		dashboardView.findViewById(R.id.content).setOnClickListener(listener);
		dashboardView.setOnClickListener(listener);
		((FrameLayout) ma.findViewById(R.id.ParentLayout)).addView(dashboardView);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			fabButton = new FloatingActionButton.Builder(ma)
					.withDrawable(ma.getResources().getDrawable(R.drawable.ic_action_map))
					.withButtonColor(ma.getResources().getColor(R.color.color_myloc_distance)).withGravity(Gravity.TOP | Gravity.RIGHT)
					.withMargins(0, 160, 16, 0).create();
			fabButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					setDashboardVisibility(false);
				}
			});
			fabButton.hideFloatingActionButton();
		}

		if (ScreenOrientationHelper.isOrientationPortrait(ma)) {
			((NotifyingScrollView) dashboardView.findViewById(R.id.main_scroll))
					.setOnScrollChangedListener(onScrollChangedListener);
		}

	}


	public void setDashboardVisibility(boolean visible) {
		this.visible = visible;
		if (visible) {
			addDashboardFragments();
			setupActionBar();
			dashboardView.setVisibility(View.VISIBLE);
			fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent));
			ma.getMapActions().disableDrawer();
			ma.findViewById(R.id.MapInfoControls).setVisibility(View.GONE);
			ma.findViewById(R.id.MapButtons).setVisibility(View.GONE);
		} else {
			ma.getMapActions().enableDrawer();
			hide(dashboardView.findViewById(R.id.animateContent));
			ma.findViewById(R.id.MapInfoControls).setVisibility(View.VISIBLE);
			ma.findViewById(R.id.MapButtons).setVisibility(View.VISIBLE);
			fabButton.hideFloatingActionButton();
		}
	}

	private void setupActionBar() {
		final Toolbar tb = (Toolbar) ma.findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		Menu menu = tb.getMenu();
		createMenuItem(menu, LIST_ID, R.string.drawer, 
				R.drawable.ic_flat_list_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, WAYPOINTS_ID, R.string.waypoints, 
				R.drawable.ic_action_flage_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, CONFIGURE_SCREEN_ID, R.string.layer_map_appearance,
				R.drawable.ic_configure_screen_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SETTINGS_ID, R.string.settings_activity, 
				R.drawable.ic_action_settings_enabled_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int icon, int menuItemType) {
		int r = icon;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		MenuItemCompat.setShowAsAction(menuItem, menuItemType);
		return menuItem;
	}


	protected boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}


	// To animate view slide out from right to left
	private void open(View view){
		TranslateAnimation animate = new TranslateAnimation(-ma.findViewById(R.id.ParentLayout).getWidth(),0,0,0);
		animate.setDuration(500);
		animate.setFillAfter(true);
		view.startAnimation(animate);
		view.setVisibility(View.VISIBLE);
	}

	private void hide(View view) {
		TranslateAnimation animate = new TranslateAnimation(0, -ma.findViewById(R.id.ParentLayout).getWidth(), 0, 0);
		animate.setDuration(500);
		animate.setFillAfter(true);
		animate.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				dashboardView.setVisibility(View.GONE);
			}
		});
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	

	private void addDashboardFragments() {
		FragmentManager manager = ma.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		if (manager.findFragmentByTag(DashErrorFragment.TAG) == null && 
				ma.getMyApplication().getAppInitializer().checkPreviousRunsForExceptions(ma)) {
			DashErrorFragment errorFragment = new DashErrorFragment();
			fragmentTransaction.add(R.id.content, errorFragment, DashErrorFragment.TAG);
		}
		if (manager.findFragmentByTag(DashSearchFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashSearchFragment(), DashSearchFragment.TAG);
		}
		if (manager.findFragmentByTag(DashRecentsFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashRecentsFragment(), DashRecentsFragment.TAG);
		}
		if (manager.findFragmentByTag(DashFavoritesFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashFavoritesFragment(), DashFavoritesFragment.TAG);
		}
		if (manager.findFragmentByTag(DashAudioVideoNotesFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashAudioVideoNotesFragment(), DashAudioVideoNotesFragment.TAG);
		}
		if (manager.findFragmentByTag(DashTrackFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashTrackFragment(), DashTrackFragment.TAG);
		}
		// fragmentTransaction.add(R.id.content, new DashUpdatesFragment(), DashUpdatesFragment.TAG);
		if (manager.findFragmentByTag(DashPluginsFragment.TAG) == null) {
			fragmentTransaction.add(R.id.content, new DashPluginsFragment(), DashPluginsFragment.TAG);
		}
		fragmentTransaction.commit();
	}




	private NotifyingScrollView.OnScrollChangedListener onScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
		public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
			int sy = who.getScrollY();
			double scale = who.getContext().getResources().getDisplayMetrics().density;
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabButton.getLayoutParams();
			lp.topMargin = (int) Math.max(30 * scale, 160 * scale - sy);
			((FrameLayout) fabButton.getParent()).updateViewLayout(fabButton, lp);
			// TODO
		}
	};

	public boolean isVisible() {
		return visible;
	}
}
