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
import android.widget.CompoundButton;
import android.widget.TextView;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;

import java.util.ArrayList;

public class DashboardSettingsDialogFragment extends DialogFragment {
	private static final String CHECKED_ITEMS = "checked_items";
	private MapActivity mapActivity;
	private DashFragmentData[] fragmentsData;
	private DashFragmentAdapter adapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mapActivity = (MapActivity) activity;
		ArrayList<DashFragmentData> fragmentsList = new ArrayList<>();
		for(DashFragmentData fragmentData : mapActivity.getDashboard().getFragmentsData()) {
			if (fragmentData.canHideFunction == null || fragmentData.canHideFunction.canHide())
				fragmentsList.add(fragmentData);
		}
		fragmentsData = fragmentsList.toArray(new DashFragmentData[fragmentsList.size()]);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		if (savedInstanceState != null && savedInstanceState.containsKey(CHECKED_ITEMS)) {
			adapter = new DashFragmentAdapter(getActivity(), fragmentsData,
					savedInstanceState.getBooleanArray(CHECKED_ITEMS));
		} else {
			adapter = new DashFragmentAdapter(getActivity(), fragmentsData,
					settings);
		}
		builder.setTitle(R.string.dahboard_options_dialog_title)
				.setAdapter(adapter, null)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int type) {
						boolean[] shouldShow = adapter.getCheckedItems();
						for (int i = 0; i < shouldShow.length; i++) {
							settings.registerBooleanPreference(
									DashboardOnMap.SHOULD_SHOW + fragmentsData[i].tag, true)
									.makeGlobal().set(shouldShow[i]);
						}
						mapActivity.getDashboard().refreshDashboardFragments();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		return builder.create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBooleanArray(CHECKED_ITEMS, adapter.getCheckedItems());
		super.onSaveInstanceState(outState);
	}

	private static class DashFragmentAdapter extends ArrayAdapter<DashFragmentData> {
		private final boolean[] checkedItems;

		public DashFragmentAdapter(Context context, DashFragmentData[] objects, boolean[] checkedItems) {
			super(context, 0, objects);
			this.checkedItems = checkedItems;
		}

		public DashFragmentAdapter(Context context, DashFragmentData[] objects, OsmandSettings settings) {
			super(context, 0, objects);
			checkedItems = new boolean[objects.length];
			for (int i = 0; i < objects.length; i++) {
				checkedItems[i] = settings.registerBooleanPreference(
						DashboardOnMap.SHOULD_SHOW + objects[i].tag, true).makeGlobal().get();
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			DashFragmentData dashFragmentData = getItem(position);
			DashViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new DashViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.dashboard_settings_dialog_item, parent, false);
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
		}
	}
}
