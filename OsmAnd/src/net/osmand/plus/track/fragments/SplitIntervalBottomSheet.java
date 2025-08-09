package net.osmand.plus.track.fragments;

import static net.osmand.plus.utils.OsmAndFormatterParams.NO_TRAILING_ZEROS;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.configmap.tracks.appearance.ChangeAppearanceController;
import net.osmand.plus.configmap.tracks.appearance.DefaultAppearanceController;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SplitIntervalBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SplitIntervalBottomSheet.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(SplitIntervalBottomSheet.class);

	public static final String SELECTED_TRACK_SPLIT_TYPE = "selected_track_split_type";
	public static final String SELECTED_TIME_SPLIT_INTERVAL = "selected_time_split_interval";
	public static final String SELECTED_DISTANCE_SPLIT_INTERVAL = "selected_distance_split_interval";

	private SelectedGpxFile selectedGpxFile;
	private TrackDrawInfo trackDrawInfo;

	private final Map<String, Integer> timeSplitOptions = new LinkedHashMap<>();
	private final Map<String, Double> distanceSplitOptions = new LinkedHashMap<>();

	private int selectedTimeSplitInterval;
	private int selectedDistanceSplitInterval;
	private GpxSplitType selectedSplitType = GpxSplitType.NO_SPLIT;

	private Slider slider;
	private View sliderContainer;
	private TextView splitValueMin;
	private TextView splitValueMax;
	private TextView selectedSplitValue;
	private TextView splitIntervalNoneDescr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getTargetFragment() instanceof TrackAppearanceFragment fragment) {
			trackDrawInfo = fragment.getTrackDrawInfo();
			selectedGpxFile = fragment.getSelectedGpxFile();
		}
		prepareSplitIntervalOptions();

		if (savedInstanceState != null) {
			selectedTimeSplitInterval = savedInstanceState.getInt(SELECTED_TIME_SPLIT_INTERVAL);
			selectedDistanceSplitInterval = savedInstanceState.getInt(SELECTED_DISTANCE_SPLIT_INTERVAL);
			selectedSplitType = GpxSplitType.valueOf(savedInstanceState.getString(SELECTED_TRACK_SPLIT_TYPE));
		} else {
			updateSelectedSplitParams();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.gpx_split_interval)));

		View view = inflate(R.layout.track_split_interval);

		sliderContainer = view.findViewById(R.id.slider_container);
		slider = sliderContainer.findViewById(R.id.split_slider);

		splitValueMin = view.findViewById(R.id.split_value_min);
		splitValueMax = view.findViewById(R.id.split_value_max);
		selectedSplitValue = view.findViewById(R.id.split_value_tv);
		splitIntervalNoneDescr = view.findViewById(R.id.split_interval_none_descr);

		UiUtilities.setupSlider(slider, nightMode, null, true);

		LinearLayout radioGroup = view.findViewById(R.id.custom_radio_buttons);
		setupTypeRadioGroup(radioGroup);

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setupTypeRadioGroup(LinearLayout buttonsContainer) {
		TextRadioItem none = createRadioButton(GpxSplitType.NO_SPLIT, R.string.shared_string_none);
		TextRadioItem time = createRadioButton(GpxSplitType.TIME, R.string.shared_string_time);
		TextRadioItem distance = createRadioButton(GpxSplitType.DISTANCE, R.string.distance);
		TextRadioItem uphillDownhill = createRadioButton(GpxSplitType.UPHILL_DOWNHILL, R.string.uphill_downhill_split);

		time.setEnabled(selectedGpxFile == null || selectedGpxFile.getTrackAnalysisToDisplay(app).getTimeSpan() > 0);

		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(none, time, distance, uphillDownhill);

		if (selectedSplitType == GpxSplitType.NO_SPLIT) {
			radioGroup.setSelectedItem(none);
		} else if (selectedSplitType == GpxSplitType.TIME) {
			radioGroup.setSelectedItem(time);
		} else if (selectedSplitType == GpxSplitType.DISTANCE) {
			radioGroup.setSelectedItem(distance);
		} else if (selectedSplitType == GpxSplitType.UPHILL_DOWNHILL) {
			radioGroup.setSelectedItem(uphillDownhill);
		}
	}

	@NonNull
	private TextRadioItem createRadioButton(GpxSplitType splitType, int titleId) {
		String title = app.getString(titleId);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener((radioItem, view) -> {
			selectedSplitType = splitType;
			updateSlider();
			return true;
		});
		return item;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_TIME_SPLIT_INTERVAL, selectedTimeSplitInterval);
		outState.putInt(SELECTED_DISTANCE_SPLIT_INTERVAL, selectedDistanceSplitInterval);
		outState.putString(SELECTED_TRACK_SPLIT_TYPE, selectedSplitType.name());
	}

	private void updateSelectedSplitParams() {
		if (trackDrawInfo != null) {
			if (trackDrawInfo.getSplitType() == GpxSplitType.DISTANCE.getType()) {
				selectedSplitType = GpxSplitType.DISTANCE;
				List<Double> splitOptions = new ArrayList<>(distanceSplitOptions.values());
				int index = splitOptions.indexOf(trackDrawInfo.getSplitInterval());
				selectedDistanceSplitInterval = Math.max(index, 0);
			} else if (trackDrawInfo.getSplitType() == GpxSplitType.TIME.getType()) {
				selectedSplitType = GpxSplitType.TIME;
				List<Integer> splitOptions = new ArrayList<>(timeSplitOptions.values());
				int index = splitOptions.indexOf((int) trackDrawInfo.getSplitInterval());
				selectedTimeSplitInterval = Math.max(index, 0);
			} else if (trackDrawInfo.getSplitType() == GpxSplitType.UPHILL_DOWNHILL.getType()) {
				selectedSplitType = GpxSplitType.UPHILL_DOWNHILL;
				selectedTimeSplitInterval = 0;
			}
		}
	}

	private void prepareSplitIntervalOptions() {
		addDistanceOptionSplit(30); // 50 feet, 20 yards, 20 m
		addDistanceOptionSplit(60); // 100 feet, 50 yards, 50 m
		addDistanceOptionSplit(150); // 200 feet, 100 yards, 100 m
		addDistanceOptionSplit(300); // 500 feet, 200 yards, 200 m
		addDistanceOptionSplit(600); // 1000 feet, 500 yards, 500 m
		addDistanceOptionSplit(1500); // 2000 feet, 1000 yards, 1 km
		addDistanceOptionSplit(3000); // 1 mi, 2 km
		addDistanceOptionSplit(6000); // 2 mi, 5 km
		addDistanceOptionSplit(15000); // 5 mi, 10 km

		addTimeOptionSplit(15);
		addTimeOptionSplit(30);
		addTimeOptionSplit(60);
		addTimeOptionSplit(120);
		addTimeOptionSplit(150);
		addTimeOptionSplit(300);
		addTimeOptionSplit(600);
		addTimeOptionSplit(900);
		addTimeOptionSplit(1800);
		addTimeOptionSplit(3600);
	}

	private void addDistanceOptionSplit(int value) {
		double roundedDist = OsmAndFormatter.calculateRoundedDist(value, app);
		String formattedDist = OsmAndFormatter.getFormattedDistanceInterval(app, value, NO_TRAILING_ZEROS);
		distanceSplitOptions.put(formattedDist, roundedDist);
	}

	private void addTimeOptionSplit(int value) {
		String time = OsmAndFormatter.getFormattedTimeInterval(app, value);
		timeSplitOptions.put(time, value);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateSlider();
	}

	private void updateSlider() {
		if (selectedSplitType != GpxSplitType.NO_SPLIT && selectedSplitType != GpxSplitType.UPHILL_DOWNHILL) {
			slider.clearOnChangeListeners();
			if (selectedSplitType == GpxSplitType.TIME) {
				updateSliderTimeInterval();
			} else {
				updateSliderDistanceInterval();
			}
			AndroidUiHelper.updateVisibility(sliderContainer, true);
			AndroidUiHelper.updateVisibility(splitIntervalNoneDescr, false);
		} else {
			AndroidUiHelper.updateVisibility(sliderContainer, false);
			AndroidUiHelper.updateVisibility(splitIntervalNoneDescr, true);
		}
		setupHeightAndBackground(getView());
	}

	private void updateSliderTimeInterval() {
		List<String> splitOptions = new ArrayList<>(timeSplitOptions.keySet());
		updateSliderMinMaxValues(splitOptions);

		slider.setValue(selectedTimeSplitInterval);
		slider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				selectedTimeSplitInterval = (int) value;
				selectedSplitValue.setText(splitOptions.get(selectedTimeSplitInterval));
			}
		});
		selectedSplitValue.setText(splitOptions.get(selectedTimeSplitInterval));
	}

	private void updateSliderDistanceInterval() {
		List<String> splitOptions = new ArrayList<>(distanceSplitOptions.keySet());
		updateSliderMinMaxValues(splitOptions);

		slider.setValue(selectedDistanceSplitInterval);
		slider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				selectedDistanceSplitInterval = (int) value;
				selectedSplitValue.setText(splitOptions.get(selectedDistanceSplitInterval));
			}
		});
		selectedSplitValue.setText(splitOptions.get(selectedDistanceSplitInterval));
	}

	private void updateSliderMinMaxValues(List<String> splitOptions) {
		int valueFrom = 0;
		int valueTo = splitOptions.size() - 1;

		slider.setValueTo(valueTo);
		slider.setValueFrom(valueFrom);
		splitValueMin.setText(splitOptions.get(valueFrom));
		splitValueMax.setText(splitOptions.get(valueTo));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		updateSplit();
		applySelectedSplit();
		dismiss();
	}

	private void updateSplit() {
		if (trackDrawInfo != null) {
			double splitInterval = 0;
			if (selectedSplitType == GpxSplitType.NO_SPLIT || selectedSplitType == GpxSplitType.UPHILL_DOWNHILL) {
				splitInterval = 0;
			} else if (selectedSplitType == GpxSplitType.DISTANCE) {
				splitInterval = new ArrayList<>(distanceSplitOptions.values()).get(selectedDistanceSplitInterval);
			} else if (selectedSplitType == GpxSplitType.TIME) {
				splitInterval = new ArrayList<>(timeSplitOptions.values()).get(selectedTimeSplitInterval);
			}
			trackDrawInfo.setSplitType(selectedSplitType.getType());
			trackDrawInfo.setSplitInterval(splitInterval);
		}
	}

	private void applySelectedSplit() {
		int timeSplit = new ArrayList<>(timeSplitOptions.values()).get(selectedTimeSplitInterval);
		double distanceSplit = new ArrayList<>(distanceSplitOptions.values()).get(selectedDistanceSplitInterval);
		double splitInterval = GpxSplitType.DISTANCE == selectedSplitType ? distanceSplit : timeSplit;

		Fragment target = getTargetFragment();
		if (target instanceof TrackAppearanceFragment) {
			((TrackAppearanceFragment) target).applySplit(selectedSplitType, timeSplit, distanceSplit);
		}
		DialogManager dialogManager = app.getDialogManager();
		ChangeAppearanceController changeAppearanceController = (ChangeAppearanceController) dialogManager.findController(ChangeAppearanceController.PROCESS_ID);
		if (changeAppearanceController != null) {
			changeAppearanceController.getSplitCardController().onSplitSelected(selectedSplitType.getType(), splitInterval);
		}
		DefaultAppearanceController defaultAppearanceController = (DefaultAppearanceController) dialogManager.findController(DefaultAppearanceController.PROCESS_ID);
		if (defaultAppearanceController != null) {
			defaultAppearanceController.getSplitCardController().onSplitSelected(selectedSplitType.getType(), splitInterval);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SplitIntervalBottomSheet fragment = new SplitIntervalBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}