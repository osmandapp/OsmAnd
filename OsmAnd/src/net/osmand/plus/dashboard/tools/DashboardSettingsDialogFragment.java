package net.osmand.plus.dashboard.tools;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.NumberPickerDialogFragment.CanAcceptNumber;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DashboardSettingsDialogFragment extends BaseAlertDialogFragment implements CanAcceptNumber {

	private static final String TAG = DashboardSettingsDialogFragment.class.getSimpleName();
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(NumberPickerDialogFragment.class);
	private static final String CHECKED_ITEMS = "checked_items";
	private static final String NUMBER_OF_ROWS_ARRAY = "number_of_rows_array";
	private MapActivity mapActivity;
	private ArrayList<DashFragmentData> mFragmentsData;
	private DashFragmentAdapter mAdapter;
	private boolean usedOnMap;
	private static final int MAXIMUM_NUMBER_OF_ROWS = 10;
	private static final int DEFAULT_NUMBER_OF_ROWS = 5;

	@ColorInt private int textColorPrimary;
	@ColorInt private int textColorSecondary;
	@ColorInt private int activeColor;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		mapActivity = (MapActivity) context;
		mFragmentsData = new ArrayList<>();
		for (DashFragmentData fragmentData : mapActivity.getDashboard().getFragmentsData()) {
			if (fragmentData.canBeDisabled()) mFragmentsData.add(fragmentData);
		}
		mFragmentsData.addAll(PluginsHelper.getPluginsCardsList());
		Collections.sort(mFragmentsData);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		View showDashboardOnStart = createCheckboxItem(settings.SHOW_DASHBOARD_ON_START,
				R.string.show_on_start, R.string.show_on_start_description);
		View accessFromMap = createCheckboxItem(settings.SHOW_DASHBOARD_ON_MAP_SCREEN,
				R.string.access_from_map, R.string.access_from_map_description);

		textColorPrimary = ColorUtilities.getPrimaryTextColor(app, nightMode);
		textColorSecondary = ColorUtilities.getSecondaryTextColor(app, nightMode);
		activeColor = ColorUtilities.getActiveColor(app, nightMode);

		if (savedInstanceState != null && savedInstanceState.containsKey(CHECKED_ITEMS)) {
			mAdapter = new DashFragmentAdapter(getThemedContext(), mFragmentsData,
					Objects.requireNonNull(savedInstanceState.getBooleanArray(CHECKED_ITEMS)),
					Objects.requireNonNull(savedInstanceState.getIntArray(NUMBER_OF_ROWS_ARRAY)));
		} else {
			mAdapter = new DashFragmentAdapter(getThemedContext(), mFragmentsData, settings);
		}

		AlertDialog.Builder builder = createDialogBuilder();
		builder.setTitle(R.string.dahboard_options_dialog_title)
				.setAdapter(mAdapter, null)
				.setPositiveButton(R.string.shared_string_apply, (dialog, type) -> {
					boolean[] shouldShow = mAdapter.getCheckedItems();
					int[] numberOfRows = mAdapter.getNumbersOfRows();
					for (int i = 0; i < shouldShow.length; i++) {
						DashFragmentData fragmentData = mFragmentsData.get(i);
						settings.registerBooleanPreference(DashboardOnMap.SHOULD_SHOW + fragmentData.tag, true).makeGlobal().set(shouldShow[i]);
						if (fragmentData.rowNumberTag != null) {
							settings.registerIntPreference(fragmentData.rowNumberTag, DEFAULT_NUMBER_OF_ROWS).makeGlobal().set(numberOfRows[i]);
						}
					}
					mapActivity.getDashboard().refreshDashboardFragments();
					settings.SHOW_DASHBOARD_ON_START.set(
							((CompoundButton) showDashboardOnStart.findViewById(R.id.toggle_item)).isChecked());
					settings.SHOW_DASHBOARD_ON_MAP_SCREEN.set(
							((CompoundButton) accessFromMap.findViewById(R.id.toggle_item)).isChecked());
					mapActivity.getMapLayers().getMapControlsLayer().refreshButtons();
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		AlertDialog dialog = builder.create();

		ListView listView = dialog.getListView();
		listView.addHeaderView(showDashboardOnStart);
		listView.addHeaderView(accessFromMap);
		return dialog;
	}

	private View createCheckboxItem(CommonPreference<Boolean> pref, int text, int description) {
		View view = inflate(R.layout.show_dashboard_on_start_dialog_item);
		TextView textView = view.findViewById(R.id.text);
		TextView subtextView = view.findViewById(R.id.subtext);
		textView.setText(text);
		subtextView.setText(description);
		CompoundButton compoundButton = view.findViewById(R.id.toggle_item);
		compoundButton.setChecked(pref.get());
		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		UiUtilities.setupCompoundButton(compoundButton, isNightMode(), UiUtilities.CompoundButtonType.GLOBAL);
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBooleanArray(CHECKED_ITEMS, mAdapter.getCheckedItems());
		outState.putIntArray(NUMBER_OF_ROWS_ARRAY, mAdapter.getNumbersOfRows());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void acceptNumber(String tag, int number) {
		mAdapter.getNumbersOfRows()[Integer.parseInt(tag)] = number;
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected boolean isUsedOnMap() {
		return usedOnMap;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DashboardSettingsDialogFragment fragment = new DashboardSettingsDialogFragment();
			fragment.usedOnMap = usedOnMap;
			fragment.show(fragmentManager, TAG);
		}
	}

	private class DashFragmentAdapter extends ArrayAdapter<DashFragmentData> {
		private final boolean[] checkedItems;
		private final int[] numbersOfRows;

		public DashFragmentAdapter(@NonNull Context context, @NonNull List<DashFragmentData> objects,
		                           @NonNull boolean[] checkedItems, @NonNull int[] numbersOfRows) {
			super(context, 0, objects);
			this.checkedItems = checkedItems;
			this.numbersOfRows = numbersOfRows;

		}

		public DashFragmentAdapter(@NonNull Context context, @NonNull List<DashFragmentData> objects,
		                           @NonNull OsmandSettings settings) {
			super(context, 0, objects);
			numbersOfRows = new int[objects.size()];
			checkedItems = new boolean[objects.size()];
			for (int i = 0; i < objects.size(); i++) {
				checkedItems[i] = settings.registerBooleanPreference(DashboardOnMap.SHOULD_SHOW + objects.get(i).tag, true).makeGlobal().get();
				if (objects.get(i).tag != null) {
					numbersOfRows[i] = settings.registerIntPreference(objects.get(i).rowNumberTag, 5).makeGlobal().get();
				}
			}
		}

		@Override
		@NonNull
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			DashViewHolder viewHolder;
			if (convertView == null) {
				convertView = inflate(R.layout.dashboard_settings_dialog_item, parent, false);
				viewHolder = new DashViewHolder(this, convertView);
			} else {
				viewHolder = (DashViewHolder) convertView.getTag();
			}
			viewHolder.bindDashView(Objects.requireNonNull(getItem(position)), position);
			convertView.setTag(viewHolder);
			return convertView;
		}

		public boolean[] getCheckedItems() {
			return checkedItems;
		}

		public int[] getNumbersOfRows() {
			return numbersOfRows;
		}

		public boolean isChecked(int position) {
			return checkedItems[position];
		}

		public int getNumberOfRows(int position) {
			return numbersOfRows[position];
		}

		final CompoundButton.OnCheckedChangeListener onTurnedOnOffListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				DashViewHolder localViewHolder = (DashViewHolder) compoundButton.getTag();
				if (localViewHolder == null) {
					return;
				}
				int position = localViewHolder.position;
				checkedItems[position] = b;
				localViewHolder.bindDashView(Objects.requireNonNull(getItem(position)), position);
			}
		};

		final View.OnClickListener onNumberClickListener = v -> {
			DashViewHolder localViewHolder = (DashViewHolder) v.getTag();
			DashFragmentData item = Objects.requireNonNull(getItem(localViewHolder.position));
			String header = getString(item.shouldShowFunction.getTitleId());
			String subheader = getString(R.string.count_of_lines);
			String stringPosition = String.valueOf(localViewHolder.position);
			int numberOfRows = getNumberOfRows(localViewHolder.position);
			NumberPickerDialogFragment.showInstance(getChildFragmentManager(),
					header, subheader, stringPosition, numberOfRows, MAXIMUM_NUMBER_OF_ROWS, true);
		};

	}

	private class DashViewHolder {
		final View view;
		final TextView textView;
		final CompoundButton compoundButton;
		final TextView numberOfRowsTextView;
		private int position;
		private final DashFragmentAdapter dashFragmentAdapter;

		public DashViewHolder(@NonNull DashFragmentAdapter dashFragmentAdapter, @NonNull View view) {
			this.view = view;
			this.dashFragmentAdapter = dashFragmentAdapter;
			this.numberOfRowsTextView = view.findViewById(R.id.numberOfRowsTextView);
			this.textView = view.findViewById(R.id.text);
			this.compoundButton = view.findViewById(R.id.toggle_item);
		}

		public void bindDashView(@NonNull DashFragmentData fragmentData, int position) {
			boolean checked = dashFragmentAdapter.isChecked(position);
			if (fragmentData.hasRows()) {
				numberOfRowsTextView.setVisibility(View.VISIBLE);
				numberOfRowsTextView.setText(String.valueOf(dashFragmentAdapter.getNumberOfRows(position)));
				numberOfRowsTextView.setTextColor(checked ? activeColor : textColorSecondary);
			} else {
				numberOfRowsTextView.setVisibility(View.GONE);
			}
			textView.setText(fragmentData.shouldShowFunction.getTitleId());
			textView.setTextColor(checked ? textColorPrimary : textColorSecondary);
			this.position = position;

			view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
			compoundButton.setChecked(checked);
			compoundButton.setTag(this);
			compoundButton.setOnCheckedChangeListener(dashFragmentAdapter.onTurnedOnOffListener);
			UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

			numberOfRowsTextView.setTag(this);
			numberOfRowsTextView.setOnClickListener(dashFragmentAdapter.onNumberClickListener);
		}
	}
}
