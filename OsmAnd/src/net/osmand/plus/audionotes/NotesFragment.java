package net.osmand.plus.audionotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.audionotes.ItemMenuBottomSheetDialogFragment.ItemMenuFragmentListener;
import net.osmand.plus.audionotes.SortByMenuBottomSheetDialogFragment.SortFragmentListener;
import net.osmand.plus.audionotes.adapters.NotesAdapter;
import net.osmand.plus.audionotes.adapters.NotesAdapter.NotesAdapterListener;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.FavoritesFragmentStateHolder;

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

import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.NOTES_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

public class NotesFragment extends OsmAndListFragment implements FavoritesFragmentStateHolder {

	public static final Recording SHARE_LOCATION_FILE = new Recording(new File("."));

	private static final Log LOG = PlatformUtil.getLog(NotesFragment.class);
	private static final int MODE_DELETE = 100;
	private static final int MODE_SHARE = 101;

	private AudioVideoNotesPlugin plugin;
	private NotesAdapter listAdapter;
	private Set<Recording> selected = new HashSet<>();

	private View footerView;
	private View emptyView;

	private boolean selectionMode;
	private int selectedItemPosition = -1;

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

		plugin = OsmandPlugin.getActivePlugin(AudioVideoNotesPlugin.class);
		setHasOptionsMenu(true);

		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ViewStub emptyStub = (ViewStub) view.findViewById(R.id.empty_view_stub);
		emptyStub.setLayoutResource(R.layout.empty_state_av_notes);
		emptyView = emptyStub.inflate();
		emptyView.setBackgroundColor(getResources().getColor(getMyApplication().getSettings()
				.isLightContent() ? R.color.activity_background_color_light : R.color.activity_background_color_dark));
		ImageView emptyImageView = (ImageView) emptyView.findViewById(R.id.empty_state_image_view);

		if (Build.VERSION.SDK_INT >= 18) {
			int icRes = getMyApplication().getSettings().isLightContent()
					? R.drawable.ic_empty_state_av_notes_day : R.drawable.ic_empty_state_av_notes_night;
			emptyImageView.setImageResource(icRes);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setBackgroundColor(getResources().getColor(getMyApplication().getSettings()
				.isLightContent() ? R.color.activity_background_color_light : R.color.activity_background_color_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		List<Object> items = createItemsList();
		ListView listView = getListView();
		listView.setDivider(null);
		listView.setEmptyView(emptyView);
		if (items.size() > 0 && footerView == null && portrait) {
			footerView = getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
			listView.setHeaderDividersEnabled(false);
			listView.setFooterDividersEnabled(false);
		}
		listAdapter = new NotesAdapter(getMyApplication(), items);
		listAdapter.setSelectionMode(selectionMode);
		listAdapter.setSelected(selected);
		listAdapter.setListener(createAdapterListener());
		listAdapter.setPortrait(portrait);
		listView.setAdapter(listAdapter);
		restoreState(getArguments());
	}

	@Override
	public void onPause() {
		super.onPause();
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		menu.clear();
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			menu = ((ActionBarProgressActivity) activity).getClearToolbar(true).getMenu();
		} else {
			((ActionBarProgressActivity) activity).getClearToolbar(false);
		}
		((ActionBarProgressActivity) activity).updateListViewFooter(footerView);

		MenuItem item = menu.add(R.string.shared_string_sort).setIcon(R.drawable.ic_action_list_sort);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showSortMenuFragment();
				return true;
			}
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		Drawable shareIcon = AndroidUtils.getDrawableForDirection(activity,
				getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_gshare_dark));
		item = menu.add(R.string.shared_string_share).setIcon(shareIcon);
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
		if (!recs.isEmpty()) {
			NotesSortByMode sortByMode = getMyApplication().getSettings().NOTES_SORT_BY_MODE.get();
			if (sortByMode.isByDate()) {
				res.add(NotesAdapter.TYPE_DATE_HEADER);
				res.addAll(sortRecsByDateDescending(recs));
			} else if (sortByMode.isByType()) {
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
				addToResIfNotEmpty(res, audios, NotesAdapter.TYPE_AUDIO_HEADER);
				addToResIfNotEmpty(res, photos, NotesAdapter.TYPE_PHOTO_HEADER);
				addToResIfNotEmpty(res, videos, NotesAdapter.TYPE_VIDEO_HEADER);
			}
		}
		return res;
	}

	private void addToResIfNotEmpty(List<Object> res, List<Recording> recs, int header) {
		if (!recs.isEmpty()) {
			res.add(header);
			res.addAll(sortRecsByDateDescending(recs));
		}
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
			public void onItemClick(Recording rec, int position) {
				showOnMap(rec, position);
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
			for (int i = 0; i < listAdapter.getItemsCount(); i++) {
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

	private List<Recording> sortRecsByDateDescending(List<Recording> recs) {
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
				recreateAdapterData();
			}
		};
	}

	private void recreateAdapterData() {
		listAdapter.clear();
		listAdapter.addAll(createItemsList());
		listAdapter.notifyDataSetChanged();
	}

	private void enterSelectionMode(final int type) {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
				LOG.debug("onCreateActionMode");
				OsmandApplication app = getMyApplication();
				if (type == MODE_SHARE) {
					listAdapter.insert(SHARE_LOCATION_FILE, 0);
				}
				switchSelectionMode(true);
				int titleRes = type == MODE_DELETE ? R.string.shared_string_delete_all : R.string.shared_string_share;
				int iconRes = type == MODE_DELETE ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_gshare_dark;
				Drawable icon = AndroidUtils.getDrawableForDirection(app,
						app.getUIUtilities().getIcon(iconRes));
				MenuItem item = menu.add(titleRes).setIcon(icon);
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
						}
						recreateAdapterData();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void shareItems(Set<Recording> selected) {
		ArrayList<Uri> uris = new ArrayList<>();
		for (Recording rec : selected) {
			File file = rec == SHARE_LOCATION_FILE ? generateGPXForRecordings(selected) : rec.getFile();
			if (file != null) {
				uris.add(AndroidUtils.getUriForFile(getMyApplication(), file));
			}
		}

		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_STREAM, uris);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (Build.VERSION.SDK_INT > 18) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivity(Intent.createChooser(intent, getString(R.string.share_note)));
	}

	private Set<Recording> getRecordingsForGpx(Set<Recording> selected) {
		if (selected.size() == 1 && selected.contains(SHARE_LOCATION_FILE)) {
			return new HashSet<>(plugin.getAllRecordings());
		}
		return selected;
	}

	private File generateGPXForRecordings(Set<Recording> selected) {
		File tmpFile = new File(getActivity().getCacheDir(), "share/noteLocations.gpx");
		tmpFile.getParentFile().mkdirs();
		GPXFile file = new GPXFile(Version.getFullVersion(getMyApplication()));
		for (Recording r : getRecordingsForGpx(selected)) {
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
				wpt.desc = r.getTypeWithDuration(getContext());
				getMyApplication().getSelectedGpxHelper().addPoint(wpt, file);
			}
		}
		GPXUtilities.writeGpxFile(tmpFile, file);
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
		showOnMap(recording, -1);
	}

	private void showOnMap(Recording recording, int itemPosition) {
		selectedItemPosition = itemPosition;
		FavoritesActivity.showOnMap(requireActivity(), this, recording.getLatitude(), recording.getLongitude(), 15,
				new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity(), true)),
				true, recording);
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

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, NOTES_TAB);
		bundle.putInt(ITEM_POSITION, selectedItemPosition);
		return bundle;
	}
	
	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.containsKey(TAB_ID) && bundle.containsKey(ITEM_POSITION)) {
			if (bundle.getInt(TAB_ID) == NOTES_TAB) {
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
