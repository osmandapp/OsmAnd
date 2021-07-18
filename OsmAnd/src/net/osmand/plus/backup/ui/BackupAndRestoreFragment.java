package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.TabActivity.OsmandFragmentPagerAdapter;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.ui.AuthorizeFragment.LoginDialogType;
import net.osmand.plus.backup.ui.status.BackupStatusFragment;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.List;

public class BackupAndRestoreFragment extends BaseOsmAndFragment implements InAppPurchaseListener {

	public static final String TAG = BackupAndRestoreFragment.class.getSimpleName();

	private static final String DIALOG_TYPE_KEY = "dialog_type_key";

	private LoginDialogType dialogType;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Nullable
	public LoginDialogType getDialogType() {
		return dialogType;
	}

	public void removeDialogType() {
		dialogType = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = !requireMyApplication().getSettings().isLightContent();

		if (savedInstanceState != null && savedInstanceState.containsKey(DIALOG_TYPE_KEY)) {
			dialogType = LoginDialogType.valueOf(savedInstanceState.getString(DIALOG_TYPE_KEY));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = themedInflater.inflate(R.layout.backup_and_restore, container, false);
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);

		setupTabs(view);
		setupToolbar(view);

		OsmandApplication app = requireMyApplication();
		NetworkSettingsHelper settingsHelper = app.getNetworkSettingsHelper();
		if (!settingsHelper.isBackupExporting()) {
			app.getBackupHelper().prepareBackup();
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (dialogType != null) {
			outState.putString(DIALOG_TYPE_KEY, dialogType.name());
		}
	}

	private void setupToolbar(View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.backup_and_restore);

		ImageView closeButton = toolbar.findViewById(R.id.action_button_icon);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		toolbar.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.action_button), false);
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_switch_container), false);
	}

	private void setupTabs(View view) {
		List<TabItem> tabItems = new ArrayList<>();
		tabItems.add(new TabItem(R.string.shared_string_status, getString(R.string.shared_string_status), BackupStatusFragment.class));
		tabItems.add(new TabItem(R.string.shared_string_settings, getString(R.string.shared_string_settings), BackupSettingsFragment.class));

		ViewPager viewPager = view.findViewById(R.id.pager);
		viewPager.setAdapter(new OsmandFragmentPagerAdapter(getChildFragmentManager(), tabItems));

		PagerSlidingTabStrip pagerSlidingTabStrip = view.findViewById(R.id.sliding_tabs);
		pagerSlidingTabStrip.setShouldExpand(true);
		pagerSlidingTabStrip.setViewPager(viewPager);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		showInstance(fragmentManager, null);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable LoginDialogType dialogType) {
		if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(TAG) == null) {
			BackupAndRestoreFragment fragment = new BackupAndRestoreFragment();
			fragment.dialogType = dialogType;
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {

	}

	@Override
	public void onGetItems() {

	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof InAppPurchaseListener && fragment.isAdded()) {
				((BackupStatusFragment) fragment).onItemPurchased(sku, active);
			}
		}
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {

	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {

	}
}