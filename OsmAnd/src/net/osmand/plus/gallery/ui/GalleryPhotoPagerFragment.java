package net.osmand.plus.gallery.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.gallery.controller.GalleryPagerController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.ui.viewer.MediaViewerPage;
import net.osmand.plus.gallery.ui.viewer.MediaViewerSheetLayout;
import net.osmand.plus.gallery.ui.viewer.ViewerSheetController;
import net.osmand.plus.plugins.audionotes.library.MediaItemMenu;
import net.osmand.plus.plugins.audionotes.library.MediaShareHelper;
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry;
import net.osmand.plus.gallery.data.GalleryKey;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.shared.media.MediaProvider;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GalleryPhotoPagerFragment extends BaseFullScreenDialogFragment implements IDialog, MediaViewerSheetLayout.Listener {

	public static final String TAG = GalleryPhotoPagerFragment.class.getSimpleName();
	public static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2000;
	public static final int PRELOAD_THUMBNAILS_COUNT = 3;

	public static final int STATE_MEDIA = MediaViewerSheetLayout.STATE_MEDIA;
	public static final int STATE_PREVIEW = MediaViewerSheetLayout.STATE_PREVIEW;

	private static final int UI_TOGGLE_ANIM_MS = 150;

	private static final String SELECTED_ITEM_ID_KEY = "selected_item_id_key";
	private static final String DETAILS_STATE_KEY = "details_state_key";

	private ImageView sourceView;
	private TextView descriptionView;
	private TextView dateView;
	private TextView authorView;
	private TextView licenseView;
	private View descriptionShadow;
	private View descriptionContainer;
	private Toolbar toolbar;
	private ViewPager pager;
	private ViewPagerAdapter pagerAdapter;
	private MediaViewerSheetLayout sheetLayout;
	private ViewerSheetController sheetController;

	private boolean uiHidden = false;
	private int selectedPosition = 0;
	private int initialState = STATE_MEDIA;
	private int statusBarColor = -1;
	private boolean statusBarSolid;

	private GalleryPagerController controller;
	private List<GalleryItem.Media> mediaItems = new ArrayList<>();
	private MediaProvider mediaProvider;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mediaProvider = new MediaProvider(app);

		controller = GalleryPagerController.getExistingInstance(app);
		if (controller == null) {
			dismiss();
			return;
		}
		controller.registerDialog(this);

		mediaItems = controller.getMediaItems();

		String selectedItemId = null;
		if (savedInstanceState != null) {
			selectedItemId = savedInstanceState.getString(SELECTED_ITEM_ID_KEY);
			initialState = savedInstanceState.getInt(DETAILS_STATE_KEY, STATE_MEDIA);
		} else if (getArguments() != null) {
			selectedItemId = getArguments().getString(SELECTED_ITEM_ID_KEY);
			initialState = getArguments().getInt(DETAILS_STATE_KEY, STATE_MEDIA);
		}
		selectedPosition = selectedItemId != null
				? controller.getIndexById(selectedItemId)
				: 0;

		if (selectedPosition >= mediaItems.size()) {
			dismiss();
		}
	}

	@NonNull
	@Override
	public Dialog createDialog(@Nullable Bundle savedInstanceState) {
		Dialog dialog = new Dialog(requireContext(), getThemeId()) {
			@Override
			public void onBackPressed() {
				if (sheetLayout == null || !sheetLayout.handleBack()) {
					super.onBackPressed();
				}
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		ViewGroup view = (ViewGroup) inflate(R.layout.gallery_viewer_fragment, container, false);

		sheetLayout = view.findViewById(R.id.viewer_root);
		sheetLayout.setAnimationsEnabled(!settings.DO_NOT_USE_ANIMATIONS.get());
		sheetLayout.setPageProvider(this::getCurrentPage);
		sheetLayout.setListener(this);
		sheetController = new ViewerSheetController(requireActivity(), view.findViewById(R.id.details_list), nightMode);

		setupToolbar(view);
		setupDetailsAppBar(view);

		sourceView = view.findViewById(R.id.source_icon);
		setupMetadataRow(view);

		descriptionShadow = view.findViewById(R.id.description_shadow);
		descriptionContainer = view.findViewById(R.id.description_container);

		if (selectedPosition < mediaItems.size()) {
			setupViewPager(view);
			preloadThumbNails();
			updateImageDescriptionRow(getSelectedMediaItem());
			sheetController.setItem(getSelectedMediaItem());
		}
		sheetLayout.setInitialState(initialState);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		GalleryItem.Media selected = getSelectedGalleryItem();
		if (selected != null) {
			outState.putString(SELECTED_ITEM_ID_KEY, selected.getMediaItem().getId());
		}
		outState.putInt(DETAILS_STATE_KEY, sheetLayout != null ? sheetLayout.getSettledState() : initialState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createBottomContainer(R.id.description_container));
		collection.replace(InsetTarget.createScrollable(R.id.details_list));
		collection.replace(InsetTarget.createHorizontalLandscape(R.id.toolbar, R.id.solid_app_bar));
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	private void preloadThumbNails() {
		preloadThumbNails(true);
		preloadThumbNails(false);
	}

	private void preloadThumbNails(boolean next) {
		if (mediaItems.size() <= 1) {
			return;
		}
		if (next) {
			int start = selectedPosition + 1;
			if (start >= mediaItems.size()) return;
			int end = Math.min(start + PRELOAD_THUMBNAILS_COUNT, mediaItems.size());
			for (int i = start; i < end; i++) {
				downloadThumbnail(mediaItems.get(i).getMediaItem());
			}
		} else {
			int start = selectedPosition - 1;
			if (start < 0) return;
			int end = Math.max(start - PRELOAD_THUMBNAILS_COUNT, -1);
			for (int i = start; i > end; i--) {
				downloadThumbnail(mediaItems.get(i).getMediaItem());
			}
		}
	}

	private void downloadThumbnail(@NonNull MediaItem mediaItem) {
		if (mediaItem.getType() == MediaType.PHOTO) {
			mediaProvider.loadThumbnail(mediaItem);
		}
	}

	private void updateImageDescriptionRow(@Nullable MediaItem mediaItem) {
		if (mediaItem == null) return;
		var details = mediaItem.getDetails();

		if (details != null) {
			dateView.setVisibility(View.VISIBLE);
			authorView.setVisibility(View.VISIBLE);
			licenseView.setVisibility(View.VISIBLE);
			setDescription(details.getDescription(app.getLanguage()));
			setMetaData(details.getAuthor(), details.getDate(), details.getLicense());
		} else {
			setDescription(null);
			dateView.setVisibility(View.INVISIBLE);
			authorView.setVisibility(View.INVISIBLE);
			licenseView.setVisibility(View.INVISIBLE);
		}

		int iconId = getDrawableId(mediaItem.getOrigin().getIconName());
		Drawable icon = iconId != 0 ? getIcon(iconId) : null;
		sourceView.setImageDrawable(icon);
		AndroidUiHelper.updateVisibility(sourceView, icon != null);
	}

	private void setDescription(@Nullable String description) {
		boolean hasDescription = !Algorithms.isEmpty(description);
		AndroidUiHelper.updateVisibility(descriptionView, hasDescription);
		descriptionView.setText(hasDescription ? description : null);
	}

	private void setMetaData(@Nullable String author, @Nullable String date,
	                         @Nullable String license) {
		String formattedDate = WikiAlgorithms.formatWikiDate(date);
		dateView.setText(getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_date),
				formattedDate != null && !formattedDate.equals("Unknown") ? formattedDate : ""));
		authorView.setText(getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_author),
				author != null && !author.equals("Unknown") ? author : ""));
		licenseView.setText(getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_license),
				license != null && !license.equals("Unknown") ? license : ""));
	}

	private void setupMetadataRow(@NonNull ViewGroup view) {
		descriptionView = view.findViewById(R.id.description);
		descriptionView.setTextColor(ColorUtilities.getColor(app, R.color.text_color_tertiary_light));

		dateView = view.findViewById(R.id.date);
		dateView.setTextColor(ColorUtilities.getColor(app, R.color.text_color_tertiary_light));

		authorView = view.findViewById(R.id.author);
		authorView.setTextColor(ColorUtilities.getColor(app, R.color.text_color_tertiary_light));

		licenseView = view.findViewById(R.id.license);
		licenseView.setTextColor(ColorUtilities.getColor(app, R.color.text_color_tertiary_light));
		setMetaData("", "", "");
	}

	public void toggleUi() {
		if (sheetLayout != null && sheetLayout.getProgress() > 0f && !uiHidden) {
			return;
		}
		boolean useAnimations = !settings.DO_NOT_USE_ANIMATIONS.get();
		uiHidden = !uiHidden;
		if (useAnimations) {
			if (uiHidden) {
				toolbar.animate()
						.translationY(toolbar.getHeight() * -1)
						.alpha(0.0f)
						.setDuration(UI_TOGGLE_ANIM_MS)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								super.onAnimationEnd(animation);
								toolbar.setVisibility(View.GONE);
							}
						});
				descriptionShadow.animate()
						.alpha(0)
						.setDuration(UI_TOGGLE_ANIM_MS)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								super.onAnimationEnd(animation);
								descriptionShadow.setVisibility(View.GONE);
							}
						});
				descriptionContainer.animate()
						.translationY(toolbar.getHeight())
						.alpha(0)
						.setDuration(UI_TOGGLE_ANIM_MS)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								super.onAnimationEnd(animation);
								descriptionContainer.setVisibility(View.GONE);
							}
						});
			} else {
				toolbar.setVisibility(View.VISIBLE);
				toolbar.setAlpha(0.0f);
				toolbar.animate().translationY(0).alpha(1.0f)
						.setDuration(UI_TOGGLE_ANIM_MS).setListener(null);

				descriptionShadow.setVisibility(View.VISIBLE);
				descriptionShadow.setAlpha(0.0f);
				descriptionShadow.animate().alpha(1.0f)
						.setDuration(UI_TOGGLE_ANIM_MS).setListener(null);

				descriptionContainer.setVisibility(View.VISIBLE);
				descriptionContainer.setAlpha(0.0f);
				descriptionContainer.animate().translationY(0).alpha(1.0f)
						.setDuration(UI_TOGGLE_ANIM_MS).setListener(null);
			}
		} else {
			toolbar.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionContainer.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionShadow.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
		}
	}

	private void setupToolbar(@NonNull View view) {
		toolbar = view.findViewById(R.id.toolbar);

		ImageView backButton = toolbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_close,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		backButton.setContentDescription(getString(R.string.shared_string_close));
		backButton.setOnClickListener(v -> dismiss());
		setupSelectableBackground(backButton);

		ImageView shareButton = toolbar.findViewById(R.id.share_button);
		shareButton.setOnClickListener(v -> shareMedia());
		shareButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_gshare_dark,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(shareButton);

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(this::showContextWidgetMenu);
		optionsButton.setImageDrawable(getPaintedIcon(R.drawable.ic_overflow_menu_white,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(optionsButton);
	}

	private void setupDetailsAppBar(@NonNull View view) {
		int iconColor = ColorUtilities.getColor(app, R.color.active_buttons_and_links_text_light);

		ImageView backButton = view.findViewById(R.id.details_back_button);
		backButton.setImageDrawable(getPaintedIcon(AndroidUtils.getNavigationIconResId(app), iconColor));
		backButton.setOnClickListener(v -> sheetLayout.animateTo(STATE_PREVIEW));
		setupSelectableBackground(backButton);

		ImageView shareButton = view.findViewById(R.id.details_share_button);
		shareButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_gshare_dark, iconColor));
		shareButton.setOnClickListener(v -> shareMedia());
		setupSelectableBackground(shareButton);

		ImageView optionsButton = view.findViewById(R.id.details_options_button);
		optionsButton.setImageDrawable(getPaintedIcon(R.drawable.ic_overflow_menu_white, iconColor));
		optionsButton.setOnClickListener(this::showContextWidgetMenu);
		setupSelectableBackground(optionsButton);
	}

	private void openInExternalApp() {
		MediaItem mediaItem = getSelectedMediaItem();
		if (mediaItem == null) return;

		Uri uri = app.getGalleryHelper().getMediaSourceResolver().getShareableUri(mediaItem);
		if (uri == null) return;

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, getMediaMimeType(mediaItem));
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		callActivity(activity -> AndroidUtils.startActivityIfSafe(activity,
				Intent.createChooser(intent, getString(R.string.gallery_open_in))));
	}

	private void shareMedia() {
		MediaItem mediaItem = getSelectedMediaItem();
		if (mediaItem == null) return;
		callActivity(activity -> MediaShareHelper.share(activity, java.util.Collections.singletonList(mediaItem)));
	}

	public void showContextWidgetMenu(@NonNull View view) {
		MediaItem mediaItem = getSelectedMediaItem();
		if (mediaItem == null) return;
		if (controller.getKey() == GalleryKey.MediaLibrary.INSTANCE) {
			MediaLibraryEntry entry = app.getGalleryHelper().getMediaLibraryRepository().getEntry(mediaItem.getId());
			if (entry != null) {
				List<String> orderedIds = new ArrayList<>();
				for (GalleryItem.Media item : mediaItems) orderedIds.add(item.getMediaItem().getId());
				callActivity(activity -> MediaItemMenu.show(activity, entry, view, nightMode, true, orderedIds));
				return;
			}
		}

		List<PopUpMenuItem> items = new ArrayList<>();
		UiUtilities uiUtilities = app.getUIUtilities();
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_details)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_info_outlined, iconColor))
				.setOnClickListener(item -> callActivity(activity ->
						controller.openDetails(activity, mediaItem)))
				.create());

		String browserUri = MediaUriResolver.getBrowserUri(mediaItem);
		if (!Algorithms.isEmpty(browserUri)) {
			items.add(new PopUpMenuItem.Builder(app)
					.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_external_link, iconColor))
					.setTitleId(R.string.open_in_browser)
					.setOnClickListener(item -> callActivity(activity ->
							AndroidUtils.openUrl(activity, browserUri, nightMode)))
					.create());
		}

		String downloadUri = MediaUriResolver.getDownloadUri(mediaItem);
		if (isDownloadableMedia(mediaItem, downloadUri)) {
			items.add(new PopUpMenuItem.Builder(app)
					.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_gsave_dark, iconColor))
					.setTitleId(R.string.shared_string_download)
					.setOnClickListener(item -> downloadMedia(downloadUri))
					.create());
		}

		boolean playable = mediaItem.getType() == MediaType.VIDEO
				|| mediaItem.getType() == MediaType.AUDIO;
		if (playable) {
			items.add(new PopUpMenuItem.Builder(app)
					.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_external_link, iconColor))
					.setTitleId(R.string.gallery_open_in)
					.setOnClickListener(item -> openInExternalApp())
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private boolean isDownloadableMedia(@NonNull MediaItem mediaItem, @Nullable String downloadUri) {
		if (!(mediaItem instanceof MediaItem.Remote) || Algorithms.isEmpty(downloadUri)) {
			return false;
		}
		String scheme = Uri.parse(downloadUri).getScheme();
		return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
	}

	private void downloadMedia(@NonNull String url) {
		callActivity(activity -> downloadMedia(activity, url));
	}

	private void downloadMedia(@NonNull FragmentActivity activity, @NonNull String url) {
		String fileName = URLUtil.guessFileName(url, null, null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startDownloading(fileName, url);
		} else {
			if (AndroidUtils.hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				startDownloading(fileName, url);
			} else {
				ActivityCompat.requestPermissions(activity,
						new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_EXTERNAL_STORAGE_PERMISSION);
			}
		}
	}

	private void startDownloading(@NonNull String fileName, @NonNull String url) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
				.setAllowedNetworkTypes(
						DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
				.setTitle(fileName)
				.setNotificationVisibility(
						DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setAllowedOverMetered(true)
				.setAllowedOverRoaming(false)
				.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
		DownloadManager downloadManager =
				(DownloadManager) requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		downloadManager.enqueue(request);
	}

	@NonNull
	private String getMediaMimeType(@NonNull MediaItem mediaItem) {
		return switch (mediaItem.getType()) {
			case VIDEO -> "video/*";
			case AUDIO -> "audio/*";
			case PHOTO -> "image/*";
			default -> "*/*";
		};
	}

	private void setupViewPager(@NonNull View view) {
		pager = view.findViewById(R.id.photo_pager);
		pager.clearOnPageChangeListeners();
		FragmentManager manager = getChildFragmentManager();

		pagerAdapter = new ViewPagerAdapter(manager, mediaItems);
		pager.setAdapter(pagerAdapter);
		pager.setCurrentItem(selectedPosition);
		pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				boolean shouldPreloadNext = selectedPosition < position;
				selectedPosition = position;
				preloadThumbNails(shouldPreloadNext);
				updateImageDescriptionRow(getSelectedMediaItem());
				if (sheetController != null) {
					sheetController.setItem(getSelectedMediaItem());
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		pager.setPageTransformer(true, new GalleryDepthTransformer());
	}

	public void refreshMediaItems(@NonNull String selectedItemId) {
		if (getView() == null || controller == null) return;
		mediaItems = controller.getMediaItems();
		selectedPosition = controller.getIndexById(selectedItemId);
		setupViewPager(getView());
		updateImageDescriptionRow(getSelectedMediaItem());
		if (sheetController != null) {
			sheetController.setItem(getSelectedMediaItem());
		}
	}

	public void showDetails(@NonNull String itemId) {
		if (sheetLayout == null || pager == null) return;
		int position = controller.getIndexById(itemId);
		if (position != selectedPosition && position < mediaItems.size()) {
			pager.setCurrentItem(position, false);
		}
		sheetLayout.animateTo(STATE_PREVIEW);
	}

	public void onPageContentChanged(@NonNull Fragment page) {
		if (sheetLayout != null && pagerAdapter != null && pagerAdapter.currentPage == page) {
			sheetLayout.onPageContentChanged();
		}
	}

	@Nullable
	private MediaViewerPage getCurrentPage() {
		return pagerAdapter != null && pagerAdapter.currentPage instanceof MediaViewerPage page ? page : null;
	}

	@Override
	public void onProgressChanged(float progress, float dismissProgress) {
		if (progress > 0f && uiHidden) {
			toggleUi();
		}
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			return;
		}
		boolean solid = progress >= MediaViewerSheetLayout.CHROME_SWITCH_PROGRESS;
		if (solid != statusBarSolid) {
			statusBarSolid = solid;
			setStatusBarColor(getColor(getStatusBarColorId()));
		} else if (!solid) {
			setStatusBarColor(ColorUtils.setAlphaComponent(Color.BLACK, Math.round(255 * (1f - dismissProgress))));
		}
	}

	@Override
	public void onDismissed() {
		dismiss();
	}

	private void setStatusBarColor(int color) {
		Window window = getDialog() != null ? getDialog().getWindow() : null;
		if (window != null && color != statusBarColor) {
			statusBarColor = color;
			AndroidUiHelper.setStatusBarColor(window, color);
		}
	}

	@Nullable
	private MediaItem getSelectedMediaItem() {
		GalleryItem.Media item = getSelectedGalleryItem();
		return item != null ? item.getMediaItem() : null;
	}

	@Nullable
	private GalleryItem.Media getSelectedGalleryItem() {
		if (selectedPosition >= 0 && selectedPosition < mediaItems.size()) {
			return mediaItems.get(selectedPosition);
		}
		return null;
	}

	public boolean isUiHidden() {
		return uiHidden;
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	@Override
	protected int getStatusBarColorId() {
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			return R.color.color_transparent;
		}
		return statusBarSolid ? ColorUtilities.getAppBarColorId(nightMode) : R.color.widget_background_color_dark;
	}

	@Override
	public void onStart() {
		super.onStart();
		Window window = getDialog() != null ? getDialog().getWindow() : null;
		if (window != null) {
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), true);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		callMapActivity(MapActivity::disableDrawer);
	}

	@Override
	public void onDestroyView() {
		if (sheetController != null) {
			sheetController.release();
			sheetController = null;
		}
		sheetLayout = null;
		pager = null;
		pagerAdapter = null;
		super.onDestroyView();
	}

	@Override
	public void onPause() {
		super.onPause();
		callMapActivity(MapActivity::enableDrawer);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String selectedItemId) {
		showInstance(activity, selectedItemId, STATE_MEDIA);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String selectedItemId, int initialState) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(SELECTED_ITEM_ID_KEY, selectedItemId);
			bundle.putInt(DETAILS_STATE_KEY, initialState);
			GalleryPhotoPagerFragment fragment = new GalleryPhotoPagerFragment();
			fragment.setArguments(bundle);
			fragment.show(manager, TAG);
		}
	}

	private static class ViewPagerAdapter extends FragmentStatePagerAdapter {

		private final List<GalleryItem.Media> mediaItems;
		private Fragment currentPage;

		public ViewPagerAdapter(@NonNull FragmentManager manager,
		                        @NonNull List<GalleryItem.Media> mediaItems) {
			super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.mediaItems = mediaItems;
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			MediaType type = mediaItems.get(position).getMediaItem().getType();
			if (type == MediaType.VIDEO || type == MediaType.AUDIO) {
				return GalleryMediaPlayerFragment.newInstance(position);
			}
			return GalleryPhotoViewerFragment.newInstance(position);
		}

		@Override
		public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			super.setPrimaryItem(container, position, object);
			currentPage = object instanceof Fragment fragment ? fragment : null;
		}

		@Override
		public int getCount() {
			return mediaItems.size();
		}
	}
}