package net.osmand.plus.mapsource;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

import org.apache.commons.logging.Log;

import java.util.List;

public class InputZoomLevelsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = InputZoomLevelsBottomSheet.class.getName();
	private static final Log LOG = PlatformUtil.getLog(InputZoomLevelsBottomSheet.class);
	private static final String MIN_ZOOM_KEY = "min_zoom_key";
	private static final String MAX_ZOOM_KEY = "max_zoom_key";
	private static final String SLIDER_DESCR_RES_KEY = "slider_descr_key";
	private static final String DIALOG_DESCR_RES_KEY = "dialog_descr_key";
	private static final String NEW_MAP_SOURCE = "new_map_source";
	private static final int SLIDER_FROM = 1;
	private static final int SLIDER_TO = 22;
	@StringRes
	private int sliderDescrRes;
	@StringRes
	private int dialogDescrRes;
	private int minZoom;
	private int maxZoom;
	private boolean newMapSource;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			minZoom = savedInstanceState.getInt(MIN_ZOOM_KEY);
			maxZoom = savedInstanceState.getInt(MAX_ZOOM_KEY);
			dialogDescrRes = savedInstanceState.getInt(DIALOG_DESCR_RES_KEY);
			sliderDescrRes = savedInstanceState.getInt(SLIDER_DESCR_RES_KEY);
			newMapSource = savedInstanceState.getBoolean(NEW_MAP_SOURCE);
		}
		TitleItem titleItem = new TitleItem(getString(R.string.shared_string_zoom_levels));
		items.add(titleItem);
		View sliderView = inflate(R.layout.zoom_levels_with_descr);
		((TextView) sliderView.findViewById(R.id.slider_descr)).setText(sliderDescrRes);
		TextView dialogDescrTv = sliderView.findViewById(R.id.dialog_descr);
		if (dialogDescrRes == R.string.map_source_zoom_levels_descr) {
			String mapSource = getString(R.string.map_source);
			String overlayUnderlay = getString(R.string.pref_overlay);
			String dialogDesr = getString(dialogDescrRes, mapSource, overlayUnderlay);
			dialogDescrTv.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), dialogDesr, mapSource, overlayUnderlay));
		} else {
			dialogDescrTv.setText(getString(dialogDescrRes));
		}
		TextView minZoomValue = sliderView.findViewById(R.id.zoom_value_min);
		minZoomValue.setText(String.valueOf(minZoom));
		TextView maxZoomValue = sliderView.findViewById(R.id.zoom_value_max);
		maxZoomValue.setText(String.valueOf(maxZoom));
		RangeSlider slider = sliderView.findViewById(R.id.zoom_slider);
		int colorProfile = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, colorProfile, true);
		slider.setValueFrom(SLIDER_FROM);
		slider.setValueTo(SLIDER_TO);
		slider.setValues((float) minZoom, (float) maxZoom);
		slider.addOnChangeListener((slider1, value, fromUser) -> {
			List<Float> values = slider1.getValues();
			if (!values.isEmpty()) {
				minZoomValue.setText(String.valueOf(values.get(0).intValue()));
				maxZoomValue.setText(String.valueOf(values.get(1).intValue()));
			}
		});
		slider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
			@Override
			public void onStartTrackingTouch(@NonNull RangeSlider slider) {
			}

			@Override
			public void onStopTrackingTouch(@NonNull RangeSlider slider) {
				List<Float> values = slider.getValues();
				if (!values.isEmpty()) {
					minZoom = values.get(0).intValue();
					maxZoom = values.get(1).intValue();
				}
			}
		});
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(sliderView).create());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(MIN_ZOOM_KEY, minZoom);
		outState.putInt(MAX_ZOOM_KEY, maxZoom);
		outState.putInt(SLIDER_DESCR_RES_KEY, sliderDescrRes);
		outState.putInt(DIALOG_DESCR_RES_KEY, dialogDescrRes);
		outState.putBoolean(NEW_MAP_SOURCE, newMapSource);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (!newMapSource) {
			showClearTilesWarningDialog(requireActivity(), nightMode, (dialog, which) -> applySelectedZooms());
		} else {
			applySelectedZooms();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	public static void showClearTilesWarningDialog(Activity activity, boolean nightMode, DialogInterface.OnClickListener onPositiveListener) {
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(activity.getString(R.string.osmand_parking_warning));
		dismissDialog.setMessage(activity.getString(R.string.clear_tiles_warning));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_continue, onPositiveListener);
		dismissDialog.show();
	}

	private void applySelectedZooms() {
		if (getTargetFragment() instanceof OnZoomSetListener listener) {
			listener.onZoomSet(minZoom, maxZoom);
		}
		dismiss();
	}

	private void setSliderDescrRes(int sliderDescrRes) {
		this.sliderDescrRes = sliderDescrRes;
	}

	private void setDialogDescrRes(int dialogDescrRes) {
		this.dialogDescrRes = dialogDescrRes;
	}

	private void setMinZoom(int minZoom) {
		this.minZoom = minZoom;
	}

	private void setMaxZoom(int maxZoom) {
		this.maxZoom = maxZoom;
	}

	public void setNewMapSource(boolean newMapSource) {
		this.newMapSource = newMapSource;
	}

	public interface OnZoomSetListener {
		void onZoomSet(int min, int max);
	}

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment,
	                                int sliderDescr, int dialogDescr, int minZoom, int maxZoom,
	                                boolean newMapSource) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			InputZoomLevelsBottomSheet bottomSheet = new InputZoomLevelsBottomSheet();
			bottomSheet.setTargetFragment(targetFragment, 0);
			bottomSheet.setSliderDescrRes(sliderDescr);
			bottomSheet.setDialogDescrRes(dialogDescr);
			bottomSheet.setMinZoom(Math.max(minZoom, SLIDER_FROM));
			bottomSheet.setMaxZoom(Math.min(maxZoom, SLIDER_TO));
			bottomSheet.setNewMapSource(newMapSource);
			bottomSheet.show(fm, TAG);
		}
	}
}
