package net.osmand.plus.audionotes;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.myplaces.FavoritesActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Denis on 18.02.2015.
 */
public class NotesFragment extends OsmAndListFragment {
	AudioVideoNotesPlugin plugin;
	List<AudioVideoNotesPlugin.Recording> items;
	NotesAdapter listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.select_all).setVisibility(View.GONE);
		((TextView) view.findViewById(R.id.header)).setText(R.string.notes);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		items = new ArrayList<Recording>(plugin.getAllRecordings());
		listAdapter = new NotesAdapter(items);
		getListView().setAdapter(listAdapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		((FavoritesActivity) getActivity()).getClearToolbar(false);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	class NotesAdapter extends ArrayAdapter<AudioVideoNotesPlugin.Recording> {

		NotesAdapter(List<AudioVideoNotesPlugin.Recording> recordingList) {
			super(getActivity(), R.layout.note, recordingList);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.note, parent, false);
			}

			final AudioVideoNotesPlugin.Recording recording = getItem(position);
			Drawable icon = DashAudioVideoNotesFragment.getNoteView(recording, row, getMyApplication());
			icon.setColorFilter(getResources().getColor(R.color.color_distance), Mode.MULTIPLY);
			((ImageView) row.findViewById(R.id.play)).setImageDrawable(getMyApplication().getIconsCache()
					.getContentIcon(R.drawable.ic_play_dark));
			row.findViewById(R.id.play).setVisibility(View.GONE);
			ImageButton options = (ImageButton) row.findViewById(R.id.options);
			options.setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openPopUpMenu(v, recording);
				}
			});
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOnMap(recording);
				}
			});
			return row;
		}
	}

	private void showOnMap(Recording recording) {
		getMyApplication().getSettings().setMapLocationToShow(recording.getLatitude(), recording.getLongitude(), 15,
				new PointDescription(recording.getSearchHistoryType(), recording.getName(getActivity())), true,
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
			playIcon = getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_view);
		} else {
			playIcon = getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_play_dark);
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
				iconsCache.getContentIcon(R.drawable.ic_show_on_map));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showOnMap(recording);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.shared_string_share)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_gshare_dark));
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
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				editNote(recording);
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.recording_context_menu_delete)
				.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.recording_delete_confirm);
				builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						plugin.deleteRecording(recording);
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
		editText.setText(recording.getName(getActivity()));
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
