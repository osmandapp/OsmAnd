package net.osmand.plus.track;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.myplaces.GPXTabItemType;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SplitSegmentDialogFragment;
import net.osmand.plus.myplaces.TrackActivityFragmentAdapter;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.widgets.IconPopupMenu;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

import static net.osmand.plus.activities.TrackActivity.CURRENT_RECORDING;
import static net.osmand.plus.activities.TrackActivity.TRACK_FILE_NAME;

public class TrackMenuFragment extends ContextMenuScrollFragment implements CardListener, SegmentActionsListener {

	public static final String TAG = TrackMenuFragment.class.getName();
	private static final Log log = PlatformUtil.getLog(TrackMenuFragment.class);

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;

	private GpxDataItem gpxDataItem;
	private SelectedGpxFile selectedGpxFile;

	private View routeMenuTopShadowAll;
	private BottomNavigationView bottomNav;
	private TrackMenuType menuType = TrackMenuType.TRACK;
	private SegmentsCard segmentsCard;

	private int menuTitleHeight;

	public enum TrackMenuType {
		TRACK(R.id.action_track, R.string.shared_string_gpx_tracks),
		POINTS(R.id.action_points, R.string.shared_string_gpx_points);

		TrackMenuType(@DrawableRes int iconId, @StringRes int titleId) {
			this.iconId = iconId;
			this.titleId = titleId;
		}

		public final int iconId;
		public final int titleId;
	}

	@Override
	public int getMainLayoutId() {
		return R.layout.track_menu;
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
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	public TrackDisplayHelper getDisplayHelper() {
		return displayHelper;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		displayHelper = new TrackDisplayHelper(app);

		String gpxFilePath = "";
		boolean currentRecording = false;
		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			gpxFilePath = savedInstanceState.getString(TRACK_FILE_NAME);
			currentRecording = savedInstanceState.getBoolean(CURRENT_RECORDING, false);
		} else if (arguments != null) {
			gpxFilePath = arguments.getString(TRACK_FILE_NAME);
			currentRecording = arguments.getBoolean(CURRENT_RECORDING, false);
		}
		if (currentRecording) {
			selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		} else {
			File file = new File(gpxFilePath);
			displayHelper.setFile(file);
			gpxDataItem = gpxDbHelper.getItem(file);
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
		}
		displayHelper.setGpxDataItem(gpxDataItem);
		displayHelper.setGpx(selectedGpxFile.getGpxFile());
	}

	public GPXFile getGpx() {
		return displayHelper.getGpx();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			bottomNav = view.findViewById(R.id.bottom_navigation);
			routeMenuTopShadowAll = view.findViewById(R.id.route_menu_top_shadow_all);
			TextView title = view.findViewById(R.id.title);
			String fileName = Algorithms.getFileWithoutDirs(getGpx().path);
			title.setText(GpxUiHelper.getGpxTitle(fileName));

			if (isPortrait()) {
				updateCardsLayout();
			}
			setupCards();
			setupButtons(view);
			enterTrackAppearanceMode();
			runLayoutListener();
		}
		return view;
	}

	private void setupCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();
			if (menuType == TrackMenuType.TRACK) {
				if (segmentsCard != null) {
					cardsContainer.addView(segmentsCard.getView());
				} else {
					segmentsCard = new SegmentsCard(mapActivity, displayHelper, this);
					cardsContainer.addView(segmentsCard.build(mapActivity));
				}
			}
		}
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = routeMenuTopShadowAll.getHeight()
				+ bottomNav.getHeight();
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
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY || menuState == MenuState.HALF_SCREEN;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adjustMapPosition(getHeight());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitTrackAppearanceMode();
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(TRACK_FILE_NAME, getGpx().path);
		outState.putBoolean(CURRENT_RECORDING, selectedGpxFile.isShowCurrentTrack());
		super.onSaveInstanceState(outState);
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
				return nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
			} else {
				if (Build.VERSION.SDK_INT >= 23 && !nightMode) {
					view.setSystemUiVisibility(view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
				}
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
	protected String getThemeInfoProviderTag() {
		return TAG;
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {

	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {

	}

	@Override
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	@Override
	protected void onHeaderClick() {
		adjustMapPosition(getViewY());
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && mapActivity.getMapView() != null) {
			GPXFile gpxFile = getGpx();
			QuadRect r = gpxFile.getRect();

			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(mapActivity);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (r.left != 0 && r.right != 0) {
				mapActivity.getMapView().fitRectToMap(r.left, r.right, r.top, r.bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
			}
		}
	}

	private void setupButtons(View view) {
		ColorStateList navColorStateList = AndroidUtils.createBottomNavColorStateList(getContext(), isNightMode());
		BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation);
		bottomNav.setItemIconTintList(navColorStateList);
		bottomNav.setItemTextColor(navColorStateList);
		bottomNav.setSelectedItemId(R.id.action_track);
		bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				for (TrackMenuType type : TrackMenuType.values()) {
					if (type.iconId == item.getItemId()) {
						menuType = type;
						setupCards();
						break;
					}
				}
				return true;
			}
		});
	}

	@Override
	public void updateContent() {
		if (segmentsCard != null) {
			segmentsCard.updateContent();
		}
		setupCards();
	}

	@Override
	public void onChartTouch() {
		getBottomScrollView().requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void scrollBy(int px) {

	}

	@Override
	public void onPointSelected(double lat, double lon) {

	}

	@Override
	public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			SplitSegmentDialogFragment.showInstance(fragmentManager, displayHelper, gpxItem, trkSegment);
		}
	}

	@Override
	public void openAnalyzeOnMap(GpxDisplayItem gpxItem, List<ILineDataSet> dataSets, GPXTabItemType tabType) {
		WptPt wpt = null;
		gpxItem.chartTypes = null;
		if (dataSets != null && dataSets.size() > 0) {
			gpxItem.chartTypes = new GPXDataSetType[dataSets.size()];
			for (int i = 0; i < dataSets.size(); i++) {
				OrderedLineDataSet orderedDataSet = (OrderedLineDataSet) dataSets.get(i);
				gpxItem.chartTypes[i] = orderedDataSet.getDataSetType();
			}
			if (gpxItem.chartHighlightPos != -1) {
				TrkSegment segment = null;
				for (Track t : gpxItem.group.getGpx().tracks) {
					for (TrkSegment s : t.segments) {
						if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
							segment = s;
							break;
						}
					}
					if (segment != null) {
						break;
					}
				}
				if (segment != null) {
					OrderedLineDataSet dataSet = (OrderedLineDataSet) dataSets.get(0);
					float distance = gpxItem.chartHighlightPos * dataSet.getDivX();
					for (WptPt p : segment.points) {
						if (p.distance >= distance) {
							wpt = p;
							break;
						}
					}
				}
			}
		}
		if (wpt != null) {
			gpxItem.locationOnMap = wpt;
		} else {
			gpxItem.locationOnMap = gpxItem.locationStart;
		}

		TrackDetailsMenu trackDetailsMenu = getMapActivity().getTrackDetailsMenu();
		trackDetailsMenu.setGpxItem(gpxItem);
		trackDetailsMenu.show();
		close();
	}

	@Override
	public void showOptionsPopupMenu(View view, final TrkSegment segment, final boolean confirmDeletion) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			IconPopupMenu optionsPopupMenu = new IconPopupMenu(activity, view.findViewById(R.id.overflow_menu));
			Menu menu = optionsPopupMenu.getMenu();
			optionsPopupMenu.getMenuInflater().inflate(R.menu.track_segment_menu, menu);
			menu.findItem(R.id.action_edit).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_edit_dark));
			menu.findItem(R.id.action_delete).setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			optionsPopupMenu.setOnMenuItemClickListener(new IconPopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					int i = item.getItemId();
					if (i == R.id.action_edit) {
						editSegment(segment);
						return true;
					} else if (i == R.id.action_delete) {
						FragmentActivity activity = getActivity();
						if (!confirmDeletion) {
							deleteAndSaveSegment(segment);
						} else if (activity != null) {
							AlertDialog.Builder builder = new AlertDialog.Builder(activity);
							builder.setMessage(R.string.recording_delete_confirm);
							builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									deleteAndSaveSegment(segment);
								}
							});
							builder.setNegativeButton(R.string.shared_string_cancel, null);
							builder.show();
						}
						return true;
					}
					return false;
				}
			});
			optionsPopupMenu.show();
		}
	}

	private void editSegment(TrkSegment segment) {
		GPXFile gpxFile = getGpx();
		openPlanRoute(new GpxData(gpxFile));
		close();
	}

	public void openPlanRoute(GpxData gpxData) {
		QuadRect qr = gpxData.getRect();
		getMapActivity().getMapView().fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
		MeasurementEditingContext editingContext = new MeasurementEditingContext();
		editingContext.setGpxData(gpxData);
		MeasurementToolFragment.showInstance(getFragmentManager(), editingContext);
	}

	private void deleteAndSaveSegment(TrkSegment segment) {
		if (deleteSegment(segment)) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				boolean showOnMap = TrackActivityFragmentAdapter.isGpxFileSelected(app, gpx);
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpx, showOnMap, false);
				saveGpx(showOnMap ? selectedGpxFile : null, gpx);
			}
		}
	}

	private boolean deleteSegment(TrkSegment segment) {
		if (segment != null) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				return gpx.removeTrkSegment(segment);
			}
		}
		return false;
	}

	private void saveGpx(final SelectedGpxFile selectedGpxFile, GPXFile gpxFile) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (selectedGpxFile != null) {
					List<GpxDisplayGroup> groups = displayHelper.getDisplayGroups(new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT});
					if (groups != null) {
						selectedGpxFile.setDisplayGroups(groups, app);
						selectedGpxFile.processPoints(app);
					}
				}
				updateContent();
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void close() {
		try {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss();
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, String path, boolean showCurrentTrack) {
		try {
			Bundle args = new Bundle();
			args.putString(TRACK_FILE_NAME, path);
			args.putBoolean(CURRENT_RECORDING, showCurrentTrack);
			args.putInt(ContextMenuFragment.MENU_STATE_KEY, MenuState.HALF_SCREEN);

			TrackMenuFragment fragment = new TrackMenuFragment();
			fragment.setArguments(args);

			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, fragment.getFragmentTag())
					.addToBackStack(fragment.getFragmentTag())
					.commitAllowingStateLoss();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}