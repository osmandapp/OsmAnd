package net.osmand.plus.settings.bottomsheets;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
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

import com.google.android.material.textfield.TextInputLayout;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.VolumeUnit;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.vehiclesize.FuelCapacityHelper;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class FuelTankCapacityBottomSheet extends BasePreferenceBottomSheet {
	private static final Log LOG = PlatformUtil.getLog(VehicleParametersBottomSheet.class);
	public static final String TAG = FuelTankCapacityBottomSheet.class.getSimpleName();

	private int contentHeightPrevious;
	private TextInputLayout tilCaption;
	private EditText etText;
	private int buttonsHeight;
	private int shadowHeight;
	private ScrollView scrollView;
	private HorizontalChipsView chipsView;

	private VolumeUnit volumeUnit;
	private float currentValue;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, parent, savedInstanceState);
		if (view != null) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(getOnGlobalLayoutListener());
		}
		return view;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			items.add(createBottomSheetItem(app));
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private BaseBottomSheetItem createBottomSheetItem(OsmandApplication app) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View mainView = inflater.inflate(R.layout.bottom_sheet_item_edit_with_chips_view, null);
		volumeUnit = app.getSettings().UNIT_OF_VOLUME.getModeValue(getAppMode());
		List<ChipItem> chips = FuelCapacityHelper.collectChipItems(app, volumeUnit);

		TextView title = mainView.findViewById(R.id.title);
		title.setText(R.string.fuel_tank_capacity);

		ImageView ivImage = mainView.findViewById(R.id.image_view);
		AndroidUiHelper.updateVisibility(ivImage, false);

		TextView tvDescription = mainView.findViewById(R.id.description);
		tvDescription.setText(R.string.fuel_tank_capacity_description);

		TextView tvMetric = mainView.findViewById(R.id.metric);
		tvMetric.setText(volumeUnit.toHumanString(app));

		chipsView = mainView.findViewById(R.id.chips_view);
		etText = mainView.findViewById(R.id.text_edit);
		tilCaption = mainView.findViewById(R.id.text_caption);

		currentValue = FuelCapacityHelper.readSavedValue(app.getSettings(), volumeUnit, getAppMode());
		etText.setText(formatInputValue(currentValue));
		etText.clearFocus();
		etText.setOnTouchListener((v, event) -> {
			etText.onTouchEvent(event);
			etText.setSelection(etText.getText().length());
			return true;
		});

		etText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				currentValue = (float) Algorithms.parseDoubleSilently(s.toString(), 0.0f);
				onCorrectInput();
				updateChips();
			}
		});

		chipsView.setItems(chips);
		chipsView.setOnSelectChipListener(chip -> {
			currentValue = (float) chip.tag;
			etText.setText(formatInputValue(currentValue));
			if (etText.hasFocus()) {
				etText.setSelection(etText.getText().length());
			}
			return true;
		});

		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
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

	private void updateChips() {
		ChipItem selected = chipsView.findChipByTag(currentValue);
		chipsView.setSelected(selected);
		if (selected != null) {
			chipsView.notifyDataSetChanged();
			chipsView.smoothScrollTo(selected);
		}
	}

	private void onCorrectInput() {
		tilCaption.setErrorEnabled(false);
		rightButton.setEnabled(true);
		rightButton.setButtonType(DialogButtonType.PRIMARY);
		rightButton.setTitleId(getRightBottomButtonTextId());
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange callback) {
			String preferenceId = getPreference().getKey();
			Float value = FuelCapacityHelper.prepareValueToSave(volumeUnit, currentValue);
			callback.onConfirmPreferenceChange(preferenceId, value, ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}

	private String formatInputValue(float input) {
		if (input == 0.0f) {
			return "";
		}
		DecimalFormat formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.US));
		return formatter.format(input);
	}

	public static void showInstance(@NonNull FragmentManager fm, String key, Fragment target,
	                                boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, key);
				FuelTankCapacityBottomSheet fragment = new FuelTankCapacityBottomSheet();
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
