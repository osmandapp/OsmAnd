package net.osmand.plus.flyover;

import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * My Places > Flyovers: the map videos recorded through the API, kept in OsmAnd's flyover folder, listed as the
 * tracks and favorites are: a sort row, a row per video with its menu (open, share, delete), selection mode on a
 * long press with share and delete of the selection, and the totals at the end. The tab exists only while there
 * are videos.
 */
public class FlyoversFragment extends BaseFullScreenFragment implements FlyoverSortByBottomSheet.FlyoverSortListener {

	private static final Log LOG = PlatformUtil.getLog(FlyoversFragment.class);
	private static final String MIME = "video/mp4";
	private static final String SELECTION_MODE_KEY = "selection_mode_key";
	private static final String SELECTED_FILES_KEY = "selected_files_key";

	private static final int TYPE_SORT = 0;
	private static final int TYPE_VIDEO = 1;
	private static final int TYPE_STATS = 2;
	private static final int THUMBNAIL_WIDTH_DP = 72;
	private static final int THUMBNAIL_HEIGHT_DP = 40;

	private final ItemsSelectionHelper<File> selectionHelper = new ItemsSelectionHelper<>();
	/** Duration and first frame of a video, read off the UI thread; the duration is -1 while unknown. */
	private static class Info {
		long durationMs = -1;
		@Nullable
		Bitmap thumbnail;
	}

	private final Map<File, Info> infos = new HashMap<>();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Adapter adapter = new Adapter();
	private List<File> files = new ArrayList<>();
	private boolean selectionMode;
	private OnBackPressedCallback backCallback;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		files = MapFlyoverRecorder.listVideos(app);
		selectionHelper.setAllItems(files);
		if (savedInstanceState != null) {
			selectionMode = savedInstanceState.getBoolean(SELECTION_MODE_KEY, false);
			List<String> selected = savedInstanceState.getStringArrayList(SELECTED_FILES_KEY);
			if (selected != null) {
				for (File file : files) {
					if (selected.contains(file.getAbsolutePath())) {
						selectionHelper.onItemsSelected(Collections.singleton(file), true);
					}
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SELECTION_MODE_KEY, selectionMode);
		ArrayList<String> selected = new ArrayList<>();
		for (File file : selectionHelper.getSelectedItems()) {
			selected.add(file.getAbsolutePath());
		}
		outState.putStringArrayList(SELECTED_FILES_KEY, selected);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.flyovers_fragment, container, false);
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		backCallback = new OnBackPressedCallback(selectionMode) {
			@Override
			public void handleOnBackPressed() {
				exitSelectionMode();
			}
		};
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createScrollable(R.id.recycler_view));
		collection.removeType(InsetTarget.Type.ROOT_INSET);
		return collection;
	}

	@Override
	public void onResume() {
		super.onResume();
		reload();
		updateSelectionToolbar();
	}

	@Override
	public void onPause() {
		if (!requireActivity().isChangingConfigurations()) {
			exitSelectionMode();
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		executor.shutdownNow();
	}

	// sorting

	@NonNull
	@Override
	public FlyoverSortMode getFlyoverSortMode() {
		return FlyoverSortMode.valueOf(settings.FLYOVERS_SORT_MODE.get(), FlyoverSortMode.DATE_NEWEST);
	}

	@Override
	public void setFlyoverSortMode(@NonNull FlyoverSortMode sortMode) {
		settings.FLYOVERS_SORT_MODE.set(sortMode.name());
		reload();
	}

	private void reload() {
		files = MapFlyoverRecorder.listVideos(app);
		FlyoverSortMode sortMode = getFlyoverSortMode();
		files.sort(sortMode.comparator(file -> info(file).durationMs));
		selectionHelper.setAllItems(files);
		List<File> stale = new ArrayList<>(selectionHelper.getSelectedItems());
		stale.removeAll(files);
		selectionHelper.onItemsSelected(stale, false);
		adapter.notifyDataSetChanged();
		if (files.isEmpty() && getActivity() instanceof MyPlacesActivity activity) {
			// the last video is gone: so is the tab
			exitSelectionMode();
			activity.updateTabs();
		}
	}

	/** Duration and first frame are read from the file in the background; the row is bound again when they are there. */
	@NonNull
	private Info info(@NonNull File file) {
		Info known = infos.get(file);
		if (known != null) {
			return known;
		}
		Info pending = new Info();
		infos.put(file, pending);
		int thumbWidth = AndroidUtils.dpToPx(app, 2 * THUMBNAIL_WIDTH_DP);
		executor.execute(() -> {
			Info loaded = new Info();
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			try {
				retriever.setDataSource(file.getAbsolutePath());
				String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
				if (!Algorithms.isEmpty(value)) {
					loaded.durationMs = Long.parseLong(value);
				}
				Bitmap frame = retriever.getFrameAtTime(0);
				if (frame != null && frame.getWidth() > thumbWidth) {
					loaded.thumbnail = Bitmap.createScaledBitmap(frame, thumbWidth,
							frame.getHeight() * thumbWidth / frame.getWidth(), true);
				} else {
					loaded.thumbnail = frame;
				}
			} catch (Exception e) {
				LOG.error("Cannot read " + file, e);
			} finally {
				try {
					retriever.release();
				} catch (Exception ignored) {
				}
			}
			app.runInUIThread(() -> {
				if (!isAdded() || infos.get(file) != pending) {
					return;
				}
				infos.put(file, loaded);
				if (getFlyoverSortMode().needsDuration()) {
					reload();
				} else {
					int position = files.indexOf(file);
					if (position >= 0) {
						adapter.notifyItemChanged(position + 1);
					}
				}
			});
		});
		return pending;
	}

	// actions

	private void open(@NonNull File file) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri(file), MIME);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		AndroidUtils.startActivityIfSafe(requireContext(), intent);
	}

	private void share(@NonNull Collection<File> toShare) {
		if (toShare.isEmpty()) {
			return;
		}
		ArrayList<Uri> uris = new ArrayList<>();
		for (File file : toShare) {
			uris.add(uri(file));
		}
		Intent intent = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
		intent.setType(MIME);
		if (uris.size() == 1) {
			intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
		} else {
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		}
		ClipData clipData = ClipData.newRawUri(null, uris.get(0));
		for (int i = 1; i < uris.size(); i++) {
			clipData.addItem(new ClipData.Item(uris.get(i)));
		}
		intent.setClipData(clipData);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		AndroidUtils.startActivityIfSafe(requireContext(), intent,
				Intent.createChooser(intent, getString(R.string.shared_string_share)));
	}

	private void confirmDelete(@NonNull Collection<File> toDelete) {
		if (toDelete.isEmpty()) {
			return;
		}
		List<File> list = new ArrayList<>(toDelete);
		String what = list.size() == 1 ? title(list.get(0))
				: getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.flyovers), String.valueOf(list.size()));
		AlertDialogData data = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.shared_string_delete)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
					for (File file : list) {
						if (!file.delete()) {
							LOG.error("Cannot delete " + file);
						}
						infos.remove(file);
					}
					exitSelectionMode();
					reload();
				})
				.setPositiveButtonTextColor(ColorUtilities.getColor(requireContext(), ColorUtilities.getWarningColorId(nightMode)));
		CustomAlert.showSimpleMessage(data, getString(R.string.delete_confirmation_msg, what));
	}

	private void showItemMenu(@NonNull File file, @NonNull View anchor) {
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(new PopUpMenuItem.Builder(requireContext())
				.setTitleId(R.string.shared_string_open)
				.setIcon(getContentIcon(R.drawable.ic_action_play_dark))
				.setOnClickListener(v -> open(file))
				.create());
		items.add(new PopUpMenuItem.Builder(requireContext())
				.setTitleId(R.string.shared_string_share)
				.setIcon(getContentIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> share(Collections.singletonList(file)))
				.create());
		items.add(new PopUpMenuItem.Builder(requireContext())
				.setTitleId(R.string.shared_string_delete)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> confirmDelete(Collections.singletonList(file)))
				.showTopDivider(true)
				.create());
		showPopUp(items, anchor);
	}

	private void showSelectionMenu(@NonNull View anchor) {
		List<File> selected = new ArrayList<>(selectionHelper.getSelectedItems());
		List<PopUpMenuItem> items = new ArrayList<>();
		items.add(new PopUpMenuItem.Builder(requireContext())
				.setTitleId(R.string.shared_string_share)
				.setIcon(getContentIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> share(selected))
				.create());
		items.add(new PopUpMenuItem.Builder(requireContext())
				.setTitleId(R.string.shared_string_delete)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> confirmDelete(selected))
				.showTopDivider(true)
				.create());
		showPopUp(items, anchor);
	}

	private void showPopUp(@NonNull List<PopUpMenuItem> items, @NonNull View anchor) {
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = anchor;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	@NonNull
	private Uri uri(@NonNull File file) {
		return AndroidUtils.getUriForFile(app, file);
	}

	@NonNull
	private static String title(@NonNull File file) {
		String name = file.getName();
		return name.toLowerCase().endsWith(".mp4") ? name.substring(0, name.length() - 4) : name;
	}

	// selection mode, as in the favorites tab

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		if (selectionMode) {
			inflater.inflate(R.menu.menu_selection_mode, menu);
		}
		requireMyPlaces().setToolbarVisibility(false);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (!selectionMode) {
			return false;
		}
		int id = item.getItemId();
		if (id == android.R.id.home) {
			exitSelectionMode();
			return true;
		} else if (id == R.id.select_all) {
			if (selectionHelper.isAllItemsSelected()) {
				selectionHelper.clearSelectedItems();
			} else {
				selectionHelper.selectAllItems();
			}
			updateSelectionToolbar();
			adapter.notifyDataSetChanged();
			return true;
		} else if (id == R.id.more_button) {
			View anchor = requireMyPlaces().findViewById(R.id.more_button);
			showSelectionMenu(anchor != null ? anchor : requireView());
			return true;
		}
		return false;
	}

	private void toggle(@NonNull File file) {
		selectionHelper.onItemsSelected(Collections.singleton(file), !selectionHelper.isItemSelected(file));
		updateSelectionToolbar();
		adapter.notifyItemChanged(files.indexOf(file) + 1);
	}

	private void setSelectionMode(boolean mode) {
		if (mode == selectionMode) {
			return;
		}
		selectionMode = mode;
		if (backCallback != null) {
			backCallback.setEnabled(mode);
		}
		if (!mode) {
			selectionHelper.clearSelectedItems();
		}
		requireMyPlaces().animateShowHideTabs(mode);
		updateSelectionToolbar();
		adapter.notifyDataSetChanged();
	}

	private void exitSelectionMode() {
		setSelectionMode(false);
	}

	private void updateSelectionToolbar() {
		MyPlacesActivity activity = requireMyPlaces();
		ActionBar ab = activity.getSupportActionBar();
		if (ab == null) {
			return;
		}
		invalidateOptionsMenu(activity);
		if (selectionMode) {
			ab.setHomeAsUpIndicator(R.drawable.ic_action_close);
			ab.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getToolbarActiveColor(app, nightMode)));
			ab.setTitle(String.valueOf(selectionHelper.getSelectedItemsSize()));
			AndroidUiHelper.setStatusBarColor(activity, ColorUtilities.getColor(app, ColorUtilities.getStatusBarActiveColorId(nightMode)));
		} else {
			ab.setHomeAsUpIndicator(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode)));
			ab.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getAppBarColor(app, nightMode)));
			ab.setTitle(R.string.shared_string_my_places);
			activity.updateStatusBarColor();
		}
	}

	@NonNull
	private MyPlacesActivity requireMyPlaces() {
		return (MyPlacesActivity) requireActivity();
	}

	// list

	private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@Override
		public int getItemViewType(int position) {
			return position == 0 ? TYPE_SORT : position <= files.size() ? TYPE_VIDEO : TYPE_STATS;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = getThemedInflater();
			switch (viewType) {
				case TYPE_SORT:
					return new SortHolder(inflater.inflate(R.layout.sort_type_view, parent, false));
				case TYPE_VIDEO:
					return new VideoHolder(inflater.inflate(R.layout.track_list_item, parent, false));
				default:
					return new StatsHolder(inflater.inflate(R.layout.folder_stats_item, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			if (holder instanceof SortHolder sort) {
				sort.bind();
			} else if (holder instanceof VideoHolder video) {
				video.bind(files.get(position - 1), position < files.size());
			} else if (holder instanceof StatsHolder stats) {
				stats.bind();
			}
		}

		@Override
		public int getItemCount() {
			return files.isEmpty() ? 0 : files.size() + 2;
		}
	}

	private class SortHolder extends RecyclerView.ViewHolder {
		final TextView description;
		final ImageView icon;

		SortHolder(@NonNull View view) {
			super(view);
			description = view.findViewById(R.id.description);
			icon = view.findViewById(R.id.icon);
			TextView title = view.findViewById(R.id.title);
			title.setTextColor(ColorUtilities.getActiveColor(app, nightMode));
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.filter_button), false);
		}

		void bind() {
			FlyoverSortMode sortMode = getFlyoverSortMode();
			description.setText(sortMode.getNameId());
			icon.setImageDrawable(getIcon(sortMode.getIconId(), ColorUtilities.getActiveIconColorId(nightMode)));
			itemView.setOnClickListener(v -> FlyoverSortByBottomSheet.showInstance(getParentFragmentManager(),
					FlyoversFragment.this, getFlyoverSortMode()));
		}
	}

	private class VideoHolder extends RecyclerView.ViewHolder {
		final TextView title;
		final TextView description;
		final ImageView icon;
		final CompoundButton checkbox;
		final View checkboxContainer;
		final View menuButton;
		final View divider;

		VideoHolder(@NonNull View view) {
			super(view);
			title = view.findViewById(R.id.title);
			description = view.findViewById(R.id.description);
			icon = view.findViewById(R.id.icon);
			checkbox = view.findViewById(R.id.checkbox);
			checkboxContainer = view.findViewById(R.id.checkbox_container);
			menuButton = view.findViewById(R.id.menu_button);
			divider = view.findViewById(R.id.divider);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.direction_icon), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.full_divider), false);
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkbox);
			// the first frame of the video in place of the 24 dp icon
			ViewGroup.LayoutParams params = icon.getLayoutParams();
			params.width = AndroidUtils.dpToPx(app, THUMBNAIL_WIDTH_DP);
			params.height = AndroidUtils.dpToPx(app, THUMBNAIL_HEIGHT_DP);
			icon.setLayoutParams(params);
			icon.setBackgroundColor(ColorUtilities.getDividerColor(app, nightMode));
		}

		void bind(@NonNull File file, boolean showDivider) {
			title.setText(title(file));
			String text = OsmAndFormatter.getFormattedDateTime(app, file.lastModified());
			Info info = info(file);
			if (info.durationMs >= 0) {
				text = getString(R.string.ltr_or_rtl_combine_via_bold_point, text, Algorithms.formatDuration((int) (info.durationMs / 1000), false));
			}
			text = getString(R.string.ltr_or_rtl_combine_via_bold_point, text, AndroidUtils.formatSize(app, file.length()));
			description.setText(text);
			if (info.thumbnail != null) {
				icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
				icon.setImageBitmap(info.thumbnail);
			} else {
				icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				icon.setImageDrawable(getIcon(R.drawable.ic_action_video_dark, ColorUtilities.getDefaultIconColorId(nightMode)));
			}
			AndroidUiHelper.updateVisibility(divider, showDivider);
			AndroidUiHelper.updateVisibility(checkboxContainer, selectionMode);
			AndroidUiHelper.updateVisibility(menuButton, !selectionMode);
			checkbox.setChecked(selectionHelper.isItemSelected(file));
			itemView.setOnClickListener(v -> {
				if (selectionMode) {
					toggle(file);
				} else {
					open(file);
				}
			});
			itemView.setOnLongClickListener(v -> {
				if (!selectionMode) {
					setSelectionMode(true);
				}
				if (!selectionHelper.isItemSelected(file)) {
					toggle(file);
				}
				return true;
			});
			menuButton.setOnClickListener(v -> showItemMenu(file, menuButton));
		}
	}

	private class StatsHolder extends RecyclerView.ViewHolder {
		final TextView stats;

		StatsHolder(@NonNull View view) {
			super(view);
			stats = view.findViewById(R.id.stats);
		}

		void bind() {
			long size = 0;
			for (File file : files) {
				size += file.length();
			}
			String count = getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.flyovers), String.valueOf(files.size()));
			String total = getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_size), AndroidUtils.formatSize(app, size));
			stats.setText(count + " · " + total);
		}
	}
}
