package net.osmand.plus.download.ui;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.BannerAndDownloadFreeVersion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

public class DownloadResourceGroupFragment extends DialogFragment implements DownloadEvents, OnChildClickListener {
	public static final String TAG = "RegionDialogFragment";
	private static final String REGION_ID_DLG_KEY = "world_region_dialog_key";
	private String groupId;
	private View view;
	private BannerAndDownloadFreeVersion banner;
	protected ExpandableListView listView;
	protected WorldItemsFragment.DownloadResourceGroupAdapter listAdapter;
	private DownloadResourceGroup group;
	private DownloadActivity activity;
	private Toolbar toolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);
		if (savedInstanceState != null) {
			groupId = savedInstanceState.getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null) {
			groupId = getArguments().getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null) {
			groupId = "";
		}
		activity = (DownloadActivity) getActivity();

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		setHasOptionsMenu(true);
		
		banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) getActivity());
		
		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		listAdapter = new WorldItemsFragment.DownloadResourceGroupAdapter(activity);
		listView.setAdapter(listAdapter);
		
		reloadData();
		return view;
	}

	private void reloadData() {
		DownloadResources indexes = activity.getDownloadThread().getIndexes();
		group = indexes.getGroupById(groupId);
		if (group != null) {
			listAdapter.update(group);
			if (group.getRegion() != null) {
				toolbar.setTitle(group.getRegion().getName());
			}
		}
		expandAllGroups();
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.bg_color_light : R.color.bg_color_dark));
	}

	@Override
	public void newDownloadIndexes() {
		banner.updateBannerInProgress();
		reloadData();
	}

	@Override
	public void downloadHasFinished() {
		banner.updateBannerInProgress();
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void downloadInProgress() {
		banner.updateBannerInProgress();
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object child = listAdapter.getChild(groupPosition, childPosition);
		if (child instanceof DownloadResourceGroup) {
			String uniqueId = ((DownloadResourceGroup) child).getUniqueId();
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(uniqueId);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			return true;
		} else if (child instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) child;
			if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				// FIXME
				// if (regularMap.getType() == DownloadActivityType.NORMAL_FILE
				// && regularMap.isAlreadyDownloaded(getMyActivity().getIndexFileNames())) {
				// ConfirmDownloadUnneededMapDialogFragment.createInstance(indexItem)
				// .show(getChildFragmentManager(), "dialog");
				// return true;
				// }
			}
			((DownloadActivity) getActivity()).startDownload(indexItem);
			return true;
		}
		return false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_DLG_KEY, groupId);
		super.onSaveInstanceState(outState);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onRegionSelected(String regionId) {
		final DownloadResourceGroupFragment regionDialogFragment = createInstance(regionId);
		getDownloadActivity().showDialog(getActivity(), regionDialogFragment);
	}

	public static DownloadResourceGroupFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_DLG_KEY, regionId);
		DownloadResourceGroupFragment fragment = new DownloadResourceGroupFragment();
		fragment.setArguments(bundle);
		return fragment;
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
			builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
					R.string.shared_string_download, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (indexItem != null) {
								((DownloadActivity) getActivity()).startDownload(indexItem);
							}
						}
					});
			return builder.create();
		}

		public static ConfirmDownloadUnneededMapDialogFragment createInstance(@NonNull IndexItem indexItem) {
			ConfirmDownloadUnneededMapDialogFragment fragment = new ConfirmDownloadUnneededMapDialogFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			return fragment;
		}
	}
}