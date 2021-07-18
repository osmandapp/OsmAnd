package net.osmand.plus.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.activities.TrackActivity.TRACK_FILE_NAME;

public class SplitIntervalBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SplitIntervalBottomSheet.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(SplitIntervalBottomSheet.class);

	public static final String SELECTED_TRACK_SPLIT_TYPE = "selected_track_split_type";
	public static final String SELECTED_TIME_SPLIT_INTERVAL = "selected_time_split_interval";
	public static final String SELECTED_DISTANCE_SPLIT_INTERVAL = "selected_distance_split_interval";

	private OsmandApplication app;
	private SelectedGpxFile selectedGpxFile;
	private TrackDrawInfo trackDrawInfo;

	private Map<String, Integer> timeSplitOptions = new LinkedHashMap<>();
	private Map<String, Double> distanceSplitOptions = new LinkedHashMap<>();

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
		app = requiredMyApplication();

		Fragment target = getTargetFragment();
		if (target instanceof TrackAppearanceFragment) {
			trackDrawInfo = ((TrackAppearanceFragment) target).getTrackDrawInfo();
		}
		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			String gpxFilePath = savedInstanceState.getString(TRACK_FILE_NAME);
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			prepareSplitIntervalOptions();

			selectedTimeSplitInterval = savedInstanceState.getInt(SELECTED_TIME_SPLIT_INTERVAL);
			selectedDistanceSplitInterval = savedInstanceState.getInt(SELECTED_DISTANCE_SPLIT_INTERVAL);
			selectedSplitType = GpxSplitType.valueOf(savedInstanceState.getString(SELECTED_TRACK_SPLIT_TYPE));
		} else if (arguments != null) {
			String gpxFilePath = arguments.getString(TRACK_FILE_NAME);
			selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFilePath);
			prepareSplitIntervalOptions();
			updateSelectedSplitParams();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.gpx_split_interval)));

		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.track_split_interval, null);

		sliderContainer = view.findViewById(R.id.slider_container);
		slider = sliderContainer.findViewById(R.id.split_slider);

		splitValueMin = view.findViewById(R.id.split_value_min);
		splitValueMax = view.findViewById(R.id.split_value_max);
		selectedSplitValue = view.findViewById(R.id.split_value_tv);
		splitIntervalNoneDescr = view.findViewById(R.id.split_interval_none_descr);

		UiUtilities.setupSlider(slider, nightMode, null, true);

		LinearLayout radioGroup = (LinearLayout) view.findViewById(R.id.custom_radio_buttons);
		setupTypeRadioGroup(radioGroup);

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(titleItem);
	}

	private void setupTypeRadioGroup(LinearLayout buttonsContainer) {
		TextRadioItem none = createRadioButton(GpxSplitType.NO_SPLIT, R.string.shared_string_none);
		TextRadioItem time = createRadioButton(GpxSplitType.TIME, R.string.shared_string_time);
		TextRadioItem distance = createRadioButton(GpxSplitType.DISTANCE, R.string.distance);

		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(none, time, distance);

		if (selectedSplitType == GpxSplitType.NO_SPLIT) {
			radioGroup.setSelectedItem(none);
		} else {
			radioGroup.setSelectedItem(selectedSplitType == GpxSplitType.TIME ? time : distance);
		}
	}

	private TextRadioItem createRadioButton(final GpxSplitType splitType, int titleId) {
		String title = app.getString(titleId);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				selectedSplitType = splitType;
				updateSlider();
				return true;
			}
		});
		return item;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_TIME_SPLIT_INTERVAL, selectedTimeSplitInterval);
		outState.putInt(SELECTED_DISTANCE_SPLIT_INTERVAL, selectedDistanceSplitInterval);
		outState.putString(SELECTED_TRACK_SPLIT_TYPE, selectedSplitType.name());
		outState.putString(TRACK_FILE_NAME, selectedGpxFile.getGpxFile().path);
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
			}
		}
	}

	private void prepareSplitIntervalOptions() {
		List<GpxDisplayGroup> groups = getDisplayGroups();
		addDistanceOptionSplit(30, groups); // 50 feet, 20 yards, 20 m
		addDistanceOptionSplit(60, groups); // 100 feet, 50 yards, 50 m
		addDistanceOptionSplit(150, groups); // 200 feet, 100 yards, 100 m
		addDistanceOptionSplit(300, groups); // 500 feet, 200 yards, 200 m
		addDistanceOptionSplit(600, groups); // 1000 feet, 500 yards, 500 m
		addDistanceOptionSplit(1500, groups); // 2000 feet, 1000 yards, 1 km
		addDistanceOptionSplit(3000, groups); // 1 mi, 2 km
		addDistanceOptionSplit(6000, groups); // 2 mi, 5 km
		addDistanceOptionSplit(15000, groups); // 5 mi, 10 km

		addTimeOptionSplit(15, groups);
		addTimeOptionSplit(30, groups);
		addTimeOptionSplit(60, groups);
		addTimeOptionSplit(120, groups);
		addTimeOptionSplit(150, groups);
		addTimeOptionSplit(300, groups);
		addTimeOptionSplit(600, groups);
		addTimeOptionSplit(900, groups);
		addTimeOptionSplit(1800, groups);
		addTimeOptionSplit(3600, groups);
	}

	private void addDistanceOptionSplit(int value, @NonNull List<GpxDisplayGroup> displayGroups) {
		if (displayGroups.size() > 0) {
			double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
			String formattedDist = OsmAndFormatter.getFormattedDistanceInterval(app, value);
			distanceSplitOptions.put(formattedDist, dvalue);
			if (Math.abs(displayGroups.get(0).getSplitDistance() - dvalue) < 1) {
				selectedDistanceSplitInterval = distanceSplitOptions.size() - 1;
			}
		}
	}

	private void addTimeOptionSplit(int value, @NonNull List<GpxDisplayGroup> model) {
		if (model.size() > 0) {
			String time = OsmAndFormatter.getFormattedTimeInterval(app, value);
			timeSplitOptions.put(time, value);
			if (model.get(0).getSplitTime() == value) {
				selectedTimeSplitInterval = timeSplitOptions.size() - 1;
			}
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateSlider();
	}

	private void updateSlider() {
		if (selectedSplitType != GpxSplitType.NO_SPLIT) {
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
		final List<String> splitOptions = new ArrayList<>(timeSplitOptions.keySet());
		updateSliderMinMaxValues(splitOptions);

		slider.setValue(selectedTimeSplitInterval);
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				if (fromUser) {
					selectedTimeSplitInterval = (int) value;
					selectedSplitValue.setText(splitOptions.get(selectedTimeSplitInterval));
				}
			}
		});
		selectedSplitValue.setText(splitOptions.get(selectedTimeSplitInterval));
	}

	private void updateSliderDistanceInterval() {
		final List<String> splitOptions = new ArrayList<>(distanceSplitOptions.keySet());
		updateSliderMinMaxValues(splitOptions);

		slider.setValue(selectedDistanceSplitInterval);
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				if (fromUser) {
					selectedDistanceSplitInterval = (int) value;
					selectedSplitValue.setText(splitOptions.get(selectedDistanceSplitInterval));
				}
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
		double splitInterval = 0;
		if (selectedSplitType == GpxSplitType.NO_SPLIT) {
			splitInterval = 0;
		} else if (selectedSplitType == GpxSplitType.DISTANCE) {
			splitInterval = new ArrayList<>(distanceSplitOptions.values()).get(selectedDistanceSplitInterval);
		} else if (selectedSplitType == GpxSplitType.TIME) {
			splitInterval = new ArrayList<>(timeSplitOptions.values()).get(selectedTimeSplitInterval);
		}
		trackDrawInfo.setSplitType(selectedSplitType.getType());
		trackDrawInfo.setSplitInterval(splitInterval);
	}

	private void applySelectedSplit() {
		int timeSplit = new ArrayList<>(timeSplitOptions.values()).get(selectedTimeSplitInterval);
		double distanceSplit = new ArrayList<>(distanceSplitOptions.values()).get(selectedDistanceSplitInterval);

		Fragment target = getTargetFragment();
		if (target instanceof TrackAppearanceFragment) {
			((TrackAppearanceFragment) target).applySplit(selectedSplitType, timeSplit, distanceSplit);
		}
	}

	@NonNull
	public List<GpxDisplayGroup> getDisplayGroups() {
		Fragment target = getTargetFragment();
		if (target instanceof TrackAppearanceFragment) {
			return ((TrackAppearanceFragment) target).getDisplaySegmentGroups();
		}
		return Collections.emptyList();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, TrackDrawInfo trackDrawInfo, Fragment target) {
		try {
			if (fragmentManager.findFragmentByTag(SplitIntervalBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(TRACK_FILE_NAME, trackDrawInfo.getFilePath());

				SplitIntervalBottomSheet splitIntervalBottomSheet = new SplitIntervalBottomSheet();
				splitIntervalBottomSheet.setArguments(args);
				splitIntervalBottomSheet.setTargetFragment(target, 0);
				splitIntervalBottomSheet.show(fragmentManager, SplitIntervalBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			log.error("showInstance", e);
		}
	}
}