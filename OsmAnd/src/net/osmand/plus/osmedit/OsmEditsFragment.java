package net.osmand.plus.osmedit;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.PointDescription;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.FavoritesFragmentStateHolder;
import net.osmand.plus.osmedit.ExportOptionsBottomSheetDialogFragment.ExportOptionsFragmentListener;
import net.osmand.plus.osmedit.FileTypeBottomSheetDialogFragment.FileTypeFragmentListener;
import net.osmand.plus.osmedit.OpenstreetmapLocalUtil.OnNodeCommittedListener;
import net.osmand.plus.osmedit.OsmEditOptionsBottomSheetDialogFragment.OsmEditOptionsFragmentListener;
import net.osmand.plus.osmedit.OsmPoint.Group;
import net.osmand.plus.osmedit.dialogs.ProgressDialogPoiUploader;
import net.osmand.plus.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiBottomSheetFragment;
import net.osmand.plus.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;
import static net.osmand.plus.osmedit.OsmEditingPlugin.OSM_EDIT_TAB;

public class OsmEditsFragment extends OsmAndListFragment implements ProgressDialogPoiUploader,
		OnNodeCommittedListener, FavoritesFragmentStateHolder, OsmAuthorizationListener {

	public static final int EXPORT_TYPE_ALL = 0;
	public static final int EXPORT_TYPE_POI = 1;
	public static final int EXPORT_TYPE_NOTES = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({EXPORT_TYPE_ALL, EXPORT_TYPE_POI, EXPORT_TYPE_NOTES})
	@interface ExportTypesDef {
	}

	public static final int FILE_TYPE_OSC = 0;
	public static final int FILE_TYPE_GPX = 1;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({FILE_TYPE_OSC, FILE_TYPE_GPX})
	@interface FileTypesDef {
	}

	private static final String EXPORT_TYPE_KEY = "export_type";

	private final static int MODE_DELETE = 100;
	private final static int MODE_UPLOAD = 101;

	private OsmandApplication app;
	private OsmEditingPlugin plugin;

	private View footerView;
	private View emptyView;

	private List<OsmPoint> osmEdits = new ArrayList<>();
	private OsmEditsAdapter listAdapter;
	private ArrayList<OsmPoint> osmEditsSelected = new ArrayList<>();

	private ActionMode actionMode;
	private long refreshId;
	private int selectedItemPosition = -1;

	private int exportType;

	public static void getOsmEditView(View v, OsmPoint child, OsmandApplication app) {
		TextView viewName = ((TextView) v.findViewById(R.id.name));
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		String name = OsmEditingPlugin.getEditName(child);
		viewName.setText(name);
		if (child.getGroup() == Group.POI) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_info_dark, R.color.color_distance));
		} else if (child.getGroup() == Group.BUG) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_osm_note_add, R.color.color_distance));
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
		app = getMyApplication();
		if (savedInstanceState != null) {
			exportType = savedInstanceState.getInt(EXPORT_TYPE_KEY);
		}

		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getActivePlugin(OsmEditingPlugin.class);

		View view = inflater.inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ViewStub emptyStub = (ViewStub) view.findViewById(R.id.empty_view_stub);
		emptyStub.setLayoutResource(R.layout.empty_state_osm_edits);
		emptyView = emptyStub.inflate();
		emptyView.setBackgroundColor(getResources().getColor(getMyApplication().getSettings()
				.isLightContent() ? R.color.activity_background_color_light : R.color.activity_background_color_dark));
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			int icRes = getMyApplication().getSettings().isLightContent()
					? R.drawable.ic_empty_state_osm_edits_day : R.drawable.ic_empty_state_osm_edits_night;
			emptyImageView.setImageResource(icRes);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		FragmentManager fm = getChildFragmentManager();
		Fragment optionsFragment = fm.findFragmentByTag(OsmEditOptionsBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((OsmEditOptionsBottomSheetDialogFragment) optionsFragment).setListener(createOsmEditOptionsFragmentListener());
		}
		Fragment exportOptFragment = fm.findFragmentByTag(ExportOptionsBottomSheetDialogFragment.TAG);
		if (exportOptFragment != null) {
			((ExportOptionsBottomSheetDialogFragment) exportOptFragment).setListener(createExportOptionsFragmentListener());
		}
		Fragment fileTypeFragment = fm.findFragmentByTag(FileTypeBottomSheetDialogFragment.TAG);
		if (fileTypeFragment != null) {
			((FileTypeBottomSheetDialogFragment) fileTypeFragment).setListener(createFileTypeFragmentListener());
		}

		plugin.getPoiModificationLocalUtil().addNodeCommittedListener(this);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXPORT_TYPE_KEY, exportType);
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

	private List<OsmPoint> getOsmEditsByGroup(Group group) {
		List<OsmPoint> res = new ArrayList<>();
		for (OsmPoint point : osmEdits) {
			if (point.getGroup() == group) {
				res.add(point);
			}
		}
		return res;
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

		Drawable shareIcon = getMyApplication().getUIUtilities().getIcon((R.drawable.ic_action_gshare_dark));
		item = menu.add(R.string.shared_string_export)
				.setIcon(AndroidUtils.getDrawableForDirection(getMyApplication(), shareIcon));
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Bundle args = new Bundle();
				args.putInt(ExportOptionsBottomSheetDialogFragment.POI_COUNT_KEY, getOsmEditsByGroup(Group.POI).size());
				args.putInt(ExportOptionsBottomSheetDialogFragment.NOTES_COUNT_KEY, getOsmEditsByGroup(Group.BUG).size());
				ExportOptionsBottomSheetDialogFragment fragment = new ExportOptionsBottomSheetDialogFragment();
				fragment.setArguments(args);
				fragment.setUsedOnMap(false);
				fragment.setListener(createExportOptionsFragmentListener());
				fragment.show(getChildFragmentManager(), ExportOptionsBottomSheetDialogFragment.TAG);
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
						if (Algorithms.isEmpty(osmEditsSelected)) {
							app.showToastMessage(R.string.toast_select_edits_for_upload);
						} else {
							uploadItems(osmEditsSelected.toArray(new OsmPoint[0]));
							mode.finish();
						}
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
				? R.color.activity_background_color_light
				: R.color.activity_background_color_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		fetchData();
		restoreState(getArguments());
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
			public void onItemShowMap(OsmPoint point,  int position) {
				showOnMap(point, position);
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
				final Entity entity = i.getEntity();
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

	private ExportOptionsFragmentListener createExportOptionsFragmentListener() {
		return new ExportOptionsFragmentListener() {
			@Override
			public void onClick(int type) {
				exportType = type;
				openFileTypeMenu();
			}
		};
	}

	private FileTypeFragmentListener createFileTypeFragmentListener() {
		return new FileTypeFragmentListener() {
			@Override
			public void onClick(int type) {
				List<OsmPoint> points = getPointsToExport();
				new BackupOpenstreetmapPointAsyncTask(type, exportType).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						points.toArray(new OsmPoint[0]));
			}
		};
	}

	private void openFileTypeMenu() {
		FileTypeBottomSheetDialogFragment fragment = new FileTypeBottomSheetDialogFragment();
		fragment.setUsedOnMap(false);
		fragment.setListener(createFileTypeFragmentListener());
		fragment.show(getChildFragmentManager(), FileTypeBottomSheetDialogFragment.TAG);
	}

	private List<OsmPoint> getPointsToExport() {
		if (exportType == EXPORT_TYPE_POI) {
			return getOsmEditsByGroup(Group.POI);
		} else if (exportType == EXPORT_TYPE_NOTES) {
			return getOsmEditsByGroup(Group.BUG);
		}
		return osmEdits;
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

	private void uploadItems(final OsmPoint[] points) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (hasPoiGroup(points)) {
				if (getMyApplication().getOsmOAuthHelper().isLogged()) {
					SendPoiBottomSheetFragment.showInstance(getChildFragmentManager(), points);
				} else {
					LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), this);
				}
			} else {
				SendOsmNoteBottomSheetFragment.showInstance(getChildFragmentManager(), points);
			}
		}
	}

	@Override
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, OSM_EDIT_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getFavoritesActivity());
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

		app.startActivity(intent);
	}

	boolean hasPoiGroup(OsmPoint[] points) {
		boolean hasPoiGroup = false;
		for (OsmPoint p : points) {
			if (p.getGroup() == OsmPoint.Group.POI) {
				hasPoiGroup = true;
				break;
			}
		}
		return hasPoiGroup;
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
				for (Map.Entry<OsmPoint, String> entry : loadErrorsMap.entrySet()) {
					if (entry.getValue() == null) {
						osmEdits.remove(entry.getKey());
					}
				}
				recreateAdapterData();
			}
		};
		dialog.show(getActivity().getSupportFragmentManager(), ProgressDialogFragment.TAG);
		UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(dialog, listener, plugin, points.length, closeChangeSet, anonymously);
		uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, points);
	}

	private void showOnMap(OsmPoint osmPoint, int itemPosition) {
		selectedItemPosition = itemPosition;
		boolean isOsmPoint = osmPoint instanceof OpenstreetmapPoint;
		String type = osmPoint.getGroup() == Group.POI ? PointDescription.POINT_TYPE_POI : PointDescription.POINT_TYPE_OSM_BUG;
		String name = (isOsmPoint ? ((OpenstreetmapPoint) osmPoint).getName() : ((OsmNotesPoint) osmPoint).getText());
		FavoritesActivity.showOnMap(requireActivity(), this, osmPoint.getLatitude(), osmPoint.getLongitude(), 15,
				new PointDescription(type, name), true, osmPoint);
	}

	private void deletePoint(OsmPoint osmPoint) {
		osmEdits.remove(osmPoint);
		recreateAdapterData();
	}

	private int getFirstVisible() {
		return getListView().getFirstVisiblePosition();
	}

	private void notifyDataSetChanged() {
		listAdapter.notifyDataSetChanged();
	}

	private void notifyDataSetChangedWithSelection(int firstVisible) {
		listAdapter.notifyDataSetChanged();
		getListView().setSelection(firstVisible);
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
			final OsmEditingPlugin plugin = OsmandPlugin.getActivePlugin(OsmEditingPlugin.class);
			@SuppressWarnings("unchecked")
			final ArrayList<OsmPoint> points = (ArrayList<OsmPoint>) getArguments().getSerializable(POINTS_LIST);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			assert points != null;
			builder.setMessage(getString(R.string.local_osm_changes_delete_all_confirm, points.size()));
			builder.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int firstVisible = parentFragment.getFirstVisible();
					Iterator<OsmPoint> it = points.iterator();
					while (it.hasNext()) {
						OsmPoint osmPoint = it.next();
						assert plugin != null;
						if (osmPoint.getGroup() == Group.POI) {
							plugin.getDBPOI().deletePOI((OpenstreetmapPoint) osmPoint);
						} else if (osmPoint.getGroup() == Group.BUG) {
							plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) osmPoint);
						}
						it.remove();
						parentFragment.deletePoint(osmPoint);
					}
					parentFragment.notifyDataSetChangedWithSelection(firstVisible);
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}
	}

	public class BackupOpenstreetmapPointAsyncTask extends AsyncTask<OsmPoint, OsmPoint, String> {

		private File osmchange;
		private boolean oscFile;

		public BackupOpenstreetmapPointAsyncTask(int fileType, int exportType) {
			OsmandApplication app = (OsmandApplication) getActivity().getApplication();
			oscFile = fileType == FILE_TYPE_OSC;
			osmchange = app.getAppPath(getFileName(exportType));
		}

		@Override
		protected String doInBackground(OsmPoint... points) {
			if (oscFile) {
				FileOutputStream out = null;
				try {
					out = new FileOutputStream(osmchange);
					XmlSerializer sz = Xml.newSerializer();

					sz.setOutput(out, "UTF-8");
					sz.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
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
			} else {
				GPXFile gpx = new GPXFile(Version.getFullVersion(getMyApplication()));
				for (OsmPoint point : points) {
					if (point.getGroup() == Group.POI) {
						OpenstreetmapPoint p = (OpenstreetmapPoint) point;
						WptPt wpt = new WptPt();
						wpt.name = p.getTagsString();
						wpt.lat = p.getLatitude();
						wpt.lon = p.getLongitude();
						wpt.desc = "id: " + String.valueOf(p.getId()) +
								" node" + " " + OsmPoint.stringAction.get(p.getAction());
						gpx.addPoint(wpt);
					} else if (point.getGroup() == Group.BUG) {
						OsmNotesPoint p = (OsmNotesPoint) point;
						WptPt wpt = new WptPt();
						wpt.name = p.getText();
						wpt.lat = p.getLatitude();
						wpt.lon = p.getLongitude();
						wpt.desc = "id: " + String.valueOf(p.getId()) +
								" note" + " " + OsmPoint.stringAction.get(p.getAction()) ;
						gpx.addPoint(wpt);
					}
				}
				GPXUtilities.writeGpxFile(osmchange, gpx);
			}

			return null;
		}

		private String getFileName(int exportType) {
			StringBuilder sb = new StringBuilder();
			if (exportType == EXPORT_TYPE_POI) {
				sb.append("osm_edits_modification");
			} else if (exportType == EXPORT_TYPE_NOTES) {
				sb.append("osm_notes_modification");
			} else {
				sb.append("osm_modification");
			}
			sb.append(oscFile ? ".osc" : IndexConstants.GPX_FILE_EXT);
			return sb.toString();
		}

		private void writeContent(XmlSerializer sz, OsmPoint[] points, OsmPoint.Action a) throws IllegalArgumentException, IllegalStateException, IOException {
			for (OsmPoint point : points) {
				if (point.getGroup() == Group.POI) {
					OpenstreetmapPoint p = (OpenstreetmapPoint) point;
					if (p.getAction() == a) {
						Entity entity = p.getEntity();
						if (entity != null && entity instanceof Node) {
							writeNode(sz, (Node) entity);
						}
					}
				} else if (point.getGroup() == Group.BUG) {
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

		private void writeNode(XmlSerializer sz, Node p) {
			try {
				sz.startTag("", "node");
				sz.attribute("", "lat", p.getLatitude() + "");
				sz.attribute("", "lon", p.getLongitude() + "");
				sz.attribute("", "id", p.getId() + "");
				sz.attribute("", "version", "1");
				writeTags(sz, p);
				sz.endTag("", "node");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void writeTags(XmlSerializer sz, Entity p) {
			for (String tag : p.getTagKeySet()) {
				String val = p.getTag(tag);
				if (p.isNotValid(tag)) {
					continue;
				}
				try {
					sz.startTag("", "tag");
					sz.attribute("", "k", tag);
					sz.attribute("", "v", val);
					sz.endTag("", "tag");
				} catch (IOException e) {
					e.printStackTrace();
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
				sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), osmchange));
				sendIntent.setType("text/plain");
				sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(sendIntent);
			}
		}
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, OSM_EDIT_TAB);
		bundle.putInt(ITEM_POSITION, selectedItemPosition);
		return bundle;
	}
	
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.containsKey(TAB_ID) && bundle.containsKey(ITEM_POSITION)) {
			if (bundle.getInt(TAB_ID) == OSM_EDIT_TAB) {
				selectedItemPosition = bundle.getInt(ITEM_POSITION, -1);
				if (selectedItemPosition != -1) {
					int itemsCount = getListView().getAdapter().getCount();
					if (itemsCount > 0 && itemsCount > selectedItemPosition) {
						if (selectedItemPosition == 1) {
							getListView().setSelection(0);
						} else {
							getListView().setSelection(selectedItemPosition);
						}
					}
				}
			}
		}
	}
}