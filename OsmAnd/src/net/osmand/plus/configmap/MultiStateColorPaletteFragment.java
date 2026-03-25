package net.osmand.plus.configmap;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.configmap.MapColorPaletteController.IMapColorPaletteControllerListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.shared.palette.domain.PaletteItem;

public abstract class MultiStateColorPaletteFragment extends ConfigureMapOptionFragment
		implements IDialogNightModeInfoProvider, IMapColorPaletteControllerListener,
		MultiStateColorPaletteController.IStateUIProvider {

	protected MultiStateCard multiStateCard;

	@NonNull
	protected abstract MultiStateColorPaletteController getScreenController();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MultiStateColorPaletteController controller = getScreenController();
		controller.setListener(this);
		controller.setUiProvider(this);

		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				controller.onCloseScreen(activity);
			}
		});
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getScreenController().getDialogTitle();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		MapActivity mapActivity = requireMapActivity();
		MultiStateColorPaletteController controller = getScreenController();

		multiStateCard = new MultiStateCard(mapActivity, controller);
		View cardView = multiStateCard.build(mapActivity);
		container.addView(cardView);
	}

	@Override
	public final void bindCustomStateContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container) {
		if (multiStateCard != null) {
			View selector = multiStateCard.getSelectorView();
			AndroidUiHelper.updateVisibility(selector, getScreenController().hasSelector());
		}
		buildCustomStateContent(activity, container);
	}

	protected abstract void buildCustomStateContent(@NonNull FragmentActivity activity,
	                                                @NonNull ViewGroup container);

	@Override
	protected void setupBottomContainer(@NonNull View bottomContainer) {
		bottomContainer.setPadding(0, 0, 0, bottomContainer.getPaddingBottom());
	}

	@Override
	protected void applyChanges() {
		getScreenController().onApplyChanges();
	}

	@Override
	protected void resetToDefault() {
		getScreenController().onResetToDefault();
		updateApplyButton(getScreenController().hasChanges());
		refreshMap();
	}

	@Override
	public void onResume() {
		super.onResume();
		getScreenController().onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		getScreenController().onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getScreenController().finishProcessIfNeeded(getActivity());
	}

	@Override
	public void onPaletteItemSelected(@NonNull PaletteItem item) {
		updateApplyButton(getScreenController().hasChanges());
	}

	@Override
	public void onPaletteItemAdded(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
	}

	@Override
	public void onPaletteModeChanged() {
		updateApplyButton(getScreenController().hasChanges());
		callMapActivity(MapActivity::refreshMap);
	}

	@Override
	public void updateStatusBar() {
		callActivity(activity -> {
			int color = getResources().getColor(getStatusBarColorId(), null);
			AndroidUiHelper.setStatusBarColor(activity, color);
		});
	}
}