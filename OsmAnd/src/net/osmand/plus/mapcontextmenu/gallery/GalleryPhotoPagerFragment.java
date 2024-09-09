package net.osmand.plus.mapcontextmenu.gallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryPhotoPagerFragment extends BaseOsmAndFragment {

	public static final String TAG = GalleryPhotoPagerFragment.class.getSimpleName();
	public static final String SELECTED_POSITION_KEY = "selected_position_key";

	private ImageView sourceView;
	private TextView dateView;
	private TextView authorView;
	private View descriptionShadow;
	private View descriptionContainer;
	private Toolbar toolbar;

	private boolean uiHidden = false;
	private int selectedPosition = 0;
	private GalleryContextHelper galleryContextHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		galleryContextHelper = app.getGalleryContextHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		ViewGroup view = (ViewGroup) themedInflater.inflate(R.layout.gallery_photo_fragment, container, false);
		AndroidUtils.enterToFullScreen(getMapActivity(), view);

		toolbar = view.findViewById(R.id.toolbar);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.topMargin = AndroidUtils.getStatusBarHeight(getMapActivity());
		toolbar.setLayoutParams(params);

		sourceView = view.findViewById(R.id.source_icon);
		dateView = view.findViewById(R.id.date);
		authorView = view.findViewById(R.id.author);
		descriptionShadow = view.findViewById(R.id.description_shadow);
		descriptionContainer = view.findViewById(R.id.description_container);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
		} else if (args != null && args.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = args.getInt(SELECTED_POSITION_KEY);
		}

		ViewPager photoPager = view.findViewById(R.id.photo_pager);
		ViewPagerAdapter adapter = new ViewPagerAdapter(getMapActivity().getSupportFragmentManager(), galleryContextHelper.getOnlinePhotoCards(), this);
		photoPager.setAdapter(adapter);
		photoPager.setCurrentItem(selectedPosition);
		photoPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				selectedPosition = position;
				updateImageDescription(galleryContextHelper.getOnlinePhotoCards().get(selectedPosition));
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});
		photoPager.setPageTransformer(true, new GalleryDepthTransformer());

		setupToolbar(view);
		setupOnBackPressedCallback();
		updateImageDescription(galleryContextHelper.getOnlinePhotoCards().get(selectedPosition));

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	private void updateImageDescription(@NonNull ImageCard imageCard) {
		Drawable icon = app.getUIUtilities().getIcon(imageCard.getTopIconId());
		if (icon != null) {
			sourceView.setImageDrawable(icon);
		} else {
			sourceView.setVisibility(View.GONE);
		}

		Date timestamp = imageCard.getTimestamp();
		if (timestamp != null) {
			String date = new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(imageCard.getTimestamp());
			dateView.setText(date);
		} else {
			dateView.setVisibility(View.GONE);
		}

		String username = imageCard.getUserName();
		if (!Algorithms.isEmpty(username)) {
			authorView.setText(imageCard.getUserName());
		} else {
			authorView.setVisibility(View.GONE);
		}
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
				toolbar.animate()
						.translationY(0)
						.alpha(1.0f)
						.setListener(null);

				descriptionShadow.setVisibility(View.VISIBLE);
				descriptionShadow.setAlpha(0.0f);
				descriptionShadow.animate()
						.alpha(1.0f)
						.setListener(null);

				descriptionContainer.setVisibility(View.VISIBLE);
				descriptionContainer.setAlpha(0.0f);
				descriptionContainer.animate()
						.translationY(0)
						.alpha(1.0f)
						.setListener(null);
			}
		} else {
			toolbar.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionContainer.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
			descriptionShadow.setVisibility(uiHidden ? View.GONE : View.VISIBLE);
		}
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		ImageView backButton = toolbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_close, ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		backButton.setContentDescription(app.getString(R.string.shared_string_close));
		backButton.setOnClickListener(view1 -> onBackPressed());

		ImageView shareButton = toolbar.findViewById(R.id.share_button);
		shareButton.setOnClickListener(v -> shareImage());
		shareButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_gshare_dark, ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(v -> openOptionsMenu());
		optionsButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_overflow_menu_white, ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
	}

	private void shareImage() {

	}

	private void openOptionsMenu() {

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
		AndroidUiHelper.setStatusBarContentColor(getView(), true);
		return R.color.color_transparent;
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return !nightMode;
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

	public static void showInstance(@NonNull FragmentActivity activity, int selectedPosition) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putInt(SELECTED_POSITION_KEY, selectedPosition);
			GalleryPhotoPagerFragment fragment = new GalleryPhotoPagerFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.setReorderingAllowed(true)
					.commit();
		}
	}

	private static class ViewPagerAdapter extends FragmentStatePagerAdapter {

		private final List<ImageCard> pictures;
		private final Fragment targetFragment;

		public ViewPagerAdapter(FragmentManager fm, List<ImageCard> pictures, Fragment target) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
			this.pictures = pictures;
			this.targetFragment = target;
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			return GalleryPhotoViewerFragment.newInstance(position, targetFragment);
		}

		@Override
		public int getCount() {
			return pictures.size();
		}
	}
}
