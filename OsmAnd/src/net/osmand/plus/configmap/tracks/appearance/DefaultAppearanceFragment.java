package net.osmand.plus.configmap.tracks.appearance;

import static net.osmand.plus.configmap.tracks.appearance.DefaultAppearanceController.PROCESS_ID;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.configmap.tracks.ConfirmDefaultAppearanceBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class DefaultAppearanceFragment extends BaseFullScreenDialogFragment implements IAskDismissDialog, IAskRefreshDialogCompletely {

	private static final String TAG = DefaultAppearanceFragment.class.getSimpleName();

	private DialogManager dialogManager;
	private DefaultAppearanceController controller;

	private View applyButton;

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getThemeId()) {
			@Override
			public void onBackPressed() {
				dismiss();
			}
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, this);
		controller = (DefaultAppearanceController) dialogManager.findController(PROCESS_ID);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_tracks_default_appearance, container, false);

		setupToolbar(view);
		setupCards(view);
		setupApplyButton(view);

		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createCollapsingAppBar(R.id.appbar));
		return collection;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	private void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(app), colorId));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	protected void setupCards(@NonNull View view) {
		FragmentActivity activity = requireActivity();
		ViewGroup container = view.findViewById(R.id.cards_container);

		MultiStateCard arrowsCard = new MultiStateCard(activity, controller.getArrowsCardController(), false);
		container.addView(arrowsCard.build());

		inflate(R.layout.list_item_divider_with_padding_basic, container, true);

		MultiStateCard iconsCard = new MultiStateCard(activity, controller.getIconsCardController(), false);
		container.addView(iconsCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard colorsCard = new MultiStateCard(activity, controller.getColorCardController(), false);
		container.addView(colorsCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard widthCard = new MultiStateCard(activity, controller.getWidthCardController(), false);
		container.addView(widthCard.build());

		inflate(R.layout.list_item_divider, container, true);

		MultiStateCard splitCard = new MultiStateCard(activity, controller.getSplitCardController(), false);
		container.addView(splitCard.build());

		setupOnNeedScrollListener();
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			TrackFolder folder = controller.getFolder();
			ConfirmDefaultAppearanceBottomSheet.showInstance(getChildFragmentManager(), folder.getTrackItems().size());
		});
		updateApplyButton();
	}

	private void updateApplyButton() {
		if (applyButton != null) {
			applyButton.setEnabled(controller.hasAnyChangesToSave());
		}
	}

	private void setupOnNeedScrollListener() {
		controller.getWidthCardController().setOnNeedScrollListener(y -> {
			View view = getView();
			if (view != null) {
				int bottomVisibleY = view.findViewById(R.id.bottom_buttons_container).getTop();
				if (y > bottomVisibleY) {
					NestedScrollView scrollView = view.findViewById(R.id.scroll_view);
					int diff = y - bottomVisibleY;
					int scrollY = scrollView.getScrollY();
					scrollView.smoothScrollTo(0, scrollY + diff);
				}
			}
		});
	}

	public void saveChanges(boolean updateExisting) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			controller.saveChanges(activity, updateExisting);
		}
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		updateApplyButton();
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		if (controller.isAppearanceSaved()) {
			onAppearanceSaved();
		}
		dismiss();
	}

	private void onAppearanceSaved() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).refreshMapComplete();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DefaultAppearanceFragment fragment = new DefaultAppearanceFragment();
			fragment.show(manager, TAG);
		}
	}
}
