package net.osmand.plus.settings.fragments.profileappearance;

import static net.osmand.plus.card.icon.IIconsPaletteController.ALL_ICONS_PROCESS_ID;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.icon.IIconsPalette;
import net.osmand.plus.card.icon.IIconsPaletteController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.FlowLayout;

public class IconsPaletteFragment<IconData> extends BaseOsmAndDialogFragment implements IIconsPalette<IconData> {

	public static final String TAG = IconsPaletteFragment.class.getSimpleName();

	private IIconsPaletteController<IconData> controller;
	private IconsPaletteElements<IconData> paletteElements;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (IIconsPaletteController<IconData>) dialogManager.findController(ALL_ICONS_PROCESS_ID);
		if (controller != null) {
			controller.bindPalette(this);
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(getColor(getStatusBarColorId()));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		this.paletteElements = controller.getPaletteElements(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.fragment_palette, container, false);
		setupToolbar(view);
		setupIconsPalette(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		String paletteName = controller.getPaletteTitle();
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(paletteName != null ? paletteName : getString(R.string.shared_string_all_icons));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);
	}

	private void askSetupIconsPalette() {
		View view = getView();
		if (view != null) {
			setupIconsPalette(view);
		}
	}

	private void setupIconsPalette(@NonNull View view) {
		FlowLayout flowLayout = view.findViewById(R.id.palette);
		flowLayout.removeAllViews();
		flowLayout.setHorizontalAutoSpacing(false);
		flowLayout.setAlignToCenter(true);
		for (IconData icon : controller.getIcons()) {
			flowLayout.addView(createIconItemView(icon, flowLayout));
		}
	}

	@NonNull
	private View createIconItemView(@NonNull IconData icon, @NonNull FlowLayout rootView) {
		View view = paletteElements.createView(rootView);
		boolean isSelected = controller.isSelectedIcon(icon);
		int controlsColor = controller.getControlsAccentColor(nightMode);
		paletteElements.bindView(view, icon, controlsColor, isSelected);

		view.setOnClickListener(v -> {
			controller.onSelectIconFromPalette(icon);
			dismiss();
		});
		view.setTag(icon);
		return view;
	}

	@Override
	public void updatePaletteColors() {
		askSetupIconsPalette();
	}

	@Override
	public void updatePaletteIcons(@Nullable IconData targetIcon) {
		askSetupIconsPalette();
	}

	@Override
	public void updatePaletteSelection(@Nullable IconData oldIcon, @NonNull IconData newIcon) {
		View view = getView();
		if (view == null) {
			return;
		}
		int controlsColor = controller.getControlsAccentColor(nightMode);
		View oldIconView = view.findViewWithTag(oldIcon);
		if (oldIcon != null && oldIconView != null) {
			paletteElements.bindView(oldIconView, oldIcon, controlsColor, false);
		}
		View newIconView = view.findViewWithTag(newIcon);
		if (newIconView != null) {
			paletteElements.bindView(newIconView, newIcon, controlsColor, true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.unbindPalette(this);
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			// Automatically unregister controller when close the dialog
			// to avoid any possible memory leaks
			DialogManager manager = app.getDialogManager();
			manager.unregister(ALL_ICONS_PROCESS_ID);
			controller.onAllIconsScreenClosed();
		}
	}

	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull IIconsPaletteController<?> controller) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(ALL_ICONS_PROCESS_ID, controller);
			new IconsPaletteFragment<>().show(fragmentManager, TAG);
		}
	}
}
