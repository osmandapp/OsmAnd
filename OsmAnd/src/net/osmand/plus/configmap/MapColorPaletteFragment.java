package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteCard;
import net.osmand.plus.configmap.MapColorPaletteController.IMapColorPaletteControllerListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class MapColorPaletteFragment extends ConfigureMapOptionFragment implements IMapColorPaletteControllerListener {

	private MapColorPaletteController controller;
	private ModedColorsPaletteCard colorsPaletteCard;

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
		return controller.getDialogTitle();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = inflate(R.layout.fragment_colors_palette_with_map_preview, container, true);
		ViewGroup cardContainer = view.findViewById(R.id.card_container);
		colorsPaletteCard = new ModedColorsPaletteCard(requireActivity(), controller.getColorsPaletteController());
		cardContainer.addView(colorsPaletteCard.build());
		controller.setListener(this);
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	protected void resetToDefault() {
		controller.onResetToDefault();
		updateApplyButton(controller.hasChanges());
	}

	@Override
	protected void updateApplyButton(boolean enable) {
		super.updateApplyButton(enable);
	}

	@Override
	public void onResume() {
		super.onResume();
		controller.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		controller.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		updateApplyButton(controller.hasChanges());
	}

	@Override
	public void onColorsPaletteModeChanged() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	@Override
	public void updateStatusBar() {
		AndroidUiHelper.setStatusBarColor(requireActivity(), getResources().getColor(getStatusBarColorId(), null));
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new MapColorPaletteFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}

