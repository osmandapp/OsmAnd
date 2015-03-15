package net.osmand.plus.osmedit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.myplaces.FavoritesActivity;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Denis
 * on 06.03.2015.
 */
public class OsmEditsFragment extends ListFragment implements OsmEditsUploadListener {
	OsmEditingPlugin plugin;
	private ArrayList<OsmPoint> dataPoints;
	private OsmEditsAdapter listAdapter;

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;

	private OpenstreetmapRemoteUtil remotepoi;
	private OsmBugsRemoteUtil remotebug;

	protected OsmPoint[] toUpload = new OsmPoint[0];

	ProgressDialog dialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.select_all).setVisibility(View.GONE);
		((TextView) view.findViewById(R.id.header)).setText(R.string.your_edits);
		dbpoi = new OpenstreetmapsDbHelper(getActivity());
		dbbug = new OsmBugsDbHelper(getActivity());

		remotepoi = new OpenstreetmapRemoteUtil(getActivity());
		remotebug = new OsmBugsRemoteUtil(getMyApplication());

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();

		if (ScreenOrientationHelper.isOrientationPortrait(getActivity())) {
			menu = ((FavoritesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((FavoritesActivity) getActivity()).getClearToolbar(false);
		}
		MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).
				setIcon(R.drawable.ic_action_export);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				toUpload = dataPoints.toArray(new OsmPoint[0]);
				showUploadItemsDialog();
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.local_osm_changes_backup).
				setIcon(R.drawable.ic_action_gsave_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new BackupOpenstreetmapPointAsyncTask().execute(dataPoints.toArray(new OsmPoint[0]));
				return true;
			}
		});
		item = menu.add(R.string.shared_string_delete_all).
				setIcon(R.drawable.ic_action_delete_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
				b.setMessage(getString(R.string.local_osm_changes_delete_all_confirm, dataPoints.size()));
				b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Iterator<OsmPoint> it = dataPoints.iterator();
						while(it.hasNext()) {
							OsmPoint info = it.next();
							if (info.getGroup() == OsmPoint.Group.POI) {
								dbpoi.deletePOI((OpenstreetmapPoint) info);
							} else if (info.getGroup() == OsmPoint.Group.BUG) {
								dbbug.deleteAllBugModifications((OsmNotesPoint) info);
							}
							it.remove();
							listAdapter.delete(info);
						}
						listAdapter.notifyDataSetChanged();
					}
				});
				b.setNegativeButton(R.string.shared_string_cancel, null);
				b.show();
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		dataPoints = new ArrayList<>();
		List<OpenstreetmapPoint> l1 = dbpoi.getOpenstreetmapPoints();
		List<OsmNotesPoint> l2 = dbbug.getOsmbugsPoints();
		dataPoints.addAll(l1);
		dataPoints.addAll(l2);
		listAdapter = new OsmEditsAdapter(dataPoints);
		getListView().setAdapter(listAdapter);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				OsmPoint it = listAdapter.getItem(position);
				openPopUpMenu(view, it);

			}
		});

	}

	public static void getOsmEditView(View v, OsmPoint child, OsmandApplication app){
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		String idPrefix = (child.getGroup() == OsmPoint.Group.POI ? "POI " : "Bug ") + " id: " + child.getId();
		if (child.getGroup() == OsmPoint.Group.POI) {
			viewName.setText(idPrefix + " (" + ((OpenstreetmapPoint) child).getSubtype() + ") " + ((OpenstreetmapPoint) child).getName());
			icon.setImageDrawable(app.getIconsCache().
					getIcon(R.drawable.ic_type_info, R.color.color_distance));
		} else if (child.getGroup() == OsmPoint.Group.BUG) {
			viewName.setText(idPrefix + " (" + ((OsmNotesPoint) child).getAuthor() + ") " + ((OsmNotesPoint) child).getText());
			icon.setImageDrawable(app.getIconsCache().
					getIcon(R.drawable.ic_type_bug, R.color.color_distance));
		}

		TextView descr = (TextView) v.findViewById(R.id.descr);
		if (child.getAction() == OsmPoint.Action.CREATE) {
			descr.setText(R.string.action_create);
		} else if (child.getAction() == OsmPoint.Action.MODIFY) {
			descr.setText(R.string.action_modify);
		} else if (child.getAction() == OsmPoint.Action.DELETE) {
			descr.setText(R.string.action_delete);
		}
	}

	protected class OsmEditsAdapter extends ArrayAdapter<OsmPoint> {

		public OsmEditsAdapter(List<OsmPoint> points) {
			super(getActivity(), net.osmand.plus.R.layout.note, points);
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

			v.findViewById(R.id.options).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openPopUpMenu(v, child);
				}
			});
			return v;
		}

	}

	private void openPopUpMenu(View v, final OsmPoint info) {
		OsmandApplication app = getMyApplication();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		MenuItem item = optionsMenu.getMenu().add(R.string.shared_string_show_on_map).
				setIcon(app.getIconsCache().getContentIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				OsmandSettings settings = getMyApplication().getSettings();
				settings.setMapLocationToShow(info.getLatitude(), info.getLongitude(), settings.getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
				return true;
			}
		});
		item = optionsMenu.getMenu().add(R.string.shared_string_delete).
				setIcon(app.getIconsCache().getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (info.getGroup() == OsmPoint.Group.POI) {
					dbpoi.deletePOI((OpenstreetmapPoint) info);
				} else if (info.getGroup() == OsmPoint.Group.BUG) {
					dbbug.deleteAllBugModifications((OsmNotesPoint) info);
				}
				listAdapter.delete(info);
				return true;
			}
		});
		item = optionsMenu.getMenu().add(R.string.local_openstreetmap_upload).
				setIcon(app.getIconsCache().getContentIcon(R.drawable.ic_action_export));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				toUpload = new OsmPoint[]{info};
				showUploadItemsDialog();
				return true;
			}
		});
		optionsMenu.show();
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private void showUploadItemsDialog(){
		dialog = ProgressImplementation.createProgressDialog(
				getActivity(),
				getString(R.string.uploading),
				getString(R.string.local_openstreetmap_uploading),
				ProgressDialog.STYLE_HORIZONTAL).getDialog();
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog, this, remotepoi,
				remotebug, toUpload.length);
		uploadTask.execute(toUpload);

		dialog.show();
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
					if(out!= null) out.close();
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
						sz.attribute("", "text", p.getText() +"");
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
				AccessibleToast.makeText(getActivity(), getString(R.string.local_osm_changes_backup_failed) + " " + result, Toast.LENGTH_LONG).show();
			} else {
				AccessibleToast.makeText(getActivity(), getString(R.string.local_osm_changes_backup_successful, osmchange.getAbsolutePath()), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void uploadUpdated(OsmPoint point){
		listAdapter.delete(point);
	}

	@Override
	public void uploadEnded(Integer result){
		listAdapter.notifyDataSetChanged();
		if (result != null) {
			AccessibleToast.makeText(getActivity(),
					MessageFormat.format(getString(R.string.local_openstreetmap_were_uploaded), result), Toast.LENGTH_LONG)
					.show();
		}
		dialog.dismiss();
	}

}
