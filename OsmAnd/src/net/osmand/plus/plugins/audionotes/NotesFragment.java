package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.NOTES_TAB;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.base.BaseOsmAndListFragment;
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper;
import net.osmand.plus.gallery.data.Cancellable;
import net.osmand.plus.gallery.data.GalleryMediaMetadata;
import net.osmand.plus.gallery.data.MediaMetadataListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu.NativeShareDialogBuilder;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.ItemMenuBottomSheetDialogFragment.ItemMenuFragmentListener;
import net.osmand.plus.plugins.audionotes.adapters.NotesAdapter;
import net.osmand.plus.plugins.audionotes.adapters.NotesAdapter.NotesAdapterListener;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Linkable;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.media.LinkMediaFactory;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotesFragment extends BaseOsmAndListFragment implements FragmentStateHolder {

	public static final MediaNote SHARE_LOCATION_FILE = MediaNote.createShareLocationItem();

	private static final Log LOG = PlatformUtil.getLog(NotesFragment.class);
	private static final int MODE_DELETE = 100;
	private static final int MODE_SHARE = 101;

	private AudioVideoNotesPlugin plugin;
	private AttachedMediaDataHelper attachedMediaDataHelper;
	private MediaStorageHelper mediaStorageHelper;
	private NotesAdapter listAdapter;
	private final Set<MediaNote> selected = new HashSet<>();

	private ShareRecordingsTask shareRecordingsTask;
	private Cancellable metadataRequest;

	private View footerView;
	private View emptyView;

	private boolean selectionMode;
	private int selectedItemPosition = -1;

	private ActionMode actionMode;
	private final FavoritesListener favoritesListener = new FavoritesListener() {
		@Override
		public void onFavoritesLoaded() {
			app.runInUIThread(NotesFragment.this::reloadMediaNotes);
		}

		@Override
		public void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
			app.runInUIThread(NotesFragment.this::reloadMediaNotes);
		}
	};

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		FragmentManager fm = getChildFragmentManager();
		Fragment itemMenu = fm.findFragmentByTag(ItemMenuBottomSheetDialogFragment.TAG);
		if (itemMenu != null) {
			((ItemMenuBottomSheetDialogFragment) itemMenu).setListener(createItemMenuFragmentListener());
		}

		plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
		attachedMediaDataHelper = new AttachedMediaDataHelper(app);
		mediaStorageHelper = new MediaStorageHelper(app);
		setHasOptionsMenu(true);

		View view = inflate(R.layout.update_index, container, false);
		view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		ViewStub emptyStub = view.findViewById(R.id.empty_view_stub);
		emptyStub.setLayoutResource(R.layout.empty_state_av_notes);
		emptyView = emptyStub.inflate();
		emptyView.setBackgroundColor(getBackgroundColor());
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);

		int icRes = !nightMode ? R.drawable.ic_empty_state_av_notes_day : R.drawable.ic_empty_state_av_notes_night;
		emptyImageView.setImageResource(icRes);

		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = new InsetTargetsCollection();
		collection.add(InsetTarget.createScrollable(android.R.id.list).build());
		return collection;
	}

	@Override
	public void onStart() {
		super.onStart();
		app.getFavoritesHelper().addListener(favoritesListener);
	}

	@Override
	public void onStop() {
		app.getFavoritesHelper().removeListener(favoritesListener);
		super.onStop();
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		List<Object> items = createItemsList();
		ListView listView = getListView();
		listView.setDivider(null);
		listView.setEmptyView(emptyView);
		if (!items.isEmpty() && footerView == null && portrait) {
			footerView = inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
			listView.setHeaderDividersEnabled(false);
			listView.setFooterDividersEnabled(false);
		}
		listAdapter = new NotesAdapter(app, items);
		listAdapter.setSelectionMode(selectionMode);
		listAdapter.setSelected(selected);
		listAdapter.setListener(createAdapterListener());
		listAdapter.setPortrait(portrait);
		listView.setAdapter(listAdapter);
		restoreState(getArguments());
		requestAttachedMediaMetadata(items);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (actionMode != null) {
			actionMode.finish();
		}
		if (metadataRequest != null) {
			metadataRequest.cancel();
			metadataRequest = null;
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
		item.setOnMenuItemClickListener(menuItem -> {
			SortByMenuBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager(), this);
			return true;
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		Drawable shareIcon = AndroidUtils.getDrawableForDirection(app, requireIcon(R.drawable.ic_action_gshare_dark));
		item = menu.add(R.string.shared_string_share).setIcon(shareIcon);
		item.setOnMenuItemClickListener(menuItem -> {
			enterSelectionMode(MODE_SHARE);
			return true;
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		item = menu.add(R.string.shared_string_delete_all).setIcon(R.drawable.ic_action_delete_dark);
		item.setOnMenuItemClickListener(menuItem -> {
			enterSelectionMode(MODE_DELETE);
			return true;
		});
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	private List<Object> createItemsList() {
		List<MediaNote> notes = collectMediaNotes();
		List<Object> res = new LinkedList<>();
		if (!notes.isEmpty()) {
			NotesSortByMode sortByMode = plugin.NOTES_SORT_BY_MODE.get();
			if (sortByMode.isByDate()) {
				res.add(NotesAdapter.TYPE_DATE_HEADER);
				res.addAll(sortNotesByDateDescending(notes));
			} else if (sortByMode.isByType()) {
				List<MediaNote> audios = new LinkedList<>();
				List<MediaNote> photos = new LinkedList<>();
				List<MediaNote> videos = new LinkedList<>();
				for (MediaNote note : notes) {
					if (note.isAudio()) {
						audios.add(note);
					} else if (note.isPhoto()) {
						photos.add(note);
					} else {
						videos.add(note);
					}
				}
				addToResIfNotEmpty(res, audios, NotesAdapter.TYPE_AUDIO_HEADER);
				addToResIfNotEmpty(res, photos, NotesAdapter.TYPE_PHOTO_HEADER);
				addToResIfNotEmpty(res, videos, NotesAdapter.TYPE_VIDEO_HEADER);
			}
		}
		return res;
	}

	@NonNull
	private List<MediaNote> collectMediaNotes() {
		List<MediaNote> notes = new ArrayList<>();
		Set<String> recordingIds = new HashSet<>();
		for (Recording recording : plugin.getAllRecordings()) {
			notes.add(MediaNote.fromRecording(recording));
			String href = mediaStorageHelper.createMediaFileHref(recording.getFile());
			String mediaId = getMediaKey(new Link(href));
			if (mediaId != null) {
				recordingIds.add(mediaId);
			}
		}

		for (FavouritePoint point : app.getFavoritesHelper().getFavouritePoints()) {
			addAttachedMediaNotes(notes, recordingIds, point, point.getLatitude(), point.getLongitude());
		}
		for (SelectedGpxFile selectedGpxFile : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			for (WptPt point : selectedGpxFile.getGpxFile().getPointsList()) {
				addAttachedMediaNotes(notes, recordingIds, point, point.getLatitude(), point.getLongitude());
			}
			for (WptPt point : selectedGpxFile.getGpxFile().getRoutePoints()) {
				addAttachedMediaNotes(notes, recordingIds, point, point.getLatitude(), point.getLongitude());
			}
		}
		return notes;
	}

	private void addAttachedMediaNotes(@NonNull List<MediaNote> notes, @NonNull Set<String> recordingIds,
	                                   @NonNull Linkable target, double latitude, double longitude) {
		List<Link> links = target.getLinks();
		if (links == null) {
			return;
		}
		for (Link link : links) {
			if (link == null) {
				continue;
			}
			List<MediaItem> mediaItems = LinkMediaFactory.fromLinks(Collections.singletonList(link));
			if (mediaItems.isEmpty()) {
				continue;
			}
			MediaItem mediaItem = mediaItems.get(0);
			if (recordingIds.contains(getMediaKey(link))) {
				continue;
			}
			boolean remote = mediaItem instanceof MediaItem.Remote;
			if (remote || app.getGalleryHelper().getMediaSourceResolver().getPlaybackUri(mediaItem) != null) {
				notes.add(MediaNote.fromAttachedMedia(mediaItem, target, link, latitude, longitude));
			}
		}
	}

	@Nullable
	private String getMediaKey(@NonNull Link link) {
		String href = link.getHref();
		if (href != null) {
			String internalPath = LinkMediaFactory.getInternalPath(href.trim());
			String fileName = LinkMediaFactory.getInternalMediaFileName(internalPath);
			if (fileName != null && !fileName.isEmpty()) {
				return "internal:" + fileName;
			}
		}
		return LinkMediaFactory.getMediaId(link);
	}

	private void addToResIfNotEmpty(List<Object> res, List<MediaNote> notes, int header) {
		if (!notes.isEmpty()) {
			res.add(header);
			res.addAll(sortNotesByDateDescending(notes));
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
			public void onCheckBoxClick(MediaNote note, boolean checked) {
				if (selectionMode) {
					if (checked) {
						selected.add(note);
					} else {
						selected.remove(note);
					}
					updateSelectionMode(actionMode);
				}
			}

			@Override
			public void onItemClick(MediaNote note, int position) {
				showOnMap(note, position);
			}

			@Override
			public void onOptionsClick(MediaNote note) {
				ItemMenuBottomSheetDialogFragment.showInstance(
						getChildFragmentManager(), createItemMenuFragmentListener(), note);
			}
		};
	}

	private List<MediaNote> getNotesByType(int type) {
		List<MediaNote> res = new LinkedList<>();
		for (int i = 0; i < listAdapter.getItemsCount(); i++) {
			Object item = listAdapter.getItem(i);
			if (item instanceof MediaNote note && note != SHARE_LOCATION_FILE && isAppropriate(note, type)) {
				res.add(note);
			}
		}
		return res;
	}

	private boolean isAppropriate(MediaNote note, int type) {
		if (type == NotesAdapter.TYPE_AUDIO_HEADER) {
			return note.isAudio();
		} else if (type == NotesAdapter.TYPE_PHOTO_HEADER) {
			return note.isPhoto();
		}
		return note.isVideo();
	}

	private void selectAll(int type) {
		if (type == NotesAdapter.TYPE_DATE_HEADER) {
			for (int i = 0; i < listAdapter.getItemsCount(); i++) {
				Object item = listAdapter.getItem(i);
				if (item instanceof MediaNote) {
					selected.add((MediaNote) item);
				}
			}
		} else {
			selected.addAll(getNotesByType(type));
		}
		listAdapter.notifyDataSetChanged();
	}

	private void deselectAll(int type) {
		if (type == NotesAdapter.TYPE_DATE_HEADER) {
			selected.clear();
		} else {
			selected.removeAll(getNotesByType(type));
		}
		listAdapter.notifyDataSetChanged();
	}

	private List<MediaNote> sortNotesByDateDescending(List<MediaNote> notes) {
		Collections.sort(notes, (first, second) -> {
			long firstTime = first.getLastModified(app.getGalleryHelper().getMetadataRepository());
			long secondTime = second.getLastModified(app.getGalleryHelper().getMetadataRepository());
			if (firstTime < secondTime) {
				return 1;
			} else if (firstTime == secondTime) {
				return 0;
			} else {
				return -1;
			}
		});
		return notes;
	}

	protected void recreateAdapterData() {
		if (listAdapter == null) {
			return;
		}
		listAdapter.clear();
		listAdapter.addAll(createItemsList());
		listAdapter.notifyDataSetChanged();
	}

	private void reloadMediaNotes() {
		if (!isResumed() || listAdapter == null || selectionMode) {
			return;
		}
		List<Object> items = createItemsList();
		listAdapter.clear();
		listAdapter.addAll(items);
		listAdapter.notifyDataSetChanged();
		requestAttachedMediaMetadata(items);
	}

	private void requestAttachedMediaMetadata(@NonNull Collection<?> items) {
		List<MediaItem> mediaItems = new ArrayList<>();
		for (Object item : items) {
			if (item instanceof MediaNote note && note.isAttachedMedia()) {
				mediaItems.add(note.getMediaItem());
			}
		}
		if (mediaItems.isEmpty()) {
			return;
		}
		if (metadataRequest != null) {
			metadataRequest.cancel();
		}
		metadataRequest = app.getGalleryHelper().getMetadataRepository().request(mediaItems, new MediaMetadataListener() {
			@Override
			public void onMetadataLoaded(@NonNull MediaItem item, @NonNull GalleryMediaMetadata metadata) {
				if (listAdapter != null) {
					listAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onBatchFinished() {
				metadataRequest = null;
				if (selectionMode) {
					if (listAdapter != null) {
						listAdapter.notifyDataSetChanged();
					}
				} else {
					recreateAdapterData();
				}
			}
		});
	}

	private void enterSelectionMode(int type) {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				LOG.debug("onCreateActionMode");
				if (type == MODE_SHARE) {
					listAdapter.insert(SHARE_LOCATION_FILE, 0);
				}
				switchSelectionMode(true);
				int titleRes = type == MODE_DELETE ? R.string.shared_string_delete_all : R.string.shared_string_share;
				int iconRes = type == MODE_DELETE ? R.drawable.ic_action_delete_dark : R.drawable.ic_action_gshare_dark;
				Drawable icon = AndroidUtils.getDrawableForDirection(app, requireIcon(iconRes));
				MenuItem menuItem = menu.add(titleRes).setIcon(icon);
				menuItem.setOnMenuItemClickListener(item -> {
					if (type == MODE_DELETE) {
						deleteItems(selected);
					} else if (type == MODE_SHARE) {
						shareItems(selected);
					}
					mode.finish();
					return true;
				});
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
				recreateAdapterData();
			}
		});
	}

	private void switchSelectionMode(boolean enable) {
		selectionMode = enable;
		listAdapter.setSelectionMode(enable);
		((MyPlacesActivity) getActivity()).setToolbarVisibility(!enable && AndroidUiHelper.isOrientationPortrait(getActivity()));
		((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);
	}

	private void updateSelectionTitle(ActionMode m) {
		if (!selected.isEmpty()) {
			m.setTitle(selected.size() + " " + getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	private void updateSelectionMode(ActionMode m) {
		updateSelectionTitle(m);
		listAdapter.notifyDataSetChanged();
	}

	private void deleteItems(Set<MediaNote> selected) {
		new AlertDialog.Builder(getThemedContext())
				.setMessage(getString(R.string.local_recordings_delete_all_confirm, selected.size()))
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					Map<Linkable, List<Link>> attachedLinks = new IdentityHashMap<>();
					Iterator<MediaNote> it = selected.iterator();
					while (it.hasNext()) {
						MediaNote note = it.next();
						if (note.isRecording()) {
							plugin.deleteRecording(note.getRecording(), true);
						} else if (note.isAttachedMedia()) {
							attachedLinks.computeIfAbsent(note.getTarget(), key -> new ArrayList<>()).add(note.getLink());
						}
						it.remove();
					}
					for (Map.Entry<Linkable, List<Link>> entry : attachedLinks.entrySet()) {
						attachedMediaDataHelper.removeMediaLinks(entry.getKey(), entry.getValue(), null);
					}
					recreateAdapterData();
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void shareItems(@NonNull Set<MediaNote> selected) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (shareRecordingsTask != null && shareRecordingsTask.getStatus() == AsyncTask.Status.RUNNING) {
				shareRecordingsTask.cancel(false);
			}
			shareRecordingsTask = new ShareRecordingsTask(activity, selected, collectMediaNotes());
			OsmAndTaskManager.executeTask(shareRecordingsTask);
		}
	}

	private ItemMenuFragmentListener createItemMenuFragmentListener() {
		return new ItemMenuFragmentListener() {
			@Override
			public void playOnClick(MediaNote note) {
				if (note.isRecording()) {
					callActivity(result -> plugin.getRecordingsPlayer().playRecording(requireActivity(), note.getRecording()));
				} else {
					openAttachedMedia(note);
				}
			}

			@Override
			public void shareOnClick(MediaNote note) {
				shareNote(note);
			}

			@Override
			public void showOnMapOnClick(MediaNote note) {
				showOnMap(note);
			}

			@Override
			public void renameOnClick(MediaNote note) {
				if (note.getRecording() != null) {
					editNote(note.getRecording());
				}
			}

			@Override
			public void deleteOnClick(MediaNote note) {
				deleteNote(note);
			}
		};
	}

	private void shareNote(MediaNote note) {
		if (note.isRecording()) {
			shareRecording(note.getRecording());
			return;
		}
		MediaItem mediaItem = note.getMediaItem();
		if (mediaItem == null) {
			return;
		}
		Uri uri = app.getGalleryHelper().getMediaSourceResolver().getShareableUri(mediaItem);
		if (uri != null) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType(note.getMimeType());
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			intent.setClipData(ClipData.newRawUri(mediaItem.getTitle(), uri));
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			AndroidUtils.startActivityIfSafe(requireActivity(),
					Intent.createChooser(intent, getString(R.string.share_note)));
			return;
		}
		String shareUri = MediaUriResolver.getShareUri(mediaItem);
		if (shareUri != null) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, shareUri);
			AndroidUtils.startActivityIfSafe(requireActivity(),
					Intent.createChooser(intent, getString(R.string.share_note)));
		}
	}

	private void shareRecording(@Nullable Recording recording) {
		if (recording == null || !recording.getFile().exists()) {
			return;
		}
		Activity activity = getActivity();
		if (activity != null) {
			String type = null;
			if (recording.isPhoto()) {
				type = "image/*";
			} else if (recording.isAudio()) {
				type = "audio/*";
			} else if (recording.isVideo()) {
				type = "video/*";
			}
			File file = recording.getFile().getAbsoluteFile();
			new NativeShareDialogBuilder()
					.addFileWithSaveAction(file, app, requireActivity(), true)
					.setChooserTitle(getString(R.string.share_note))
					.setExtraStream(AndroidUtils.getUriForFile(app, file))
					.setType(type)
					.setNewDocument(true)
					.build(app);
		}
	}

	private void openAttachedMedia(@NonNull MediaNote note) {
		MediaItem mediaItem = note.getMediaItem();
		if (mediaItem == null) {
			return;
		}
		Uri uri = app.getGalleryHelper().getMediaSourceResolver().getShareableUri(mediaItem);
		if (uri != null) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(uri, note.getMimeType());
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			AndroidUtils.startActivityIfSafe(requireActivity(),
					Intent.createChooser(intent, getString(R.string.gallery_open_in)));
			return;
		}
		String detailsUri = MediaUriResolver.getDetailsLink(mediaItem);
		if (detailsUri != null) {
			AndroidUtils.startActivityIfSafe(requireActivity(), new Intent(Intent.ACTION_VIEW, Uri.parse(detailsUri)));
		}
	}

	private void showOnMap(MediaNote note) {
		showOnMap(note, -1);
	}

	private void showOnMap(MediaNote note, int itemPosition) {
		selectedItemPosition = itemPosition;
		Object target = note.isRecording() ? note.getRecording() : note.getTarget();
		String pointType = note.getTarget() instanceof FavouritePoint
				? PointDescription.POINT_TYPE_FAVORITE
				: note.getTarget() instanceof WptPt
						? PointDescription.POINT_TYPE_WPT
						: note.getSearchHistoryType();
		((MyPlacesActivity) requireActivity()).showOnMap(this, note.getLatitude(), note.getLongitude(), 15,
				new PointDescription(pointType,
						note.getName(requireActivity(), app.getGalleryHelper().getMetadataRepository(), true)),
				true, target);
	}

	private void editNote(Recording recording) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getThemedContext());
		builder.setTitle(R.string.shared_string_rename);
		View v = inflate(R.layout.note_edit_dialog, getListView(), false);
		EditText editText = v.findViewById(R.id.name);
		builder.setView(v);
		editText.setText(recording.getName(getActivity(), true));
		InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			if (!recording.setName(editText.getText().toString())) {
				app.showShortToastMessage(R.string.rename_failed);
			}
			listAdapter.notifyDataSetInvalidated();
		});
		builder.create().show();
		editText.requestFocus();
	}

	private void deleteNote(MediaNote note) {
		Activity activity = requireActivity();
		String recordingName = note.getName(activity, app.getGalleryHelper().getMetadataRepository(), false);
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setMessage(getString(R.string.delete_confirmation_msg, recordingName));
		bld.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			if (note.isRecording()) {
				plugin.deleteRecording(note.getRecording(), true);
			} else if (note.isAttachedMedia()) {
				attachedMediaDataHelper.removeMediaLinks(note.getTarget(),
						Collections.singletonList(note.getLink()), null);
			}
			recreateAdapterData();
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();
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

	@Override
	protected int getBackgroundColor() {
		return ColorUtilities.getActivityBgColor(app, nightMode);
	}
}
