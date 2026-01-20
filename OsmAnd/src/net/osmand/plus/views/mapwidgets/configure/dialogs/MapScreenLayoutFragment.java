package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class MapScreenLayoutFragment extends BaseFullScreenFragment {

	private static final String TAG = MapScreenLayoutFragment.class.getSimpleName();
	private static final String USE_SEPARATE_LAYOUTS_KEY = "use_separate_layouts_key";

	private ImageView previewIcon;
	private View singleLayoutButton;
	private View separateLayoutsButton;
	private DialogButton applyButton;

	private boolean useSeparateLayouts;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getActivityBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey(USE_SEPARATE_LAYOUTS_KEY)) {
			useSeparateLayouts = savedInstanceState.getBoolean(USE_SEPARATE_LAYOUTS_KEY);
		} else {
			useSeparateLayouts = settings.USE_SEPARATE_LAYOUTS.get();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();

		View view = inflate(R.layout.map_screen_layout_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		setupContent(view);
		setupApplyButton(view);

		update();

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(toolbar, 5);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.map_screen_layout);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		closeButton.setContentDescription(app.getString(R.string.access_shared_string_navigate_up));

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.action_button), false);
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void setupContent(@NonNull View view) {
		TextView description = view.findViewById(R.id.description);
		description.setText(R.string.map_screen_layout_descr);

		previewIcon = view.findViewById(R.id.preview_icon);
		singleLayoutButton = view.findViewById(R.id.single_layout_button);
		separateLayoutsButton = view.findViewById(R.id.separate_layouts_button);

		setupActionButton(singleLayoutButton, R.string.single_layout, false);
		setupActionButton(separateLayoutsButton, R.string.separate_layouts, true);
	}

	private void updatePreviewIcon() {
		if (previewIcon != null) {
			previewIcon.setImageDrawable(getPreviewIcon());
		}
	}

	private Drawable getPreviewIcon() {
		if (useSeparateLayouts) {
			return getIcon(nightMode ? R.drawable.img_map_screen_layout_separate_night : R.drawable.img_map_screen_layout_separate_day);
		} else {
			return getIcon(nightMode ? R.drawable.img_map_screen_layout_single_night : R.drawable.img_map_screen_layout_single_day);
		}
	}

	private void setupActionButton(@NonNull View view, @StringRes int titleId, boolean mode) {
		TextView title = view.findViewById(R.id.title);
		title.setText(titleId);

		view.setTag(mode);
		view.setOnClickListener(v -> {
			boolean selectedMode = (boolean) v.getTag();
			if (useSeparateLayouts != selectedMode) {
				useSeparateLayouts = selectedMode;
				update();
			}
		});
		UiUtilities.setupCompoundButton(view.findViewById(R.id.compound_button), nightMode, PROFILE_DEPENDENT);
	}

	private void update() {
		updateButtonsState();
		updatePreviewIcon();
		updateApplyButton();
	}

	private void updateButtonsState() {
		updateButtonChecked(singleLayoutButton);
		updateButtonChecked(separateLayoutsButton);
	}

	private void updateButtonChecked(@Nullable View button) {
		if (button != null && button.getTag() instanceof Boolean mode) {
			CompoundButton compoundButton = button.findViewById(R.id.compound_button);
			if (compoundButton != null) {
				compoundButton.setChecked(useSeparateLayouts == mode);
			}
		}
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			settings.USE_SEPARATE_LAYOUTS.set(useSeparateLayouts);

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void updateApplyButton() {
		if (applyButton != null) {
			applyButton.setEnabled(useSeparateLayouts != settings.USE_SEPARATE_LAYOUTS.get());
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(USE_SEPARATE_LAYOUTS_KEY, useSeparateLayouts);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		callMapActivity(MapActivity::disableDrawer);
	}

	@Override
	public void onPause() {
		super.onPause();
		callMapActivity(MapActivity::enableDrawer);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			MapScreenLayoutFragment fragment = new MapScreenLayoutFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}