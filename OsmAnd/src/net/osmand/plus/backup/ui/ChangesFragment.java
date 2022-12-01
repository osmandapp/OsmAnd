package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;

public class ChangesFragment extends BaseOsmAndFragment {
	public static final String TAG = ChangesFragment.class.getSimpleName();

	private final int BACKGROUND_ACTIVE_COLOR_ALPHA = 10;

	private boolean nightMode;
	private ChangesTabType currentTabType;
	private BackupInfo info;
	private View view;
	private ChangesPagerAdapter adapter;
	private ViewPager viewPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = !requireMyApplication().getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_osmand_cloud_changes, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		setupTabs();
		setupToolbar();
		updateActionButtons();
		return view;
	}

	private void setupToolbar() {
		view.findViewById(R.id.close_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void updateActionButtons() {
		TextViewEx downloadTextView = view.findViewById(R.id.download_button_text);
		AppCompatImageView downloadImageView = view.findViewById(R.id.download_button_icon);
		View downloadButton = view.findViewById(R.id.download_button);
		View downloadContainer = view.findViewById(R.id.download_container);

		ChangesTabFragment fragment = (ChangesTabFragment) adapter.getItem(viewPager.getCurrentItem());
		ChangesTabType currentChangesType = fragment.getTabType();

		StringBuilder downloadButtonText = new StringBuilder("");
		int iconColor;
		int textColor;
		int backgroundColor;
		int backgroundAlpha;

		if (Algorithms.isEmpty(fragment.getChangeList())) {
			iconColor = ColorUtilities.getDefaultIconColor(requireMyApplication(), nightMode);
			textColor = ColorUtilities.getSecondaryTextColor(requireMyApplication(), nightMode);
			backgroundColor = ColorUtilities.getInactiveButtonsAndLinksColor(requireMyApplication(), nightMode);
			backgroundAlpha = 100;
		} else {
			iconColor = ColorUtilities.getActiveIconColor(requireMyApplication(), nightMode);
			textColor = ColorUtilities.getActiveColor(requireMyApplication(), nightMode);
			backgroundColor = ColorUtilities.getActiveColor(requireMyApplication(), nightMode);
			backgroundAlpha = BACKGROUND_ACTIVE_COLOR_ALPHA;
		}

		if (currentChangesType.equals(ChangesTabType.LOCAL_CHANGES)) {
			downloadButtonText.append(getString(R.string.shared_string_upload));
			downloadImageView.setImageDrawable(getIcon(R.drawable.ic_action_cloud_upload));
		} else if (currentChangesType.equals(ChangesTabType.CLOUD_CHANGES)) {
			downloadButtonText.append(getString(R.string.shared_string_download));
			downloadImageView.setImageDrawable(getIcon(R.drawable.ic_action_cloud_upload));
		} else if (currentChangesType.equals(ChangesTabType.CONFLICTS)) {
			downloadImageView.setImageDrawable(getIcon(R.drawable.ic_action_cloud_upload));
		}

		downloadImageView.getDrawable().setTint(iconColor);
		downloadButtonText.append(" ").append(getString(R.string.shared_string_all));

		downloadTextView.setTextColor(textColor);
		downloadTextView.setText(downloadButtonText);
		downloadContainer.setBackgroundColor(backgroundColor);
		downloadContainer.getBackground().setAlpha(backgroundAlpha);

		downloadButton.setOnClickListener(view1 -> {

		});
	}

	private void setupTabs() {
		ArrayList<Fragment> fragments = new ArrayList<>();
		for (ChangesTabType type : ChangesTabType.values()) {
			fragments.add(createTabFragment(type));
		}
		viewPager = view.findViewById(R.id.pager);
		adapter = new ChangesPagerAdapter(getChildFragmentManager(), fragments);
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(currentTabType.ordinal());
		viewPager.addOnPageChangeListener(getPageChangeListener());

		PagerSlidingTabStrip pagerSlidingTabStrip = view.findViewById(R.id.sliding_tabs);
		pagerSlidingTabStrip.setShouldExpand(true);
		pagerSlidingTabStrip.setViewPager(viewPager);
	}

	private ChangesTabFragment createTabFragment(ChangesTabType type) {
		ChangesTabFragment fragment = new ChangesTabFragment(requireMyApplication());
		if (type.equals(ChangesTabType.LOCAL_CHANGES)) {
			fragment.setChangeList(new ArrayList<>());
		} else if (type.equals(ChangesTabType.CLOUD_CHANGES)) {
			fragment.setChangeList(info.itemsToUpload);
		} else if (type.equals(ChangesTabType.CONFLICTS)) {
			fragment.setChangeList(info.itemsToUpload);
		}
		fragment.setTabType(type);
		return fragment;
	}

	private OnPageChangeListener getPageChangeListener() {
		return new OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				updateActionButtons();
			}

			@Override
			public void onPageSelected(int position) {

			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		};
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

		@Override
		public CharSequence getPageTitle(int position) {
			ChangesTabFragment tabFragment = (ChangesTabFragment) mPages.get(position);
			return tabFragment.getTitle();
		}
	}
}
