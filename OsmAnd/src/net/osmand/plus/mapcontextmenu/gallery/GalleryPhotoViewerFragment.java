package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.imageview.GalleryImageView;

public class GalleryPhotoViewerFragment extends BaseOsmAndFragment {

	public static final String TAG = GalleryPhotoViewerFragment.class.getSimpleName();

	private GalleryImageView imageView;
	private int selectedPosition = 0;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		ViewGroup view = (ViewGroup) themedInflater.inflate(R.layout.gallery_photo_item, container, false);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
		} else if (args != null && args.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = args.getInt(SELECTED_POSITION_KEY);
		}

		imageView = view.findViewById(R.id.image);
		GalleryContextHelper galleryContextHelper = app.getGalleryContextHelper();
		ImageCard selectedImageCard = galleryContextHelper.getOnlinePhotoCards().get(selectedPosition);
		Picasso.get().load(selectedImageCard.getImageUrl()).into(imageView);

		imageView.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
			@Override
			public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
				Fragment target = getTargetFragment();
				if (target instanceof GalleryPhotoPagerFragment galleryPhotoPagerFragment) {
					galleryPhotoPagerFragment.toggleUi();
					return true;
				}
				return false;
			}

			@Override
			public boolean onDoubleTap(@NonNull MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
				return false;
			}
		});

		return view;
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
	public static Fragment newInstance(int selectedPosition, Fragment targetFragment) {
		Bundle bundle = new Bundle();
		bundle.putInt(SELECTED_POSITION_KEY, selectedPosition);
		GalleryPhotoViewerFragment fragment = new GalleryPhotoViewerFragment();
		fragment.setArguments(bundle);
		fragment.setTargetFragment(targetFragment, 0);
		return fragment;
	}
}