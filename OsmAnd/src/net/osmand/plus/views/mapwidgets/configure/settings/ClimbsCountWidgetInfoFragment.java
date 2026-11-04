package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.ClimbsCountWidget;

public class ClimbsCountWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String MIN_ELEVATION_KEY = "min_elevation_key";
	private static final String MIN_GRADE_KEY = "min_grade_key";
	private static final String SHOW_OVERLAY_KEY = "show_overlay_key";
	private static final String VAM_TARGET_KEY = "vam_target_key";

	private CommonPreference<Integer> minClimbElevationPref;
	private CommonPreference<Float> minClimbGradePref;
	private CommonPreference<Boolean> showOverlayPref;
	private CommonPreference<Integer> vamTargetPref;

	private int minElevation = 30;
	private float minGrade = 3.0f;
	private boolean showOverlay = true;
	private int vamTarget = 800;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof ClimbsCountWidget climbsCountWidget) {
			minClimbElevationPref = climbsCountWidget.getMinClimbElevationPref();
			minClimbGradePref = climbsCountWidget.getMinClimbGradePref();
			showOverlayPref = climbsCountWidget.getWidgetState().getShowOverlayPref();
			vamTargetPref = climbsCountWidget.getWidgetState().getVamTargetPref();
			minElevation = bundle.containsKey(MIN_ELEVATION_KEY) ? bundle.getInt(MIN_ELEVATION_KEY) : minClimbElevationPref.get();
			minGrade = bundle.containsKey(MIN_GRADE_KEY) ? bundle.getFloat(MIN_GRADE_KEY) : minClimbGradePref.get();
			showOverlay = bundle.containsKey(SHOW_OVERLAY_KEY) ? bundle.getBoolean(SHOW_OVERLAY_KEY) : showOverlayPref.get();
			vamTarget = bundle.containsKey(VAM_TARGET_KEY) ? bundle.getInt(VAM_TARGET_KEY) : vamTargetPref.get();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(MIN_ELEVATION_KEY, minElevation);
		outState.putFloat(MIN_GRADE_KEY, minGrade);
		outState.putBoolean(SHOW_OVERLAY_KEY, showOverlay);
		outState.putInt(VAM_TARGET_KEY, vamTarget);
	}

	@Override
	protected void setupTopContent(@NonNull ViewGroup container) {
		super.setupTopContent(container);
		View view = inflate(R.layout.climbs_count_widget_settings, container);

		// Setup min elevation field
		View minElevationContainer = view.findViewById(R.id.min_elevation_container);
		TextView minElevationTitle = view.findViewById(R.id.min_elevation_title);
		TextView minElevationDesc = view.findViewById(R.id.min_elevation_desc);
		EditText minElevationEdit = view.findViewById(R.id.min_elevation_edit);

		minElevationTitle.setText(R.string.climbs_count_min_elevation);
		minElevationDesc.setText(R.string.climbs_count_min_elevation_desc);
		minElevationEdit.setText(String.valueOf(minElevation));
		minElevationEdit.setHint(R.string.climbs_count_min_elevation);

		minElevationContainer.setOnClickListener(v -> {
			// Focus the edit text for direct editing
			minElevationEdit.requestFocus();
		});

		minElevationEdit.setOnFocusChangeListener((v, hasFocus) -> {
			if (!hasFocus) {
				try {
					minElevation = Integer.parseInt(minElevationEdit.getText().toString());
				} catch (NumberFormatException e) {
					minElevation = 30;
					minElevationEdit.setText("30");
				}
			}
		});

		// Setup min grade field
		View minGradeContainer = view.findViewById(R.id.min_grade_container);
		TextView minGradeTitle = view.findViewById(R.id.min_grade_title);
		TextView minGradeDesc = view.findViewById(R.id.min_grade_desc);
		EditText minGradeEdit = view.findViewById(R.id.min_grade_edit);

		minGradeTitle.setText(R.string.climbs_count_min_grade);
		minGradeDesc.setText(R.string.climbs_count_min_grade_desc);
		minGradeEdit.setText(String.valueOf(minGrade));
		minGradeEdit.setHint(R.string.climbs_count_min_grade);

		minGradeContainer.setOnClickListener(v -> {
			// Focus the edit text for direct editing
			minGradeEdit.requestFocus();
		});

		minGradeEdit.setOnFocusChangeListener((v, hasFocus) -> {
			if (!hasFocus) {
				try {
					minGrade = Float.parseFloat(minGradeEdit.getText().toString());
				} catch (NumberFormatException e) {
					minGrade = 3.0f;
					minGradeEdit.setText("3.0");
				}
			}
		});

		// Setup overlay toggle
		View showOverlayContainer = view.findViewById(R.id.show_overlay_container);
		TextView showOverlayTitle = view.findViewById(R.id.show_overlay_title);
		TextView showOverlayDesc = view.findViewById(R.id.show_overlay_desc);
		androidx.appcompat.widget.SwitchCompat showOverlayToggle = view.findViewById(R.id.show_overlay_toggle);

		showOverlayTitle.setText(R.string.climb_profile_show_overlay);
		showOverlayDesc.setText(R.string.climb_profile_show_overlay_desc);
		showOverlayToggle.setChecked(showOverlay);

		showOverlayContainer.setOnClickListener(v -> {
			showOverlay = !showOverlay;
			showOverlayToggle.setChecked(showOverlay);
		});

		showOverlayToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
			showOverlay = isChecked;
		});

		// Setup click listeners for edit texts to show keyboard
		minElevationEdit.setOnClickListener(v -> {
			minElevationEdit.requestFocus();
			AndroidUtils.showSoftKeyboard(getActivity(), minElevationEdit);
		});

		minGradeEdit.setOnClickListener(v -> {
			minGradeEdit.requestFocus();
			AndroidUtils.showSoftKeyboard(getActivity(), minGradeEdit);
		});

		// Setup VAM target field
		View vamTargetContainer = view.findViewById(R.id.vam_target_container);
		TextView vamTargetTitle = view.findViewById(R.id.vam_target_title);
		TextView vamTargetDesc = view.findViewById(R.id.vam_target_desc);
		EditText vamTargetEdit = view.findViewById(R.id.vam_target_edit);

		vamTargetTitle.setText(R.string.climbs_count_vam_target);
		vamTargetDesc.setText(R.string.climbs_count_vam_target_desc);
		vamTargetEdit.setText(String.valueOf(vamTarget));
		vamTargetEdit.setHint(R.string.climbs_count_vam_target);

		vamTargetContainer.setOnClickListener(v -> {
			// Focus the edit text for direct editing
			vamTargetEdit.requestFocus();
		});

		vamTargetEdit.setOnFocusChangeListener((v, hasFocus) -> {
			if (!hasFocus) {
				try {
					vamTarget = Integer.parseInt(vamTargetEdit.getText().toString());
				} catch (NumberFormatException e) {
					vamTarget = 800;
					vamTargetEdit.setText("800");
				}
			}
		});

		vamTargetEdit.setOnClickListener(v -> {
			vamTargetEdit.requestFocus();
			AndroidUtils.showSoftKeyboard(getActivity(), vamTargetEdit);
		});
	}

	@Override
	protected void applySettings() {
		boolean elevationChanged = minElevation != minClimbElevationPref.get();
		boolean gradeChanged = Math.abs(minGrade - minClimbGradePref.get()) > 0.01f;
		boolean overlayChanged = showOverlay != showOverlayPref.get();
		boolean vamTargetChanged = vamTarget != vamTargetPref.get();

		minClimbElevationPref.set(minElevation);
		minClimbGradePref.set(minGrade);
		showOverlayPref.set(showOverlay);
		vamTargetPref.set(vamTarget);

		if (widgetInfo != null && widgetInfo.widget instanceof ClimbsCountWidget) {
			ClimbsCountWidget widget = (ClimbsCountWidget) widgetInfo.widget;
			widget.updateWidgetView();
			if (elevationChanged || gradeChanged || overlayChanged || vamTargetChanged) {
				widget.updateInfo(null);
			}
		}

		super.applySettings();
	}
}
