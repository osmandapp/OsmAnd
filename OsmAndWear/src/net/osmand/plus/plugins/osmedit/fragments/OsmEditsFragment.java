package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.plugins.osmedit.OsmEditingPlugin.OSM_EDIT_TAB;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.PointDescription;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dialogs.ProgressDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditsAdapter;
import net.osmand.plus.plugins.osmedit.OsmEditsUploadListener;
import net.osmand.plus.plugins.osmedit.asynctasks.ShareOsmPointsAsyncTask;
import net.osmand.plus.plugins.osmedit.asynctasks.ShareOsmPointsAsyncTask.ShareOsmPointsListener;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadOpenstreetmapPointAsyncTask;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Group;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.ExportOptionsBottomSheetDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.ExportOptionsBottomSheetDialogFragment.ExportOptionsFragmentListener;
import net.osmand.plus.plugins.osmedit.dialogs.FileTypeBottomSheetDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.FileTypeBottomSheetDialogFragment.FileTypeFragmentListener;
import net.osmand.plus.plugins.osmedit.dialogs.OsmEditOptionsBottomSheetDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.OsmEditOptionsBottomSheetDialogFragment.OsmEditOptionsFragmentListener;
import net.osmand.plus.plugins.osmedit.dialogs.ProgressDialogPoiUploader;
import net.osmand.plus.plugins.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.dialogs.SendPoiBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil.OnNodeCommittedListener;
import net.osmand.plus.plugins.osmedit.helpers.OsmEditsUploadListenerHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OsmEditsFragment extends OsmAndListFragment implements ProgressDialogPoiUploader,
		OnNodeCommittedListener, FragmentStateHolder, OsmAuthorizationListener, ShareOsmPointsListener {

	public static final int EXPORT_TYPE_ALL = 0;
	public static final int EXPORT_TYPE_POI = 1;
	public static final int EXPORT_TYPE_NOTES = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({EXPORT_TYPE_ALL, EXPORT_TYPE_POI, EXPORT_TYPE_NOTES})
	public @interface ExportTypesDef {
	}

	public static final int FILE_TYPE_OSC = 0;
	public static final int FILE_TYPE_GPX = 1;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({FILE_TYPE_OSC, FILE_TYPE_GPX})
	public @interface FileTypesDef {
	}

	private static final String EXPORT_TYPE_KEY = "export_type";

	private static final int MODE_DELETE = 100;
	private static final int MODE_UPLOAD = 101;

	private OsmandApplication app;
	private OsmandSettings settings;
	private OsmEditingPlugin plugin;

	private List<OsmPoint> osmEdits = new ArrayList<>();
	private final ArrayList<OsmPoint> osmEditsSelected = new ArrayList<>();

	private View emptyView;
	private View footerView;
	private OsmEditsAdapter listAdapter;

	private ActionMode actionMode;
	private long refreshId;
	private int selectedItemPosition = -1;

	private int exportType;
	private boolean nightMode;

	public static void getOsmEditView(View v, OsmPoint child, OsmandApplication app) {
		TextView viewName = v.findViewById(R.id.name);
		ImageView icon = v.findViewById(R.id.icon);
		String name = OsmEditingPlugin.getEditName(child);
		viewName.setText(name);
		if (child.getGroup() == Group.POI) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_info_dark, R.color.color_distance));
		} else if (child.getGroup() == Group.BUG) {
			icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_osm_note_add, R.color.color_distance));
		}

		TextView descr = v.findViewById(R.id.description);
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
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		settings = app.getSettings();
		plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		nightMode = !settings.isLightContent();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			exportType = savedInstanceState.getInt(EXPORT_TYPE_KEY);
		}
		setHasOptionsMenu(true);

		View view = inflater.inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ViewStub emptyStub = view.findViewById(R.id.empty_view_stub);
		emptyStub.setLayoutResource(R.layout.empty_state_osm_edits);
		emptyView = emptyStub.inflate();
		emptyView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		emptyImageView.setImageResource(nightMode ? R.drawable.ic_empty_state_osm_edits_night : R.drawable.ic_empty_state_osm_edits_day);

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
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((ActionBarProgressActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((ActionBarProgressActivity) getActivity()).getClearToolbar(false);
		}
		((ActionBarProgressActivity) getActivity()).updateListViewFooter(footerView);

		MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).setIcon(R.drawable.ic_action_export);
		item.setOnMenuItemClickListener(uploadItem -> {
			enterSelectionMode(MODE_UPLOAD);
			return true;
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		Drawable shareIcon = app.getUIUtilities().getIcon((R.drawable.ic_action_gshare_dark));
		item = menu.add(R.string.shared_string_export)
				.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon));
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(exportItem -> {
			Bundle args = new Bundle();
			args.putInt(ExportOptionsBottomSheetDialogFragment.POI_COUNT_KEY, getOsmEditsByGroup(Group.POI).size());
			args.putInt(ExportOptionsBottomSheetDialogFragment.NOTES_COUNT_KEY, getOsmEditsByGroup(Group.BUG).size());
			ExportOptionsBottomSheetDialogFragment fragment = new ExportOptionsBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.setListener(createExportOptionsFragmentListener());
			fragment.show(getChildFragmentManager(), ExportOptionsBottomSheetDialogFragment.TAG);
			return true;
		});
		item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(deleteItem -> {
			enterSelectionMode(MODE_DELETE);
			return true;
		});
	}

	private void enterUploadMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem item = menu.add(R.string.local_openstreetmap_uploadall).setIcon(R.drawable.ic_action_export);
				item.setOnMenuItemClickListener(uploadItem -> {
					if (Algorithms.isEmpty(osmEditsSelected)) {
						app.showToastMessage(R.string.toast_select_edits_for_upload);
					} else {
						uploadItems(osmEditsSelected.toArray(new OsmPoint[0]));
						mode.finish();
					}
					return true;
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
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				MenuItem item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
				item.setOnMenuItemClickListener(deleteItem -> {
					deleteItems(osmEditsSelected);
					mode.finish();
					return true;
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
		((MyPlacesActivity) getActivity()).setToolbarVisibility(!selectionMode && AndroidUiHelper.isOrientationPortrait(getActivity()));
		((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);
	}

	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}

	private void deleteItems(ArrayList<OsmPoint> points) {
		DeleteOsmEditsConfirmDialogFragment.createInstance(points).show(getChildFragmentManager(), DeleteOsmEditsConfirmDialogFragment.TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
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
		List<OsmNotesPoint> l2 = plugin.getDBBug().getOsmBugsPoints();
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
		listAdapter = new OsmEditsAdapter(app, items);
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
			public void onItemShowMap(OsmPoint point, int position) {
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

	private void showBugDialog(OsmNotesPoint point) {
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.open_bug, null);
		view.findViewById(R.id.user_name_field).setVisibility(View.GONE);
		view.findViewById(R.id.userNameEditTextLabel).setVisibility(View.GONE);
		view.findViewById(R.id.password_field).setVisibility(View.GONE);
		view.findViewById(R.id.passwordEditTextLabel).setVisibility(View.GONE);
		String text = point.getText();
		if (!Algorithms.isEmpty(text)) {
			((EditText) view.findViewById(R.id.message_field)).setText(text);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.shared_string_commit);
		builder.setView(view);
		builder.setPositiveButton(R.string.osn_modify_dialog_title, (dialog, which) -> {
			String text1 = ((EditText) view.findViewById(R.id.message_field)).getText().toString();
			plugin.getDBBug().updateOsmBug(point.getId(), text1);
			point.setText(text1);
			notifyDataSetChanged();
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.create().show();
	}

	@Override
	public void onNoteCommitted() {
		app.runInUIThread(this::fetchData);
	}

	private void openPopUpMenu(OsmPoint info) {
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
				uploadItems(new OsmPoint[] {getPointAfterModify(osmPoint)});
			}

			@Override
			public void onShowOnMapClick(OsmPoint osmPoint) {
				settings.setMapLocationToShow(osmPoint.getLatitude(), osmPoint.getLongitude(), settings.getLastKnownMapZoom());
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}

			@Override
			public void onModifyOsmChangeClick(OsmPoint osmPoint) {
				OpenstreetmapPoint point = (OpenstreetmapPoint) getPointAfterModify(osmPoint);
				Entity entity = point.getEntity();
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
		return type -> {
			exportType = type;
			openFileTypeMenu();
		};
	}

	private FileTypeFragmentListener createFileTypeFragmentListener() {
		return type -> {
			List<OsmPoint> points = getPointsToExport();
			ShareOsmPointsAsyncTask backupTask = new ShareOsmPointsAsyncTask(app, type, exportType, OsmEditsFragment.this);
			backupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, points.toArray(new OsmPoint[0]));
		};
	}

	@Override
	public void shareOsmPointsStarted() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	public void shareOsmPointsFinished() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
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

	private void uploadItems(OsmPoint[] points) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (hasPoiGroup(points)) {
				if (app.getOsmOAuthHelper().isLogged(plugin)) {
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

		Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
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
		((MyPlacesActivity) getActivity()).showOnMap(this, osmPoint.getLatitude(), osmPoint.getLongitude(), 15,
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

		public static DeleteOsmEditsConfirmDialogFragment createInstance(ArrayList<OsmPoint> points) {
			DeleteOsmEditsConfirmDialogFragment fragment = new DeleteOsmEditsConfirmDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(POINTS_LIST, points);
			fragment.setArguments(args);
			return fragment;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			OsmEditsFragment parentFragment = (OsmEditsFragment) getParentFragment();
			OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
			List<OsmPoint> points = (List<OsmPoint>) AndroidUtils.getSerializable(getArguments(), POINTS_LIST, ArrayList.class);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			assert points != null;
			builder.setMessage(getString(R.string.local_osm_changes_delete_all_confirm, points.size()));
			builder.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
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
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
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
		if (bundle != null && bundle.containsKey(TAB_ID) && bundle.containsKey(ITEM_POSITION) && bundle.getInt(TAB_ID) == OSM_EDIT_TAB) {
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