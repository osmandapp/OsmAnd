package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.util.ArrayList;

public class ChangesFragment extends BaseOsmAndFragment {
	public static final String TAG = ChangesFragment.class.getSimpleName();

	private boolean nightMode;
	private ChangesTabType currentTabType;
	private BackupInfo info;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = !requireMyApplication().getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.fragment_osmand_cloud_changes, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		setupTabs(view);
		setupToolbar(view);

		return view;
	}

	private void setupToolbar(View view) {
		view.findViewById(R.id.close_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void setupTabs(View view) {
		ArrayList<Fragment> fragments = new ArrayList<>();
		for (ChangesTabType type : ChangesTabType.values()) {
			fragments.add(createTabFragment(type));
		}
		ViewPager viewPager = view.findViewById(R.id.pager);
		ChangesPagerAdapter adapter = new ChangesPagerAdapter(getChildFragmentManager(), fragments);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(currentTabType.ordinal());

		PagerSlidingTabStrip pagerSlidingTabStrip = view.findViewById(R.id.sliding_tabs);
		pagerSlidingTabStrip.setShouldExpand(true);
		pagerSlidingTabStrip.setViewPager(viewPager);
	}

	private ChangesTabFragment createTabFragment(ChangesTabType type) {
		ChangesTabFragment fragment = new ChangesTabFragment(requireMyApplication());
		if (type.equals(ChangesTabType.LOCAL_CHANGES)) {
			fragment.setChangeList(info.itemsToUpload);
		} else if (type.equals(ChangesTabType.CLOUD_CHANGES)) {
			fragment.setChangeList(info.itemsToUpload);
		} else if (type.equals(ChangesTabType.CONFLICTS)) {
			fragment.setChangeList(info.itemsToUpload);
		}
		fragment.setTabType(type);
		return fragment;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, ChangesTabType tabType, BackupInfo info) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ChangesFragment fragment = new ChangesFragment();
			fragment.currentTabType = tabType;
			fragment.info = info;
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public enum ChangesTabType {
		LOCAL_CHANGES(R.string.shared_string_local),
		CLOUD_CHANGES(R.string.shared_string_cloud),
		CONFLICTS(R.string.shared_string_conflicts);

		ChangesTabType(@StringRes int resId) {
			this.resId = resId;
		}

		@StringRes
		int resId;
	}

	public static class ChangesPagerAdapter extends FragmentStatePagerAdapter {
		private final ArrayList<Fragment> mPages;

		public ChangesPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			mPages = fragments;
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			return mPages.get(position);
		}

		@Override
		public int getCount() {
			return mPages.size();
		}

		public Fragment getItemAt(int position) {
			return mPages.get(position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			ChangesTabFragment tabFragment = (ChangesTabFragment) mPages.get(position);
			return tabFragment.getTitle();
		}
	}
}
