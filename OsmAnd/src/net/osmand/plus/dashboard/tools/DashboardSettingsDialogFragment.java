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

public class DashboardSettingsDialogFragment extends DialogFragment {
	private MapActivity mapActivity;
	private DashFragmentData[] fragmentsData;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mapActivity = (MapActivity) activity;
		fragmentsData = mapActivity.getDashboard().getFragmentsData();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final DashFragmentAdapter adapter =
				new DashFragmentAdapter(getActivity(), fragmentsData,
						settings);
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
				});
		final AlertDialog dialog = builder.create();
		return dialog;
	}

	private static class DashFragmentAdapter extends ArrayAdapter<DashFragmentData> {
		private final boolean[] checkedItems;

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
								Integer position = (Integer) compoundButton.getTag();
								checkedItems[position] = b;
							}
						});
			} else {
				viewHolder = (DashViewHolder) convertView.getTag();
			}
			viewHolder.compoundButton.setTag(position);
			viewHolder.compoundButton.setChecked(checkedItems[position]);
			viewHolder.textView.setText(dashFragmentData.title);
			convertView.setTag(viewHolder);
			return convertView;
		}

		public boolean[] getCheckedItems() {
			return checkedItems;
		}

		private class DashViewHolder {
			TextView textView;
			CompoundButton compoundButton;
		}
	}
}
