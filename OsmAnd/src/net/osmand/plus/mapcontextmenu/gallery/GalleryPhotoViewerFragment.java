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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.imageview.GalleryImageView;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;

import org.apache.commons.logging.Log;

import java.util.Set;

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

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	private void setupImageView(@NonNull ViewGroup view) {
		imageView = view.findViewById(R.id.image);

		if (controller != null) {
			ImageCard imageCard = controller.getOnlinePhotoCards().get(selectedPosition);
			if (imageCard != null) {
				if (loadingImage != null) {
					loadingImage.cancel();
				}
				if (!app.getSettings().isInternetConnectionAvailable()) {
					downloadHiResImage(imageCard, true);
				} else{
					downloadThumbnail(imageCard);
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

	private void downloadThumbnail(@NonNull ImageCard imageCard) {
		String thumbnailUrl = imageCard.getThumbnailUrl();
		if (thumbnailUrl != null) {
			loadingImage = controller.getImageLoader().loadImage(thumbnailUrl, new ImageLoaderCallback() {
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

					downloadHiResImage(imageCard, false);
				}

				@Override
				public void onError() {
					downloadHiResImage(imageCard, true);
				}
			}, false);
		} else {
			downloadHiResImage(imageCard, false);
		}
	}

	private void downloadHiResImage(@NonNull ImageCard imageCard, boolean loadPreviewOnError) {
		String hiResUrl = imageCard.getGalleryFullSizeUrl();
		if (hiResUrl != null) {
			loadingImage = controller.getImageLoader().loadImage(imageCard.getGalleryFullSizeUrl(), new ImageLoaderCallback() {
				@Override
				public void onStart(@Nullable Bitmap bitmap) {

				}

				@Override
				public void onSuccess(@NonNull Bitmap bitmap) {
					Drawable previous = imageView.getDrawable() != null ? imageView.getDrawable() : new ColorDrawable(Color.TRANSPARENT);
					Drawable next = new BitmapDrawable(imageView.getResources(), bitmap);

					AndroidUiHelper.crossFadeDrawables(imageView,
							previous,
							next);
				}

				@Override
				public void onError() {
					if (loadPreviewOnError) {
						tryLoadCachePreviewImage(imageCard);
					} else {
						LOG.error("Unable to download hi res image: " + hiResUrl);
					}
				}
			}, false);
		}
	}

	private void tryLoadCachePreviewImage(@NonNull ImageCard imageCard) {
		String imageUrl = imageCard.getImageUrl();
		if (imageUrl != null) {
			loadingImage = controller.getImageLoader().loadImage(imageUrl, new ImageLoaderCallback() {
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
			}, false);
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