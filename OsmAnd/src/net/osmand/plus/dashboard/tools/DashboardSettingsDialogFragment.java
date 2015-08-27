package net.osmand.plus.dashboard.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardSettingsDialogFragment extends DialogFragment {
	private static final String CHECKED_ITEMS = "checked_items";
	private MapActivity mapActivity;
	private ArrayList<DashFragmentData> mFragmentsData;
	private DashFragmentAdapter mAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mapActivity = (MapActivity) activity;
		mFragmentsData = new ArrayList<>();
		for(DashFragmentData fragmentData : mapActivity.getDashboard().getFragmentsData()) {
			if (!fragmentData.customDeletionLogic) mFragmentsData.add(fragmentData);
		}
		mFragmentsData.addAll(OsmandPlugin.getPluginsCardsList());
		Collections.sort(mFragmentsData);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();

		View view = LayoutInflater.from(getActivity()).inflate(
				R.layout.dashboard_settings_dialog_item, null, false);
		final TextView textView = (TextView) view.findViewById(R.id.text);
		textView.setText("Show on start");
		final OsmandSettings.CommonPreference<Boolean> shouldShowDashboardOnStart =
				settings.registerBooleanPreference(MapActivity.SHOULD_SHOW_DASHBOARD_ON_START, true);
		final CompoundButton compoundButton = (CompoundButton) view.findViewById(R.id.check_item);
		compoundButton.setChecked(shouldShowDashboardOnStart.get());
		textView.setTextColor(shouldShowDashboardOnStart.get() ? 0xFF212121 : 0xFF8c8c8c);
		compoundButton.setOnCheckedChangeListener(
				new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						textView.setTextColor(b ? 0xFF212121 : 0xFF8c8c8c);
					}
				});

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if (savedInstanceState != null && savedInstanceState.containsKey(CHECKED_ITEMS)) {
			mAdapter = new DashFragmentAdapter(getActivity(), mFragmentsData,
					savedInstanceState.getBooleanArray(CHECKED_ITEMS),
					new int[mFragmentsData.size()]);
		} else {
			mAdapter = new DashFragmentAdapter(getActivity(), mFragmentsData,
					settings);
		}
		builder.setTitle(R.string.dahboard_options_dialog_title)
				.setAdapter(mAdapter, null)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int type) {
						boolean[] shouldShow = mAdapter.getCheckedItems();
						for (int i = 0; i < shouldShow.length; i++) {
							settings.registerBooleanPreference(
									DashboardOnMap.SHOULD_SHOW + mFragmentsData.get(i).tag, true)
									.makeGlobal().set(shouldShow[i]);
						}
						mapActivity.getDashboard().refreshDashboardFragments();
						shouldShowDashboardOnStart.set(compoundButton.isChecked());
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		final AlertDialog dialog = builder.create();

		ListView listView = dialog.getListView();
		listView.addHeaderView(view);
		return dialog;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBooleanArray(CHECKED_ITEMS, mAdapter.getCheckedItems());
		super.onSaveInstanceState(outState);
	}

	private static class DashFragmentAdapter extends ArrayAdapter<DashFragmentData> {
		private final boolean[] checkedItems;
		private final int[] numbersOfRows;

		public DashFragmentAdapter(Context context, List<DashFragmentData> objects,
								   boolean[] checkedItems, int[] numbersOfRows) {
			super(context, 0, objects);
			this.checkedItems = checkedItems;
			this.numbersOfRows = numbersOfRows;
		}

		public DashFragmentAdapter(Context context, List<DashFragmentData> objects,
								   OsmandSettings settings) {
			super(context, 0, objects);
			numbersOfRows = new int[objects.size()];
			checkedItems = new boolean[objects.size()];
			for (int i = 0; i < objects.size(); i++) {
				checkedItems[i] = settings.registerBooleanPreference(
						DashboardOnMap.SHOULD_SHOW + objects.get(i).tag, true).makeGlobal().get();
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position).rowNumberTag == null ? 0 : 1;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			DashFragmentData dashFragmentData = getItem(position);
			DashViewHolder viewHolder;
			boolean hasRows = false;//getItemViewType(position) == 1;
			if (convertView == null) {
				viewHolder = new DashViewHolder();
				if (hasRows) {
					convertView = LayoutInflater.from(getContext()).inflate(
							R.layout.dashboard_settings_dialog_item_1, parent, false);

					viewHolder.numberOfRowsTextView = (TextView) convertView.findViewById(R.id.numberOfRowsTextView);
					viewHolder.decrementButton = (Button) convertView.findViewById(R.id.decrementButton);
					viewHolder.incrementButton = (Button) convertView.findViewById(R.id.incrementButton);
				} else {
					convertView = LayoutInflater.from(getContext()).inflate(
							R.layout.dashboard_settings_dialog_item, parent, false);
				}
				viewHolder.textView = (TextView) convertView.findViewById(R.id.text);
				viewHolder.compoundButton = (CompoundButton) convertView.findViewById(R.id.check_item);
				viewHolder.compoundButton.setOnCheckedChangeListener(
						new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
								DashViewHolder localViewHolder = (DashViewHolder) compoundButton.getTag();
								checkedItems[localViewHolder.position] = b;
								localViewHolder.textView.setTextColor(
										checkedItems[localViewHolder.position] ? 0xFF212121
												: 0xFF8c8c8c);
							}
						});
			} else {
				viewHolder = (DashViewHolder) convertView.getTag();
			}
			viewHolder.position = position;
			viewHolder.compoundButton.setTag(viewHolder);
			viewHolder.compoundButton.setChecked(checkedItems[position]);
			viewHolder.textView.setText(dashFragmentData.title);
			viewHolder.textView.setTextColor(checkedItems[position] ? 0xFF212121 : 0xFF8c8c8c);
			convertView.setTag(viewHolder);
			return convertView;
		}

		public boolean[] getCheckedItems() {
			return checkedItems;
		}

		private class DashViewHolder {
			TextView textView;
			CompoundButton compoundButton;
			int position;
			TextView numberOfRowsTextView;
			Button decrementButton;
			Button incrementButton;
		}
	}
}
