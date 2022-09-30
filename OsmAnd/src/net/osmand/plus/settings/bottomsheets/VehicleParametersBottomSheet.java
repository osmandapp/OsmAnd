package net.osmand.plus.settings.bottomsheets;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SizePreference;
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
		selectedItem = preference.getEntryFromValue(preference.getValue());
		String currentValueStr = currentValue == 0.0f ? "" : df.format(currentValue + 0.01f);
		text.setText(currentValueStr);
		text.clearFocus();
		text.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				text.onTouchEvent(event);
				text.setSelection(text.getText().length());
				return true;
			}
		});
		text.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (!Algorithms.isEmpty(s)) {
					try {
						currentValue = Float.parseFloat(s.toString()) - 0.01f;
					} catch (NumberFormatException e) {
						currentValue = 0.0f;
					}
				} else {
					currentValue = 0.0f;
				}
				selectedItem = preference.getEntryFromValue(String.valueOf(currentValue));
				ChipItem selected = chipsView.getChipById(selectedItem);
				chipsView.setSelected(selected);
				if (selected != null) {
					chipsView.notifyDataSetChanged();
					chipsView.smoothScrollTo(selected);
				}
			}
		});

		List<ChipItem> chips = new ArrayList<>();
		for (String entry : preference.getEntries()) {
			ChipItem chip = new ChipItem(entry);
			chip.title = entry;
			chips.add(chip);
		}
		chipsView.setItems(chips);

		chipsView.setOnSelectChipListener(chip -> {
			selectedItem = chip.id;
			currentValue = preference.getValueFromEntries(selectedItem);
			String currentValueStr1 = currentValue == 0.0f
					? "" : df.format(currentValue + 0.01f);
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
