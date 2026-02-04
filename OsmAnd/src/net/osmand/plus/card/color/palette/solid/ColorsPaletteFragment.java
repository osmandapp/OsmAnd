package net.osmand.plus.card.color.palette.solid;

import static net.osmand.plus.palette.contract.IPaletteController.ALL_PALETTE_ITEMS_PROCESS_ID;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.palette.contract.IPaletteController;
import net.osmand.plus.palette.contract.IPaletteInteractionListener;
import net.osmand.plus.palette.contract.IPaletteView;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.palette.view.PaletteElements;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.shared.palette.data.PaletteSortMode;
import net.osmand.shared.palette.domain.PaletteItem;

import org.jetbrains.annotations.NotNull;

// TODO: I think we should have 2 versions of this screen (or use 2 strategies) - list and flow
//  currently we use flow layout for solid colors (this screen) and list view for the gradient colors
//  but view is just a representation, so we can use the same representation for gradient and solid colors
public class ColorsPaletteFragment extends BaseFullScreenDialogFragment implements IPaletteView {

	public static final String TAG = ColorsPaletteFragment.class.getSimpleName();

	private IPaletteController controller;
	private IPaletteInteractionListener listener;
	private PaletteElements paletteElements;

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (IPaletteController) dialogManager.findController(ALL_PALETTE_ITEMS_PROCESS_ID);
		if (controller instanceof IPaletteInteractionListener l) {
			this.listener = l;
			controller.attachView(this);
		} else {
			dismiss();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		paletteElements = new PaletteElements(requireContext(), nightMode);

		View view = inflate(R.layout.fragment_palette, container, false);
		setupToolbar(view);
		setupColorsPalette(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_all_colors);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> listener.onAddButtonClick(requireActivity()));
		actionButton.setImageDrawable(getIcon(R.drawable.ic_action_add_no_bg));
		actionButton.setContentDescription(getString(R.string.shared_string_add));
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void setupColorsPalette(@NonNull View view) {
		FlowLayout flowLayout = view.findViewById(R.id.palette);
		flowLayout.removeAllViews();
		flowLayout.setHorizontalAutoSpacing(true);
		for (PaletteItem item : controller.getPaletteItems(PaletteSortMode.ORIGINAL_ORDER)) {
			flowLayout.addView(createColorItemView(item, flowLayout));
		}
		flowLayout.addView(createAddCustomColorItemView(flowLayout));
	}

	@NonNull
	private View createColorItemView(@NonNull PaletteItem item, FlowLayout rootView) {
		View view = paletteElements.createCircleView(rootView);
		boolean isSelected = controller.isPaletteItemSelected(item);

		int color = 0;
		if (item instanceof PaletteItem.Solid) {
			color = ((PaletteItem.Solid) item).getColorInt();
		}
		paletteElements.updateColorItemView(view, color, isSelected);

		ImageView background = view.findViewById(R.id.background);
		background.setOnClickListener(v -> {
			// TODO: should we immediately renew last used time of the item
			listener.onPaletteItemClick(item, true);
			dismiss();
		});
		background.setOnLongClickListener(v -> {
			listener.onPaletteItemLongClick(v, item);
			return false;
		});
		view.setTag(item);
		return view;
	}

	@Override
	public void updatePaletteItems(@Nullable PaletteItem targetItem) {
		View view = getView();
		if (view != null) {
			setupColorsPalette(view);
		}
	}

	@Override
	public void updatePaletteSelection(@org.jetbrains.annotations.Nullable PaletteItem oldItem, @NotNull PaletteItem newItem) {
		View view = getView();
		if (view == null) {
			return;
		}
		View oldColorContainer = view.findViewWithTag(oldItem);
		if (oldColorContainer != null) {
			oldColorContainer.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColorContainer.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(
					icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
		}
		View newColorContainer = view.findViewWithTag(newItem);
		if (newColorContainer != null) {
			AppCompatImageView outline = newColorContainer.findViewById(R.id.outline);

			int color = 0;
			if (newItem instanceof PaletteItem.Solid) {
				color = ((PaletteItem.Solid) newItem).getColorInt();
			}
			Drawable border = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle_contour, color);
			outline.setImageDrawable(border);
			outline.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void askScrollToPaletteItemPosition(@Nullable PaletteItem targetItem, boolean smoothScroll) {
		// Not relevant for this type of Palette View
	}

	@NonNull
	private View createAddCustomColorItemView(FlowLayout rootView) {
		View view = paletteElements.createAddButtonView(rootView);
		view.setOnClickListener(v -> listener.onAddButtonClick(requireActivity()));
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.detachView(this);
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			// Automatically unregister controller when close the dialog
			// to avoid any possible memory leaks
			DialogManager manager = app.getDialogManager();
			manager.unregister(ALL_PALETTE_ITEMS_PROCESS_ID);
			controller.onPaletteScreenClosed();
		}
	}

	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull IPaletteController controller) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
			DialogManager dialogManager = app.getDialogManager();
			dialogManager.register(ALL_PALETTE_ITEMS_PROCESS_ID, controller);
			new ColorsPaletteFragment().show(fragmentManager, TAG);
		}
	}
}
