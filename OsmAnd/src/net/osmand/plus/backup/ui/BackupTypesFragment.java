package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IContextDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class BackupTypesFragment extends BaseOsmAndFragment
		implements IContextDialog, IDialogNightModeInfoProvider, InAppPurchaseListener {

	private static final String TAG = BackupTypesFragment.class.getSimpleName();
	private static final String PROCESS_ID_KEY = "process_id";

	protected BaseBackupTypesController controller;
	protected String processId;

	protected ProgressBar progressBar;
	protected BackupTypesAdapter adapter;

	protected boolean wasDrawerDisabled;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			processId = savedInstanceState.getString(PROCESS_ID_KEY);
		}
		DialogManager dialogManager = app.getDialogManager();
		controller = (BaseBackupTypesController) dialogManager.findController(processId);
		if (controller != null) {
			controller.registerDialog(this);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_backup_types, container);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		progressBar = view.findViewById(R.id.progress_bar);

		adapter = new BackupTypesAdapter(view.getContext(), controller);
		adapter.updateSettingsItems();

		ExpandableListView expandableList = view.findViewById(R.id.list);
		expandableList.setAdapter(adapter);
		BaseSettingsListFragment.setupListView(expandableList);

		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(controller.getTitleId());

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			controller.onResume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
		controller.onPause();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PROCESS_ID_KEY, processId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		controller.updateData();
		if (isResumed() && adapter != null) {
			adapter.updateSettingsItems();
		}
	}

	public void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@NonNull
	public MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String processId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			BackupTypesFragment fragment = new BackupTypesFragment();
			fragment.processId = processId;
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}