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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.PoiUploaderType;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OsmEditsFragment extends OsmAndListFragment
		implements SendPoiDialogFragment.ProgressDialogPoiUploader {
	OsmEditingPlugin plugin;

	private OsmEditsAdapter listAdapter;
	private View footerView;

	private boolean selectionMode = false;


	private final static int MODE_DELETE = 100;
	private final static int MODE_UPLOAD = 101;

	private ActionMode actionMode;
	private long refreshId;
	private ArrayList<OsmPoint> osmEditsSelected = new ArrayList<>();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		((TextView) view.findViewById(R.id.header)).setText(R.string.your_edits);

		final CheckBox selectAll = (CheckBox) view.findViewById(R.id.select_all);
		selectAll.setVisibility(View.GONE);
		selectAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectAll.isChecked()) {
					selectAll();
				} else {
					deselectAll();
				}
				updateSelectionTitle(actionMode);
			}
		});
		return view;
	}

	
	public android.widget.ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}
	
	private void selectAll() {
		for (int i = 0; i < listAdapter.getCount(); i++) {
			OsmPoint point = listAdapter.getItem(i);
			if (!osmEditsSelected.contains(point)) {
				osmEditsSelected.add(point);
			}
		}
		listAdapter.notifyDataSetInvalidated();
	}

	private void deselectAll() {
		osmEditsSelected.clear();
		listAdapter.notifyDataSetInvalidated();
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

		MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).
				setIcon(R.drawable.ic_action_export);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_UPLOAD);
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.local_osm_changes_backup).
				setIcon(R.drawable.ic_action_gshare_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new BackupOpenstreetmapPointAsyncTask().execute(
						listAdapter.dataPoints.toArray(new OsmPoint[listAdapter.dataPoints.size()]));
				return true;
			}
		});
		item = menu.add(R.string.shared_string_delete_all).
				setIcon(R.drawable.ic_action_delete_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
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
				MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).
						setIcon(R.drawable.ic_action_export);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						uploadItems(osmEditsSelected.toArray(new OsmPoint[osmEditsSelected.size()]));
						mode.finish();
						return true;
					}
				});
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				osmEditsSelected.clear();
				listAdapter.notifyDataSetInvalidated();
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
				listAdapter.notifyDataSetInvalidated();
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
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				osmEditsSelected.clear();
				listAdapter.notifyDataSetInvalidated();
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
				listAdapter.notifyDataSetInvalidated();
			}

		});
	}

	private void updateSelectionMode(ActionMode m) {
		updateSelectionTitle(m);
		refreshSelectAll();
	}

	private void updateSelectionTitle(ActionMode m) {
		if (osmEditsSelected.size() > 0) {
			m.setTitle(osmEditsSelected.size() + " "
					+ getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	private void refreshSelectAll() {
		View view = getView();
		if (view == null) {
			return;
		}
		CheckBox selectAll = (CheckBox) view.findViewById(R.id.select_all);
		for (int i = 0; i < listAdapter.getCount(); i++) {
			OsmPoint point = listAdapter.getItem(i);
			if (!osmEditsSelected.contains(point)) {
				selectAll.setChecked(false);
				return;
			}
		}
		selectAll.setChecked(true);
	}

	private void enableSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		//noinspection ConstantConditions
		getView().findViewById(R.id.select_all).setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		((FavoritesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
				AndroidUiHelper.isOrientationPortrait(getActivity()));
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
	}

	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}

	private void deleteItems(final ArrayList<OsmPoint> points) {
		DeleteOsmEditsConfirmDialogFragment.createInstance(points).
				show(getChildFragmentManager(), DeleteOsmEditsConfirmDialogFragment.TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(
				getResources().getColor(
						getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
								: R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		ArrayList<OsmPoint> dataPoints = new ArrayList<>();
		List<OpenstreetmapPoint> l1 = plugin.getDBPOI().getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmbugsPoints();
		dataPoints.addAll(l1);
		dataPoints.addAll(l2);
		if (listAdapter == null) {
			listAdapter = new OsmEditsAdapter(dataPoints);
			ListView listView = getListView();
			if (dataPoints.size() > 0 && footerView == null) {
				//listView.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_header, null, false));
				footerView = getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false);
				listView.addFooterView(footerView);
				listView.setHeaderDividersEnabled(false);
				listView.setFooterDividersEnabled(false);
			}
			listView.setAdapter(listAdapter);
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					OsmPoint it = listAdapter.getItem(position);
					openPopUpMenu(view, it);

				}
			});
		} else {
			listAdapter.setNewList(dataPoints);
		}

	}

	public static void getOsmEditView(View v, OsmPoint child, OsmandApplication app) {
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		String name = OsmEditingPlugin.getEditName(child);
		viewName.setText(name);
		if (child.getGroup() == OsmPoint.Group.POI) {
			icon.setImageDrawable(app.getIconsCache().
					getIcon(R.drawable.ic_type_info, R.color.color_distance));
		} else if (child.getGroup() == OsmPoint.Group.BUG) {
			icon.setImageDrawable(app.getIconsCache().
					getIcon(R.drawable.ic_type_bug, R.color.color_distance));
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

	protected class OsmEditsAdapter extends ArrayAdapter<OsmPoint> {
		private List<OsmPoint> dataPoints;


		public OsmEditsAdapter(List<OsmPoint> points) {
			super(getActivity(), net.osmand.plus.R.layout.note, points);
			dataPoints = points;
		}

		public void setNewList(List<OsmPoint> dp) {
			dataPoints = dp;
			setNotifyOnChange(false);
			clear();
			for (OsmPoint pnt : dp) {
				add(pnt);
			}
			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

		public List<OsmPoint> getDataPoints() {
			return dataPoints;
		}

		public void delete(OsmPoint i) {
			dataPoints.remove(i);
			remove(i);
			listAdapter.notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			final OsmPoint child = getItem(position);
			if (v == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.note, parent, false);
			}
			getOsmEditView(v, child, getMyApplication());

			v.findViewById(R.id.play).setVisibility(View.GONE);

			final CheckBox ch = (CheckBox) v.findViewById(R.id.check_local_index);
			View options = v.findViewById(R.id.options);
			if (selectionMode) {
				options.setVisibility(View.GONE);
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(osmEditsSelected.contains(child));
				v.findViewById(R.id.icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemSelect(ch, child);
					}
				});
			} else {
				v.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}

			((ImageView) options).setImageDrawable(getMyApplication().getIconsCache()
					.getThemedIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openPopUpMenu(v, child);
				}
			});
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectionMode) {
						ch.setChecked(!ch.isChecked());
						onItemSelect(ch, child);
					} else {
						showOnMap(child);
					}

				}
			});
			return v;
		}

		public void onItemSelect(CheckBox ch, OsmPoint child) {
			if (ch.isChecked()) {
				osmEditsSelected.add(child);
			} else {
				osmEditsSelected.remove(child);
			}
			updateSelectionMode(actionMode);
		}

	}

	private void openPopUpMenu(View v, final OsmPoint info) {
		OsmandApplication app = getMyApplication();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		MenuItem item = optionsMenu.getMenu().add(R.string.shared_string_show_on_map).
				setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				OsmandSettings settings = getMyApplication().getSettings();
				settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
				return true;
			}
		});
		if (info instanceof OpenstreetmapPoint && info.getAction() != Action.DELETE) {
			item = optionsMenu.getMenu().add(R.string.poi_context_menu_modify_osm_change)
					.setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_action_edit_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					OpenstreetmapPoint i = (OpenstreetmapPoint) getPointAfterModify(info);
					final Node entity = i.getEntity();
					refreshId = entity.getId();
					EditPoiDialogFragment.createInstance(entity, false)
							.show(getActivity().getSupportFragmentManager(), "edit_poi");
					return true;
				}
			});
		}
		item = optionsMenu.getMenu().add(R.string.shared_string_delete).
				setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				ArrayList<OsmPoint> points = new ArrayList<>();
				points.add(info);
				deleteItems(new ArrayList<>(points));
				return true;

			}
		});
		item = optionsMenu.getMenu().add(R.string.local_openstreetmap_upload).
				setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_action_export));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				uploadItems(new OsmPoint[]{getPointAfterModify(info)});
				return true;
			}
		});
		optionsMenu.show();
	}

	protected OsmPoint getPointAfterModify(OsmPoint info) {
		if(info instanceof OpenstreetmapPoint && info.getId() == refreshId) {
			for(OpenstreetmapPoint p : plugin.getDBPOI().getOpenstreetmapPoints()) {
				if(p.getId() == info.getId()) {
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
						listAdapter.remove(osmPoint);
					}
				}
				listAdapter.notifyDataSetChanged();
			}
		};
		dialog.show(getActivity().getSupportFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
				dialog, listener, plugin, points.length, closeChangeSet, anonymously);
		uploadTask.execute(points);
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

	private void showOnMap(OsmPoint osmPoint) {
		boolean isOsmPoint = osmPoint instanceof OpenstreetmapPoint;
		String type = osmPoint.getGroup() == OsmPoint.Group.POI ? PointDescription.POINT_TYPE_POI : PointDescription.POINT_TYPE_OSM_BUG;
		String name = (isOsmPoint ? ((OpenstreetmapPoint) osmPoint).getName() : ((OsmNotesPoint) osmPoint).getText());
		getMyApplication().getSettings().setMapLocationToShow(osmPoint.getLatitude(), osmPoint.getLongitude(), 15,
				new PointDescription(type, name), true, osmPoint); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void deletePoint(OsmPoint osmPoint) {
		listAdapter.delete(osmPoint);
	}

	private void notifyDataSetChanged() {
		listAdapter.notifyDataSetChanged();
	}

	public static class DeleteOsmEditsConfirmDialogFragment extends DialogFragment {
		public static final String TAG = "DeleteOsmEditsConfirmDialogFragment";
		private static final String POINTS_LIST = "points_list";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final OsmEditsFragment parentFragment = (OsmEditsFragment) getParentFragment();
			final OsmEditingPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
			@SuppressWarnings("unchecked")
			final ArrayList<OsmPoint> points =
					(ArrayList<OsmPoint>) getArguments().getSerializable(POINTS_LIST);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			assert points != null;
			builder.setMessage(getString(R.string.local_osm_changes_delete_all_confirm,
					points.size()));
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

		public static DeleteOsmEditsConfirmDialogFragment createInstance(
				ArrayList<OsmPoint> points) {
			DeleteOsmEditsConfirmDialogFragment fragment = new DeleteOsmEditsConfirmDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(POINTS_LIST, points);
			fragment.setArguments(args);
			return fragment;
		}
	}

}