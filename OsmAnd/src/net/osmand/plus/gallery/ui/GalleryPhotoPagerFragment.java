package net.osmand.plus.gallery.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.gallery.controller.GalleryPagerController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.shared.media.MediaProvider;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class GalleryPhotoPagerFragment extends BaseFullScreenFragment implements IDialog {

	public static final String TAG = GalleryPhotoPagerFragment.class.getSimpleName();
	public static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2000;
	public static final int PRELOAD_THUMBNAILS_COUNT = 3;

	private static final String SELECTED_ITEM_ID_KEY = "selected_item_id_key";

	private ImageView sourceView;
	private TextView descriptionView;
	private TextView dateView;
	private TextView authorView;
	private TextView licenseView;
	private View descriptionShadow;
	private View descriptionContainer;
	private Toolbar toolbar;

	private boolean uiHidden = false;
	private int selectedPosition = 0;

	private GalleryPagerController controller;
	private List<GalleryItem.Media> photoItems = new ArrayList<>();
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

		photoItems = controller.getPhotoItems();

		String selectedItemId = null;
		if (savedInstanceState != null) {
			selectedItemId = savedInstanceState.getString(SELECTED_ITEM_ID_KEY);
		} else if (getArguments() != null) {
			selectedItemId = getArguments().getString(SELECTED_ITEM_ID_KEY);
		}
		selectedPosition = selectedItemId != null
				? controller.getIndexById(selectedItemId)
				: 0;

		if (selectedPosition >= photoItems.size()) {
			dismiss();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		ViewGroup view = (ViewGroup) inflate(R.layout.gallery_photo_fragment, container, false);

		setupToolbar(view);
		setupOnBackPressedCallback();

		sourceView = view.findViewById(R.id.source_icon);
		setupMetadataRow(view);

		descriptionShadow = view.findViewById(R.id.description_shadow);
		descriptionContainer = view.findViewById(R.id.description_container);

		if (selectedPosition < photoItems.size()) {
			setupViewPager(view);
			preloadThumbNails();
			updateImageDescriptionRow(getSelectedMediaItem());
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		GalleryItem.Media selected = getSelectedGalleryItem();
		if (selected != null) {
			outState.putString(SELECTED_ITEM_ID_KEY, selected.getMediaItem().getId());
		}
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
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	private void preloadThumbNails() {
		preloadThumbNails(true);
		preloadThumbNails(false);
	}

	private void preloadThumbNails(boolean next) {
		if (photoItems.size() <= 1) {
			return;
		}
		if (next) {
			int start = selectedPosition + 1;
			if (start >= photoItems.size()) return;
			int end = Math.min(start + PRELOAD_THUMBNAILS_COUNT, photoItems.size());
			for (int i = start; i < end; i++) {
				downloadThumbnail(photoItems.get(i).getMediaItem());
			}
		} else {
			int start = selectedPosition - 1;
			if (start < 0) return;
			int end = Math.max(start - PRELOAD_THUMBNAILS_COUNT, -1);
			for (int i = start; i > end; i--) {
				downloadThumbnail(photoItems.get(i).getMediaItem());
			}
		}
	}

	private void downloadThumbnail(@NonNull MediaItem mediaItem) {
		mediaProvider.loadThumbnail(mediaItem);
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
		boolean useAnimations = !settings.DO_NOT_USE_ANIMATIONS.get();
		uiHidden = !uiHidden;
		if (useAnimations) {
			if (uiHidden) {
				toolbar.animate()
						.translationY(toolbar.getHeight() * -1)
						.alpha(0.0f)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								super.onAnimationEnd(animation);
								toolbar.setVisibility(View.GONE);
							}
						});
				descriptionShadow.animate()
						.alpha(0)
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
				toolbar.animate().translationY(0).alpha(1.0f).setListener(null);

				descriptionShadow.setVisibility(View.VISIBLE);
				descriptionShadow.setAlpha(0.0f);
				descriptionShadow.animate().alpha(1.0f).setListener(null);

				descriptionContainer.setVisibility(View.VISIBLE);
				descriptionContainer.setAlpha(0.0f);
				descriptionContainer.animate().translationY(0).alpha(1.0f).setListener(null);
			}
		} else {
			toolbar.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionContainer.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionShadow.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
		}
	}

	private void setupToolbar(@NonNull View view) {
		toolbar = view.findViewById(R.id.toolbar);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		params.topMargin = AndroidUtils.getStatusBarHeight(getMapActivity());
		toolbar.setLayoutParams(params);

		ImageView backButton = toolbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_close,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		backButton.setContentDescription(getString(R.string.shared_string_close));
		backButton.setOnClickListener(v -> dismiss());
		setupSelectableBackground(backButton);

		ImageView shareButton = toolbar.findViewById(R.id.share_button);
		shareButton.setOnClickListener(v -> shareImage());
		shareButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_gshare_dark,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(shareButton);

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(this::showContextWidgetMenu);
		optionsButton.setImageDrawable(getPaintedIcon(R.drawable.ic_overflow_menu_white,
				ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(optionsButton);
	}

	private void shareImage() {
		String shareUri = MediaUriResolver.getShareUri(getSelectedMediaItem());
		if (!Algorithms.isEmpty(shareUri)) {
			callActivity(activity -> shareImageUri(activity, shareUri));
		}
	}

	private void shareImageUri(@NonNull FragmentActivity activity, @NonNull String shareUri) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.setType("text/plain");
		sendIntent.putExtra(Intent.EXTRA_TEXT, shareUri);
		AndroidUtils.startActivityIfSafe(activity,
				Intent.createChooser(sendIntent, getString(R.string.shared_string_share)));
	}

	public void showContextWidgetMenu(@NonNull View view) {
		MediaItem mediaItem = getSelectedMediaItem();
		if (mediaItem == null) return;

		List<PopUpMenuItem> items = new ArrayList<>();
		UiUtilities uiUtilities = app.getUIUtilities();
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_details)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_info_outlined, iconColor))
				.setOnClickListener(item -> callActivity(activity ->
						GalleryDetailsFragment.showInstance(activity, mediaItem.getId())))
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

		items.add(new PopUpMenuItem.Builder(app)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_gsave_dark, iconColor))
				.setTitleId(R.string.shared_string_download)
				.setOnClickListener(item -> {
					String downloadUri = MediaUriResolver.getDownloadUri(mediaItem);
					if (!Algorithms.isEmpty(downloadUri)) {
						downloadImage(downloadUri);
					}
				})
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void downloadImage(@NonNull String url) {
		callMapActivity(activity -> downloadImage(activity, url));
	}

	private void downloadImage(@NonNull MapActivity activity, @NonNull String url) {
		String fileName = url.substring(url.lastIndexOf('/') + 1);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startDownloading(fileName, url);
		} else {
			if (AndroidUtils.hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				startDownloading(fileName, url);
			} else {
				ActivityCompat.requestPermissions(activity,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_EXTERNAL_STORAGE_PERMISSION);
			}
		}
	}

	private void startDownloading(String fileName, String url) {
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
				(DownloadManager) getMapActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		downloadManager.enqueue(request);
	}

	private void setupViewPager(@NonNull View view) {
		ViewPager pager = view.findViewById(R.id.photo_pager);
		FragmentManager manager = getChildFragmentManager();

		ViewPagerAdapter adapter = new ViewPagerAdapter(manager, photoItems);
		pager.setAdapter(adapter);
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
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		pager.setPageTransformer(true, new GalleryDepthTransformer());
	}

	@Nullable
	private MediaItem getSelectedMediaItem() {
		GalleryItem.Media item = getSelectedGalleryItem();
		return item != null ? item.getMediaItem() : null;
	}

	@Nullable
	private GalleryItem.Media getSelectedGalleryItem() {
		if (selectedPosition >= 0 && selectedPosition < photoItems.size()) {
			return photoItems.get(selectedPosition);
		}
		return null;
	}

	private void setupSelectableBackground(@NonNull View view) {
		int color = app.getSettings().getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	private void setupOnBackPressedCallback() {
		OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				dismiss();
			}
		};
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
	}

	private void dismiss() {
		callActivity(activity -> activity.getSupportFragmentManager().popBackStack());
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), false);
		return R.color.color_transparent;
	}

	@Override
	public void onResume() {
		super.onResume();
		callMapActivity(MapActivity::disableDrawer);
	}

	@Override
	public void onPause() {
		super.onPause();
		callMapActivity(MapActivity::enableDrawer);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String selectedItemId) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(SELECTED_ITEM_ID_KEY, selectedItemId);
			GalleryPhotoPagerFragment fragment = new GalleryPhotoPagerFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	private static class ViewPagerAdapter extends FragmentStatePagerAdapter {

		private final List<GalleryItem.Media> pictures;

		public ViewPagerAdapter(@NonNull FragmentManager manager,
		                        @NonNull List<GalleryItem.Media> pictures) {
			super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.pictures = pictures;
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			return GalleryPhotoViewerFragment.newInstance(position);
		}

		@Override
		public int getCount() {
			return pictures.size();
		}
	}
}