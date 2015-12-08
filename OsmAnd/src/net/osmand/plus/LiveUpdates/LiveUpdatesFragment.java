package net.osmand.plus.liveupdates;


import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;

import java.util.Comparator;
import java.util.List;

public class LiveUpdatesFragment extends Fragment {
	public static final String TITILE = "Live Updates";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		View header = inflater.inflate(R.layout.live_updates_header, listView, false);
		listView.addHeaderView(header);
		LiveUpdatesAdapter adapter = new LiveUpdatesAdapter(this);
		listView.setAdapter(adapter);
		new LoadLocalIndexTask(adapter, (ActionBarProgressActivity) getActivity()).execute();
		return view;
	}

	private OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	private static class LiveUpdatesAdapter extends ArrayAdapter<LocalIndexInfo> {
		final LiveUpdatesFragment fragment;
		public LiveUpdatesAdapter(LiveUpdatesFragment fragment) {
			super(fragment.getActivity(), R.layout.local_index_list_item, R.id.nameTextView);
			this.fragment = fragment;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(getContext());
				view = inflater.inflate(R.layout.local_index_list_item, parent, false);
				view.setTag(new LocalFullMapsViewHolder(view, fragment));
			}
			LocalFullMapsViewHolder viewHolder = (LocalFullMapsViewHolder) view.getTag();
			viewHolder.bindLocalIndexInfo(getItem(position));
			return view;
		}
	}

	private static class LocalFullMapsViewHolder {
		private final ImageView icon;
		private final TextView nameTextView;
		private final TextView descriptionTextView;
		private final ImageButton options;
		private final LiveUpdatesFragment fragment;

		private LocalFullMapsViewHolder(View view, LiveUpdatesFragment context) {
			icon = (ImageView) view.findViewById(R.id.icon);
			nameTextView = (TextView) view.findViewById(R.id.nameTextView);
			descriptionTextView = (TextView) view.findViewById(R.id.descriptionTextView);
			options = (ImageButton) view.findViewById(R.id.options);
			this.fragment = context;
		}

		public void bindLocalIndexInfo(LocalIndexInfo item) {
			nameTextView.setText(item.getName());
			descriptionTextView.setText(item.getDescription());
			OsmandApplication context = fragment.getMyActivity().getMyApplication();
			icon.setImageDrawable(context.getIconsCache().getContentIcon(R.drawable.ic_map));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final FragmentManager fragmentManager = fragment.getChildFragmentManager();
					new SettingsDialogFragment().show(fragmentManager, "settings");
				}
			});
		}
	}

	public static class LoadLocalIndexTask
			extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		private List<LocalIndexInfo> result;
		private ArrayAdapter<LocalIndexInfo> adapter;
		private ActionBarProgressActivity activity;

		public LoadLocalIndexTask(ArrayAdapter<LocalIndexInfo> adapter,
								  ActionBarProgressActivity activity) {
			this.adapter = adapter;
			this.activity = activity;
		}

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			LocalIndexHelper helper = new LocalIndexHelper(activity.getMyApplication());
			return helper.getLocalIndexData(this);
		}

		@Override
		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo localIndexInfo : values) {
				if (localIndexInfo.getType() == LocalIndexHelper.LocalIndexType.MAP_DATA) {
					adapter.add(localIndexInfo);
				}
			}
			adapter.notifyDataSetChanged();
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
			adapter.sort(new Comparator<LocalIndexInfo>() {
				@Override
				public int compare(@NonNull LocalIndexInfo lhs, @NonNull LocalIndexInfo rhs) {
					return lhs.getName().compareTo(rhs.getName());
				}
			});
		}
	}

	public static class SettingsDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			View view = LayoutInflater.from(getActivity())
					.inflate(R.layout.dialog_live_updates_item_settings, null);
			builder.setView(view)
					.setPositiveButton("SAVE", null)
					.setNegativeButton("CANCEL", null)
					.setNeutralButton("UPDATE NOW", null);
			return builder.create();
		}
	}
}
