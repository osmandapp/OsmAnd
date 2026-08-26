package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteCard;
import net.osmand.plus.configmap.MapColorPaletteController.IMapColorPaletteControllerListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.shared.palette.domain.PaletteItem;

public class MapColorPaletteFragment extends ConfigureMapOptionFragment implements IMapColorPaletteControllerListener {

	private MapColorPaletteController controller;
	private ModedColorsPaletteCard colorsPaletteCard;
	private ViewGroup cardContainer;
	private View bottomContainer;
	@Nullable
	private Boolean contentAvailable;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = MapColorPaletteController.getExistedInstance(app);
		if (controller != null) {
			MapActivity activity = requireMapActivity();
			activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
				@Override
				public void handleOnBackPressed() {
					controller.onCloseScreen(activity);
				}
			});
		} else {
			dismiss();
		}
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return controller != null ? controller.getDialogTitle() : null;
	}

	@Override
	public boolean shouldShowMapWidgets() {
		return controller != null && controller.shouldShowMapWidgets();
	}

	@Nullable
	public WidgetsPanel getPreviewPanel() {
		return controller != null ? controller.getPreviewPanel() : null;
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		if (controller == null) {
			return;
		}
		View view = inflate(R.layout.fragment_colors_palette_with_map_preview, container, true);
		TextView sectionTitle = view.findViewById(R.id.color_section_title);
		sectionTitle.setText(controller.getColorSectionTitleId());
		cardContainer = view.findViewById(R.id.card_container);
		controller.setListener(this);
		bindColorContent();
	}

	private void bindColorContent() {
		if (controller == null || cardContainer == null) {
			return;
		}
		boolean available = controller.isColorSelectionAvailable();
		cardContainer.removeAllViews();
		if (available) {
			colorsPaletteCard = new ModedColorsPaletteCard(requireActivity(), controller.getColorsPaletteController());
			cardContainer.addView(colorsPaletteCard.build());
			updateApplyButton(controller.hasChanges());
		} else {
			colorsPaletteCard = null;
			setupUnavailableContent(cardContainer);
		}
		contentAvailable = available;
		updateApplyButtonVisibility();
	}

	private void setupUnavailableContent(@NonNull ViewGroup container) {
		View emptyState = inflate(R.layout.panel_color_palette_empty_state, container, false);
		ImageView icon = emptyState.findViewById(R.id.icon);
		TextView title = emptyState.findViewById(R.id.title);
		TextView description = emptyState.findViewById(R.id.description);
		View actionButton = emptyState.findViewById(R.id.button);
		TextView actionTitle = emptyState.findViewById(R.id.action_title);

		icon.setImageResource(controller.getUnavailableIconId());
		title.setText(controller.getUnavailableTitleId());
		description.setText(controller.getUnavailableDescriptionId());
		actionTitle.setText(controller.getUnavailableActionTitleId());
		actionButton.setOnClickListener(v -> controller.onUnavailableAction(requireActivity()));
		container.addView(emptyState);
	}

	@Override
	protected void setupBottomContainer(@NonNull View bottomContainer) {
		this.bottomContainer = bottomContainer;
		updateApplyButtonVisibility();
	}

	private void updateApplyButtonVisibility() {
		if (bottomContainer != null && controller != null) {
			AndroidUiHelper.updateVisibility(bottomContainer.findViewById(R.id.apply_button),
					controller.isColorSelectionAvailable());
		}
	}

	@Override
	protected void applyChanges() {
		if (controller != null) {
			controller.onApplyChanges();
		}
	}

	@Override
	protected void resetToDefault() {
		if (controller != null) {
			controller.onResetToDefault();
			updateApplyButton(controller.hasChanges());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (controller == null) {
			return;
		}
		controller.onResume();
		if (cardContainer != null
				&& contentAvailable != null
				&& contentAvailable != controller.isColorSelectionAvailable()) {
			bindColorContent();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (controller != null) {
			controller.onPause();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		colorsPaletteCard = null;
		cardContainer = null;
		bottomContainer = null;
		contentAvailable = null;
	}

	@Override
	public void onPaletteItemSelected(@NonNull PaletteItem item) {
		if (controller != null) {
			updateApplyButton(controller.hasChanges());
		}
	}

	@Override
	public void onPaletteItemAdded(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
	}

	@Override
	public void onPaletteModeChanged() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	@Override
	public void updateStatusBar() {
		AndroidUiHelper.setStatusBarColor(requireActivity(), getResources().getColor(getStatusBarColorId(), null));
	}

	public static boolean showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new MapColorPaletteFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void onAvailabilityChanged() {
		bindColorContent();
	}
}

