package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.OnSuccessfulGpxImport;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.routepreparationmenu.cards.AttachTrackToRoadsCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.ImportTrackCard;
import net.osmand.plus.routepreparationmenu.cards.NavigateTrackOptionsCard;
import net.osmand.plus.routepreparationmenu.cards.ReverseTrackCard;
import net.osmand.plus.routepreparationmenu.cards.SelectTrackCard;
import net.osmand.plus.routepreparationmenu.cards.SelectedTrackToFollowCard;
import net.osmand.plus.routepreparationmenu.cards.TrackEditCard;
import net.osmand.plus.routepreparationmenu.cards.TracksToFollowCard;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FollowTrackFragment extends ContextMenuScrollFragment implements CardListener,
		OnSegmentSelectedListener {

	public static final String TAG = FollowTrackFragment.class.getName();

	private static final Log log = PlatformUtil.getLog(FollowTrackFragment.class);

	private static final String SELECTING_TRACK = "selecting_track";

	private ImportHelper importHelper;

	private GPXFile gpxFile;

	private View buttonsShadow;
	private ImageButton sortButton;

	private TracksToFollowCard tracksCard;
	private TracksSortByMode sortByMode = TracksSortByMode.BY_DATE;

	private boolean editingTrack;
	private boolean selectingTrack;
	private int menuTitleHeight;

	@Override
	public int getMainLayoutId() {
		return R.layout.follow_track_options;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		importHelper = app.getImportHelper();

		GPXRouteParamsBuilder routeParamsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (routeParamsBuilder != null) {
			gpxFile = routeParamsBuilder.getFile();
		}

		if (savedInstanceState != null) {
			selectingTrack = savedInstanceState.getBoolean(SELECTING_TRACK, gpxFile == null);
		} else {
			selectingTrack = gpxFile == null;
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			ImageButton closeButton = view.findViewById(R.id.close_button);
			buttonsShadow = view.findViewById(R.id.buttons_shadow);
			sortButton = view.findViewById(R.id.sort_button);
			closeButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
			closeButton.setOnClickListener(v -> dismiss());

			if (isPortrait()) {
				updateCardsLayout();
			}
			setupCards();
			setupButtons(view);
			setupSortButton(view);
			if (!isPortrait()) {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
			}
			enterTrackAppearanceMode();
			runLayoutListener();
		}
		return view;
	}

	public void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	public void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);
	}

	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			if (gpxFile == null || selectingTrack) {
				setupTracksCard();
			} else {
				sortButton.setVisibility(View.GONE);
				SelectedTrackToFollowCard selectedTrackToFollowCard =
						new SelectedTrackToFollowCard(mapActivity, this, gpxFile);
				getCardsContainer().addView(selectedTrackToFollowCard.build(mapActivity));
			}
		}
	}

	private void setupTracksCard() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			List<String> selectedTrackNames = GpxUiHelper.getSelectedTrackPaths(app);
			List<GPXInfo> list = GpxUiHelper.getSortedGPXFilesInfo(dir, selectedTrackNames, false);
			if (list.size() > 0) {
				String defaultCategory = app.getString(R.string.shared_string_all);
				tracksCard = new TracksToFollowCard(mapActivity, this, list, defaultCategory);
				tracksCard.setListener(this);
				getCardsContainer().addView(tracksCard.build(mapActivity));
				sortButton.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		MapRouteInfoMenu.followTrackVisible = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		MapRouteInfoMenu.followTrackVisible = false;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateStatusBarColor();
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateStatusBarColor();
	}

	@Override
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			QuadRect rect = mapActivity.getMapRouteInfoMenu().getRouteRect(mapActivity);

			if (gpxFile != null) {
				QuadRect gpxRect = gpxFile.getRect();

				rect.left = Math.min(rect.left, gpxRect.left);
				rect.right = Math.max(rect.right, gpxRect.right);
				rect.top = Math.max(rect.top, gpxRect.top);
				rect.bottom = Math.min(rect.bottom, gpxRect.bottom);
			}

			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(app);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (rect.left != 0 && rect.right != 0) {
				mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
						tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (!editingTrack) {
			exitTrackAppearanceMode();
		}
		onDismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SELECTING_TRACK, selectingTrack);
	}

	private void enterTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitTrackAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
		}
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (!nightMode) {
					AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), true);
				}
				return ColorUtilities.getDividerColorId(nightMode);
			} else if (!nightMode) {
				AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), false);
			}
		}
		return -1;
	}

	private void updateStatusBarColor() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.updateStatusBarColor();
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof ImportTrackCard) {
				importTrack();
			} else if (card instanceof AttachTrackToRoadsCard) {
				openPlanRoute(true);
			} else if (card instanceof TrackEditCard) {
				openPlanRoute(false);
			} else if (card instanceof SelectTrackCard) {
				SelectTrackTabsFragment.GpxFileSelectionListener gpxFileSelectionListener = gpxFile -> {
					selectTrackToFollow(gpxFile, true);
					updateSelectionMode(false);
				};
				SelectTrackTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), gpxFileSelectionListener);
			} else if (card instanceof ReverseTrackCard
					|| card instanceof NavigateTrackOptionsCard) {
				updateMenu();
			}
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof TracksToFollowCard) {
				if (buttonIndex >= 0) {
					loadAndFollowTrack((TracksToFollowCard) card, buttonIndex);
				}
			}
		}
	}

	private void loadAndFollowTrack(TracksToFollowCard card, int index) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && index < card.getGpxInfoList().size()) {
			GPXInfo gpxInfo = card.getGpxInfoList().get(index);
			String filePath = gpxInfo.getFilePath();
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
			if (selectedGpxFile != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				selectTrackToFollow(gpxFile, true);
				updateSelectionMode(gpxFile.getNonEmptySegmentsCount() > 1);
			} else {
				CallbackWithObject<GPXFile[]> callback = result -> {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						selectTrackToFollow(result[0], true);
						updateSelectionMode(result[0].getNonEmptySegmentsCount() != 1);
					}
					return true;
				};
				String fileName = gpxInfo.getFileName();
				File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
				GpxUiHelper.loadGPXFileInDifferentThread(mapActivity, callback, dir, null, fileName);
			}
		}
	}

	private void selectTrackToFollow(@NonNull GPXFile gpxFile, boolean showSelectionDialog) {
		this.gpxFile = gpxFile;

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().selectTrack(gpxFile, showSelectionDialog);
		}
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	private void updateSelectionMode(boolean selecting) {
		this.selectingTrack = selecting;
		setupCards();
	}

	public void importTrack() {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, ImportHelper.IMPORT_FILE_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ImportHelper.IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				importHelper.setGpxImportListener(new GpxImportListener() {
					@Override
					public void onSaveComplete(boolean success, GPXFile gpxFile) {
						if (success) {
							selectTrackToFollow(gpxFile, true);
							updateSelectionMode(false);
						} else {
							app.showShortToastMessage(app.getString(R.string.error_occurred_loading_gpx));
						}
						importHelper.setGpxImportListener(null);
					}
				});
				importHelper.handleGpxImport(uri, OnSuccessfulGpxImport.OPEN_PLAN_ROUTE_FRAGMENT, true);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void openPlanRoute(boolean showSnapWarning) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && MeasurementToolFragment.showSnapToRoadsDialog(mapActivity, showSnapWarning)) {
			editingTrack = true;
			close();
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (bottomContainer == null) {
				return;
			}
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackground(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int listBgColor = ColorUtilities.getListBgColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, listBgColor);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, listBgColor);
			}
		}
	}

	private void setupSortButton(View view) {
		ImageButton sortButton = view.findViewById(R.id.sort_button);
		int colorId = ColorUtilities.getInactiveButtonsAndLinksColorId(isNightMode());
		Drawable background = app.getUIUtilities().getIcon(R.drawable.bg_dash_line_dark, colorId);
		sortButton.setImageResource(sortByMode.getIconId());
		AndroidUtils.setBackground(sortButton, background);
		sortButton.setOnClickListener(v -> {
			List<PopUpMenuItem> items = new ArrayList<>();
			for (TracksSortByMode mode : TracksSortByMode.values()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(mode.getNameId())
						.setIcon(app.getUIUtilities().getThemedIcon(mode.getIconId()))
						.setOnClickListener(menuItem -> {
							sortByMode = mode;
							sortButton.setImageResource(mode.getIconId());
							if (tracksCard != null) {
								tracksCard.setSortByMode(mode);
							}
						})
						.setSelected(sortByMode == mode)
						.create()
				);
			}
			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = v;
			displayData.menuItems = items;
			displayData.nightMode = isNightMode();
			PopUpMenu.show(displayData);
		});
	}

	private void setupButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));

		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(v -> dismiss());
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_close);
	}

	private void close() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.remove(this)
					.commitAllowingStateLoss();
		}
	}

	private void onDismiss() {
		try {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && !editingTrack) {
				if (!mapActivity.isChangingConfigurations()) {
					mapActivity.getMapRouteInfoMenu().cancelSelectionFromTracks();
				}
				mapActivity.getMapLayers().getMapActionsHelper().showRouteInfoControlDialog();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	@Override
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onSegmentSelect(@NonNull GPXFile gpxFile, int selectedSegment) {
		app.getSettings().GPX_SEGMENT_INDEX.set(selectedSegment);
		selectTrackToFollow(gpxFile, false);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedSegment(selectedSegment);
			app.getRoutingHelper().onSettingsChanged(true);
		}
		updateSelectionMode(false);
	}


	@Override
	public void onRouteSelected(@NonNull GPXFile gpxFile, int selectedRoute) {
		app.getSettings().GPX_ROUTE_INDEX.set(selectedRoute);
		selectTrackToFollow(gpxFile, false);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedRoute(selectedRoute);
			app.getRoutingHelper().onSettingsChanged(true);
		}
		updateSelectionMode(false);
	}
}