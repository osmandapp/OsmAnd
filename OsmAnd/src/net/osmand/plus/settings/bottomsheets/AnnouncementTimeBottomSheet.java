package net.osmand.plus.settings.bottomsheets;

import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet.SELECTED_ENTRY_INDEX_KEY;


public class AnnouncementTimeBottomSheet extends BasePreferenceBottomSheet
		implements SeekBar.OnSeekBarChangeListener {

	public static final String TAG = AnnouncementTimeBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(AnnouncementTimeBottomSheet.class);

	private OsmandApplication app;
	private AnnounceTimeDistances announceTimeDistances;

	private ListPreferenceEx listPreference;
	private int selectedEntryIndex = -1;

	private TextViewEx tvSeekBarLabel;
	private SeekBar seekBarArrival;
	private ImageView ivArrow;
	private TextViewEx tvIntervalsDescr;

	private boolean collapsed = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		announceTimeDistances = new AnnounceTimeDistances(getAppMode(), app.getSettings());

		listPreference = getListPreference();
		if (listPreference == null || listPreference.getEntries() == null ||
				listPreference.getEntryValues() == null) {
			return;
		}
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
		} else {
			selectedEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
		}

		items.add(createBottomSheetItem());
	}

	@Override
	public void onResume() {
		super.onResume();
		updateViews();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Object[] entryValues = listPreference.getEntryValues();
		if (entryValues != null && selectedEntryIndex >= 0) {
			Object value = entryValues[selectedEntryIndex];
			if (listPreference.callChangeListener(value)) {
				listPreference.setValue(value);
			}
			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
			}
		}

		dismiss();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (progress != selectedEntryIndex) {
			selectedEntryIndex = progress;
			updateViews();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	private ListPreferenceEx getListPreference() {
		return (ListPreferenceEx) getPreference();
	}

	private BaseBottomSheetItem createBottomSheetItem() {
		View rootView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_announcement_time, null);

		tvSeekBarLabel = rootView.findViewById(R.id.tv_seek_bar_label);
		seekBarArrival = rootView.findViewById(R.id.seek_bar_arrival);
		ivArrow = rootView.findViewById(R.id.iv_arrow);
		tvIntervalsDescr = rootView.findViewById(R.id.tv_interval_descr);

		setProfileColorToSeekBar();
		seekBarArrival.setOnSeekBarChangeListener(this);
		seekBarArrival.setProgress(selectedEntryIndex);
		seekBarArrival.setMax(listPreference.getEntries().length - 1);
		rootView.findViewById(R.id.description_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleDescriptionVisibility();
			}
		});

		return new BaseBottomSheetItem.Builder()
				.setCustomView(rootView)
				.create();
	}

	private void updateViews() {
		seekBarArrival.setProgress(selectedEntryIndex);
		tvSeekBarLabel.setText(listPreference.getEntries()[selectedEntryIndex]);

		float value = (float) listPreference.getEntryValues()[selectedEntryIndex];
		announceTimeDistances.setArrivalDistances(value);
		tvIntervalsDescr.setText(announceTimeDistances.getIntervalsDescription(app));
	}

	private void toggleDescriptionVisibility() {
		collapsed = !collapsed;
		ivArrow.setImageResource(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		AndroidUiHelper.updateVisibility(tvIntervalsDescr, !collapsed);
	}

	private void setProfileColorToSeekBar() {
		int color = getAppMode().getProfileColor(nightMode);

		LayerDrawable seekBarProgressLayer =
				(LayerDrawable) ContextCompat.getDrawable(app, R.drawable.seekbar_progress_announcement_time);

		GradientDrawable background = (GradientDrawable) seekBarProgressLayer.findDrawableByLayerId(R.id.background);
		background.setColor(color);
		background.setAlpha(70);

		GradientDrawable progress = (GradientDrawable) seekBarProgressLayer.findDrawableByLayerId(R.id.progress);
		progress.setColor(color);
		Drawable clippedProgress = new ClipDrawable(progress, Gravity.CENTER_VERTICAL | Gravity.START, 1);

		seekBarArrival.setProgressDrawable(new LayerDrawable(new Drawable[] {
				background, clippedProgress
		}));

		LayerDrawable seekBarThumpLayer =
				(LayerDrawable) ContextCompat.getDrawable(app, R.drawable.seekbar_thumb_announcement_time);
		GradientDrawable thump = (GradientDrawable) seekBarThumpLayer.findDrawableByLayerId(R.id.thump);
		thump.setColor(color);
		seekBarArrival.setThumb(thump);
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefKey, Fragment target,
									@Nullable ApplicationMode appMode, boolean usedOnMap) {
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefKey);
				AnnouncementTimeBottomSheet fragment = new AnnouncementTimeBottomSheet();
				fragment.setArguments(args);
				fragment.setAppMode(appMode);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}