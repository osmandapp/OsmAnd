package net.osmand.plus.osmedit;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.myplaces.FavoritesActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 06.03.2015.
 */
public class OsmEditsFragment extends ListFragment {
	OsmEditingPlugin plugin;
	private ArrayList<OsmPoint> dataPoints;
	private OsmEditsAdapter listAdapter;

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;

	protected OsmPoint[] toUpload = new OsmPoint[0];

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.select_all).setVisibility(View.GONE);
		((TextView) view.findViewById(R.id.header)).setText(R.string.your_edits);
		dbpoi = new OpenstreetmapsDbHelper(getActivity());
		dbbug = new OsmBugsDbHelper(getActivity());

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		((FavoritesActivity) getActivity()).getClearToolbar(false);
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
			TextView viewName = ((TextView) v.findViewById(R.id.name));
			ImageView icon = (ImageView) v.findViewById(R.id.icon);

			v.findViewById(R.id.play).setVisibility(View.GONE);
			String idPrefix = (child.getGroup() == OsmPoint.Group.POI ? "POI " : "Bug ") + " id: " + child.getId();
			if (child.getGroup() == OsmPoint.Group.POI) {
				viewName.setText(idPrefix + " (" + ((OpenstreetmapPoint) child).getSubtype() + ") " + ((OpenstreetmapPoint) child).getName());
				icon.setImageDrawable(getMyApplication().getIconsCache().
						getIcon(R.drawable.ic_type_info, R.color.color_distance));
			} else if (child.getGroup() == OsmPoint.Group.BUG) {
				viewName.setText(idPrefix + " (" + ((OsmNotesPoint) child).getAuthor() + ") " + ((OsmNotesPoint) child).getText());
				icon.setImageDrawable(getMyApplication().getIconsCache().
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
		MenuItem item = optionsMenu.getMenu().add(R.string.showed_on_map).
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
		item = optionsMenu.getMenu().add(R.string.local_openstreetmap_delete).
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
				setIcon(app.getIconsCache().getContentIcon(R.drawable.ic_action_gup_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				toUpload = new OsmPoint[]{info};
				ProgressDialog implementation = ProgressImplementation.createProgressDialog(
						getActivity(),
						getString(R.string.uploading),
						getString(R.string.local_openstreetmap_uploading),
						ProgressDialog.STYLE_HORIZONTAL).getDialog();
				implementation.show();
				return true;
			}
		});
		optionsMenu.show();
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

}
