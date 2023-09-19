package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemsHolder;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationBottomSheet.ConfirmDeletionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocalItemFragment extends LocalBaseFragment implements ConfirmDeletionListener,
		OperationListener, OnMapSourceUpdateListener {

	public static final String TAG = LocalItemFragment.class.getSimpleName();

	private LocalItem localItem;
	private ViewGroup itemsContainer;

	@Nullable
	@Override
	public LocalItemsHolder getItemsHolder() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalCategoriesFragment) {
			return ((LocalCategoriesFragment) fragment).getItemsHolder();
		}
		return null;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.local_item_fragment, container, false);
		itemsContainer = view.findViewById(R.id.container);

		setupToolbar(view);
		updateContent();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		CollapsingToolbarLayout toolbarLayout = view.findViewById(R.id.toolbar_layout);
		toolbarLayout.setTitle(localItem.getName());
		ViewCompat.setElevation(toolbarLayout, 5);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		toolbar.addMenuProvider(getMenuProvider(), this);
	}

	private void updateContent() {
		String type = getString(localItem.getType().getTitleId());
		addRow(itemsContainer, getString(R.string.shared_string_type), type, false);

		DateFormat format = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
		String date = format.format(localItem.getFile().lastModified());
		addRow(itemsContainer, getString(R.string.shared_string_created), date, false);

		String size = AndroidUtils.formatSize(app, localItem.getSize());
		addRow(itemsContainer, getString(R.string.shared_string_size), size, true);
	}

	private void addRow(@NonNull ViewGroup container, String title, String description, boolean lastItem) {
		View view = themedInflater.inflate(R.layout.local_item_row, container, false);
		container.addView(view);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(description);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !lastItem);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_shadow), lastItem);
	}

	@NonNull
	private MenuProvider getMenuProvider() {
		ItemMenuProvider menuProvider = new ItemMenuProvider(requireDownloadActivity(), this);
		menuProvider.setLocalItem(localItem);
		menuProvider.setShowInfoItem(false);
		menuProvider.setColorId(ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode));
		return menuProvider;
	}

	@Override
	public void onOperationStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		updateProgressVisibility(false);

		if (!Algorithms.isEmpty(result)) {
			app.showToastMessage(result);
		}

		DownloadActivity activity = getDownloadActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			if (Algorithms.equalsToAny(type, RESTORE_OPERATION, BACKUP_OPERATION, CLEAR_TILES_OPERATION)) {
				activity.reloadLocalIndexes();
			} else {
				activity.onUpdatedIndexesList();
			}
			if (type == DELETE_OPERATION) {
				activity.onBackPressed();
			}
		}
	}

	@Override
	public void onDeletionConfirmed(@NonNull LocalItem localItem) {
		performOperation(DELETE_OPERATION, localItem);
	}

	public void performOperation(@NonNull OperationType type, @NonNull LocalItem... items) {
		LocalOperationTask task = new LocalOperationTask(app, type, this);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, items);
	}

	@Override
	public void onMapSourceUpdated() {
		reloadLocalIndexes();
	}

	private void reloadLocalIndexes() {
		DownloadActivity activity = getDownloadActivity();
		if (activity != null) {
			activity.reloadLocalIndexes();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		AndroidUiHelper.updateActionBarVisibility(getDownloadActivity(), false);
	}

	@Override
	public void onPause() {
		super.onPause();
		AndroidUiHelper.updateActionBarVisibility(getDownloadActivity(), true);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LocalItem localItem, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			LocalItemFragment fragment = new LocalItemFragment();
			fragment.localItem = localItem;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
