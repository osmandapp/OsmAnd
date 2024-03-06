package net.osmand.plus.card.color.palette.main;

import static net.osmand.plus.card.color.palette.main.IColorsPaletteController.ALL_COLORS_PROCESS_ID;
import static net.osmand.plus.utils.ColorUtilities.getColor;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.FlowLayout.LayoutParams;

public class ColorsPaletteFragment extends BaseOsmAndDialogFragment implements IColorsPalette {

	public static final String TAG = ColorsPaletteFragment.class.getSimpleName();

	private IColorsPaletteController controller;
	private ColorsPaletteElements paletteElements;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (IColorsPaletteController) dialogManager.findController(ALL_COLORS_PROCESS_ID);
		if (controller != null) {
			controller.bindPalette(this);
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
		paletteElements = new ColorsPaletteElements(requireActivity(), nightMode);
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(getColor(ctx, getStatusBarColorId()));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_colors_palette, container, false);
		setupToolbar(view);
		setupColorsPalette(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_sting_all_colors);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> controller.onAddColorButtonClicked(requireActivity()));
		actionButton.setImageDrawable(getIcon(R.drawable.ic_action_add_no_bg));
		actionButton.setContentDescription(getString(R.string.shared_string_add));
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void setupColorsPalette(@NonNull View view) {
		FlowLayout flowLayout = view.findViewById(R.id.colors_palette);
		flowLayout.removeAllViews();
		flowLayout.setHorizontalAutoSpacing(true);
		int minimalPaddingBetweenIcon = getDimension(R.dimen.favorites_select_icon_button_right_padding);

		for (PaletteColor paletteColor : controller.getColors(PaletteSortingMode.ORIGINAL)) {
			flowLayout.addView(createColorItemView(paletteColor, flowLayout), new LayoutParams(minimalPaddingBetweenIcon, 0));
		}
		flowLayout.addView(createAddCustomColorItemView(flowLayout), new LayoutParams(minimalPaddingBetweenIcon, 0));
	}

	@NonNull
	private View createColorItemView(@NonNull PaletteColor paletteColor, FlowLayout rootView) {
		View view = paletteElements.createCircleView(rootView);
		boolean isSelected = controller.isSelectedColor(paletteColor);
		paletteElements.updateColorItemView(view, paletteColor.getColor(), isSelected);

		ImageView background = view.findViewById(R.id.background);
		background.setOnClickListener(v -> {
			controller.onSelectColorFromPalette(paletteColor);
			dismiss();
		});
		background.setOnLongClickListener(v -> {
			controller.onColorLongClick(requireActivity(), v, paletteColor, nightMode);
			return false;
		});
		view.setTag(paletteColor);
		return view;
	}


	@Override
	public void updatePaletteColors(@Nullable PaletteColor targetPaletteColor) {
		View view = getView();
		if (view != null) {
			setupColorsPalette(view);
		}
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		View view = getView();
		if (view == null) {
			return;
		}
		View oldColorContainer = view.findViewWithTag(oldColor);
		if (oldColorContainer != null) {
			oldColorContainer.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColorContainer.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(
					icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
		}
		View newColorContainer = view.findViewWithTag(newColor);
		if (newColorContainer != null) {
			AppCompatImageView outline = newColorContainer.findViewById(R.id.outline);
			Drawable border = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle_contour, newColor.getColor());
			outline.setImageDrawable(border);
			outline.setVisibility(View.VISIBLE);
		}
	}

	@NonNull
	private View createAddCustomColorItemView(FlowLayout rootView) {
		View view = paletteElements.createButtonAddColorView(rootView);
		view.setOnClickListener(v -> controller.onAddColorButtonClicked(requireActivity()));
		return view;
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
			manager.unregister(ALL_COLORS_PROCESS_ID);
		}
	}

	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull IColorsPaletteController controller) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(ALL_COLORS_PROCESS_ID, controller);
			new ColorsPaletteFragment().show(fragmentManager, TAG);
		}
	}
}
