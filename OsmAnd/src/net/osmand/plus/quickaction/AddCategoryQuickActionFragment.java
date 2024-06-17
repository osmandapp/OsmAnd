package net.osmand.plus.quickaction;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.controller.AddQuickActionController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class AddCategoryQuickActionFragment extends BaseOsmAndFragment implements AddQuickActionsAdapter.ItemClickListener, CreateEditActionDialog.AddQuickActionListener {

	public static final String TAG = AddCategoryQuickActionFragment.class.getSimpleName();

	public static final String QUICK_ACTION_CATEGORY_KEY = "quick_action_category_key";

	private AddQuickActionController controller;
	private QuickActionType categoryAction;

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = AddQuickActionController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
		} else {
			controller.registerDialog(TAG);
			Bundle args = getArguments();
			int categoryTypeId = args != null ? args.getInt(QUICK_ACTION_CATEGORY_KEY, -1) : -1;
			if (categoryTypeId != -1) {
				categoryAction = controller.getCategoryActionTypeFromId(categoryTypeId);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_add_category_quick_action, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		}
		setupToolbar(view);
		setupContent(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		CollapsingToolbarLayout toolbarLayout = view.findViewById(R.id.toolbar_layout);
		ViewCompat.setElevation(toolbarLayout, 5);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		if (categoryAction != null) {
			toolbar.setTitle(app.getString(categoryAction.getNameRes()));
		}
		toolbar.setNavigationIcon(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});
	}

	private void setupContent(@NonNull View view) {
		AddQuickActionsAdapter adapter = new AddQuickActionsAdapter(app, requireActivity(), this, nightMode);
		adapter.setItems(controller.getCategoryTypes(categoryAction));
		RecyclerView recyclerView = view.findViewById(R.id.content_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			controller.unregisterDialog(TAG);
		}
	}

	@Override
	public void onItemClick(@NonNull QuickActionType quickActionType) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			if (quickActionType.getId() != 0) {
				CreateEditActionDialog.showInstance(manager, quickActionType.getId());
			}
		}
	}

	@Override
	public void onQuickActionAdded() {
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, int quickActionCategoryId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putInt(QUICK_ACTION_CATEGORY_KEY, quickActionCategoryId);

			AddCategoryQuickActionFragment fragment = new AddCategoryQuickActionFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}