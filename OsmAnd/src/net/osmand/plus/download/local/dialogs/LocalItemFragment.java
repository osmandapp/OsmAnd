package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment.OnMapSourceUpdateListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalItemFragment extends LocalBaseFragment
		implements OperationListener, OnMapSourceUpdateListener {

	public static final String TAG = LocalItemFragment.class.getSimpleName();

	private BaseLocalItem localItem;
	private ItemMenuProvider menuProvider;
	private ViewGroup itemsContainer;
	private CollapsingToolbarLayout toolbarLayout;

	@Nullable
	@Override
	public Map<CategoryType, LocalCategory> getCategories() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof LocalBaseFragment) {
			return ((LocalBaseFragment) fragment).getCategories();
		}
		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		menuProvider = new ItemMenuProvider(requireDownloadActivity(), this);
		menuProvider.setShowInfoItem(false);
		menuProvider.setItem(localItem);
		menuProvider.setColorId(ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode));
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.local_item_fragment, container, false);
		itemsContainer = view.findViewById(R.id.container);

		setupToolbar(view);
		updateToolbar();
		updateContent();

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	private void setupToolbar(@NonNull View view) {
		toolbarLayout = view.findViewById(R.id.toolbar_layout);
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
		toolbar.addMenuProvider(menuProvider);
	}

	private void updateToolbar() {
		menuProvider.setItem(localItem);
		toolbarLayout.setTitle(localItem.getName(app).toString());
	}

	private void updateContent() {
		itemsContainer.removeAllViews();

		FragmentActivity activity = getActivity();
		if (activity != null) {
			LocalItemInfoCard card = new LocalItemInfoCard(activity, localItem);
			itemsContainer.addView(card.build(itemsContainer.getContext()));
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

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		super.onOperationFinished(type, result);

		DownloadActivity activity = getDownloadActivity();
		if (type == DELETE_OPERATION && AndroidUtils.isActivityNotDestroyed(activity)) {
			activity.onBackPressed();
		}
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
		super.fileRenamed(src, dest);

		LocalItemType type = LocalItemUtils.getItemType(app, dest);
		if (type != null && localItem instanceof LocalItem) {
			localItem = new LocalItem(dest, type);
			LocalItemUtils.updateItem(app, (LocalItem) localItem);
		}
		updateToolbar();
		updateContent();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull BaseLocalItem localItem, @Nullable Fragment target) {
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
