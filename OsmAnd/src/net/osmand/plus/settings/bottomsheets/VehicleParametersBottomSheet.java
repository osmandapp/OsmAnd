package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleParametersBottomSheet extends BasePreferenceBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = VehicleParametersBottomSheet.class.getSimpleName();
	private String selectedItem;
	private float currentValue;
	private int contentHeightPrevious;
	private EditText text;
	private int buttonsHeight;
	private int shadowHeight;
	private ScrollView scrollView;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		items.add(createBottomSheetItem(app));
	}

	@SuppressLint("ClickableViewAccessibility")
	private BaseBottomSheetItem createBottomSheetItem(OsmandApplication app) {
		SizePreference preference = (SizePreference) getPreference();
		MetricsConstants metricsConstants = app.getSettings().METRIC_SYSTEM.get();
		View mainView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_item_edit_with_chips_view, null);
		TextView title = mainView.findViewById(R.id.title);
		title.setText(preference.getTitle().toString());
		VehicleSizeAssets vehicleSizeAssets = preference.getAssets();
		if (vehicleSizeAssets != null) {
			ImageView imageView = mainView.findViewById(R.id.image_view);
			imageView.setImageDrawable(app.getUIUtilities()
					.getIcon(!nightMode ? vehicleSizeAssets.getDayIconId() : vehicleSizeAssets.getNightIconId()));
			TextView description = mainView.findViewById(R.id.description);
			description.setText(app.getString(vehicleSizeAssets.getDescriptionRes()));
		}
		HorizontalChipsView chipsView = mainView.findViewById(R.id.chips_view);
		TextView metric = mainView.findViewById(R.id.metric);
		metric.setText(app.getString(preference.getAssets().getMetricRes()));
		DecimalFormat df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
		text = mainView.findViewById(R.id.text_edit);
		try {
			currentValue = Float.parseFloat(preference.getValue());
		} catch (NumberFormatException e) {
			currentValue = 0.0f;
		}

		String currentValueStr;
		if (preference.isLengthAssets()) {
			selectedItem = OsmAndFormatter.convertLength(app, metricsConstants, currentValue).value;
			currentValueStr = currentValue == 0.0f ? "" : selectedItem;
		} else {
			selectedItem = preference.getEntryFromValue(preference.getValue());
			currentValueStr = currentValue == 0.0f ? "" : df.format(currentValue + 0.01f);
		}
		text.setText(currentValueStr);
		text.clearFocus();
		text.setOnTouchListener((v, event) -> {
			text.onTouchEvent(event);
			text.setSelection(text.getText().length());
			return true;
		});
		text.addTextChangedListener(new TextWatcher() {
			final DecimalFormat df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				float value = 0.0f;

				if (!Algorithms.isEmpty(s)) {
					try {
						if (preference.isLengthAssets()) {
							value = Float.parseFloat(s.toString());
							currentValue = Float.parseFloat(df.format(convertLengthToMeters(app, value)));
						} else {
							currentValue = Float.parseFloat(s.toString()) - 0.01f;
						}
					} catch (NumberFormatException e) {
						currentValue = 0.0f;
					}
				} else {
					currentValue = 0.0f;
				}

				selectedItem = preference.isLengthAssets()
						? df.format(value)
						: preference.getEntryFromValue(String.valueOf(currentValue));

				ChipItem selected = chipsView.getChipById(selectedItem);
				chipsView.setSelected(selected);
				if (selected != null) {
					chipsView.notifyDataSetChanged();
					chipsView.smoothScrollTo(selected);
				}
			}
		});

		List<ChipItem> chips = new ArrayList<>();
		for (int i = 0; i < preference.getEntryValues().length; i++) {
			String value;
			String chipTitle;
			if (preference.isLengthAssets()) {
				float entryValue = Float.parseFloat(preference.getEntryValues()[i]) + 0.01f;
				FormattedValue formattedValue = OsmAndFormatter.convertLength(app, metricsConstants, entryValue);
				value = formattedValue.value;
				chipTitle = (entryValue - 0.01f) == 0f ? getString(R.string.shared_string_none) : formattedValue.format(app);
			} else {
				String entryValue = preference.getEntries()[i];
				value = entryValue;
				chipTitle = entryValue;
			}

			ChipItem chip = new ChipItem(value);
			chip.title = chipTitle;
			chips.add(chip);
		}
		chipsView.setItems(chips);

		chipsView.setOnSelectChipListener(chip -> {
			selectedItem = chip.id;
			String currentValueStr1;
			if (preference.isLengthAssets()) {
				try {
					currentValue = (float) convertLengthToMeters(app, Float.parseFloat(chip.id));
				} catch (NumberFormatException e){
					currentValue = 0.0f;
				}
				currentValueStr1 = currentValue == 0.0f ? "" : chip.id;
			} else {
				currentValue = preference.getValueFromEntries(selectedItem);
				currentValueStr1 = currentValue == 0.0f
						? "" : df.format(currentValue + 0.01f);
			}
			text.setText(currentValueStr1);
			if (text.hasFocus()) {
				text.setSelection(text.getText().length());
			}
			return true;
		});
		ChipItem selected = chipsView.getChipById(selectedItem);
		chipsView.setSelected(selected);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
	}

	private double convertLengthToMeters(@NonNull OsmandApplication app, float value) {
		MetricsConstants mc = app.getSettings().METRIC_SYSTEM.get();
		float resultValue;
		if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			resultValue = value / FEET_IN_ONE_METER;
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			resultValue = value / YARDS_IN_ONE_METER;
		} else {
			resultValue = value;
		}
		return resultValue;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
		return view;
	}

	private ViewTreeObserver.OnGlobalLayoutListener getOnGlobalLayoutListener() {
		return () -> {
			Rect visibleDisplayFrame = new Rect();
			buttonsHeight = getResources().getDimensionPixelSize(R.dimen.dialog_button_ex_height);
			shadowHeight = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_top_shadow_height);
			scrollView = requireView().findViewById(R.id.scroll_view);
			scrollView.getWindowVisibleDisplayFrame(visibleDisplayFrame);
			int contentHeight = visibleDisplayFrame.bottom - visibleDisplayFrame.top - buttonsHeight;
			if (contentHeightPrevious != contentHeight) {
				boolean showTopShadow;
				if (scrollView.getHeight() + shadowHeight > contentHeight) {
					scrollView.getLayoutParams().height = contentHeight;
					showTopShadow = false;
				} else {
					scrollView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					showTopShadow = true;
				}
				scrollView.requestLayout();
				scrollView.postDelayed(() -> scrollView.scrollTo(0, scrollView.getHeight()), 300);
				contentHeightPrevious = contentHeight;
				drawTopShadow(showTopShadow);
			}
		};
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange) {

			((OnConfirmPreferenceChange) target).onConfirmPreferenceChange(
					getPreference().getKey(), String.valueOf(currentValue), ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fm, String key, Fragment target,
	                                boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, key);
				VehicleParametersBottomSheet fragment = new VehicleParametersBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
