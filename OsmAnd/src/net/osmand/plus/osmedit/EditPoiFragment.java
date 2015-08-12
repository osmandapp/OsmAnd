package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;

public class EditPoiFragment extends Fragment {
	public static final String TAG = "EditPoiFragment";

	// XXX this fragment wont work on older devices
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_edit_poi, container, false);
		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.poi_create_title);
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction().remove(EditPoiFragment.this).commit();
				fragmentManager.popBackStack();
			}
		});
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		MyAdapter pagerAdapter = new MyAdapter(getChildFragmentManager());
		viewPager.setAdapter(pagerAdapter);

		final TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		// Hack due to problems with design support library v22.2.1
		// https://code.google.com/p/android/issues/detail?id=180462
		// TODO remove in new version
		if (ViewCompat.isLaidOut(tabLayout)) {
			tabLayout.setupWithViewPager(viewPager);
		} else {
			tabLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom,
										   int oldLeft, int oldTop, int oldRight, int oldBottom) {
					tabLayout.setupWithViewPager(viewPager);
					tabLayout.removeOnLayoutChangeListener(this);
				}
			});
		}
		int orangeColor = getResources().getColor(R.color.osmand_orange);
		int grayColor = getResources().getColor(android.R.color.darker_gray);
		tabLayout.setTabTextColors(grayColor, orangeColor);

		return view;
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		public MyAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new NormalDataFragment();
				case 1:
					return new AdvancedDataFragment();
			}
			throw new IllegalArgumentException("Unexpected position");
		}

		@Override
		public CharSequence getPageTitle(int position) {
			// TODO replace with string resources
			switch (position) {
				case 0:
					return "Normal";
				case 1:
					return "Advanced";
			}
			throw new IllegalArgumentException("Unexpected position");
		}
	}

	public static class NormalDataFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_edit_poi_normal, container, false);
		}
	}

	public static class AdvancedDataFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_edit_poi_advanced, container, false);
		}
	}
}
