package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.IMAGES_COUNT_TYPE;
import static net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.IMAGE_TYPE;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryGridAdapter.ImageCardListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.ArrayList;
import java.util.List;

public class GalleryGridFragment extends BaseOsmAndFragment {

	public static final String TAG = GalleryGridFragment.class.getSimpleName();

	protected static final float SCALE_MULTIPLIER = 3f;

	private Toolbar toolbar;

	private GalleryGridRecyclerView recyclerView;

	private GalleryContextHelper galleryContextHelper;
	private GalleryGridAdapter galleryGridAdapter;
	private ScaleGestureDetector scaleDetector;
	private GridLayoutManager gridLayoutManager;

	private float newScaleFactor;

	private boolean zoomedForPinch = false;

	private static final int MAX_GALLERY_GRID_SPAN_COUNT = 7;
	private static final int MIN_GALLERY_GRID_SPAN_COUNT = 2;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		galleryContextHelper = app.getGalleryContextHelper();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.gallery_grid_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupScaleDetector();
		recyclerView = view.findViewById(R.id.content_list);
		recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				galleryGridAdapter = new GalleryGridAdapter(getMapActivity(), galleryContextHelper,
						getImageCardListener(), recyclerView.getMeasuredWidth(), true, nightMode);
				galleryGridAdapter.setResizeBySpanCount(true);

				List<Object> items = new ArrayList<>();
				items.add(IMAGES_COUNT_TYPE);
				items.addAll(galleryContextHelper.getOnlinePhotoCards());
				galleryGridAdapter.setItems(items);

				recyclerView.setAdapter(galleryGridAdapter);
				recyclerView.setScaleDetector(scaleDetector);

				gridLayoutManager = new GridLayoutManager(app, GalleryContextHelper.getSettingsSpanCount(getMapActivity()));
				gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
					@Override
					public int getSpanSize(int position) {
						return galleryGridAdapter.getItemViewType(position) == IMAGE_TYPE ? 1 : gridLayoutManager.getSpanCount();
					}
				});
				recyclerView.setLayoutManager(gridLayoutManager);
				GalleryGridItemDecorator itemDecorator = new GalleryGridItemDecorator(app);
				recyclerView.addItemDecoration(itemDecorator);
			}
		});

		toolbar = view.findViewById(R.id.toolbar);
		setupToolbar();
		setupOnBackPressedCallback();

		return view;
	}

	private ImageCardListener getImageCardListener() {
		return new ImageCardListener() {
			@Override
			public void onImageClicked(@NonNull ImageCard imageCard) {
				GalleryPhotoPagerFragment.showInstance(getMapActivity(), galleryContextHelper.getImageCardFromUrl(imageCard.getImageUrl()));
			}
		};
	}

	private void setupScaleDetector() {
		scaleDetector = new ScaleGestureDetector(app, new ScaleGestureDetector.OnScaleGestureListener() {
			@Override
			public boolean onScale(@NonNull ScaleGestureDetector detector) {
				if (zoomedForPinch) {
					return false;
				}
				if (detector.getScaleFactor() < 1) {
					float b = (detector.getScaleFactor() - 1) * SCALE_MULTIPLIER;
					newScaleFactor = newScaleFactor - b;
				} else {
					float a = (1 - detector.getScaleFactor()) * SCALE_MULTIPLIER;
					newScaleFactor = newScaleFactor + a;
				}
				int previousCount = GalleryContextHelper.getSettingsSpanCount(getMapActivity());
				int newCount = (int) newScaleFactor + previousCount;

				if (newCount != previousCount) {
					newScaleFactor = 0;
					if (newCount <= MAX_GALLERY_GRID_SPAN_COUNT && newCount >= MIN_GALLERY_GRID_SPAN_COUNT) {
						GalleryContextHelper.setSpanSettings(getMapActivity(), newCount);
						updateSpan();
						zoomedForPinch = true;
					}
				}
				return true;
			}

			@Override
			public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
				newScaleFactor = 0;
				return true;
			}

			@Override
			public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
				newScaleFactor = 0;
				zoomedForPinch = false;
			}
		});
	}

	private void updateSpan() {
		gridLayoutManager.setSpanCount(GalleryContextHelper.getSettingsSpanCount(getMapActivity()));
		for (int i = 0; i < galleryGridAdapter.getItemCount(); i++) {
			Object object = galleryGridAdapter.getItems().get(i);
			if (object instanceof ImageCard) {
				galleryGridAdapter.notifyItemChanged(i);
			}
		}
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(getMapActivity().getContextMenu().getTitleStr());

		ImageView navigationIcon = toolbar.findViewById(R.id.back_button);
		navigationIcon.setOnClickListener(view -> {
			onBackPressed();
		});
		navigationIcon.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));

		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void setupOnBackPressedCallback() {
		OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				onBackPressed();
			}
		};
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
	}


	private void onBackPressed() {
		FragmentManager manager = getMapActivity().getSupportFragmentManager();
		manager.popBackStack();
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), !nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			GalleryGridFragment fragment = new GalleryGridFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
