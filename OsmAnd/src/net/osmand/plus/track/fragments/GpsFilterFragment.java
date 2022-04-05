package net.osmand.plus.track.fragments;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.FilteredSelectedGpxFile;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.GpsFilterHelper.GpsFilterListener;
import net.osmand.plus.measurementtool.SaveAsNewTrackBottomSheetDialogFragment.SaveAsNewTrackFragmentListener;
import net.osmand.plus.measurementtool.SavedTrackBottomSheetDialogFragment;
import net.osmand.plus.track.GpsFilterScreensAdapter;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.cards.GpsFilterBaseCard.SaveIntoFileListener;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;

public class GpsFilterFragment extends ContextMenuScrollFragment implements SaveAsNewTrackFragmentListener,
		SaveIntoFileListener, GpsFilterListener {

	public static final String TAG = GpsFilterFragment.class.getName();

	private static final Log LOG = PlatformUtil.getLog(GpsFilterFragment.class);

	private static final String KEY_GPX_FILE_PATH = "gpx_file_path";
	private static final String KEY_SAVED_GPX_FILE_PATH = "saved_gpx_file_path";

	private OsmandApplication app;
	private GpsFilterHelper gpsFilterHelper;
	private SelectedGpxFile selectedGpxFile;

	private int toolbarHeight;
	private int menuTitleHeight;

	private View routeMenuTopShadowAll;
	private View view;
	private GpsFilterScreensAdapter gpsFilterScreensAdapter;

	private String savedGpxFilePath;

	@Override
	public int getMainLayoutId() {
		return R.layout.gps_filter_fragment;
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
		return isPortrait() ? toolbarHeight : 0;
	}

	@Override
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
		app = requireMyApplication();
		gpsFilterHelper = app.getGpsFilterHelper();
		toolbarHeight = getResources().getDimensionPixelSize(R.dimen.toolbar_height);

		if (savedInstanceState != null) {
			if (selectedGpxFile == null) {
				restoreSelectedGpxFile(savedInstanceState.getString(KEY_GPX_FILE_PATH));
			}
			if (Algorithms.isEmpty(savedGpxFilePath)) {
				savedGpxFilePath = savedInstanceState.getString(KEY_SAVED_GPX_FILE_PATH);
			}
		}

		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss(false);
			}
		});
	}

	private void restoreSelectedGpxFile(String gpxFilePath) {
		if (!Algorithms.isEmpty(gpxFilePath)) {
			TrackMenuFragment.loadSelectedGpxFile(requireMapActivity(), gpxFilePath, false, (gpxFile) -> {
				selectedGpxFile = gpxFile;
				FilteredSelectedGpxFile filteredSelectedGpxFile = setFileToFilter(selectedGpxFile);
				if (view != null && filteredSelectedGpxFile != null) {
					initContent(filteredSelectedGpxFile);
				}
				return true;
			});
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);

			if (isPortrait()) {
				updateCardsLayout();
			}
			if (selectedGpxFile != null) {
				FilteredSelectedGpxFile filteredSelectedGpxFile = setFileToFilter(selectedGpxFile);
				if (filteredSelectedGpxFile != null) {
					initContent(filteredSelectedGpxFile);
				}
			}
		}
		return view;
	}

	@Nullable
	private FilteredSelectedGpxFile setFileToFilter(@NonNull SelectedGpxFile selectedGpxFile) {
		FilteredSelectedGpxFile filteredSelectedGpxFile = selectedGpxFile.getFilteredSelectedGpxFile();
		if (app != null && selectedGpxFile.getFilteredSelectedGpxFile() == null) {
			filteredSelectedGpxFile = selectedGpxFile.createFilteredSelectedGpxFile(app, null);
		}
		return filteredSelectedGpxFile;
	}

	private void initContent(@NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		updateStatusBarColor();
		setupToolbar();
		setupTabsAndPager(filteredSelectedGpxFile);
		enterGpsFilterMode();
		runLayoutListener();
	}

	private void setupToolbar() {
		View toolbar = view.findViewById(R.id.toolbar);
		AndroidUiHelper.updateVisibility(toolbar, isPortrait());

		ImageButton closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss(false));
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(toolbar.getContext()));

		View resetToOriginalButton = toolbar.findViewById(R.id.reset_to_original_button);
		resetToOriginalButton.setOnClickListener(v -> {
			FilteredSelectedGpxFile filteredSelectedGpxFile = selectedGpxFile.getFilteredSelectedGpxFile();
			if (filteredSelectedGpxFile != null && app != null) {
				filteredSelectedGpxFile.resetFilters(app);
				gpsFilterScreensAdapter.onResetFilters();
			}
		});

		View scrollToActionsButton = toolbar.findViewById(R.id.scroll_to_actions_button);
		scrollToActionsButton.setOnClickListener(v -> {
			if (gpsFilterScreensAdapter != null) {
				gpsFilterScreensAdapter.softScrollToActionsCard();
			}
		});
	}

	private void setupTabsAndPager(@NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		PagerSlidingTabStrip tabLayout = view.findViewById(R.id.sliding_tabs);
		WrapContentHeightViewPager pager = view.findViewById(R.id.pager);

		gpsFilterScreensAdapter = new GpsFilterScreensAdapter(requireMapActivity(), this,
				filteredSelectedGpxFile, isNightMode());

		pager.setAdapter(gpsFilterScreensAdapter);
		pager.setOffscreenPageLimit(1);
		pager.setSwipeable(true);

		tabLayout.setTabBackground(0);
		tabLayout.setShouldExpand(true);
		tabLayout.setIndicatorHeight(dpToPx(2));
		tabLayout.setIndicatorBgColor(Color.TRANSPARENT);
		tabLayout.setViewPager(pager);
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = routeMenuTopShadowAll.getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected void setupControlButtons(@NonNull View view) {
		if (isPortrait()) {
			super.setupControlButtons(view);
		} else {
			View mapHudControls = view.findViewById(R.id.map_hud_controls);
			AndroidUiHelper.updateVisibility(mapHudControls, false);
			setupMapRulerWidget(view, requireMapActivity().getMapLayers());
		}
		setupCenterOnTrackButton(view.findViewById(R.id.map_center_on_track));
	}

	private void setupCenterOnTrackButton(@NonNull ImageButton centerOnTrackButton) {
		int backgroundId = isNightMode() ? R.drawable.btn_circle_night : R.drawable.btn_circle;
		centerOnTrackButton.setBackgroundResource(backgroundId);

		int iconColorId = ColorUtilities.getDefaultIconColorId(isNightMode());
		Drawable centerOnTrackIcon = getIcon(R.drawable.ic_action_center_on_track, iconColorId);
		Drawable directedIcon = AndroidUtils.getDrawableForDirection(requireContext(), centerOnTrackIcon);
		centerOnTrackButton.setImageDrawable(directedIcon);

		centerOnTrackButton.setOnClickListener(v -> adjustMapPosition(getViewY()));
		AndroidUiHelper.updateVisibility(centerOnTrackButton, true);
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY
				|| menuState == MenuState.HALF_SCREEN
				|| !isPortrait();
	}

	@Override
	public void updateMapControlsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		if (isPortrait()) {
			super.updateMapControlsPos(fragment, y, animated);
		} else {
			View mainView = getMainView();
			View mapBottomHudButtons = getMapBottomHudButtons();
			if (mainView != null && mapBottomHudButtons != null) {
				int bottomPadding = getResources().getDimensionPixelSize(R.dimen.map_button_margin);
				AndroidUtils.setPadding(mapBottomHudButtons, mainView.getWidth(), 0, 0, bottomPadding);
			}
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adjustMapPosition(getHeight());
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getGpsFilterHelper().addListener(this);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_GPX_FILE_PATH, selectedGpxFile.getGpxFile().path);
		outState.putString(KEY_SAVED_GPX_FILE_PATH, savedGpxFilePath);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitGpsFilterMode();
	}

	private void enterGpsFilterMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitGpsFilterMode() {
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
		if (view != null && !isDismissing()) {
			boolean nightMode = isNightMode();
			if (!nightMode) {
				view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
			return ColorUtilities.getStatusBarColorId(nightMode);
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
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust,
	                        int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			GPXFile gpxFile = selectedGpxFile.getGpxFileToDisplay();
			QuadRect r = gpxFile.getRect();

			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;
			int marginStartPx = 0;
			int marginTopPx = 0;

			if (isPortrait()) {
				int contextMenuHeight = getViewHeight() - y;
				int invisibleMapHeight = contextMenuHeight + getToolbarHeight();
				tileBoxHeightPx = tb.getPixHeight() - invisibleMapHeight;
				marginTopPx = getToolbarHeight() + AndroidUtils.getStatusBarHeight(mapActivity);
			} else {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
				marginStartPx = getWidth();
			}
			if (r.left != 0 && r.right != 0) {
				mapActivity.getMapView().fitRectToMap(r.left, r.right, r.top, r.bottom,
						tileBoxWidthPx, tileBoxHeightPx, marginTopPx, marginStartPx);
			}
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
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(),
						R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int listBgColor = ColorUtilities.getListBgColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, listBgColor);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, listBgColor);
			}
		}
	}

	@Override
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onSaveAsNewTrack(@Nullable String folderName, @NonNull String fileName,
	                             boolean showOnMap, boolean simplifiedTrack) {
		if (app != null && selectedGpxFile.getFilteredSelectedGpxFile() != null) {
			File destFile = app.getAppPath(GPX_INDEX_DIR);
			if (!Algorithms.isEmpty(folderName) && !destFile.getName().equals(folderName)) {
				destFile = new File(destFile, folderName);
			}
			destFile = new File(destFile, fileName + GPX_FILE_EXT);

			GPXFile filteredGpxFile = selectedGpxFile.getFilteredSelectedGpxFile().getGpxFile();
			GPXFile gpxFileToWrite = GpsFilterHelper.copyGpxFile(app, filteredGpxFile);
			gpxFileToWrite.path = destFile.getAbsolutePath();

			new SaveGpxAsyncTask(destFile, gpxFileToWrite, new SaveGpxListener() {

				@Override
				public void gpxSavingStarted() {
				}

				@Override
				public void gpxSavingFinished(Exception errorMessage) {
					onGpxSavingFinished(gpxFileToWrite, errorMessage, showOnMap);
				}
			}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void onGpxSavingFinished(@NonNull GPXFile gpxFile, @Nullable Exception error, boolean showOnMap) {
		MapActivity mapActivity = getMapActivity();
		if (error != null) {
			LOG.error(error);
		} else if (mapActivity != null) {
			app.getSelectedGpxHelper().selectGpxFile(gpxFile, showOnMap, false);

			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			SavedTrackBottomSheetDialogFragment.showInstance(fragmentManager, gpxFile.path, false);
		}

		dismiss(true);
	}

	private void dismiss(boolean savedCopy) {
		dismiss();

		boolean isGpxFileExist = new File(selectedGpxFile.getGpxFile().path).exists();
		if (app != null && !isGpxFileExist) {
			app.getSelectedGpxHelper().selectGpxFile(selectedGpxFile.getGpxFile(), false, false);
		}
		gpsFilterHelper.clearListeners();

		Fragment target = getTargetFragment();
		if (target instanceof GpsFilterFragmentLister) {
			((GpsFilterFragmentLister) target)
					.onDismissGpsFilterFragment(savedCopy, savedGpxFilePath);
		}
	}

	@Override
	public void onSavedIntoFile(@NonNull String filePath) {
		savedGpxFilePath = filePath;
	}

	@Override
	public void onFinishFiltering(@NonNull GPXFile filteredGpxFile) {
		gpsFilterScreensAdapter.onFinishFiltering();
		Fragment target = getTargetFragment();
		if (target instanceof GpsFilterFragmentLister) {
			((GpsFilterFragmentLister) target).onFinishFiltering(filteredGpxFile);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull SelectedGpxFile selectedGpxFile,
	                                   @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			GpsFilterFragment fragment = new GpsFilterFragment();
			fragment.setRetainInstance(true);
			fragment.selectedGpxFile = selectedGpxFile;
			fragment.setTargetFragment(target, 0);

			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	public interface GpsFilterFragmentLister {

		void onFinishFiltering(@NonNull GPXFile filteredGpxFile);

		void onDismissGpsFilterFragment(boolean savedCopy, @Nullable String savedFilePath);
	}
}