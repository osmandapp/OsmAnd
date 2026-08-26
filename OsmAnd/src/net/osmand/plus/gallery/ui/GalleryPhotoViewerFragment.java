package net.osmand.plus.gallery.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.gallery.controller.GalleryPagerController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.ui.imageview.GalleryImageView;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.media.MediaProvider;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;

import org.apache.commons.logging.Log;

import java.util.List;

public class GalleryPhotoViewerFragment extends BaseFullScreenFragment {

	private static final Log LOG = PlatformUtil.getLog(GalleryPhotoViewerFragment.class);

	public static final String TAG = GalleryPhotoViewerFragment.class.getSimpleName();

	public static final String SELECTED_POSITION_KEY = "selected_position_key";

	private GalleryImageView imageView;
	private int selectedPosition = 0;
	private LoadingImage loadingImage;
	private MediaProvider mediaProvider;
	private GalleryPagerController controller;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mediaProvider = new MediaProvider(app);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
		} else if (args != null && args.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = args.getInt(SELECTED_POSITION_KEY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();

		controller = GalleryPagerController.getExistingInstance(app);
		if (controller == null) {
			return null;
		}

		ViewGroup view = (ViewGroup) inflate(R.layout.gallery_photo_item, container, false);
		setupImageView(view);
		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	private void setupImageView(@NonNull ViewGroup view) {
		imageView = view.findViewById(R.id.image);

		GalleryItem.Media photoItem = getPhotoItem(selectedPosition);
		if (photoItem != null) {
			MediaItem mediaItem = photoItem.getMediaItem();
			cancelLoadingImage();
			if (!app.getSettings().isInternetConnectionAvailable()) {
				downloadFullImage(mediaItem, true);
			} else {
				downloadThumbnail(mediaItem);
			}
		}

		imageView.setOnDoubleTapListener(new SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
				Fragment target = getParentFragment();
				if (target instanceof GalleryPhotoPagerFragment fragment) {
					fragment.toggleUi();
					return true;
				}
				return false;
			}
		});
	}

	@Nullable
	private GalleryItem.Media getPhotoItem(int position) {
		if (controller == null) {
			return null;
		}

		List<GalleryItem.Media> mediaItems = controller.getMediaItems();
		return mediaItems.size() > position ? mediaItems.get(position) : null;
	}

	private void downloadThumbnail(@NonNull MediaItem mediaItem) {
		trackLoadingImage(mediaProvider.loadThumbnail(mediaItem, new ImageLoaderCallback() {
			@Override
			public void onStart(@Nullable Bitmap bitmap) {
			}

			@Override
			public void onSuccess(@NonNull Bitmap bitmap) {
				Drawable previous = new ColorDrawable(Color.TRANSPARENT);
				Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

				AndroidUiHelper.crossFadeDrawables(imageView, previous, next);

				downloadFullImage(mediaItem, false);
			}

			@Override
			public void onError() {
				downloadFullImage(mediaItem, true);
			}
		}));
	}

	private void downloadFullImage(@NonNull MediaItem mediaItem, boolean fallbackToPreview) {
		trackLoadingImage(mediaProvider.loadFullSizeImage(mediaItem, new ImageLoaderCallback() {
			@Override
			public void onStart(@Nullable Bitmap bitmap) {
			}

			@Override
			public void onSuccess(@NonNull Bitmap bitmap) {
				Drawable previous = imageView.getDrawable() != null
						? imageView.getDrawable()
						: new ColorDrawable(Color.TRANSPARENT);
				Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

				AndroidUiHelper.crossFadeDrawables(imageView, previous, next);
			}

			@Override
			public void onError() {
				if (fallbackToPreview) {
					tryLoadCachePreviewImage(mediaItem);
				} else {
					markMediaLoadFailed(mediaItem);
					LOG.error("Unable to download full image: " + mediaItem.getPreviewUris().getFullSizeUri());
				}
			}
		}));
	}

	private void tryLoadCachePreviewImage(@NonNull MediaItem mediaItem) {
		trackLoadingImage(mediaProvider.loadStandardSizeImage(mediaItem, new ImageLoaderCallback() {
			@Override
			public void onStart(@Nullable Bitmap bitmap) {
			}

			@Override
			public void onSuccess(@NonNull Bitmap bitmap) {
				Drawable previous = new ColorDrawable(Color.TRANSPARENT);
				Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

				AndroidUiHelper.crossFadeDrawables(imageView, previous, next);
			}

			@Override
			public void onError() {
				markMediaLoadFailed(mediaItem);
			}
		}));
	}

	private void markMediaLoadFailed(@NonNull MediaItem mediaItem) {
		app.getGalleryHelper().getLoadStateRegistry().markFailed(mediaItem);
	}

	private void trackLoadingImage(@Nullable LoadingImage image) {
		if (image != null) {
			loadingImage = image;
		}
	}

	private void cancelLoadingImage() {
		if (loadingImage != null) {
			loadingImage.cancel();
			loadingImage = null;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		imageView.resetZoom();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelLoadingImage();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	@NonNull
	public static Fragment newInstance(int selectedPosition) {
		Bundle bundle = new Bundle();
		bundle.putInt(SELECTED_POSITION_KEY, selectedPosition);

		GalleryPhotoViewerFragment fragment = new GalleryPhotoViewerFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}