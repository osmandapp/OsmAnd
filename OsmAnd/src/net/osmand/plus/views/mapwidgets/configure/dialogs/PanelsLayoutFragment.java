package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;
import static net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment.SCREEN_LAYOUT_MODE;

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
import net.osmand.plus.settings.enums.PanelsLayoutMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class PanelsLayoutFragment extends BaseFullScreenFragment {

	private static final String TAG = PanelsLayoutFragment.class.getSimpleName();
	private static final String PANELS_LAYOUT_KEY = "panels_layout_key";

	private ImageView previewIcon;
	private View wideLayoutButton;
	private View compactLayoutButton;
	private DialogButton applyButton;

	private PanelsLayoutMode panelsMode;
	private ScreenLayoutMode layoutMode;

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

		Bundle args = getArguments();
		if (args != null && args.containsKey(SCREEN_LAYOUT_MODE)) {
			layoutMode = AndroidUtils.getSerializable(args, SCREEN_LAYOUT_MODE, ScreenLayoutMode.class);
		}
		if (savedInstanceState != null && savedInstanceState.containsKey(PANELS_LAYOUT_KEY)) {
			panelsMode = (PanelsLayoutMode) savedInstanceState.getSerializable(PANELS_LAYOUT_KEY);
		} else {
			panelsMode = settings.getPanelsLayoutMode(requireContext(), layoutMode).get();
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
		title.setText(R.string.panels_layout);

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
		description.setText(R.string.panels_layout_descr);

		previewIcon = view.findViewById(R.id.preview_icon);
		wideLayoutButton = view.findViewById(R.id.single_layout_button);
		compactLayoutButton = view.findViewById(R.id.separate_layouts_button);

		setupActionButton(wideLayoutButton, R.string.panels_layout_wide, PanelsLayoutMode.WIDE);
		setupActionButton(compactLayoutButton, R.string.panels_layout_compact, PanelsLayoutMode.COMPACT);
	}

	private void updatePreviewIcon() {
		if (previewIcon != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(requireContext());
			previewIcon.setImageDrawable(getIcon(panelsMode.getImage(portrait, nightMode)));
		}
	}

	private void setupActionButton(@NonNull View view, @StringRes int titleId,
			@NonNull PanelsLayoutMode mode) {
		TextView title = view.findViewById(R.id.title);
		title.setText(titleId);

		view.setTag(mode);
		view.setOnClickListener(v -> {
			PanelsLayoutMode selectedMode = (PanelsLayoutMode) v.getTag();
			if (panelsMode != selectedMode) {
				panelsMode = selectedMode;
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
		updateButtonChecked(wideLayoutButton);
		updateButtonChecked(compactLayoutButton);
	}

	private void updateButtonChecked(@Nullable View button) {
		if (button != null && button.getTag() instanceof PanelsLayoutMode mode) {
			CompoundButton compoundButton = button.findViewById(R.id.compound_button);
			if (compoundButton != null) {
				compoundButton.setChecked(panelsMode == mode);
			}
		}
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			settings.getPanelsLayoutMode(requireContext(), layoutMode).set(panelsMode);

			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		updateApplyButton();
	}

	private void updateApplyButton() {
		if (applyButton != null) {
			applyButton.setEnabled(panelsMode != settings.getPanelsLayoutMode(requireContext(), layoutMode).get());
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(PANELS_LAYOUT_KEY, panelsMode);
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

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable ScreenLayoutMode layoutMode) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putSerializable(SCREEN_LAYOUT_MODE, layoutMode);

			PanelsLayoutFragment fragment = new PanelsLayoutFragment();
			fragment.setArguments(args);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}