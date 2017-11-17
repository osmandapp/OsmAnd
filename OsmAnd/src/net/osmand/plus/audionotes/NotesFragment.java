package net.osmand.plus.audionotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.audionotes.ItemMenuBottomSheetDialogFragment.ItemMenuFragmentListener;
import net.osmand.plus.audionotes.SortByMenuBottomSheetDialogFragment.SortFragmentListener;
import net.osmand.plus.audionotes.adapters.NotesAdapter;
import net.osmand.plus.audionotes.adapters.NotesAdapter.NotesAdapterListener;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NotesFragment extends OsmAndListFragment {

	public static final Recording SHARE_LOCATION_FILE = new Recording(new File("."));

	private static final Log LOG = PlatformUtil.getLog(NotesFragment.class);
	private static final int MODE_DELETE = 100;
	private static final int MODE_SHARE = 101;

	private AudioVideoNotesPlugin plugin;
	private NotesAdapter listAdapter;
	private Set<Recording> selected = new HashSet<>();

	private View footerView;

	private boolean selectionMode;

	private ActionMode actionMode;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Handle screen rotation:
		FragmentManager fm = getChildFragmentManager();
		Fragment sortByMenu = fm.findFragmentByTag(SortByMenuBottomSheetDialogFragment.TAG);
		if (sortByMenu != null) {
			((SortByMenuBottomSheetDialogFragment) sortByMenu).setListener(createSortFragmentListener());
		}
		Fragment itemMenu = fm.findFragmentByTag(ItemMenuBottomSheetDialogFragment.TAG);
		if (itemMenu != null) {
			((ItemMenuBottomSheetDialogFragment) itemMenu).setListener(createItemMenuFragmentListener());
		}

		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		setHasOptionsMenu(true);

		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(getMyApplication().getSettings()
				.isLightContent() ? R.color.ctx_menu_info_view_bg_light : R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		List<Object> items = createItemsList();
		ListView listView = getListView();
		listView.setDivider(null);
		if (items.size() > 0 && footerView == null) {
			footerView = getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
			listView.setHeaderDividersEnabled(false);
			listView.setFooterDividersEnabled(false);
		}
		listAdapter = new NotesAdapter(getMyApplication(), items);
		listAdapter.setSelectionMode(selectionMode);
		listAdapter.setSelected(selected);
		listAdapter.setListener(createAdapterListener());
		listView.setAdapter(listAdapter);
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
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

		MenuItem item = menu.add(R.string.shared_string_sort).setIcon(R.drawable.ic_action_list_sort);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showSortMenuFragment();
				return true;
			}
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.shared_string_share).setIcon(R.drawable.ic_action_gshare_dark);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_SHARE);
				return true;
			}
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_DELETE);
				return true;
			}
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}

	private List<Object> createItemsList() {
		List<Recording> recs = new LinkedList<>(plugin.getAllRecordings());
		List<Object> res = new LinkedList<>();
		OsmandSettings settings = getMyApplication().getSettings();
		if (settings.NOTES_SORT_BY_MODE.get().isByDate()) {
			res.add(NotesAdapter.TYPE_DATE_HEADER);
			res.addAll(sortItemsByDateDescending(recs));
		} else if (settings.NOTES_SORT_BY_MODE.get().isByType()) {
			List<Recording> audios = new LinkedList<>();
			List<Recording> photos = new LinkedList<>();
			List<Recording> videos = new LinkedList<>();
			for (Recording rec : recs) {
				if (rec.isAudio()) {
					audios.add(rec);
				} else if (rec.isPhoto()) {
					photos.add(rec);
				} else {
					videos.add(rec);
				}
			}
			if (!audios.isEmpty()) {
				res.add(NotesAdapter.TYPE_AUDIO_HEADER);
				res.addAll(audios);
			}
			if (!photos.isEmpty()) {
				res.add(NotesAdapter.TYPE_PHOTO_HEADER);
				res.addAll(photos);
			}
			if (!videos.isEmpty()) {
				res.add(NotesAdapter.TYPE_VIDEO_HEADER);
				res.addAll(videos);
			}
		}
		return res;
	}

	private NotesAdapterListener createAdapterListener() {
		return new NotesAdapterListener() {

			@Override
			public void onHeaderClick(int type, boolean checked) {
				if (checked) {
					selectAll(type);
				} else {
					deselectAll(type);
				}
				updateSelectionTitle(actionMode);
			}

			@Override
			public void onCheckBoxClick(Recording rec, boolean checked) {
				if (selectionMode) {
					if (checked) {
						selected.add(rec);
					} else {
						selected.remove(rec);
					}
					updateSelectionMode(actionMode);
				}
			}

			@Override
			public void onItemClick(Recording rec) {
				showOnMap(rec);
			}

			@Override
			public void onOptionsClick(Recording rec) {
				ItemMenuBottomSheetDialogFragment fragment = new ItemMenuBottomSheetDialogFragment();
				fragment.setUsedOnMap(false);
				fragment.setListener(createItemMenuFragmentListener());
				fragment.setRecording(rec);
				fragment.setRetainInstance(true);
				fragment.show(getChildFragmentManager(), ItemMenuBottomSheetDialogFragment.TAG);
			}
		};
	}

	private void showSortMenuFragment() {
		SortByMenuBottomSheetDialogFragment fragment = new SortByMenuBottomSheetDialogFragment();
		fragment.setUsedOnMap(false);
		fragment.setListener(createSortFragmentListener());
		fragment.show(getChildFragmentManager(), SortByMenuBottomSheetDialogFragment.TAG);
	}

	private List<Recording> getRecordingsByType(int type) {
		List<Recording> allRecs = new LinkedList<>(plugin.getAllRecordings());
		List<Recording> res = new LinkedList<>();
		for (Recording rec : allRecs) {
			if (isAppropriate(rec, type)) {
				res.add(rec);
			}
		}
		return res;
	}

	private boolean isAppropriate(Recording rec, int type) {
		if (type == NotesAdapter.TYPE_AUDIO_HEADER) {
			return rec.isAudio();
		} else if (type == NotesAdapter.TYPE_PHOTO_HEADER) {
			return rec.isPhoto();
		}
		return rec.isVideo();
	}

	private void selectAll(int type) {
		if (type == NotesAdapter.TYPE_DATE_HEADER) {
			for (int i = 0; i < listAdapter.getCount(); i++) {
				Object item = listAdapter.getItem(i);
				if (item instanceof Recording) {
					selected.add((Recording) item);
				}
			}
		} else {
			selected.addAll(getRecordingsByType(type));
		}
		listAdapter.notifyDataSetChanged();
	}

	private void deselectAll(int type) {
		if (type == NotesAdapter.TYPE_DATE_HEADER) {
			selected.clear();
		} else {
			selected.removeAll(getRecordingsByType(type));
		}
		listAdapter.notifyDataSetChanged();
	}

	private List<Recording> sortItemsByDateDescending(List<Recording> recs) {
		Collections.sort(recs, new Comparator<Recording>() {
			@Override
			public int compare(Recording first, Recording second) {
				long firstTime = first.getLastModified();
				long secondTime = second.getLastModified();
				if (firstTime < secondTime) {
					return 1;
				} else if (firstTime == secondTime) {
					return 0;
				} else {
					return -1;
				}
			}
		});
		return recs;
	}

	private SortFragmentListener createSortFragmentListener() {
		return new SortFragmentListener() {
			@Override
			public void onSortModeChanged() {
				listAdapter.clear();
				listAdapter.addAll(createItemsList());
				listAdapter.notifyDataSetChanged();
			}
		};
	}

	private void enterSelectionMode(final int type) {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
				LOG.debug("onCreateActionMode");
				if (type == MODE_SHARE) {
					listAdapter.insert(SHARE_LOCATION_FILE, 0);
				}
				switchSelectionMode(true);
				int titleRes = type == MODE_DELETE ? R.string.shared_string_delete_all : R.string.shared_string_share;
				int iconRes = type == MODE_DELETE ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_gshare_dark;
				MenuItem item = menu.add(titleRes).setIcon(iconRes);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (type == MODE_DELETE) {
							deleteItems(selected);
						} else if (type == MODE_SHARE) {
							shareItems(selected);
						}
						mode.finish();
						return true;
					}
				});
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
				selected.clear();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				LOG.debug("onPrepareActionMode");
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
				LOG.debug("onActionItemClicked");
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				LOG.debug("onDestroyActionMode");
				if (type == MODE_SHARE) {
					listAdapter.remove(SHARE_LOCATION_FILE);
				}
				switchSelectionMode(false);
				listAdapter.notifyDataSetInvalidated();
			}
		});
	}

	private void switchSelectionMode(boolean enable) {
		selectionMode = enable;
		listAdapter.setSelectionMode(enable);
		((FavoritesActivity) getActivity()).setToolbarVisibility(!enable && AndroidUiHelper.isOrientationPortrait(getActivity()));
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
	}

	private void updateSelectionTitle(ActionMode m) {
		if (selected.size() > 0) {
			m.setTitle(selected.size() + " " + getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	private void updateSelectionMode(ActionMode m) {
		updateSelectionTitle(m);
		listAdapter.notifyDataSetChanged();
	}

	private void deleteItems(final Set<Recording> selected) {
		new AlertDialog.Builder(getActivity())
				.setMessage(getString(R.string.local_recordings_delete_all_confirm, selected.size()))
				.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Iterator<Recording> it = selected.iterator();
						while (it.hasNext()) {
							Recording rec = it.next();
							plugin.deleteRecording(rec, true);
							it.remove();
							listAdapter.remove(rec);
						}
						listAdapter.notifyDataSetChanged();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void shareItems(Set<Recording> selected) {
		ArrayList<Uri> files = new ArrayList<>();
		for (Recording rec : selected) {
			File file = rec == SHARE_LOCATION_FILE ? generateGPXForRecordings(selected) : rec.getFile();
			if (file != null) {
				files.add(Uri.parse(file.getAbsolutePath()));
			}
		}
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_STREAM, files);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (Build.VERSION.SDK_INT > 18) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivity(Intent.createChooser(intent, getString(R.string.share_note)));
	}

	private File generateGPXForRecordings(Set<Recording> selected) {
		File tmpFile = new File(getActivity().getCacheDir(), "share/noteLocations.gpx");
		tmpFile.getParentFile().mkdirs();
		GPXFile file = new GPXFile();
		for (Recording r : selected) {
			if (r != SHARE_LOCATION_FILE) {
				String desc = r.getDescriptionName(r.getFileName());
				if (desc == null) {
					desc = r.getFileName();
				}
				WptPt wpt = new WptPt();
				wpt.lat = r.getLatitude();
				wpt.lon = r.getLongitude();
				wpt.name = desc;
				wpt.link = r.getFileName();
				wpt.time = r.getFile().lastModified();
				wpt.category = r.getSearchHistoryType();
				getMyApplication().getSelectedGpxHelper().addPoint(wpt, file);
			}
		}
		GPXUtilities.writeGpxFile(tmpFile, file, getMyApplication());
		return tmpFile;
	}

	private ItemMenuFragmentListener createItemMenuFragmentListener() {
		return new ItemMenuFragmentListener() {
			@Override
			public void playOnClick(Recording recording) {
				plugin.playRecording(getActivity(), recording);
			}

			@Override
			public void shareOnClick(Recording recording) {
				shareNote(recording);
			}

			@Override
			public void showOnMapOnClick(Recording recording) {
				showOnMap(recording);
			}

			@Override
			public void renameOnClick(Recording recording) {
				editNote(recording);
			}

			@Override
			public void deleteOnClick(final Recording recording) {
				deleteNote(recording);
			}
		};
	}

	private void shareNote(final Recording recording) {
		if (!recording.getFile().exists()) {
			return;
		}
		MediaScannerConnection.scanFile(getActivity(), new String[]{recording.getFile().getAbsolutePath()},
				null, new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
						Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
						if (recording.isPhoto()) {
							shareIntent.setType("image/*");
						} else if (recording.isAudio()) {
							shareIntent.setType("audio/*");
						} else if (recording.isVideo()) {
							shareIntent.setType("video/*");
						}
						shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
						shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
						startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)));
					}
				});
	}

	private void showOnMap(Recording recording) {
		getMyApplication().getSettings().setMapLocationToShow(recording.getLatitude(), recording.getLongitude(), 15,
				new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity(), true)),
				true, recording);
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void editNote(final Recording recording) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.shared_string_rename);
		final View v = getActivity().getLayoutInflater().inflate(R.layout.note_edit_dialog, getListView(), false);
		final EditText editText = (EditText) v.findViewById(R.id.name);
		builder.setView(v);
		editText.setText(recording.getName(getActivity(), true));
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (!recording.setName(editText.getText().toString())) {
					Toast.makeText(getActivity(), R.string.rename_failed, Toast.LENGTH_SHORT).show();
				}
				listAdapter.notifyDataSetInvalidated();
			}
		});
		builder.create().show();
		editText.requestFocus();
	}

	private void deleteNote(final Recording recording) {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.recording_delete_confirm)
				.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						plugin.deleteRecording(recording, true);
						listAdapter.remove(recording);
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}
}
