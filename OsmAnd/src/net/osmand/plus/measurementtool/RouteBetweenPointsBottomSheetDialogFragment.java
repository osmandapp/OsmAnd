package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetBehaviourDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.UiUtilities.CustomRadioButtonType.END;
import static net.osmand.plus.UiUtilities.CustomRadioButtonType.START;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.DEFAULT_APP_MODE;
import static net.osmand.plus.measurementtool.SelectFileBottomSheet.BOTTOM_SHEET_HEIGHT_DP;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

public class RouteBetweenPointsBottomSheetDialogFragment extends BottomSheetBehaviourDialogFragment {


	private static final Log LOG = PlatformUtil.getLog(RouteBetweenPointsBottomSheetDialogFragment.class);
	public static final String TAG = RouteBetweenPointsBottomSheetDialogFragment.class.getSimpleName();
	public static final int STRAIGHT_LINE_TAG = -1;
	public static final String DIALOG_TYPE_KEY = "dialog_type_key";
	public static final String DEFAULT_DIALOG_MODE_KEY = "default_dialog_mode_key";
	public static final String ROUTE_APP_MODE_KEY = "route_app_mode";

	private boolean nightMode;
	private TextView btnDescription;
	private RouteBetweenPointsDialogType dialogType = RouteBetweenPointsDialogType.WHOLE_ROUTE_CALCULATION;
	private RouteBetweenPointsDialogMode defaultDialogMode = RouteBetweenPointsDialogMode.SINGLE;
	private ApplicationMode appMode;

	private LinearLayout customRadioButton;

	public enum RouteBetweenPointsDialogType {
		WHOLE_ROUTE_CALCULATION,
		NEXT_ROUTE_CALCULATION,
		PREV_ROUTE_CALCULATION
	}

	public enum RouteBetweenPointsDialogMode {
		SINGLE,
		ALL,
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			appMode = ApplicationMode.valueOfStringKey(args.getString(ROUTE_APP_MODE_KEY), null);
			dialogType = (RouteBetweenPointsDialogType) args.get(DIALOG_TYPE_KEY);
			defaultDialogMode = (RouteBetweenPointsDialogMode) args.get(DEFAULT_DIALOG_MODE_KEY);
		}
		if (savedInstanceState != null) {
			dialogType = (RouteBetweenPointsDialogType) savedInstanceState.get(DIALOG_TYPE_KEY);
			defaultDialogMode = (RouteBetweenPointsDialogMode) savedInstanceState.get(DEFAULT_DIALOG_MODE_KEY);
		}
		OsmandApplication app = requiredMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final View mainView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.fragment_route_between_points_bottom_sheet_dialog,
						null, false);
		customRadioButton = mainView.findViewById(R.id.custom_radio_buttons);
		customRadioButton.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.route_info_control_buttons_height));
		TextView singleModeButton = mainView.findViewById(R.id.left_button);
		singleModeButton.setText(getButtonText(RouteBetweenPointsDialogMode.SINGLE));
		TextView allModeButton = mainView.findViewById(R.id.right_button);
		allModeButton.setText(getButtonText(RouteBetweenPointsDialogMode.ALL));
		btnDescription = mainView.findViewById(R.id.button_description);

		LinearLayout navigationType = mainView.findViewById(R.id.navigation_types_container);
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(ApplicationMode.DEFAULT);

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode mode = DEFAULT_APP_MODE;
				if ((int) view.getTag() != STRAIGHT_LINE_TAG) {
					mode = modes.get((int) view.getTag());
				}
				Fragment fragment = getTargetFragment();
				if (fragment instanceof RouteBetweenPointsFragmentListener) {
					((RouteBetweenPointsFragmentListener) fragment).onChangeApplicationMode(mode, dialogType, defaultDialogMode);
				}
				dismiss();
			}
		};

		Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_split_interval, nightMode);
		addProfileView(navigationType, onClickListener, STRAIGHT_LINE_TAG, icon,
				app.getText(R.string.routing_profile_straightline), appMode == DEFAULT_APP_MODE);
		addDelimiterView(navigationType);

		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			if (!PUBLIC_TRANSPORT_KEY.equals(mode.getRoutingProfile())) {
				icon = app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode));
				addProfileView(navigationType, onClickListener, i, icon, mode.toHumanString(), mode.equals(appMode));
			}
		}

		singleModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDefaultDialogMode(RouteBetweenPointsDialogMode.SINGLE);
			}
		});
		allModeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDefaultDialogMode(RouteBetweenPointsDialogMode.ALL);
			}
		});
		updateModeButtons();
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	@Override
	protected int getPeekHeight() {
		return AndroidUtils.dpToPx(getContext(), BOTTOM_SHEET_HEIGHT_DP);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DIALOG_TYPE_KEY, dialogType);
		outState.putSerializable(DEFAULT_DIALOG_MODE_KEY, defaultDialogMode);
	}

	@Override
	public void onDestroyView() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof RouteBetweenPointsFragmentListener) {
			((RouteBetweenPointsFragmentListener) fragment).onCloseRouteDialog();
		}
		super.onDestroyView();
	}

	private void addDelimiterView(LinearLayout container) {
		View row = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.divider, container, false);
		View divider = row.findViewById(R.id.divider);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
		params.topMargin = row.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		params.bottomMargin = row.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		container.addView(row);
	}

	public void setDefaultDialogMode(RouteBetweenPointsDialogMode defaultDialogMode) {
		this.defaultDialogMode = defaultDialogMode;
		updateModeButtons();
	}

	public void updateModeButtons() {
		UiUtilities.updateCustomRadioButtons(getMyApplication(), customRadioButton, nightMode,
				defaultDialogMode == RouteBetweenPointsDialogMode.SINGLE ? START : END);
		btnDescription.setText(getButtonDescr(defaultDialogMode));
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

	private String getButtonText(RouteBetweenPointsDialogMode dialogMode) {
		switch (dialogType) {
			case WHOLE_ROUTE_CALCULATION:
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.next_segment);
					case ALL:
						return getString(R.string.whole_track);
				}
				break;
			case NEXT_ROUTE_CALCULATION:
				String nextDescr = getDescription(false, dialogMode);
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.next_segment), nextDescr);
					case ALL:
						return getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.all_next_segments), nextDescr);
				}
				break;
			case PREV_ROUTE_CALCULATION:
				String prevDescr = getDescription(true, dialogMode);
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.previous_segment), prevDescr);
					case ALL:
						return getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.all_previous_segments), prevDescr);
				}
				break;
		}
		return "";
	}

	private String getButtonDescr(RouteBetweenPointsDialogMode dialogMode) {
		switch (dialogType) {
			case WHOLE_ROUTE_CALCULATION:
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.route_between_points_next_segment_button_desc);
					case ALL:
						return getString(R.string.route_between_points_whole_track_button_desc);
				}
				break;
			case NEXT_ROUTE_CALCULATION:
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.only_selected_segment_recalc);
					case ALL:
						return getString(R.string.all_next_segments_will_be_recalc);
				}
				break;
			case PREV_ROUTE_CALCULATION:
				switch (dialogMode) {
					case SINGLE:
						return getString(R.string.only_selected_segment_recalc);
					case ALL:
						return getString(R.string.all_previous_segments_will_be_recalc);
				}
				break;
		}
		return "";
	}

	@NonNull
	private String getDescription(boolean before, RouteBetweenPointsDialogMode dialogMode) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity == null) {
			return "";
		}
		MeasurementEditingContext editingCtx = mapActivity.getMapLayers().getMeasurementToolLayer().getEditingCtx();
		int pos = editingCtx.getSelectedPointPosition();
		List<WptPt> points = editingCtx.getPoints();

		float dist = 0;
		if (dialogMode == RouteBetweenPointsDialogMode.SINGLE) {
			WptPt selectedPoint = points.get(pos);
			WptPt second = points.get(before ? pos - 1 : pos + 1);
			dist += MapUtils.getDistance(selectedPoint.lat, selectedPoint.lon, second.lat, second.lon);
		} else {
			int startIdx;
			int endIdx;
			if (before) {
				startIdx = 1;
				endIdx = pos;
			} else {
				startIdx = pos + 1;
				endIdx = points.size() - 1;
			}
			for (int i = startIdx; i <= endIdx; i++) {
				WptPt first = points.get(i - 1);
				WptPt second = points.get(i);
				dist += MapUtils.getDistance(first.lat, first.lon, second.lat, second.lon);
			}
		}
		return OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication());
	}

	public static void showInstance(FragmentManager fm, Fragment targetFragment,
									RouteBetweenPointsDialogType dialogType,
									RouteBetweenPointsDialogMode defaultDialogMode,
									ApplicationMode applicationMode) {
		try {
			if (!fm.isStateSaved()) {
				RouteBetweenPointsBottomSheetDialogFragment fragment = new RouteBetweenPointsBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putString(ROUTE_APP_MODE_KEY, applicationMode != null ? applicationMode.getStringKey() : null);
				args.putSerializable(DIALOG_TYPE_KEY, dialogType);
				args.putSerializable(DEFAULT_DIALOG_MODE_KEY, defaultDialogMode);
				fragment.setArguments(args);
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface RouteBetweenPointsFragmentListener {

		void onCloseRouteDialog();

		void onChangeApplicationMode(ApplicationMode mode, RouteBetweenPointsDialogType dialogType,
									 RouteBetweenPointsDialogMode dialogMode);

	}
}
