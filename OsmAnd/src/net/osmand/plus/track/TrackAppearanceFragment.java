package net.osmand.plus.track;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.DirectionArrowsCard;
import net.osmand.plus.myplaces.SaveGpxAsyncTask;
import net.osmand.plus.myplaces.SplitTrackAsyncTask;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.track.TrackDrawInfo.TRACK_FILE_PATH;

public class TrackAppearanceFragment extends ContextMenuFragment {

	public static final String TAG = TrackAppearanceFragment.class.getName();

	private static final Log log = PlatformUtil.getLog(TrackAppearanceFragment.class);

	private OsmandApplication app;

	private GpxDataItem gpxDataItem;
	private TrackDrawInfo trackDrawInfo;
	private SelectedGpxFile selectedGpxFile;
	private List<GpxDisplayGroup> displayGroups;

	private int menuTitleHeight;
	private long modifiedTime = -1;

	@Override
	public int getMainLayoutId() {
		return R.layout.track_appearance;
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

	public TrackDrawInfo getTrackDrawInfo() {
		return trackDrawInfo;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();

		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			trackDrawInfo = new TrackDrawInfo();
			trackDrawInfo.readBundle(savedInstanceState);
			gpxDataItem = app.getGpxDbHelper().getItem(new File(trackDrawInfo.getFilePath()));
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(trackDrawInfo.getFilePath());
		} else if (arguments != null) {
			String gpxFilePath = arguments.getString(TRACK_FILE_PATH);
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			File file = new File(selectedGpxFile.getGpxFile().path);
			gpxDataItem = app.getGpxDbHelper().getItem(file);
			trackDrawInfo = new TrackDrawInfo(gpxDataItem);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			if (isPortrait()) {
				updateCardsLayout();
			}
			updateCards();
			updateButtons(view);
			if (!isPortrait()) {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
			}
			runLayoutListener();
		}
		return view;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		super.calculateLayout(view, initLayout);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackDrawInfo(trackDrawInfo);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getGpxLayer().setTrackDrawInfo(null);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		trackDrawInfo.saveToBundle(outState);
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
			}
		}
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.route_info_bg));
		View saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveTrackInfo();
				dismiss();
			}
		});

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				discardChanges();
				dismiss();
			}
		});

		UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(isNightMode(), saveButton, DialogButtonType.PRIMARY, R.string.shared_string_apply);

		AndroidUiHelper.updateVisibility(saveButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void saveTrackInfo() {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();

		gpxFile.setWidth(trackDrawInfo.getWidth());
		gpxFile.setGradientScaleType(trackDrawInfo.getGradientScaleType().name());
		gpxFile.setColor(trackDrawInfo.getColor());

		for (GpxSplitType gpxSplitType : GpxSplitType.values()) {
			if (gpxSplitType.getType() == trackDrawInfo.getSplitType()) {
				gpxFile.setSplitType(gpxSplitType.name());
				break;
			}
		}

		gpxFile.setSplitInterval(trackDrawInfo.getSplitInterval());
		gpxFile.setShowArrows(trackDrawInfo.isShowArrows());
		gpxFile.setShowStartFinish(trackDrawInfo.isShowStartFinish());

		app.getSelectedGpxHelper().updateSelectedGpxFile(selectedGpxFile);

		gpxDataItem = new GpxDataItem(new File(gpxFile.path), gpxFile);
		app.getGpxDbHelper().add(gpxDataItem);
		saveGpx(gpxFile);
	}

	private void discardChanges() {
		if (gpxDataItem.getSplitType() != trackDrawInfo.getSplitType() || gpxDataItem.getSplitInterval() != trackDrawInfo.getSplitInterval()) {
			int timeSplit = (int) gpxDataItem.getSplitInterval();
			double distanceSplit = gpxDataItem.getSplitInterval();

			GpxSplitType splitType = null;
			if (gpxDataItem.getSplitType() == GpxSplitType.DISTANCE.getType()) {
				splitType = GpxSplitType.DISTANCE;
			} else if (gpxDataItem.getSplitType() == GpxSplitType.TIME.getType()) {
				splitType = GpxSplitType.TIME;
			}
			if (splitType != null) {
				SplitTrackAsyncTask.SplitTrackListener splitTrackListener = new SplitTrackAsyncTask.SplitTrackListener() {

					@Override
					public void trackSplittingStarted() {

					}

					@Override
					public void trackSplittingFinished() {
						if (selectedGpxFile != null) {
							List<GpxDisplayGroup> groups = getGpxDisplayGroups();
							selectedGpxFile.setDisplayGroups(groups, app);
						}
					}
				};
				List<GpxDisplayGroup> groups = getGpxDisplayGroups();
				new SplitTrackAsyncTask(app, splitType, groups, splitTrackListener, trackDrawInfo.isJoinSegments(),
						timeSplit, distanceSplit).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}

	private void saveGpx(final GPXFile gpxFile) {
		new SaveGpxAsyncTask(gpxFile, new SaveGpxAsyncTask.SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished() {
				app.showShortToastMessage(R.string.shared_string_track_is_saved, Algorithms.getFileWithoutDirs(gpxFile.path));
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			BaseCard splitIntervalCard = new SplitIntervalCard(mapActivity, trackDrawInfo, this);
			cardsContainer.addView(splitIntervalCard.build(mapActivity));

			BaseCard arrowsCard = new DirectionArrowsCard(mapActivity, trackDrawInfo);
			cardsContainer.addView(arrowsCard.build(mapActivity));

			TrackColoringCard trackColoringCard = new TrackColoringCard(mapActivity, selectedGpxFile, trackDrawInfo);
			cardsContainer.addView(trackColoringCard.build(mapActivity));

			BaseCard width = new TrackWidthCard(mapActivity, trackDrawInfo);
			cardsContainer.addView(width.build(mapActivity));
		}
	}

	public List<GpxDisplayGroup> getGpxDisplayGroups() {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		if (gpxFile == null) {
			return new ArrayList<>();
		}
		if (gpxFile.modifiedTime != modifiedTime) {
			modifiedTime = gpxFile.modifiedTime;
			displayGroups = app.getSelectedGpxHelper().collectDisplayGroups(gpxFile);
			if (selectedGpxFile.getDisplayGroups(app) != null) {
				displayGroups = selectedGpxFile.getDisplayGroups(app);
			}
		}
		return displayGroups;
	}

	public void dismissImmediate() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, TrackAppearanceFragment fragment) {
		try {
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