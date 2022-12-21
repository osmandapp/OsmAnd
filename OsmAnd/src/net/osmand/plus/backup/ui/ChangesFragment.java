package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.R;
import net.osmand.plus.activities.TabActivity.OsmandFragmentPagerAdapter;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

public class ChangesFragment extends BaseOsmAndFragment {

	public static final String TAG = ChangesFragment.class.getSimpleName();

	private ChangesTabType currentTabType;

	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = isNightMode(false);
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

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.shared_string_changes);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void setupTabs(@NonNull View view) {
		List<TabItem> tabItems = new ArrayList<>();
		tabItems.add(new TabItem(R.string.shared_string_local, getString(R.string.shared_string_local), LocalTabFragment.class));
		tabItems.add(new TabItem(R.string.shared_string_cloud, getString(R.string.shared_string_cloud), CloudTabFragment.class));
		tabItems.add(new TabItem(R.string.shared_string_conflicts, getString(R.string.shared_string_conflicts), ConflictsTabFragment.class));

		ViewPager viewPager = view.findViewById(R.id.pager);
		viewPager.setAdapter(new OsmandFragmentPagerAdapter(getChildFragmentManager(), tabItems));
		viewPager.setCurrentItem(currentTabType.ordinal());

		PagerSlidingTabStrip pagerSlidingTabStrip = view.findViewById(R.id.sliding_tabs);
		pagerSlidingTabStrip.setShouldExpand(true);
		pagerSlidingTabStrip.setViewPager(viewPager);
	}

	public enum ChangesTabType {

		LOCAL_CHANGES(R.string.shared_string_local),
		CLOUD_CHANGES(R.string.shared_string_cloud),
		CONFLICTS(R.string.shared_string_conflicts);

		@StringRes
		int titleId;

		ChangesTabType(@StringRes int titleId) {
			this.titleId = titleId;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ChangesTabType tabType) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ChangesFragment fragment = new ChangesFragment();
			fragment.currentTabType = tabType;
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
