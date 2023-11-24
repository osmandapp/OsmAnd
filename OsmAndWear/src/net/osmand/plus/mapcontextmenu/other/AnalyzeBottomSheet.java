package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalyzeBottomSheet extends BottomSheetDialogFragment {
	public static final String TAG = AnalyzeBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private boolean nightMode;

	private AxisSelectedListener axisSelectedListener;
	private GpxDisplayItem gpxDisplayItem;

	private List<GPXDataSetAxisType> xAxisTypes;
	private List<GPXDataSetType[]> yAxisTypes;

	private final ArrayList<View> xAxisViews = new ArrayList<>();
	private final ArrayList<View> yAxisViews = new ArrayList<>();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		mapActivity = (MapActivity) requireActivity();
		TrackDetailsMenu trackDetailsMenu = mapActivity.getTrackDetailsMenu();

		if (app != null) {
			nightMode = !app.getSettings().isLightContent();
		}
		gpxDisplayItem = trackDetailsMenu.getGpxItem();
		if (gpxDisplayItem != null) {
			xAxisTypes = trackDetailsMenu.getAvailableXTypes(gpxDisplayItem.analysis);
			yAxisTypes = trackDetailsMenu.getAvailableYTypes(gpxDisplayItem.analysis);
			axisSelectedListener = trackDetailsMenu.getAxisSelectedListener();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View dialogView = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.analyze_charts_bottom_sheet, null);
		setupDialogView(dialogView);
		return dialogView;
	}

	private void setupDialogView(View dialogView) {
		LinearLayout xTypesContainer = dialogView.findViewById(R.id.x_axis);
		LinearLayout yTypesContainer = dialogView.findViewById(R.id.y_axis);

		for (GPXDataSetType[] yTypes : yAxisTypes) {
			View typeView = createAxisType(yTypes);
			yAxisViews.add(typeView);
			yTypesContainer.addView(typeView);
		}

		for (GPXDataSetAxisType xTypes : xAxisTypes) {
			View typeView = createAxisType(xTypes);
			xAxisViews.add(typeView);
			xTypesContainer.addView(typeView);
		}
	}

	private View createAxisType(GPXDataSetType[] types) {
		String title = TrackDetailsMenu.getGpxDataSetsName(app, types);
		int iconId = types[0].getIconId();
		boolean selected = Arrays.equals(types, gpxDisplayItem.chartTypes);

		return createAxisTypeItem(title, iconId, selected, types);
	}

	private View createAxisType(GPXDataSetAxisType type) {
		boolean selected = type == gpxDisplayItem.chartAxisType;
		int iconId = type.getIconId();
		String title = getString(type.getTitleId());

		return createAxisTypeItem(title, iconId, selected, type);
	}

	private View createAxisTypeItem(String title, @DrawableRes int iconId, boolean selected, Object type) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		View itemView = inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(title);
		updateItemView(itemView, selected, iconId);

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

	private void updateViews(ArrayList<View> views) {
		for (View view : views) {
			Object obj = view.getTag();
			boolean selected = false;
			int iconId = 0;
			if (obj instanceof GPXDataSetType[]) {
				GPXDataSetType[] types = (GPXDataSetType[]) obj;
				selected = Arrays.equals(types, gpxDisplayItem.chartTypes);
				iconId = types[0].getIconId();
			} else if (obj instanceof GPXDataSetAxisType) {
				GPXDataSetAxisType types = (GPXDataSetAxisType) obj;
				selected = types.equals(gpxDisplayItem.chartAxisType);
				iconId = types.getIconId();
			}

			updateItemView(view, selected, iconId);
		}
	}

	private void updateItemView(View view, boolean selected, @DrawableRes int iconId) {
		ImageView ivIcon = view.findViewById(R.id.icon);
		Drawable drawableIcon = app.getUIUtilities().getIcon(iconId, selected ?
				ColorUtilities.getActiveColorId(nightMode) :
				ColorUtilities.getDefaultIconColorId(nightMode));
		ivIcon.setImageDrawable(drawableIcon);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(selected);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AnalyzeBottomSheet fragment = new AnalyzeBottomSheet();
			fragment.show(manager, TAG);
		}
	}
}

interface AxisSelectedListener {
	void onXAxisSelected(GPXDataSetAxisType type);

	void onYAxisSelected(GPXDataSetType[] type);
}
