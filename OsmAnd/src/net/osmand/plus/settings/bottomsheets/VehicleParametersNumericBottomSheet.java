package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.Arrays;

public class VehicleParametersNumericBottomSheet extends BasePreferenceBottomSheet {
	public enum VehicleSizeAssets {
		WIDTH(GeneralRouter.VEHICLE_WIDTH, R.drawable.img_help_width_limit_day, R.drawable.img_help_width_limit_night,
				R.string.width_limit_description, R.string.shared_string_meters, R.string.m),
		HEIGHT(GeneralRouter.VEHICLE_HEIGHT, R.drawable.img_help_height_limit_day, R.drawable.img_help_height_limit_night,
				R.string.height_limit_description, R.string.shared_string_meters, R.string.m),
		WEIGHT(GeneralRouter.VEHICLE_WEIGHT, R.drawable.img_help_weight_limit_day, R.drawable.img_help_weight_limit_night,
				R.string.weight_limit_description, R.string.shared_string_tones, R.string.metric_ton);

		String routerParameterName;
		int dayIconId;
		int nightIconId;
		int descriptionRes;
		int metricRes;
		int metricShortRes;

		VehicleSizeAssets(String routerParameterName, int dayIconId, int nightIconId, int descriptionRes, int metricRes,
		                  int metricShortRes) {
			this.routerParameterName = routerParameterName;
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
			this.descriptionRes = descriptionRes;
			this.metricRes = metricRes;
			this.metricShortRes = metricShortRes;
		}

		public static VehicleSizeAssets getAssets(String parameterName) {
			for (VehicleSizeAssets type : VehicleSizeAssets.values()) {
				if (type.routerParameterName.equals(parameterName)) {
					return type;
				}
			}
			return null;
		}

		public int getDayIconId() {
			return dayIconId;
		}

		public int getNightIconId() {
			return nightIconId;
		}

		public int getDescriptionRes() {
			return descriptionRes;
		}

		public int getMetricRes() {
			return metricRes;
		}

		public int getMetricShortRes() {
			return metricShortRes;
		}
	}

	public static final String TAG = VehicleParametersNumericBottomSheet.class.getSimpleName();
	private String selectedItem;
	private float currentValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		SizePreference preference = (SizePreference) getPreference();

		String key = preference.getKey();
		String parameterName = key.substring(key.lastIndexOf("_") + 1);
		VehicleSizeAssets vehicleSizeAssets = VehicleSizeAssets.getAssets(parameterName);
		if (vehicleSizeAssets == null) {
			return;
		}
		items.add(new TitleItem(preference.getTitle().toString()));
		ImageView imageView = new ImageView(getContext());
		imageView.setImageDrawable(ContextCompat.getDrawable(app,
				!nightMode ? vehicleSizeAssets.getDayIconId() : vehicleSizeAssets.getNightIconId()));
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(imageView).create());
		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_margin_small)));
		BaseBottomSheetItem description = new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(vehicleSizeAssets.getDescriptionRes()))
				.setLayoutId(R.layout.bottom_sheet_item_preference_info)
				.create();
		items.add(description);
		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.bottom_sheet_content_margin_small)));
		items.add(createComboView(app, preference));
	}

	private BaseBottomSheetItem createComboView(OsmandApplication app, final SizePreference preference) {
		View mainView = UiUtilities.getMaterialInflater(app, nightMode)
				.inflate(R.layout.bottom_sheet_item_edit_with_recyclerview, null);
		final HorizontalSelectionAdapter adapter = new HorizontalSelectionAdapter(app, nightMode);
		final TextView metric = mainView.findViewById(R.id.metric);
		metric.setText(app.getString(preference.getAssets().getMetricRes()));
		final TextView text = mainView.findViewById(R.id.text_edit);
		currentValue = Float.parseFloat(preference.getValue());
		selectedItem = preference.getEntryFromValue(preference.getValue());

		String currentValueStr = currentValue == 0.0f ? "" : String.valueOf(currentValue + 0.01f);
		text.setText(currentValueStr);
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
					currentValue = Float.parseFloat(s.toString()) - 0.01f;
				} else {
					currentValue = 0.0f;
				}
				selectedItem = preference.getEntryFromValue(String.valueOf(currentValue));
				adapter.setSelectedItem(selectedItem);
			}
		});

		adapter.setItems(Arrays.asList(preference.getEntries()));
		adapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				selectedItem = item;
				currentValue = preference.getValueFromEntries(selectedItem);
				String currentValueStr = currentValue == 0.0f ? "" : String.valueOf(currentValue + 0.01f);
				text.setText(currentValueStr);
				adapter.notifyDataSetChanged();
			}
		});

		RecyclerView recyclerView = mainView.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(adapter);
		adapter.setSelectedItem(selectedItem);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(mainView)
				.create();
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

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
	                                   boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			VehicleParametersNumericBottomSheet fragment = new VehicleParametersNumericBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
