package net.osmand.plus.osmedit;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.data.PointDescription;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.PoiUploaderType;
import net.osmand.plus.osmedit.OsmEditOptionsBottomSheetDialogFragment.OsmEditOptionsFragmentListener;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OsmEditsFragment extends OsmAndListFragment implements SendPoiDialogFragment.ProgressDialogPoiUploader, OpenstreetmapLocalUtil.OnNodeCommittedListener {

	private final static int MODE_DELETE = 100;
	private final static int MODE_UPLOAD = 101;

	private OsmEditingPlugin plugin;

	private View footerView;
	private View emptyView;

	private List<OsmPoint> osmEdits = new ArrayList<>();
	private OsmEditsAdapter listAdapter;
	private ArrayList<OsmPoint> osmEditsSelected = new ArrayList<>();

	private ActionMode actionMode;
	private long refreshId;

	public static void getOsmEditView(View v, OsmPoint child, OsmandApplication app) {
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		String name = OsmEditingPlugin.getEditName(child);
		viewName.setText(name);
		if (child.getGroup() == OsmPoint.Group.POI) {
			icon.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_type_info, R.color.color_distance));
		} else if (child.getGroup() == OsmPoint.Group.BUG) {
			icon.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_type_bug, R.color.color_distance));
		}

		TextView descr = (TextView) v.findViewById(R.id.description);
		if (child.getAction() == OsmPoint.Action.CREATE) {
			descr.setText(R.string.action_create);
		} else if (child.getAction() == OsmPoint.Action.MODIFY) {
			descr.setText(R.string.action_modify);
		} else if (child.getAction() == OsmPoint.Action.DELETE) {
			descr.setText(R.string.action_delete);
		} else if (child.getAction() == OsmPoint.Action.REOPEN) {
			descr.setText(R.string.action_modify);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);

		View view = inflater.inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ViewStub emptyStub = (ViewStub) view.findViewById(R.id.empty_view_stub);
		emptyStub.setLayoutResource(R.layout.empty_state_osm_edits);
		emptyView = emptyStub.inflate();
		int icRes = getMyApplication().getSettings().isLightContent()
				? R.drawable.ic_empty_state_osm_edits_day : R.drawable.ic_empty_state_osm_edits_night;
		((ImageView) emptyView.findViewById(R.id.empty_state_image_view)).setImageResource(icRes);
		emptyView.setBackgroundColor(getResources().getColor(getMyApplication().getSettings()
				.isLightContent() ? R.color.ctx_menu_info_view_bg_light : R.color.ctx_menu_info_view_bg_dark));

		Fragment optionsFragment = getChildFragmentManager().findFragmentByTag(OsmEditOptionsBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((OsmEditOptionsBottomSheetDialogFragment) optionsFragment).setListener(createOsmEditOptionsFragmentListener());
		}

		plugin.getPoiModificationLocalUtil().addNodeCommittedListener(this);
		return view;
	}

	@Override
	public void onDestroyView() {
		plugin.getPoiModificationLocalUtil().removeNodeCommittedListener(this);
		super.onDestroyView();
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}

	private void recreateAdapterData() {
		listAdapter.clear();
		listAdapter.addAll(createItemsList());
		listAdapter.notifyDataSetChanged();
	}

	private void selectAll() {
		for (int i = 0; i < osmEdits.size(); i++) {
			OsmPoint point = osmEdits.get(i);
			if (!osmEditsSelected.contains(point)) {
				osmEditsSelected.add(point);
			}
		}
		listAdapter.notifyDataSetChanged();
	}

	private void deselectAll() {
		osmEditsSelected.clear();
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((ActionBarProgressActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((ActionBarProgressActivity) getActivity()).getClearToolbar(false);
		}
		((ActionBarProgressActivity) getActivity()).updateListViewFooter(footerView);

		MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).setIcon(R.drawable.ic_action_export);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_UPLOAD);
				return true;
			}
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.local_osm_changes_backup).setIcon(R.drawable.ic_action_gshare_dark);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new BackupOpenstreetmapPointAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						osmEdits.toArray(new OsmPoint[osmEdits.size()]));
				return true;
			}
		});
		item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_DELETE);
				return true;
			}
		});
	}

	private void enterUploadMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).setIcon(R.drawable.ic_action_export);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						uploadItems(osmEditsSelected.toArray(new OsmPoint[osmEditsSelected.size()]));
						mode.finish();
						return true;
					}
				});
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
				osmEditsSelected.clear();
				listAdapter.notifyDataSetChanged();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				enableSelectionMode(false);
				listAdapter.notifyDataSetChanged();
			}

		});
	}

	private void enterSelectionMode(int type) {
		switch (type) {
			case MODE_DELETE:
				enterDeleteMode();
				break;
			case MODE_UPLOAD:
				enterUploadMode();
				break;
		}
	}

	private void enterDeleteMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						deleteItems(osmEditsSelected);
						mode.finish();
						return true;
					}
				});
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
				osmEditsSelected.clear();
				listAdapter.notifyDataSetChanged();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				enableSelectionMode(false);
				listAdapter.notifyDataSetChanged();
			}

		});
	}

	private void updateSelectionMode(ActionMode m) {
		updateSelectionTitle(m);
		listAdapter.notifyDataSetChanged();
	}

	private void updateSelectionTitle(ActionMode m) {
		if (osmEditsSelected.size() > 0) {
			m.setTitle(osmEditsSelected.size() + " " + getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	private void enableSelectionMode(boolean selectionMode) {
		listAdapter.setSelectionMode(selectionMode);
		//noinspection ConstantConditions
		((FavoritesActivity) getActivity()).setToolbarVisibility(!selectionMode && AndroidUiHelper.isOrientationPortrait(getActivity()));
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
	}

	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}

	private void deleteItems(final ArrayList<OsmPoint> points) {
		DeleteOsmEditsConfirmDialogFragment.createInstance(points).show(getChildFragmentManager(), DeleteOsmEditsConfirmDialogFragment.TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(getMyApplication().getSettings().isLightContent()
				? R.color.ctx_menu_info_view_bg_light
				: R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		fetchData();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	private void fetchData() {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		osmEdits = new ArrayList<>();
		List<OpenstreetmapPoint> l1 = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmbugsPoints();
		osmEdits.addAll(l1);
		osmEdits.addAll(l2);
		ListView listView = getListView();
		listView.setDivider(null);
		listView.setEmptyView(emptyView);

		if (osmEdits.size() > 0 && footerView == null && portrait) {
			footerView = getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, listView, false);
			listView.addFooterView(footerView);
		}
		List<Object> items = createItemsList();
		listAdapter = new OsmEditsAdapter(getMyApplication(), items);
		listAdapter.setSelectedOsmEdits(osmEditsSelected);
		listAdapter.setAdapterListener(new OsmEditsAdapter.OsmEditsAdapterListener() {
			@Override
			public void onHeaderCheckboxClick(boolean checked) {
				if (checked) {
					selectAll();
				} else {
					deselectAll();
				}
				updateSelectionTitle(actionMode);
			}

			@Override
			public void onItemSelect(OsmPoint point, boolean checked) {
				if (checked) {
					osmEditsSelected.add(point);
				} else {
					osmEditsSelected.remove(point);
				}
				updateSelectionMode(actionMode);
			}

			@Override
			public void onItemShowMap(OsmPoint point) {
				showOnMap(point);
			}

			@Override
			public void onOptionsClick(OsmPoint note) {
				openPopUpMenu(note);
			}
		});
		listAdapter.setPortrait(portrait);
		listView.setAdapter(listAdapter);
	}

	private List<Object> createItemsList() {
		List<Object> items = new ArrayList<>();
		if (!osmEdits.isEmpty()) {
			items.add(OsmEditsAdapter.TYPE_HEADER);
			items.addAll(osmEdits);
		}
		return items;
	}

	private void showBugDialog(final OsmNotesPoint point) {
		final View view = LayoutInflater.from(getActivity()).inflate(R.layout.open_bug, null);
		view.findViewById(R.id.user_name_field).setVisibility(View.GONE);
		view.findViewById(R.id.userNameEditTextLabel).setVisibility(View.GONE);
		view.findViewById(R.id.password_field).setVisibility(View.GONE);
		view.findViewById(R.id.passwordEditTextLabel).setVisibility(View.GONE);
		String text = point.getText();
		if (!Algorithms.isEmpty(text)) {
			((EditText) view.findViewById(R.id.message_field)).setText(text);
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.shared_string_commit);
		builder.setView(view);
		builder.setPositiveButton(R.string.osn_modify_dialog_title, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String text = ((EditText) view.findViewById(R.id.message_field)).getText().toString();
				plugin.getDBBug().updateOsmBug(point.getId(), text);
				point.setText(text);
				notifyDataSetChanged();
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
	}

	@Override
	public void onNoteCommitted() {
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				fetchData();
			}
		});
	}

	private void openPopUpMenu(final OsmPoint info) {
		OsmEditOptionsBottomSheetDialogFragment optionsFragment = new OsmEditOptionsBottomSheetDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(OsmEditOptionsBottomSheetDialogFragment.OSM_POINT, info);
		optionsFragment.setUsedOnMap(false);
		optionsFragment.setArguments(args);
		optionsFragment.setListener(createOsmEditOptionsFragmentListener());
		optionsFragment.show(getChildFragmentManager(), OsmEditOptionsBottomSheetDialogFragment.TAG);
	}

	private OsmEditOptionsFragmentListener createOsmEditOptionsFragmentListener() {
		return new OsmEditOptionsFragmentListener() {
			@Override
			public void onUploadClick(OsmPoint osmPoint) {
				uploadItems(new OsmPoint[]{getPointAfterModify(osmPoint)});
			}

			@Override
			public void onShowOnMapClick(OsmPoint osmPoint) {
				OsmandSettings settings = getMyApplication().getSettings();
				settings.setMapLocationToShow(osmPoint.getLatitude(), osmPoint.getLongitude(), settings.getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}

			@Override
			public void onModifyOsmChangeClick(OsmPoint osmPoint) {
				OpenstreetmapPoint i = (OpenstreetmapPoint) getPointAfterModify(osmPoint);
				final Node entity = i.getEntity();
				refreshId = entity.getId();
				EditPoiDialogFragment.createInstance(entity, false).show(getActivity().getSupportFragmentManager(), "edit_poi");
			}

			@Override
			public void onModifyOsmNoteClick(OsmPoint osmPoint) {
				showBugDialog((OsmNotesPoint) osmPoint);
			}

			@Override
			public void onDeleteClick(OsmPoint osmPoint) {
				ArrayList<OsmPoint> points = new ArrayList<>();
				points.add(osmPoint);
				deleteItems(new ArrayList<>(points));
			}
		};
	}

	protected OsmPoint getPointAfterModify(OsmPoint info) {
		if (info instanceof OpenstreetmapPoint && info.getId() == refreshId) {
			for (OpenstreetmapPoint p : plugin.getDBPOI().getOpenstreetmapPoints()) {
				if (p.getId() == info.getId()) {
					return p;
				}
			}
		}
		return info;
	}


	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private void uploadItems(final OsmPoint[] items) {
		SendPoiDialogFragment.createInstance(items, PoiUploaderType.FRAGMENT)
				.show(getChildFragmentManager(), SendPoiDialogFragment.TAG);
//		UploadOsmEditsConfirmDialogFragment.createInstancee(items).show(getChildFragmentManager(),
//				UploadOsmEditsConfirmDialogFragment.TAG);
	}

	public void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously) {
		ProgressDialogFragment dialog = ProgressDialogFragment.createInstance(
				R.string.uploading,
				R.string.local_openstreetmap_uploading,
				ProgressDialog.STYLE_HORIZONTAL);
		OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(getActivity(),
				getString(R.string.local_openstreetmap_were_uploaded)) {
			@Override
			public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
				super.uploadEnded(loadErrorsMap);
				for (OsmPoint osmPoint : loadErrorsMap.keySet()) {
					if (loadErrorsMap.get(osmPoint) == null) {
						osmEdits.remove(osmPoint);
					}
				}
				recreateAdapterData();
			}
		};
		dialog.show(getActivity().getSupportFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog, listener, plugin, points.length, closeChangeSet, anonymously);
		uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, points);
	}

	private void showOnMap(OsmPoint osmPoint) {
		boolean isOsmPoint = osmPoint instanceof OpenstreetmapPoint;
		String type = osmPoint.getGroup() == OsmPoint.Group.POI ? PointDescription.POINT_TYPE_POI : PointDescription.POINT_TYPE_OSM_BUG;
		String name = (isOsmPoint ? ((OpenstreetmapPoint) osmPoint).getName() : ((OsmNotesPoint) osmPoint).getText());
		getMyApplication().getSettings().setMapLocationToShow(osmPoint.getLatitude(), osmPoint.getLongitude(), 15,
				new PointDescription(type, name), true, osmPoint); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void deletePoint(OsmPoint osmPoint) {
		osmEdits.remove(osmPoint);
		recreateAdapterData();
	}

	private void notifyDataSetChanged() {
		listAdapter.notifyDataSetChanged();
	}

	public static class DeleteOsmEditsConfirmDialogFragment extends DialogFragment {
		public static final String TAG = "DeleteOsmEditsConfirmDialogFragment";
		private static final String POINTS_LIST = "points_list";

		public static DeleteOsmEditsConfirmDialogFragment createInstance(
				ArrayList<OsmPoint> points) {
			DeleteOsmEditsConfirmDialogFragment fragment = new DeleteOsmEditsConfirmDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(POINTS_LIST, points);
			fragment.setArguments(args);
			return fragment;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final OsmEditsFragment parentFragment = (OsmEditsFragment) getParentFragment();
			final OsmEditingPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
			@SuppressWarnings("unchecked")
			final ArrayList<OsmPoint> points = (ArrayList<OsmPoint>) getArguments().getSerializable(POINTS_LIST);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			assert points != null;
			builder.setMessage(getString(R.string.local_osm_changes_delete_all_confirm, points.size()));
			builder.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Iterator<OsmPoint> it = points.iterator();
					while (it.hasNext()) {
						OsmPoint osmPoint = it.next();
						assert plugin != null;
						if (osmPoint.getGroup() == OsmPoint.Group.POI) {
							plugin.getDBPOI().deletePOI((OpenstreetmapPoint) osmPoint);
						} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
							plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) osmPoint);
						}
						it.remove();
						parentFragment.deletePoint(osmPoint);
					}
					parentFragment.notifyDataSetChanged();

				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}
	}

	public class BackupOpenstreetmapPointAsyncTask extends AsyncTask<OsmPoint, OsmPoint, String> {

		private File osmchange;

		public BackupOpenstreetmapPointAsyncTask() {
			OsmandApplication app = (OsmandApplication) getActivity().getApplication();
			osmchange = app.getAppPath("poi_modification.osc");
		}


		@Override
		protected String doInBackground(OsmPoint... points) {
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(osmchange);
				XmlSerializer sz = Xml.newSerializer();

				sz.setOutput(out, "UTF-8");
				sz.startDocument("UTF-8", true);
				sz.startTag("", "osmChange");
				sz.attribute("", "generator", "OsmAnd");
				sz.attribute("", "version", "0.6");
				sz.startTag("", "create");
				writeContent(sz, points, OsmPoint.Action.CREATE);
				sz.endTag("", "create");
				sz.startTag("", "modify");
				writeContent(sz, points, OsmPoint.Action.MODIFY);
				writeContent(sz, points, OsmPoint.Action.REOPEN);

				sz.endTag("", "modify");
				sz.startTag("", "delete");
				writeContent(sz, points, OsmPoint.Action.DELETE);
				sz.endTag("", "delete");
				sz.endTag("", "osmChange");
				sz.endDocument();
			} catch (Exception e) {
				return e.getMessage();
			} finally {
				try {
					if (out != null) out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return null;
		}

		private void writeContent(XmlSerializer sz, OsmPoint[] points, OsmPoint.Action a) throws IllegalArgumentException, IllegalStateException, IOException {
			for (OsmPoint point : points) {
				if (point.getGroup() == OsmPoint.Group.POI) {
					OpenstreetmapPoint p = (OpenstreetmapPoint) point;
					if (p.getAction() == a) {
						sz.startTag("", "node");
						sz.attribute("", "lat", p.getLatitude() + "");
						sz.attribute("", "lon", p.getLongitude() + "");
						sz.attribute("", "id", p.getId() + "");
						sz.attribute("", "version", "1");
						for (String tag : p.getEntity().getTagKeySet()) {
							String val = p.getEntity().getTag(tag);
							if (val == null || val.length() == 0 || tag.length() == 0 || "poi_type_tag".equals(tag)) {
								continue;
							}
							sz.startTag("", "tag");
							sz.attribute("", "k", tag);
							sz.attribute("", "v", val);
							sz.endTag("", "tag");
						}
						sz.endTag("", "node");
					}
				} else if (point.getGroup() == OsmPoint.Group.BUG) {
					OsmNotesPoint p = (OsmNotesPoint) point;
					if (p.getAction() == a) {
						sz.startTag("", "note");
						sz.attribute("", "lat", p.getLatitude() + "");
						sz.attribute("", "lon", p.getLongitude() + "");
						sz.attribute("", "id", p.getId() + "");
						sz.startTag("", "comment");
						sz.attribute("", "text", p.getText() + "");
						sz.endTag("", "comment");
						sz.endTag("", "note");
					}
				}
			}
		}


		@Override
		protected void onPreExecute() {
			getActivity().setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onPostExecute(String result) {
			getActivity().setProgressBarIndeterminateVisibility(false);
			if (result != null) {
				Toast.makeText(getActivity(), getString(R.string.local_osm_changes_backup_failed) + " " + result, Toast.LENGTH_LONG).show();
			} else {
				final Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_osm_edits_subject));
				sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(osmchange));
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
			}
		}
	}

}