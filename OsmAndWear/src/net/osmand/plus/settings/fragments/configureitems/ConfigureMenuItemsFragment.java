package net.osmand.plus.settings.fragments.configureitems;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.configureitems.RearrangeItemsHelper.SCREEN_TYPE_KEY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.PRIMARY;
import static net.osmand.plus.widgets.dialogbutton.DialogButtonType.SECONDARY;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.menuitems.ContextMenuItemsSettings;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet;
import net.osmand.plus.settings.bottomsheets.ChangeGeneralProfilesPrefBottomSheet.OnChangeSettingListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.List;

public class ConfigureMenuItemsFragment extends BaseOsmAndFragment implements CopyAppModePrefsListener, OnChangeSettingListener {

	public static final String TAG = ConfigureMenuItemsFragment.class.getName();

	private RearrangeItemsHelper itemsHelper;
	private RearrangeMenuItemsAdapter adapter;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		itemsHelper = new RearrangeItemsHelper(this);

		if (savedInstanceState != null) {
			itemsHelper.loadBundle(savedInstanceState);
		} else {
			itemsHelper.initSavedIds(requireArguments());
		}
		itemsHelper.loadItemsOrder();

		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (itemsHelper.isChanged()) {
					showExitDialog();
				} else {
					dismiss();
				}
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = themedInflater.inflate(R.layout.edit_arrangement_list_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupRecyclerView(view);
		setupBottomButtons(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		AppBarLayout appbar = view.findViewById(R.id.appbar);
		View toolbar = themedInflater.inflate(R.layout.global_preference_toolbar, appbar, false);
		toolbar.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		appbar.addView(toolbar);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(itemsHelper.getScreenType().titleId);
		title.setTextColor(getColor(nightMode ? R.color.text_color_primary_dark : R.color.list_background_color_dark));

		ImageButton button = toolbar.findViewById(R.id.close_button);
		button.setOnClickListener(v -> onBackPressed());
		button.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
	}

	private void setupRecyclerView(@NonNull View view) {
		RecyclerView recyclerView = view.findViewById(R.id.profiles_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.dialog_button_ex_min_width));

		adapter = new RearrangeMenuItemsAdapter(itemsHelper.getAdapterItems(), nightMode);
		adapter.setListener(getMenuItemsAdapterListener(recyclerView));
		recyclerView.setAdapter(adapter);
	}

	@NonNull
	private MenuItemsAdapterListener getMenuItemsAdapterListener(@NonNull RecyclerView recyclerView) {
		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(recyclerView);

		return new RearrangeItemsAdapterListener(touchHelper, itemsHelper, adapter);
	}

	private void setupBottomButtons(@NonNull View view) {
		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> onBackPressed());

		DialogButton applyButton = view.findViewById(R.id.right_bottom_button);
		applyButton.setButtonType(PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setOnClickListener(v -> applyChanges());

		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void onBackPressed() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void applyChanges() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			List<String> ids = itemsHelper.getItemsIdsToSave();
			String prefId = itemsHelper.getSettingForScreen().getId();
			ContextMenuItemsSettings preference = itemsHelper.getPreferenceToSave(ids);
			ChangeGeneralProfilesPrefBottomSheet.showInstance(manager, prefId, preference,
					getTargetFragment(), false, R.string.back_to_editing, itemsHelper.getAppMode(), this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).disableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		itemsHelper.saveToBundle(outState);
	}

	public void resetToDefault() {
		itemsHelper.resetToDefault();
		adapter.updateItems(itemsHelper.getAdapterItems());
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		itemsHelper.copyAppModePrefs(appMode);
		adapter.updateItems(itemsHelper.getAdapterItems());
	}

	@Override
	public void onPreferenceApplied(boolean profileOnly) {
		dismiss();
	}

	private void showExitDialog() {
		Context context = UiUtilities.getThemedContext(requireContext(), nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.shared_string_dismiss);
		builder.setMessage(R.string.exit_without_saving);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismiss());
		builder.show();
	}

	private void dismiss() {
		FragmentManager manager = getFragmentManager();
		if (manager != null && !manager.isStateSaved()) {
			manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull ApplicationMode appMode, @NonNull ScreenType screenType) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(SCREEN_TYPE_KEY, screenType);
			bundle.putString(APP_MODE_KEY, appMode.getStringKey());

			ConfigureMenuItemsFragment fragment = new ConfigureMenuItemsFragment();
			fragment.setArguments(bundle);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
