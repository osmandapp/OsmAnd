package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

import android.os.Bundle;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.imageview.GalleryImageView;

import org.apache.commons.logging.Log;

public class GalleryPhotoViewerFragment extends BaseOsmAndFragment {
	private static final Log LOG = PlatformUtil.getLog(GalleryPhotoViewerFragment.class);

	public static final String TAG = GalleryPhotoViewerFragment.class.getSimpleName();

	private GalleryController controller;

	private GalleryImageView imageView;
	private int selectedPosition = 0;

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
		ViewGroup view = (ViewGroup) themedInflater.inflate(R.layout.gallery_photo_item, container, false);

		setupImageView(view);

		return view;
	}

	private void setupImageView(@NonNull ViewGroup view) {
		imageView = view.findViewById(R.id.image);

		ImageCard imageCard = controller.getOnlinePhotoCards().get(selectedPosition);
		downloadThumbnail(imageCard);

		imageView.setOnDoubleTapListener(new SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
				Fragment target = getTargetFragment();
				if (target instanceof GalleryPhotoPagerFragment fragment) {
					fragment.toggleUi();
					return true;
				}
				return false;
			}
		});
	}

	private void downloadThumbnail(@NonNull ImageCard imageCard) {
		Picasso.get()
				.load(imageCard.getThumbnailUrl())
				.into(imageView, new Callback() {
					@Override
					public void onSuccess() {
						downloadHiResImage(imageCard);
					}

					@Override
					public void onError(Exception e) {
						downloadHiResImage(imageCard);
						LOG.error(e);
					}
				});
	}

	private void downloadHiResImage(@NonNull ImageCard imageCard) {
		Picasso.get()
				.load(imageCard.getGalleryFullSizeUrl())
				.placeholder(imageView.getDrawable())
				.into(imageView);
	}

	@Override
	public void onPause() {
		super.onPause();
		imageView.resetZoom();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	@NonNull
	public static Fragment newInstance(int selectedPosition, Fragment target) {
		Bundle bundle = new Bundle();
		bundle.putInt(SELECTED_POSITION_KEY, selectedPosition);

		GalleryPhotoViewerFragment fragment = new GalleryPhotoViewerFragment();
		fragment.setArguments(bundle);
		fragment.setTargetFragment(target, 0);
		return fragment;
	}
}