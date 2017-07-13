package net.osmand.plus.audionotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class NotesFragment extends OsmAndListFragment {
	private static final Log LOG = PlatformUtil.getLog(NotesFragment.class);

	AudioVideoNotesPlugin plugin;
	List<AudioVideoNotesPlugin.Recording> items;
	NotesAdapter listAdapter;
	private View footerView;

	private boolean selectionMode = false;

	private final static int MODE_DELETE = 100;
	private final static int MODE_SHARE = 101;
	
	private ActionMode actionMode;

	private ArrayList<AudioVideoNotesPlugin.Recording> selected = new ArrayList<>();
	Recording shareLocationFile = new Recording(new File("."));

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.select_all).setVisibility(View.GONE);
		((TextView) view.findViewById(R.id.header)).setText(R.string.notes);
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
	
	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}
	
	private void selectAll() {
		for (int i = 0; i < listAdapter.getCount(); i++) {
			Recording point = listAdapter.getItem(i);
			if (!selected.contains(point)) {
				selected.add(point);
			}
		}
		listAdapter.notifyDataSetInvalidated();
	}

	private void deselectAll(){
		selected.clear();
		listAdapter.notifyDataSetInvalidated();
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
		items = new ArrayList<>(plugin.getAllRecordings());
		sortItemsDescending();
		ListView listView = getListView();
		if (items.size() > 0 && footerView == null) {
			//listView.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_header, null, false));
			footerView = getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
			listView.setHeaderDividersEnabled(false);
			listView.setFooterDividersEnabled(false);
		}
		listAdapter = new NotesAdapter(items);
		listView.setAdapter(listAdapter);
	}

	private void sortItemsDescending() {
		Collections.sort(items, new Comparator<Recording>() {
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

		MenuItem item = menu.add(R.string.shared_string_share).
				setIcon(R.drawable.ic_action_export);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				enterSelectionMode(MODE_SHARE);
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

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
	
	private void enterSelectionMode(int type){
		enterDeleteMode(type);
	}
	
	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}
	
	private void enableSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		View view = getView();
		if (view != null) {
			view.findViewById(R.id.select_all).setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			((FavoritesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
					AndroidUiHelper.isOrientationPortrait(getActivity()));
			((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
		}
	}
	
	private void updateSelectionTitle(ActionMode m){
		if(selected.size() > 0) {
			m.setTitle(selected.size() + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else{
			m.setTitle("");
		}
	}
	
	private void updateSelectionMode(ActionMode m) {
		updateSelectionTitle(m);
		refreshSelectAll();
	}
	
	private void refreshSelectAll() {
		View view = getView();
		if (view == null) {
			return;
		}
		CheckBox selectAll = (CheckBox) view.findViewById(R.id.select_all);
		for (int i = 0; i < listAdapter.getCount(); i++) {
			Recording point = listAdapter.getItem(i);
			if (!selected.contains(point)) {
				selectAll.setChecked(false);
				return;
			}
		}
		selectAll.setChecked(true);
	}
	
	private void deleteItems(final ArrayList<Recording> selected) {
		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
		b.setMessage(getString(R.string.local_recordings_delete_all_confirm, selected.size()));
		b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Iterator<Recording> it = selected.iterator();
				while (it.hasNext()) {
					Recording pnt = it.next();
					plugin.deleteRecording(pnt, true);
					it.remove();
					listAdapter.delete(pnt);
				}
				listAdapter.notifyDataSetChanged();

			}
		});
		b.setNegativeButton(R.string.shared_string_cancel, null);
		b.show();		
	}
	
	private void shareItems(ArrayList<Recording> selected) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("image/*"); /* This example is sharing jpeg images. */
		ArrayList<Uri> files = new ArrayList<Uri>();
		for(Recording path : selected) {
			if(path == shareLocationFile) {
				File fl = generateGPXForRecordings(selected);
				if(fl != null) {
					files.add(FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", fl));
				}
			} else {
				File src = path.getFile();
				File dst = new File(getActivity().getCacheDir(), "share/"+src.getName());
				try {
					Algorithms.fileCopy(src, dst);
					files.add(FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", dst));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		startActivity(Intent.createChooser(intent, getString(R.string.share_note)));
	}
	
	private File generateGPXForRecordings(ArrayList<Recording> selected) {
//		File tmpFile = getMyApplication().getAppPath("cache/noteLocations.gpx");
		File tmpFile = new File(getActivity().getCacheDir(), "share/noteLocations.gpx");
		tmpFile.getParentFile().mkdirs();
		GPXFile file = new GPXFile();
		for(Recording r : selected) {
			if(r != shareLocationFile) {
				String desc = r.getDescriptionName(r.getFileName());
				if(desc == null) {
					desc = r.getFileName();
				}
				WptPt wpt = new WptPt();
				wpt.lat = r.getLatitude();
				wpt.lon = r.getLongitude();
				wpt.name = desc;
				wpt.link = r.getFileName();
				wpt.time = r.getFile().lastModified();
				wpt.category = r.getSearchHistoryType();
				file.points.add(wpt);
			}
		}
		GPXUtilities.writeGpxFile(tmpFile, file, getMyApplication());
		return tmpFile;
	}

	private void enterDeleteMode(final int type) {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
				LOG.debug("onCreateActionMode");
				if(type == MODE_SHARE) {
					listAdapter.insert(shareLocationFile, 0);
				}
				enableSelectionMode(true);
				MenuItem item;
				if(type == MODE_DELETE) {
					item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
				} else {
					item = menu.add(R.string.shared_string_share).setIcon(R.drawable.ic_action_export);
				}
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if(type == MODE_DELETE) {
							deleteItems(selected);
						} else if(type == MODE_SHARE) {
							shareItems(selected);
						}
						mode.finish();
						return true;
					}
				});
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				selected.clear();
				listAdapter.notifyDataSetInvalidated();
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
				if(type == MODE_SHARE) {
					listAdapter.remove(shareLocationFile);
				}
				enableSelectionMode(false);
				listAdapter.notifyDataSetInvalidated();
			}

		});
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	class NotesAdapter extends ArrayAdapter<AudioVideoNotesPlugin.Recording> {

		NotesAdapter(List<AudioVideoNotesPlugin.Recording> recordingList) {
			super(getActivity(), R.layout.note, recordingList);
		}

		public void delete(Recording pnt) {
			remove(pnt);
			
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.note, parent, false);
			}

			final AudioVideoNotesPlugin.Recording recording = getItem(position);
			if (recording == shareLocationFile) {
				((TextView) row.findViewById(R.id.name)).setText(R.string.av_locations);
				((TextView) row.findViewById(R.id.description)).setText(R.string.av_locations_descr);
			} else {
				DashAudioVideoNotesFragment.getNoteView(recording, row, getMyApplication());
			}
//			((ImageView) row.findViewById(R.id.play)).setImageDrawable(getMyApplication().getIconsCache()
//					.getIcon(R.drawable.ic_play_dark));
			row.findViewById(R.id.play).setVisibility(View.GONE);
			
			
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_local_index);
			ImageButton options = (ImageButton) row.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
			if(selectionMode) {
				options.setVisibility(View.GONE);
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selected.contains(recording));
				row.findViewById(R.id.icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onItemSelect(ch, recording);
					}
				});
			} else {
				row.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				options.setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}
			
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openPopUpMenu(v, recording);
				}
			});
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (selectionMode) {
						ch.setChecked(!ch.isChecked());
						onItemSelect(ch, recording);
					} else {
						showOnMap(recording);
					}
				}
			});
			return row;
		}
		
		public void onItemSelect(CheckBox ch, Recording child) {
			if (ch.isChecked()) {
				selected.add(child);
			} else {
				selected.remove(child);
			}
			updateSelectionMode(actionMode);
		}
	}

	private void showOnMap(Recording recording) {
		getMyApplication().getSettings().setMapLocationToShow(recording.getLatitude(), recording.getLongitude(), 15,
				new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity(), true)), true,
				recording); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void openPopUpMenu(View v, final AudioVideoNotesPlugin.Recording recording) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		MenuItem item;
		boolean isPhoto = recording.isPhoto();
		Drawable playIcon;
		if (isPhoto) {
			playIcon = getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_view);
		} else {
			playIcon = getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_play_dark);
		}
		item = optionsMenu.getMenu().add(isPhoto ? R.string.watch : R.string.recording_context_menu_play)
				.setIcon(playIcon);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				plugin.playRecording(getActivity(), recording);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_show_on_map).setIcon(
				iconsCache.getThemedIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showOnMap(recording);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_share)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_gshare_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				if (recording.isPhoto()) {
					Uri screenshotUri = Uri.parse(recording.getFile().getAbsolutePath());
					sharingIntent.setType("image/*");
					sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
				} else if (recording.isAudio()) {
					Uri audioUri = Uri.parse(recording.getFile().getAbsolutePath());
					sharingIntent.setType("audio/*");
					sharingIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
				} else if (recording.isVideo()) {
					Uri videoUri = Uri.parse(recording.getFile().getAbsolutePath());
					sharingIntent.setType("video/*");
					sharingIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
				}
				startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_note)));
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_rename)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				editNote(recording);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.recording_context_menu_delete)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.recording_delete_confirm);
				builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						plugin.deleteRecording(recording, true);
						listAdapter.remove(recording);
					}
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
				return true;
			}
		});
		optionsMenu.show();
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

}
