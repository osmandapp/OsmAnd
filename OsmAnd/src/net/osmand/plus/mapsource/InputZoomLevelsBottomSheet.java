package net.osmand.plus.mapsource;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

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

	public static void showInstance(@NonNull FragmentManager fm,
									@Nullable Fragment targetFragment,
									int sliderDescr,
									int dialogDescr,
									int minZoom,
									int maxZoom,
									boolean newMapSource) {
		InputZoomLevelsBottomSheet bottomSheet = new InputZoomLevelsBottomSheet();
		bottomSheet.setTargetFragment(targetFragment, 0);
		bottomSheet.setSliderDescrRes(sliderDescr);
		bottomSheet.setDialogDescrRes(dialogDescr);
		bottomSheet.setMinZoom(Math.max(minZoom, SLIDER_FROM));
		bottomSheet.setMaxZoom(Math.min(maxZoom, SLIDER_TO));
		bottomSheet.setNewMapSource(newMapSource);
		bottomSheet.show(fm, TAG);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		if (savedInstanceState != null) {
			minZoom = savedInstanceState.getInt(MIN_ZOOM_KEY);
			maxZoom = savedInstanceState.getInt(MAX_ZOOM_KEY);
			dialogDescrRes = savedInstanceState.getInt(DIALOG_DESCR_RES_KEY);
			sliderDescrRes = savedInstanceState.getInt(SLIDER_DESCR_RES_KEY);
			newMapSource = savedInstanceState.getBoolean(NEW_MAP_SOURCE);
		}
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		TitleItem titleItem = new TitleItem(getString(R.string.shared_string_zoom_levels));
		items.add(titleItem);
		final View sliderView = inflater.inflate(R.layout.zoom_levels_with_descr, null);
		((TextView) sliderView.findViewById(R.id.slider_descr)).setText(sliderDescrRes);
		TextView dialogDescrTv = sliderView.findViewById(R.id.dialog_descr);
		if (dialogDescrRes == R.string.map_source_zoom_levels_descr) {
			String mapSource = getString(R.string.map_source);
			String overlayUnderlay = getString(R.string.pref_overlay);
			String dialogDesr = getString(dialogDescrRes, mapSource, overlayUnderlay);
			dialogDescrTv.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), dialogDesr, mapSource, overlayUnderlay));
		} else {
			dialogDescrTv.setText(getString(dialogDescrRes));
		}
		final TextView minZoomValue = sliderView.findViewById(R.id.zoom_value_min);
		minZoomValue.setText(String.valueOf(minZoom));
		final TextView maxZoomValue = sliderView.findViewById(R.id.zoom_value_max);
		maxZoomValue.setText(String.valueOf(maxZoom));
		RangeSlider slider = sliderView.findViewById(R.id.zoom_slider);
		int colorProfile = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, colorProfile, true);
		slider.setValueFrom(SLIDER_FROM);
		slider.setValueTo(SLIDER_TO);
		slider.setValues((float) minZoom, (float) maxZoom);
		slider.addOnChangeListener(new RangeSlider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
				List<Float> values = slider.getValues();
				if (values.size() > 0) {
					minZoomValue.setText(String.valueOf(values.get(0).intValue()));
					maxZoomValue.setText(String.valueOf(values.get(1).intValue()));
				}
			}
		});
		slider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
			@Override
			public void onStartTrackingTouch(@NonNull RangeSlider slider) {
			}

			@Override
			public void onStopTrackingTouch(@NonNull RangeSlider slider) {
				List<Float> values = slider.getValues();
				if (values.size() > 0) {
					minZoom = values.get(0).intValue();
					maxZoom = values.get(1).intValue();
				}
			}
		});
		final SimpleBottomSheetItem sliderItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create();
		items.add(sliderItem);
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
			showClearTilesWarningDialog(requireActivity(), nightMode, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					applySelectedZooms();
				}
			});
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
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnZoomSetListener) {
			((OnZoomSetListener) fragment).onZoomSet(minZoom, maxZoom);
		}
		dismiss();
	}

	private SpannableString createSpannableString(@NonNull String text, @NonNull String... textToStyle) {
		SpannableString spannable = new SpannableString(text);
		for (String t : textToStyle) {
			try {
				int startIndex = text.indexOf(t);
				spannable.setSpan(
						new CustomTypefaceSpan(FontCache.getRobotoMedium(requireContext())),
						startIndex,
						startIndex + t.length(),
						Spanned.SPAN_INCLUSIVE_INCLUSIVE);
			} catch (RuntimeException e) {
				LOG.error("Error trying to find index of " + t + " " + e);
			}
		}
		return spannable;
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
}
