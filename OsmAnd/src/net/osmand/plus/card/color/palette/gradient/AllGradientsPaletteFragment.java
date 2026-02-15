package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.palette.contract.IPaletteController.ALL_PALETTE_ITEMS_PROCESS_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.palette.contract.IPaletteController;
import net.osmand.plus.palette.contract.IPaletteView;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.palette.controller.BasePaletteController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.palette.domain.PaletteItem;

public class AllGradientsPaletteFragment extends BaseFullScreenDialogFragment implements IPaletteView {

	public static final String TAG = AllGradientsPaletteFragment.class.getSimpleName();

	private BasePaletteController controller;
	private AllGradientsPaletteAdapter adapter;

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (BasePaletteController) dialogManager.findController(ALL_PALETTE_ITEMS_PROCESS_ID);
		if (controller != null) {
			controller.attachView(this);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_gradients_palette, container, false);
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
		actionButton.setOnClickListener(v -> controller.onAddButtonClick(requireActivity()));
		actionButton.setImageDrawable(getIcon(R.drawable.ic_action_add_no_bg));
		actionButton.setContentDescription(getString(R.string.shared_string_add));
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	private void setupColorsPalette(@NonNull View view) {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		adapter = new AllGradientsPaletteAdapter(app, requireActivity(), controller, nightMode);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
	}

	@Override
	public void updatePaletteItems(@Nullable PaletteItem targetItem) {
		if (adapter != null) {
			adapter.update();
		}
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
		if (adapter != null) {
			adapter.askNotifyItemChanged(oldItem);
			adapter.askNotifyItemChanged(newItem);
		}
		if (!controller.shouldKeepAllItemsScreen()) {
			dismiss();
		}
	}

	@Override
	public void askScrollToPaletteItemPosition(@Nullable PaletteItem targetItem, boolean smoothScroll) {
		// Not relevant for this type of Palette View
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
			new AllGradientsPaletteFragment().show(fragmentManager, TAG);
		}
	}
}
