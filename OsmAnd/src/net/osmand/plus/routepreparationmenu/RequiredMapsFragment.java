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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RequiredMapsController.RequiredMapItem;
import net.osmand.plus.routepreparationmenu.RequiredMapsController.RequiredMapType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.List;

public class RequiredMapsFragment extends BaseFullScreenDialogFragment implements IAskRefreshDialogCompletely, DownloadEvents {

	private static final String TAG = RequiredMapsFragment.class.getSimpleName();
	public static final String OPEN_FRAGMENT_KEY = "REQUIRED_MAPS_FRAGMENT_OPEN_FRAGMENT_KEY";

	private View view;

	private RequiredMapsController controller;

	@Override
	protected int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		return new Dialog(requireActivity(), getThemeId()) {
			@Override
			public void onBackPressed() {
				closeDialog();
			}
		};
	}

	public void closeDialog() {
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null) {
			carNavigationSession.onRequiredMapsDialogClosed();
		}
		dismiss();
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
		view = inflate(R.layout.fragment_required_maps, container, false);
		view.setBackgroundColor(getActivityBgColor(app, nightMode));
		setupToolbar();
		updateContent();
		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.add(InsetTarget.createBottomContainer(R.id.bottom_panel).build());
		return collection;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			controller.askCancelOnlineCalculation();
			app.getDialogManager().unregister(PROCESS_ID);
			app.getSettings().setStopOnMissingMaps(false);
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
		closeButton.setOnClickListener(v -> closeDialog());

		updateToolbarTitle();

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
		updateToolbarTitle();
		if (controller.isLoadingInProgress()) {
			updateVisibility(view.findViewById(R.id.loading_state), true);
			updateVisibility(view.findViewById(R.id.main_state), false);
		} else {
			updateVisibility(view.findViewById(R.id.loading_state), false);
			updateVisibility(view.findViewById(R.id.main_state), true);
			updateRouteOverviewCard();
			setupItemsList();
		}
		updateSelectionButtonVisibility();
		updateDownloadButton();
	}

	private void updateToolbarTitle() {
		if (view != null) {
			TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
			if (toolbarTitle != null) {
				toolbarTitle.setText(controller.getToolbarTitleId());
			}
		}
	}

	private void updateSelection() {
		updateToolbarMenu();
		updateListSelection();
		updateDownloadButton();
	}

	private void updateSelectionButtonVisibility() {
		ImageView actionButton = view.findViewById(R.id.action_button);
		updateVisibility(actionButton, !controller.isLoadingInProgress()
				&& !Algorithms.isEmpty(controller.getMapsToProcess()));
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
		List<RequiredMapItem> items = controller.getMapsToProcess();
		for (int i = 0; i < items.size(); i++) {
			RequiredMapItem downloadItem = items.get(i);
			boolean showBottomDivider = i < items.size() - 1;
			container.addView(createItemView(downloadItem, showBottomDivider));
		}
		updateListSelection();
	}

	@NonNull
	private View createItemView(@NonNull RequiredMapItem mapItem, boolean showBottomDivider) {
		View view = inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_and_divider_56dp);
		DownloadItem downloadItem = mapItem.downloadItem();
		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageResource(mapItem.type() == RequiredMapType.OUTDATED
				? R.drawable.ic_action_map_update : R.drawable.ic_action_map_missing);

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
		String date = downloadItem.isDownloaded()
				? downloadItem.getDate(dateFormat, false)
				: downloadItem.getDate(dateFormat, true);
		String fullDescription = Algorithms.isEmpty(date) ? size : String.format(pattern, size, date);
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

	private void updateRouteOverviewCard() {
		List<RequiredMapItem> items = controller.getRouteOverviewItems();
		View card = view.findViewById(R.id.card_route_overview);
		updateVisibility(card, !Algorithms.isEmpty(items));
		if (Algorithms.isEmpty(items)) {
			return;
		}

		TextView maps = view.findViewById(R.id.route_overview_maps);
		maps.setText(createRouteOverviewText(items));

		TextView getAccurateList = view.findViewById(R.id.route_overview_calculate_online_button);
		boolean showOnlineAction = controller.shouldShowOnlineCalculationBanner();
		updateVisibility(getAccurateList, showOnlineAction);
		getAccurateList.setOnClickListener(v -> controller.onCalculateOnlineButtonClicked());
		setupSelectableBackground(getAccurateList);

		TextView calculateExisting = view.findViewById(R.id.route_overview_use_existing_button);
		boolean showUseExistingAction = controller.shouldShowUseDownloadedMapsBanner();
		updateVisibility(calculateExisting, showUseExistingAction);
		calculateExisting.setOnClickListener(v -> {
			controller.onIgnoreMissingMapsButtonClicked();
			dismiss();
		});
		setupSelectableBackground(calculateExisting);

		updateVisibility(view.findViewById(R.id.route_overview_actions_divider),
				showOnlineAction || showUseExistingAction);
		updateVisibility(view.findViewById(R.id.route_overview_actions_middle_divider),
				showOnlineAction && showUseExistingAction);
	}

	@NonNull
	private SpannableStringBuilder createRouteOverviewText(@NonNull List<RequiredMapItem> items) {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				builder.append(" — ");
			}
			appendRouteOverviewItem(builder, items.get(i));
		}
		return builder;
	}

	private void appendRouteOverviewItem(@NonNull SpannableStringBuilder builder, @NonNull RequiredMapItem item) {
		int color = item.type() == RequiredMapType.USED
				? ColorUtilities.getSecondaryTextColor(app, nightMode)
				: ColorUtilities.getPrimaryTextColor(app, nightMode);
		int start = builder.length();
		builder.append(getRouteOverviewIcon(item.type()));
		builder.append(" ");
		builder.append(getMapTitle(item.downloadItem()));
		builder.setSpan(new ForegroundColorSpan(color), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	@NonNull
	private String getRouteOverviewIcon(@NonNull RequiredMapType type) {
		return switch (type) {
			case OUTDATED -> "🔄";
			case MISSING -> "❌";
			default -> "✅";
		};
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

	private void updateDownloadButton() {
		boolean hasItemsToProcess = !Algorithms.isEmpty(controller.getMapsToProcess());
		updateVisibility(view.findViewById(R.id.bottom_panel), hasItemsToProcess);
		updateVisibility(view.findViewById(R.id.bottom_panel_shadow), hasItemsToProcess);
		if (!hasItemsToProcess) {
			return;
		}
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
