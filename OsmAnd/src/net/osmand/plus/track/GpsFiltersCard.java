package net.osmand.plus.track;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpsFilterHelper;
import net.osmand.plus.helpers.GpsFilterHelper.GpsFilter;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class GpsFiltersCard extends MapBaseCard {

	private final SelectedGpxFile selectedGpxFile;
	private final GpsFilterHelper filterHelper;

	public GpsFiltersCard(@NonNull MapActivity mapActivity, @NonNull SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.selectedGpxFile = selectedGpxFile;
		this.filterHelper = new GpsFilterHelper(app, selectedGpxFile);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gps_filters_card;
	}

	@Override
	protected void updateContent() {
		setupPointsRatio();
		setupSmoothingFilter();
		setupSpeedFilter();
		setupAltitudeFilter();
		setupHdopFilter();
	}

	private void setupPointsRatio() {
		String pointsString = app.getString(R.string.shared_string_gpx_points);
		String leftPoints = String.valueOf(filterHelper.getLeftPoints());
		String totalPoints = String.valueOf(filterHelper.getTotalPoints());
		String ratio = app.getString(R.string.ltr_or_rtl_combine_via_slash_with_space, leftPoints, totalPoints);
		String fullText = app.getString(R.string.ltr_or_rtl_combine_via_colon, pointsString, ratio);
		SpannableString spannedText = new SpannableString(fullText);
		spannedText.setSpan(new StyleSpan(Typeface.BOLD), 0, pointsString.length() + 1,
				Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

		TextView pointsRatio = view.findViewById(R.id.points_ratio);
		pointsRatio.setText(spannedText);
	}

	private void setupSmoothingFilter() {
		setupFilter(view.findViewById(R.id.smoothing_filter), filterHelper.smoothingFilter);
	}

	private void setupSpeedFilter() {
		setupFilter(view.findViewById(R.id.speed_filter), filterHelper.speedFilter);
	}

	private void setupAltitudeFilter() {
		setupFilter(view.findViewById(R.id.altitude_filter), filterHelper.altitudeFilter);
	}

	private void setupHdopFilter() {
		setupFilter(view.findViewById(R.id.hdop_filter), filterHelper.hdopFilter);
	}

	private void setupFilter(View container, GpsFilter filter) {
		View header = container.findViewById(R.id.filter_header);
		View content = container.findViewById(R.id.filter_content);
		AppCompatImageView upDownButton = container.findViewById(R.id.up_down_button);

		if (!filter.isNeeded()) {
			updateUpDownButton(upDownButton, false);
			AndroidUiHelper.updateVisibility(content, false);
			return;
		}

		header.setOnClickListener(v -> {
			boolean expanded = content.getVisibility() == View.VISIBLE;
			updateUpDownButton(upDownButton, !expanded);
			AndroidUiHelper.updateVisibility(content, !expanded);
		});

		setupSlider(container, filter);
		updateDisplayedNumbers(container, filter);

		TextView minFilterValue = container.findViewById(R.id.min_filter_value);
		minFilterValue.setText(filter.getFormattedStyledValue(filter.getMinValue()));

		TextView maxFilterValue = container.findViewById(R.id.max_filter_value);
		maxFilterValue.setText(filter.getFormattedStyledValue(filter.getMaxValue()));

		TextView filterDescription = container.findViewById(R.id.filter_description);
		filterDescription.setText(filter.getDescriptionId());
	}

	private void updateUpDownButton(AppCompatImageView upDownButton, boolean up) {
		int upDownIconId = up ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
		Drawable upDownIcon = getColoredIcon(upDownIconId, ColorUtilities.getDefaultIconColorId(nightMode));
		upDownButton.setImageDrawable(upDownIcon);
	}

	private void setupSlider(final View container, final GpsFilter filter) {
		boolean range = filter.isRangeSupported();
		Slider slider = container.findViewById(R.id.filter_slider);
		RangeSlider rangeSlider = container.findViewById(R.id.filter_range_slider);

		AndroidUiHelper.updateVisibility(slider, !range);
		AndroidUiHelper.updateVisibility(rangeSlider, range);

		if (range) {
			rangeSlider.setValues(filter.getSelectedMinValue().floatValue(), filter.getSelectedMaxValue().floatValue());
			rangeSlider.setValueFrom(filter.getMinValue().floatValue());
			rangeSlider.setValueTo(filter.getMaxValue().floatValue());
			rangeSlider.addOnChangeListener((slider1, value, fromUser) -> {
				List<Float> values = rangeSlider.getValues();
				if (fromUser && values.size() == 2) {
					filter.updateValues((values.get(0)), values.get(1));
					updateDisplayedNumbers(container, filter);
				}
			});
			UiUtilities.setupSlider(rangeSlider, nightMode, ColorUtilities.getActiveColor(app, nightMode), false);
		} else {
			slider.setValue(filter.getSelectedMaxValue().floatValue());
			slider.setValueFrom(filter.getMinValue().floatValue());
			slider.setValueTo(filter.getMaxValue().floatValue());
			slider.addOnChangeListener((slider1, value, fromUser) -> {
				if (fromUser) {
					filter.updateValue((slider.getValue()));
					updateDisplayedNumbers(container, filter);
				}
			});
			UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode));
		}
	}

	private void updateDisplayedNumbers(View container, GpsFilter filter) {
		TextView title = container.findViewById(R.id.filter_title);
		title.setText(filter.getFilterTitle());

		TextView leftText = container.findViewById(R.id.left_text);
		leftText.setText(filter.getLeftText());

		TextView rightText = container.findViewById(R.id.right_text);
		rightText.setText(filter.getRightText());
	}
}