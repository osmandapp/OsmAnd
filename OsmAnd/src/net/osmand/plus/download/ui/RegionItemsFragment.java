package net.osmand.plus.download.ui;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class RegionItemsFragment extends OsmandExpandableListFragment {
	public static final String TAG = "RegionItemsFragment";
	private static final String REGION_ID_KEY = "world_region_id_key";
	private String regionId;
	private WorldItemsFragment.DownloadResourceGroupAdapter listAdapter;
	private DownloadActivity activity;
	private DownloadResourceGroup group;

	public static RegionItemsFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_KEY, regionId);
		RegionItemsFragment fragment = new RegionItemsFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_items_fragment, container, false);

		if (savedInstanceState != null) {
			regionId = savedInstanceState.getString(REGION_ID_KEY);
		}
		if (regionId == null) {
			regionId = getArguments().getString(REGION_ID_KEY);
		}
		if (regionId == null)
			regionId = "";

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		activity = (DownloadActivity) getActivity();
		DownloadResources indexes = activity.getDownloadThread().getIndexes();
		group = indexes.getGroupById(regionId);
		listAdapter = new WorldItemsFragment.DownloadResourceGroupAdapter(activity);
		listView.setAdapter(listAdapter);
		setListView(listView);
		if(group != null) {
			listAdapter.update(group);
		}
		expandAllGroups();
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_KEY, regionId);
		super.onSaveInstanceState(outState);
	}

	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object child = listAdapter.getChild(groupPosition, childPosition);
		if (child instanceof DownloadResourceGroup) {
			String uniqueId = ((DownloadResourceGroup) child).getUniqueId();
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment.createInstance();
			regionDialogFragment.setOnDismissListener(getDownloadActivity());
			getDownloadActivity().showDialog(getActivity(), regionDialogFragment);
			((DownloadResourceGroupFragment) getParentFragment()).onRegionSelected(uniqueId);
			return true;
		} else if (child instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) child;
			if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				// FIXME
//				if (regularMap.getType() == DownloadActivityType.NORMAL_FILE
//						&& regularMap.isAlreadyDownloaded(getMyActivity().getIndexFileNames())) {
//					ConfirmDownloadUnneededMapDialogFragment.createInstance(indexItem)
//							.show(getChildFragmentManager(), "dialog");
//					return true;
//				}
			}
			((BaseDownloadActivity) getActivity()).startDownload(indexItem);
			return true;
		}
		return false;
	}
	
	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}


	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	

	public static class ConfirmDownloadUnneededMapDialogFragment extends DialogFragment {
		private static final String INDEX_ITEM = "index_item";
		private static IndexItem item = null;

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final IndexItem indexItem = item;
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.are_you_sure);
			builder.setMessage(R.string.confirm_download_roadmaps);
			builder.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_download,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if(indexItem != null) {
										((DownloadActivity) getActivity()).startDownload(indexItem);
									}
								}
							});
			return builder.create();
		}

		public static ConfirmDownloadUnneededMapDialogFragment createInstance(@NonNull IndexItem indexItem) {
			ConfirmDownloadUnneededMapDialogFragment fragment =
					new ConfirmDownloadUnneededMapDialogFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}
	}	
}
