package net.osmand.plus.download.local.dialogs.livegroup;

import static net.osmand.plus.download.local.dialogs.livegroup.LiveGroupItemsFragment.*;

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
import net.osmand.plus.download.local.CategoryType;
import net.osmand.plus.download.local.LocalCategory;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.download.local.dialogs.LocalItemInfoCard;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.Map;

public class LiveGroupItemFragment extends LocalBaseFragment {

	public static final String TAG = LiveGroupItemFragment.class.getSimpleName();

	private LiveGroupItem localItem;
	private LiveGroupMenuProvider menuProvider;
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

		menuProvider = new LiveGroupMenuProvider(requireDownloadActivity(), this);
		menuProvider.setShowInfoItem(false);
		menuProvider.setLiveGroupItem(localItem);
		menuProvider.setColorId(ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode));
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.local_item_fragment, container, false);
		itemsContainer = view.findViewById(R.id.container);

		setupToolbar(view);
		updateToolbar();
		updateContent();

		return view;
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
		menuProvider.setLiveGroupItem(localItem);
		toolbarLayout.setTitle(localItem.name);
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull LiveGroupItem localItem, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			LiveGroupItemFragment fragment = new LiveGroupItemFragment();
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