package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

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
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.gallery.imageview.GalleryImageView;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;

import org.apache.commons.logging.Log;

import java.util.List;

public class GalleryPhotoViewerFragment extends BaseFullScreenFragment {
	private static final Log LOG = PlatformUtil.getLog(GalleryPhotoViewerFragment.class);

	public static final String TAG = GalleryPhotoViewerFragment.class.getSimpleName();

	private GalleryController controller;

	private GalleryImageView imageView;
	private int selectedPosition = 0;
	private LoadingImage loadingImage;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);

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

		if (controller != null) {
			List<GalleryItem> photoItems = controller.getOnlinePhotoItems();
			int position = selectedPosition;
			if (photoItems.size() > position) {
				MediaItem mediaItem = getMediaItem(photoItems.get(position));
				if (mediaItem != null) {
					if (loadingImage != null) {
						loadingImage.cancel();
					}
					if (!app.getSettings().isInternetConnectionAvailable()) {
						downloadFullImage(mediaItem, true);
					} else {
						downloadThumbnail(mediaItem);
					}
				}
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

	private void downloadThumbnail(@NonNull MediaItem mediaItem) {
		loadingImage = controller.getMediaProvider().loadThumbnail(mediaItem, new ImageLoaderCallback() {
			@Override
			public void onStart(@Nullable Bitmap bitmap) {
			}

			@Override
			public void onSuccess(@NonNull Bitmap bitmap) {
				Drawable previous = new ColorDrawable(Color.TRANSPARENT);
				Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

				AndroidUiHelper.crossFadeDrawables(imageView,
						previous,
						next);

				downloadFullImage(mediaItem, false);
			}

			@Override
			public void onError() {
				downloadFullImage(mediaItem, true);
			}
		});
	}

	private void downloadFullImage(@NonNull MediaItem mediaItem, boolean fallbackToPreview) {
		loadingImage = controller.getMediaProvider().loadFull(mediaItem, new ImageLoaderCallback() {
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
					LOG.error("Unable to download full image: " + mediaItem.getResource().getFullUri());
				}
			}
		});
	}

	private void tryLoadCachePreviewImage(@NonNull MediaItem mediaItem) {
		loadingImage = controller.getMediaProvider().loadPreview(mediaItem, new ImageLoaderCallback() {
			@Override
			public void onStart(@Nullable Bitmap bitmap) {
			}

			@Override
			public void onSuccess(@NonNull Bitmap bitmap) {
				Drawable previous = new ColorDrawable(Color.TRANSPARENT);
				Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

				AndroidUiHelper.crossFadeDrawables(imageView,
						previous,
						next);
			}

			@Override
			public void onError() {
			}
		});
	}

	@Nullable
	private MediaItem getMediaItem(@Nullable GalleryItem item) {
		return item instanceof GalleryItem.Media media ? media.getMediaItem() : null;
	}

	@Override
	public void onPause() {
		super.onPause();
		imageView.resetZoom();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadingImage != null) {
			loadingImage.cancel();
		}
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