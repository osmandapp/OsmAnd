package net.osmand.plus.routepreparationmenu;

import static net.osmand.plus.helpers.AndroidUiHelper.updateVisibility;
import static net.osmand.plus.routepreparationmenu.RequiredMapsController.PROCESS_ID;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.ColorUtilities.getActivityBgColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryIconColor;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class RequiredMapsFragment extends BaseOsmAndDialogFragment implements IAskRefreshDialogCompletely, DownloadEvents {

	private static final String TAG = RequiredMapsFragment.class.getSimpleName();

	private View view;

	private RequiredMapsController controller;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		Activity activity = requireActivity();
		Dialog dialog = new Dialog(activity, getThemeId()) {
			@Override
			public void onBackPressed() {
				RequiredMapsFragment.this.dismiss();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			window.setStatusBarColor(ColorUtilities.getStatusBarColor(app, nightMode));
		}
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DialogManager dialogManager = app.getDialogManager();
		controller = (RequiredMapsController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new RequiredMapsController(app);
			dialogManager.register(PROCESS_ID, controller);
		}
		dialogManager.register(PROCESS_ID, this);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.fragment_required_maps, container);
		view.setBackgroundColor(getActivityBgColor(app, nightMode));
		setupToolbar();
		updateContent();
		setupCalculateOnlineCard();
		return view;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			controller.askCancelOnlineCalculation();
			app.getDialogManager().unregister(PROCESS_ID);
		}
	}

	protected void setupToolbar() {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		int appBarColor = ColorUtilities.getAppBarColor(app, nightMode);
		appbar.setBackgroundColor(appBarColor);

		Toolbar toolbar = appbar.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(appBarColor);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.required_maps);

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> {
			controller.onSelectAllClicked();
			updateSelection();
		});
		actionButton.setImageDrawable(getIcon(R.drawable.ic_action_add_no_bg));
		actionButton.setContentDescription(getString(R.string.shared_string_add));
		AndroidUiHelper.updateVisibility(actionButton, true);

		updateToolbarMenu();
	}

	private void updateContent() {
		if (controller.isLoadingInProgress()) {
			updateVisibility(view.findViewById(R.id.loading_state), true);
			updateVisibility(view.findViewById(R.id.main_state), false);
		} else {
			updateVisibility(view.findViewById(R.id.loading_state), false);
			updateVisibility(view.findViewById(R.id.main_state), true);
			setupItemsList();
			updateUsedMapsSummary();
		}
		updateSelectionButtonVisibility();
		updateIgnoreMissingMapsCard();
		updateDownloadButton();
	}

	private void updateSelection() {
		updateToolbarMenu();
		updateListSelection();
		updateDownloadButton();
	}

	private void updateSelectionButtonVisibility() {
		ImageView actionButton = view.findViewById(R.id.action_button);
		updateVisibility(actionButton, !controller.isLoadingInProgress());
	}

	private void updateToolbarMenu() {
		boolean selected = controller.isAllItemsSelected();
		ImageView actionButton = view.findViewById(R.id.action_button);
		actionButton.setImageDrawable(getIcon(selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			actionButton.setContentDescription(getString(selected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));
		}
	}

	private void setupItemsList() {
		ViewGroup container = view.findViewById(R.id.items_container);
		container.removeAllViews();
		List<DownloadItem> items = controller.getMapsToDownload();
		for (int i = 0; i < items.size(); i++) {
			DownloadItem downloadItem = items.get(i);
			boolean showBottomDivider = i < items.size() - 1;
			container.addView(createItemView(downloadItem, showBottomDivider));
		}
		updateListSelection();
	}

	@NonNull
	private View createItemView(@NonNull DownloadItem downloadItem, boolean showBottomDivider) {
		View view = inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_and_divider_56dp);
		ImageView icon = view.findViewById(R.id.icon);
		boolean downloaded = downloadItem.isDownloaded();
		icon.setImageResource(downloaded ? R.drawable.ic_action_map_update : R.drawable.ic_action_map_download);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(getMapTitle(downloadItem));

		TextView tvDescription = view.findViewById(R.id.description);
		String pattern = getString(R.string.ltr_or_rtl_combine_via_bold_point);
		String size = downloadItem.getSizeDescription(app);
		String addDesc = downloadItem.getAdditionalDescription(app);
		if (addDesc != null) {
			size += " " + addDesc;
		}
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(app);
		String date = downloadItem.getDate(dateFormat, true);
		String fullDescription = String.format(pattern, size, date);
		tvDescription.setText(fullDescription);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		setupSelectableBackground(view);
		view.setOnClickListener(v -> {
			controller.onItemClicked(downloadItem);
			updateSelection();
		});
		view.setTag(downloadItem);
		updateVisibility(view.findViewById(R.id.divider_bottom), showBottomDivider);
		return view;
	}

	private void updateUsedMapsSummary() {
		List<String> regionNames = new ArrayList<>();
		for (DownloadItem downloadItem : controller.getUsedMaps()) {
			String regionName = "\"" + getMapTitle(downloadItem) + "\"";
			regionNames.add(regionName);
		}
		if (Algorithms.isEmpty(regionNames)) {
			updateVisibility(view.findViewById(R.id.available_maps_summary_container), false);
		} else {
			updateVisibility(view.findViewById(R.id.available_maps_summary_container), true);
			TextView tvSummary = view.findViewById(R.id.available_maps_summary);
			tvSummary.setText(getString(R.string.maps_that_also_be_used, TextUtils.join(", ", regionNames)));
		}
	}

	private void updateListSelection() {
		ViewGroup container = view.findViewById(R.id.items_container);
		for (int i = 0; i < container.getChildCount(); i++) {
			View view = container.getChildAt(i);
			if (view != null) {
				DownloadItem downloadItem = (DownloadItem) view.getTag();
				ImageView icon = view.findViewById(R.id.icon);
				CompoundButton compoundButton = view.findViewById(R.id.compound_button);
				boolean selected = controller.isItemSelected(downloadItem);
				compoundButton.setChecked(selected);
				int iconColor = selected
						? getActiveColor(app, nightMode) : getSecondaryIconColor(app, nightMode);
				icon.setColorFilter(iconColor);
			}
		}
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		updateContent();
	}

	private void setupCalculateOnlineCard() {
		boolean showOnlineCalculationBanner = controller.shouldShowOnlineCalculationBanner();
		updateVisibility(view.findViewById(R.id.card_calculate_online), showOnlineCalculationBanner);
		View buttonCalculateOnline = view.findViewById(R.id.calculate_online_button);
		buttonCalculateOnline.setOnClickListener(v -> controller.onCalculateOnlineButtonClicked());
		setupSelectableBackground(buttonCalculateOnline);
	}

	private void updateIgnoreMissingMapsCard() {
		updateVisibility(view.findViewById(R.id.card_ignore_missing_maps), controller.shouldShowUseDownloadedMapsBanner());
		View buttonIgnoreMissingMaps = view.findViewById(R.id.ignore_missing_maps_button);
		buttonIgnoreMissingMaps.setOnClickListener(v -> {
			controller.onIgnoreMissingMapsButtonClicked();
			dismiss();
		});
	}

	private void updateDownloadButton() {
		DialogButton downloadButton = view.findViewById(R.id.download_button);
		downloadButton.setTitle(controller.getDownloadButtonTitle());
		downloadButton.setEnabled(controller.isDownloadButtonEnabled());
		downloadButton.setOnClickListener(v -> {
			Activity activity = getActivity();
			if (activity instanceof MapActivity) {
				controller.onDownloadButtonClicked((MapActivity) activity);
				dismiss();
			}
		});
	}

	private void setupSelectableBackground(@NonNull View view) {
		int activeColor = getActiveColor(app, nightMode);
		UiUtilities.setupListItemBackground(app, view, activeColor);
	}

	@NonNull
	private String getMapTitle(@NonNull DownloadItem downloadItem) {
		OsmandRegions regions = app.getRegions();
		String basename = downloadItem.getBasename();
		return regions.getLocaleName(basename, true, true);
	}

	public int getThemeId() {
		return nightMode
				? R.style.OsmandDarkTheme_DarkActionbar
				: R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@Override
	public void onUpdatedIndexesList() {
		controller.onUpdatedIndexesList();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			RequiredMapsFragment requiredMapsFragment = new RequiredMapsFragment();
			requiredMapsFragment.show(manager, TAG);
		}
	}
}
