package net.osmand.plus.charts;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ChartModeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ChartModeBottomSheet.class.getSimpleName();

	private LinearLayout container;
	private GraphModeListener listener;
	private final List<View> itemViews = new ArrayList<>();

	private GPXDataSetAxisType selectedXAxisMode;
	private final ArrayList<GPXDataSetType> selectedYAxisMode = new ArrayList<>();
	boolean showYAxis = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (listener == null || listener.getSelectedAxisType() == null) {
			dismiss();
		} else {
			selectedXAxisMode = listener.getSelectedAxisType();
			selectedYAxisMode.addAll(listener.getSelectedDataSetTypes());
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View itemView = inflate(R.layout.chart_mode_bottom_sheet);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create());
		container = itemView.findViewById(R.id.segments_container);
		setupAxisCard(itemView);
	}

	private void setupAxisCard(@NonNull View view) {
		TextToggleButton.TextRadioItem xAxis = createRadioButton(R.string.x_axis, false);
		TextToggleButton.TextRadioItem yAxis = createRadioButton(R.string.y_axis, true);

		TextToggleButton radioGroup = new TextToggleButton(app, view.findViewById(R.id.custom_radio_buttons), nightMode);
		radioGroup.setItems(yAxis, xAxis);
		radioGroup.setSelectedItem(showYAxis ? yAxis :xAxis);

		TextView axisDescription = view.findViewById(R.id.axis_description);
		axisDescription.setText(R.string.y_axis_description);
		AndroidUiHelper.updateVisibility(axisDescription, showYAxis);

		if (showYAxis) {
			createYAxisItems();
		} else {
			createXAxisItems();
		}
		updateItems();
	}

	private void createXAxisItems() {
		container.removeAllViews();
		itemViews.clear();
		for (GPXDataSetAxisType type : getAvailableXTypes(listener.getAnalysis())) {
			View itemView = inflate(R.layout.bottom_sheet_item_with_radio_btn);

			itemView.setTag(type);
			itemView.setOnClickListener(v -> {
				selectedXAxisMode = type;
				updateItems();
			});

			TextView textView = itemView.findViewById(R.id.title);
			textView.setText(type.getTitleId());

			container.addView(itemView);
			itemViews.add(itemView);
		}
	}

	private void updateItems() {
		for (View itemView : itemViews) {
			if (itemView.getTag() instanceof GPXDataSetAxisType type) {
				updateXAxisItem(itemView, type);
			} else if (itemView.getTag() instanceof GPXDataSetType type) {
				updateYAxisItem(itemView, type);
			}
		}
	}

	private void updateApplyButton(){
		rightButton.setEnabled(!Algorithms.isEmpty(selectedYAxisMode));
	}

	private void updateXAxisItem(@NonNull View view, @NonNull GPXDataSetAxisType type){
		boolean checked = selectedXAxisMode == type;

		RadioButton radioButton = view.findViewById(R.id.compound_button);
		radioButton.setChecked(type == selectedXAxisMode);
		ImageView imageView = view.findViewById(R.id.icon);
		int iconColor = checked ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getDefaultIconColor(app, nightMode);
		imageView.setImageDrawable(getPaintedIcon(type.getIconId(), iconColor));
	}

	private void updateYAxisItem(@NonNull View view, @NonNull GPXDataSetType type) {
		boolean checked = selectedYAxisMode.contains(type);
		int iconColor;
		int textColor;
		if (selectedYAxisMode.size() >= 2 && !checked) {
			view.setEnabled(false);
			iconColor = ColorUtilities.getSecondaryIconColor(app, nightMode);
			textColor = ColorUtilities.getDisabledTextColor(app, nightMode);
		} else {
			view.setEnabled(true);
			iconColor = checked ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getDefaultIconColor(app, nightMode);
			textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		}

		CheckBox checkBox = view.findViewById(R.id.compound_button);
		checkBox.setChecked(checked);

		ImageView imageView = view.findViewById(R.id.icon);
		imageView.setImageDrawable(getPaintedIcon(type.getIconId(), iconColor));

		TextView textView = view.findViewById(R.id.title);
		textView.setTextColor(textColor);
	}

	private void createYAxisItems() {
		if (listener == null) {
			return;
		}
		container.removeAllViews();
		itemViews.clear();
		GpxTrackAnalysis analysis = listener.getAnalysis();
		List<GPXDataSetType[]> defaultTypes = getAvailableDefaultYTypes(analysis);
		for (GPXDataSetType[] types : defaultTypes) {
			createYAxisItem(types);
		}
		List<GPXDataSetType[]> sensorTypes = getAvailableSensorYTypes(analysis);
		if (!Algorithms.isEmpty(sensorTypes)) {
			container.addView(createDivider());
		}
		for (GPXDataSetType[] types : sensorTypes) {
			createYAxisItem(types);
		}
	}

	private View createDivider(){
		View divider = inflate(R.layout.divider);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(params.leftMargin, dpToPx(8), params.rightMargin, dpToPx(8));
		divider.setLayoutParams(params);
		return divider;
	}

	private void createYAxisItem(GPXDataSetType[] types) {
		if (types.length > 1) {
			return;
		}
		GPXDataSetType singleSetType = types[0];
		View itemView = inflate(R.layout.bottom_sheet_item_title_icon_with_checkbox);

		itemView.setTag(singleSetType);
		itemView.setOnClickListener(v -> {
			if (selectedYAxisMode.contains(singleSetType)) {
				selectedYAxisMode.remove(singleSetType);
				updateItems();
				updateApplyButton();
				return;
			}
			if (selectedYAxisMode.size() < 2) {
				selectedYAxisMode.add(singleSetType);
				updateItems();
				updateApplyButton();
			}
		});

		CheckBox checkBox = itemView.findViewById(R.id.compound_button);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkBox);

		TextView textView = itemView.findViewById(R.id.title);
		textView.setText(singleSetType.getTitleId());

		container.addView(itemView);
		itemViews.add(itemView);
	}

	@Override
	protected int getCustomHeight() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			int screenHeight = AndroidUtils.getScreenHeight(activity);
			int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
			int navBarHeight = AndroidUtils.getNavBarHeight(activity);
			int buttonsHeight = getDimensionPixelSize(R.dimen.dialog_button_ex_height);

			return screenHeight - statusBarHeight - buttonsHeight - navBarHeight - getDimensionPixelSize(R.dimen.toolbar_height);
		}
		return super.getCustomHeight();
	}

	@NonNull
	private TextToggleButton.TextRadioItem createRadioButton(int titleId, boolean showYAxis) {
		TextToggleButton.TextRadioItem item = new TextToggleButton.TextRadioItem(getString(titleId));
		item.setOnClickListener((radioItem, view) -> {
			View mainView = getView();
			if (mainView != null) {
				this.showYAxis = showYAxis;
				setupAxisCard(mainView);
			}
			return true;
		});
		return item;
	}

	@NonNull
	public static List<GPXDataSetAxisType> getAvailableXTypes(GpxTrackAnalysis analysis) {
		List<GPXDataSetAxisType> availableTypes = new ArrayList<>();
		for (GPXDataSetAxisType type : GPXDataSetAxisType.values()) {
			if (type == GPXDataSetAxisType.TIME || type == GPXDataSetAxisType.TIME_OF_DAY) {
				if (analysis.isTimeSpecified()) {
					availableTypes.add(type);
				}
			} else {
				availableTypes.add(type);
			}
		}
		return availableTypes;
	}

	@NonNull
	public static List<GPXDataSetType[]> getAvailableDefaultYTypes(@NonNull GpxTrackAnalysis analysis) {
		List<GPXDataSetType[]> availableTypes = new ArrayList<>();
		boolean hasElevationData = analysis.hasElevationData();
		boolean hasSpeedData = analysis.hasSpeedData();
		if (hasElevationData) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.ALTITUDE});
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.SLOPE});
		}
		if (hasSpeedData) {
			availableTypes.add(new GPXDataSetType[]{GPXDataSetType.SPEED});
		}
		return availableTypes;
	}

	@NonNull
	public static List<GPXDataSetType[]> getAvailableSensorYTypes(@NonNull GpxTrackAnalysis analysis) {
		List<GPXDataSetType[]> availableTypes = new ArrayList<>();
		PluginsHelper.getAvailableGPXDataSetTypes(analysis, availableTypes);
		return availableTypes;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		//outState.putString(SELECTED_STYLE_KEY, selectedStyle);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (listener != null) {
			listener.onGraphModeChanged(selectedXAxisMode, selectedYAxisMode);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, GraphModeListener listener, boolean showYAxis) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ChartModeBottomSheet fragment = new ChartModeBottomSheet();
			fragment.listener = listener;
			fragment.showYAxis = showYAxis;
			fragment.show(fragmentManager, TAG);
		}
	}
}
