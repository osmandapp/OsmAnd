package net.osmand.plus.measurementtool;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.UiUtilities.CustomRadioButtonType.*;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.NEXT_SEGMENT;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationMode.WHOLE_TRACK;

public class RouteBetweenPointsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(RouteBetweenPointsBottomSheetDialogFragment.class);
	public static final String TAG = RouteBetweenPointsBottomSheetDialogFragment.class.getSimpleName();
	public static final int STRAIGHT_LINE_TAG = -1;
	public static final String CALCULATION_MODE_KEY = "calculation_type";
	public static final String ROUTE_APP_MODE_KEY = "route_app_mode";

	private boolean nightMode;
	private boolean portrait;
	private boolean snapToRoadEnabled = true;
	private TextView btnDescription;
	private CalculationMode calculationMode = WHOLE_TRACK;
	private ApplicationMode snapToRoadAppMode;

	private LinearLayout customRadioButton;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			snapToRoadAppMode = ApplicationMode.valueOfStringKey(args.getString(ROUTE_APP_MODE_KEY), null);
			calculationMode = (CalculationMode) args.get(CALCULATION_MODE_KEY);
		}
		if (savedInstanceState != null) {
			calculationMode = (CalculationMode) savedInstanceState.get(CALCULATION_MODE_KEY);
		}
		OsmandApplication app = requiredMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		FragmentActivity activity = requireActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(activity);
		final View mainView = inflater.inflate(R.layout.fragment_route_between_points_bottom_sheet_dialog,
				container, false);
		AndroidUtils.setBackground(activity, mainView, nightMode,
				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

		View cancelButton = mainView.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, cancelButton, UiUtilities.DialogButtonType.SECONDARY,
				R.string.shared_string_close);
		mainView.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		customRadioButton = mainView.findViewById(R.id.custom_radio_buttons);
		TextView segmentBtn = mainView.findViewById(R.id.left_button);
		segmentBtn.setText(R.string.next_segment);
		TextView wholeTrackBtn = mainView.findViewById(R.id.right_button);
		wholeTrackBtn.setText(R.string.whole_track);
		btnDescription = mainView.findViewById(R.id.button_description);

		LinearLayout navigationType = mainView.findViewById(R.id.navigation_types_container);
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(ApplicationMode.DEFAULT);

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				snapToRoadEnabled = false;
				ApplicationMode mode = null;
				if ((int) view.getTag() != STRAIGHT_LINE_TAG) {
					mode = modes.get((int) view.getTag());
					snapToRoadEnabled = true;
				}
				Fragment fragment = getTargetFragment();
				if (fragment instanceof RouteBetweenPointsFragmentListener) {
					((RouteBetweenPointsFragmentListener) fragment).onChangeApplicationMode(mode, calculationMode);
				}
				dismiss();
			}
		};

		Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_split_interval, nightMode);
		addProfileView(navigationType, onClickListener, STRAIGHT_LINE_TAG, icon,
				app.getText(R.string.routing_profile_straightline), snapToRoadAppMode == null);
		addDelimiterView(navigationType);

		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			icon = app.getUIUtilities().getIcon(mode.getIconRes(), mode.getIconColorInfo().getColor(nightMode));
			addProfileView(navigationType, onClickListener, i, icon, mode.toHumanString(), mode.equals(snapToRoadAppMode));
		}

		segmentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateModeButtons(NEXT_SEGMENT);
			}
		});
		wholeTrackBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateModeButtons(WHOLE_TRACK);
			}
		});
		updateModeButtons(calculationMode);
		return mainView;
	}

	private void addDelimiterView(LinearLayout container) {
		View row = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.divider, container, false);
		View divider = row.findViewById(R.id.divider);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
		params.topMargin = row.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		params.bottomMargin = row.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		container.addView(row);
	}

	private void updateModeButtons(CalculationMode calculationMode) {
		if (calculationMode == NEXT_SEGMENT) {
			UiUtilities.updateCustomRadioButtons(getMyApplication(), customRadioButton, nightMode, LEFT);
			btnDescription.setText(R.string.rourte_between_points_next_segment_button_desc);
		} else {
			btnDescription.setText(R.string.rourte_between_points_whole_track_button_desc);
			UiUtilities.updateCustomRadioButtons(getMyApplication(), customRadioButton, nightMode, RIGHT);
		}
		setCalculationMode(calculationMode);
	}

	private void addProfileView(LinearLayout container, View.OnClickListener onClickListener, Object tag,
	                            Drawable icon, CharSequence title, boolean check) {
		View row = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_item_with_radio_btn, container, false);
		((RadioButton) row.findViewById(R.id.compound_button)).setChecked(check);
		ImageView imageView = row.findViewById(R.id.icon);
		imageView.setImageDrawable(icon);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
		params.rightMargin = container.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
		((TextView) row.findViewById(R.id.title)).setText(title);
		row.setOnClickListener(onClickListener);
		row.setTag(tag);
		container.addView(row);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			Dialog dialog = getDialog();
			if (dialog != null && dialog.getWindow() != null) {
				Window window = dialog.getWindow();
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = dialog.getContext().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(CALCULATION_MODE_KEY, calculationMode);
	}

	@Override
	public void onDestroyView() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof RouteBetweenPointsFragmentListener) {
			((RouteBetweenPointsFragmentListener) fragment).onCloseRouteDialog(snapToRoadEnabled);
		}
		super.onDestroyView();
	}

	public void setCalculationMode(CalculationMode calculationMode) {
		this.calculationMode = calculationMode;
	}

	public static void showInstance(FragmentManager fm, Fragment targetFragment, CalculationMode calculationMode,
	                                ApplicationMode applicationMode) {
		try {
			if (!fm.isStateSaved()) {
				RouteBetweenPointsBottomSheetDialogFragment fragment = new RouteBetweenPointsBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putString(ROUTE_APP_MODE_KEY, applicationMode != null ? applicationMode.getStringKey() : null);
				args.putSerializable(CALCULATION_MODE_KEY, calculationMode);
				fragment.setArguments(args);
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface RouteBetweenPointsFragmentListener {

		void onCloseRouteDialog(boolean snapToRoadEnabled);

		void onChangeApplicationMode(ApplicationMode mode, CalculationMode calculationMode);

	}
}
