package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalyzeBottomSheet extends BottomSheetDialog {
	protected OsmandApplication app;
	public static final String TAG = AnalyzeBottomSheet.class.getSimpleName();
	private final List<GPXDataSetAxisType> xAxisTypes;
	private final List<GPXDataSetType[]> yAxisTypes;
	private final AxisSelectedListener axisSelectedListener;
	private final GpxDisplayItem gpxDisplayItem;
	private final boolean nightMode;
	private final MapActivity mapActivity;

	private final ArrayList<View> xAxisViews = new ArrayList<>();
	private final ArrayList<View> yAxisViews = new ArrayList<>();

	public AnalyzeBottomSheet(OsmandApplication app,
	                          MapActivity mapActivity,
	                          GpxDisplayItem gpxDisplayItem,
	                          List<GPXDataSetAxisType> xAxisTypes,
	                          List<GPXDataSetType[]> yAxisTypes,
	                          AxisSelectedListener axisSelectedListener) {
		super(mapActivity);
		this.app = app;
		this.xAxisTypes = xAxisTypes;
		this.yAxisTypes = yAxisTypes;
		this.axisSelectedListener = axisSelectedListener;
		this.gpxDisplayItem = gpxDisplayItem;
		this.nightMode = !app.getSettings().isLightContent();
		this.mapActivity = mapActivity;
	}

	@Override
	public void show() {
		View dialogView = LayoutInflater.from(mapActivity).inflate(R.layout.analyze_charts_bottom_sheet, null);
		setupDialogView(dialogView);
		setContentView(dialogView);
		super.show();
	}

	private void setupDialogView(View dialogView) {
		LinearLayout xTypesLinearLayout = dialogView.findViewById(R.id.x_axis);
		LinearLayout yTypesLinearLayout = dialogView.findViewById(R.id.y_axis);

		for (GPXDataSetType[] yTypes : yAxisTypes) {
			View typeView = createAxisType(yTypes);
			yAxisViews.add(typeView);
			yTypesLinearLayout.addView(typeView);
		}

		for (GPXDataSetAxisType xTypes : xAxisTypes) {
			View typeView = createAxisType(xTypes);
			xAxisViews.add(typeView);
			xTypesLinearLayout.addView(typeView);
		}
	}

	private View createAxisType(GPXDataSetType[] types) {
		String title = GPXDataSetType.getName(app, types);
		int iconId = types[0].getImageId();
		boolean selected = Arrays.equals(types, gpxDisplayItem.chartTypes);

		return createAxisTypeItem(title, iconId, selected, types);
	}

	private View createAxisType(GPXDataSetAxisType type) {
		String title;
		int iconId;
		boolean selected = type == gpxDisplayItem.chartAxisType;

		if (type == GPXDataSetAxisType.TIME) {
			iconId = R.drawable.ic_action_time;
			title = app.getString(R.string.shared_string_time);
		} else if (type == GPXDataSetAxisType.TIMEOFDAY) {
			iconId = R.drawable.ic_action_time_span;
			title = app.getString(R.string.time_of_day);
		} else {
			iconId = R.drawable.ic_action_marker_dark;
			title = app.getString(R.string.distance);
		}

		return createAxisTypeItem(title, iconId, selected, type);
	}

	private View createAxisTypeItem(String title, @DrawableRes int iconId, boolean selected, Object type) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		itemView.setTag(type);
		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(title);

		ImageView ivIcon = itemView.findViewById(R.id.icon);
		Drawable drawableIcon = AppCompatResources.getDrawable(app, iconId);
		if (drawableIcon != null) {
			drawableIcon.setTint(selected ?
					ColorUtilities.getActiveColor(app, nightMode) :
					ColorUtilities.getDefaultIconColor(app, nightMode));
		}
		ivIcon.setImageDrawable(drawableIcon);

		TextView tvDescription = itemView.findViewById(R.id.description);
		AndroidUiHelper.updateVisibility(tvDescription, false);

		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(selected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

		itemView.setTag(type);
		itemView.setOnClickListener(view -> {
			Object obj = view.getTag();
			if (obj instanceof GPXDataSetType[]) {
				GPXDataSetType[] types = (GPXDataSetType[]) obj;
				gpxDisplayItem.chartTypes = types;
				axisSelectedListener.onYAxisSelected(types);
			} else if (obj instanceof GPXDataSetAxisType) {
				GPXDataSetAxisType types = (GPXDataSetAxisType) obj;
				axisSelectedListener.onXAxisSelected(types);
			}
			updateViews();
			dismiss();
		});
		return itemView;
	}

	private void updateViews() {
		updateViews(yAxisViews);
		updateViews(xAxisViews);
	}

	private void updateViews(ArrayList<View> views){
		for (View view1 : views) {
			Object obj = view1.getTag();
			boolean selected = false;

			if (obj instanceof GPXDataSetType[]) {
				GPXDataSetType[] types = (GPXDataSetType[]) obj;
				selected = Arrays.equals(types, gpxDisplayItem.chartTypes);
			} else if (obj instanceof GPXDataSetAxisType) {
				GPXDataSetAxisType types = (GPXDataSetAxisType) obj;
				selected = types.equals(gpxDisplayItem.chartAxisType);
			}

			ImageView ivIcon = view1.findViewById(R.id.icon);
			ivIcon.setColorFilter(selected ?
					ColorUtilities.getActiveColor(app, nightMode) :
					ColorUtilities.getDefaultIconColor(app, nightMode));;

			CompoundButton compoundButton = view1.findViewById(R.id.compound_button);
			compoundButton.setChecked(selected);
		}
	}
}

interface AxisSelectedListener {
	void onXAxisSelected(GPXDataSetAxisType type);

	void onYAxisSelected(GPXDataSetType[] type);
}
