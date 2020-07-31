package net.osmand.plus.measurementtool;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationType;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationType.NEXT_SEGMENT;
import static net.osmand.plus.measurementtool.MeasurementEditingContext.CalculationType.WHOLE_TRACK;

public class RouteBetweenPointsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil.getLog(RouteBetweenPointsBottomSheetDialogFragment.class);
	public static final String TAG = RouteBetweenPointsBottomSheetDialogFragment.class.getSimpleName();
	public static final int STRAIGHT_LINE_TAG = -1;

	private RouteBetweenPointsFragmentListener listener;
	private boolean nightMode;
	private boolean portrait;
	private boolean snapToRoadEnabled;
	private TextView segmentBtn;
	private TextView wholeTrackBtn;
	private TextView btnDescription;
	private FrameLayout segmentBtnContainer;
	private FrameLayout wholeTrackBtnContainer;
	private CalculationType calculationType = WHOLE_TRACK;
	private ApplicationMode snapToRoadAppMode;

	public void setListener(RouteBetweenPointsFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
		segmentBtnContainer = mainView.findViewById(R.id.next_segment_btn_container);
		segmentBtn = mainView.findViewById(R.id.next_segment_btn);
		wholeTrackBtnContainer = mainView.findViewById(R.id.whole_track_btn_container);
		wholeTrackBtn = mainView.findViewById(R.id.whole_track_btn);
		btnDescription = mainView.findViewById(R.id.button_description);

		LinearLayout navigationType = (LinearLayout) mainView.findViewById(R.id.navigation_types_container);
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		modes.remove(ApplicationMode.DEFAULT);

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				snapToRoadEnabled = false;
				if (listener != null) {
					ApplicationMode mode = null;
					if ((int) view.getTag() != STRAIGHT_LINE_TAG) {
						mode = modes.get((int) view.getTag());
						snapToRoadEnabled = true;
					}
					listener.onApplicationModeItemClick(mode);
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
		updateModeButtons(calculationType);
		return mainView;
	}

	private void addDelimiterView(LinearLayout container) {
		View row = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.divider, container, false);
		View divider = row.findViewById(R.id.divider);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
		params.topMargin = container.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		params.bottomMargin = container.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_title_padding_bottom);
		container.addView(row);
	}

	private void updateModeButtons(CalculationType calculationType) {
		OsmandApplication app = requiredMyApplication();
		int activeColor = ContextCompat.getColor(app, nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light);
		int textColor = ContextCompat.getColor(app, nightMode
				? R.color.text_color_primary_dark
				: R.color.text_color_primary_light);
		int radius = AndroidUtils.dpToPx(app, 4);

		GradientDrawable background = new GradientDrawable();
		background.setColor(UiUtilities.getColorWithAlpha(activeColor, 0.1f));
		background.setStroke(AndroidUtils.dpToPx(app, 1), UiUtilities.getColorWithAlpha(activeColor, 0.5f));

		if (calculationType == NEXT_SEGMENT) {
			background.setCornerRadii(new float[]{radius, radius, 0, 0, 0, 0, radius, radius});
			wholeTrackBtnContainer.setBackgroundColor(Color.TRANSPARENT);
			wholeTrackBtn.setTextColor(activeColor);
			segmentBtnContainer.setBackgroundDrawable(background);
			segmentBtn.setTextColor(textColor);
			btnDescription.setText(R.string.rourte_between_points_next_segment_button_desc);
		} else {
			background.setCornerRadii(new float[]{0, 0, radius, radius, radius, radius, 0, 0});
			wholeTrackBtnContainer.setBackgroundDrawable(background);
			wholeTrackBtn.setTextColor(textColor);
			segmentBtnContainer.setBackgroundColor(Color.TRANSPARENT);
			segmentBtn.setTextColor(activeColor);
			btnDescription.setText(R.string.rourte_between_points_whole_track_button_desc);
		}
		setCalculationType(calculationType);
		if (listener != null) {
			listener.onCalculationTypeChanges(calculationType);
		}
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
	public void onDestroyView() {
		if (listener != null) {
			listener.onDestroyView(snapToRoadEnabled);
		}
		super.onDestroyView();
	}


	public void setRouteMode(ApplicationMode snapToRoadAppMode) {
		this.snapToRoadAppMode = snapToRoadAppMode;
	}

	public void setCalculationType(CalculationType calculationType) {
		this.calculationType = calculationType;
	}

	public static void showInstance(FragmentManager fm, RouteBetweenPointsFragmentListener listener,
	                                CalculationType calculationType, ApplicationMode applicationMode) {
		try {
			if (!fm.isStateSaved()) {
				RouteBetweenPointsBottomSheetDialogFragment fragment = new RouteBetweenPointsBottomSheetDialogFragment();
				fragment.setListener(listener);
				fragment.setCalculationType(calculationType);
				fragment.setRouteMode(applicationMode);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface RouteBetweenPointsFragmentListener {

		void onDestroyView(boolean snapToRoadEnabled);

		void onApplicationModeItemClick(ApplicationMode mode);

		void onCalculationTypeChanges(CalculationType calculationType);
	}
}
