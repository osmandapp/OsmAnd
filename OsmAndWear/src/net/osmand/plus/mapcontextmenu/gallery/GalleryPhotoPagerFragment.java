package net.osmand.plus.mapcontextmenu.gallery;

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

import com.squareup.picasso.Picasso;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.AbstractCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController.DownloadMetadataListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.Metadata;

import java.util.ArrayList;
import java.util.List;

public class GalleryPhotoPagerFragment extends BaseOsmAndFragment implements DownloadMetadataListener {

	public static final String TAG = GalleryPhotoPagerFragment.class.getSimpleName();
	public static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 2000;
	public static final int PRELOAD_THUMBNAILS_COUNT = 5;
	public static final String SELECTED_POSITION_KEY = "selected_position_key";

	private GalleryController controller;

	private ImageView sourceView;
	private TextView dateView;
	private TextView authorView;
	private TextView licenseView;
	private View descriptionShadow;
	private View descriptionContainer;
	private Toolbar toolbar;

	private boolean uiHidden = false;
	private int selectedPosition = 0;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		controller = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);
	}

	@Override
	public void onMetadataDownloaded(@NonNull WikiImageCard imageCard) {
		if (imageCard.getImageUrl().equals(getSelectedImageCard().getImageUrl())) {
			Metadata metadata = imageCard.getWikiImage().getMetadata();
			setMetaData(metadata.getAuthor(), metadata.getDate(), metadata.getLicense());
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		ViewGroup view = (ViewGroup) themedInflater.inflate(R.layout.gallery_photo_fragment, container, false);

		toolbar = view.findViewById(R.id.toolbar);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.topMargin = AndroidUtils.getStatusBarHeight(getMapActivity());
		toolbar.setLayoutParams(params);

		sourceView = view.findViewById(R.id.source_icon);
		setupMetadataRow(view);

		descriptionShadow = view.findViewById(R.id.description_shadow);
		descriptionContainer = view.findViewById(R.id.description_container);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
		} else if (args != null && args.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = args.getInt(SELECTED_POSITION_KEY);
		}

		ViewPager photoPager = view.findViewById(R.id.photo_pager);
		ViewPagerAdapter adapter = new ViewPagerAdapter(getMapActivity().getSupportFragmentManager(), controller.getOnlinePhotoCards(), this);
		photoPager.setAdapter(adapter);
		photoPager.setCurrentItem(selectedPosition);
		photoPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				boolean shouldPreloadNext = selectedPosition < position;
				selectedPosition = position;
				preloadThumbNails(shouldPreloadNext);
				updateImageDescriptionRow(getSelectedImageCard());
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});
		photoPager.setPageTransformer(true, new GalleryDepthTransformer());

		setupToolbar(view);
		setupOnBackPressedCallback();
		preloadThumbNails();
		updateImageDescriptionRow(getSelectedImageCard());
		return view;
	}

	private void preloadThumbNails() {
		preloadThumbNails(true);
		preloadThumbNails(false);
	}

	private void preloadThumbNails(boolean next) {
		List<ImageCard> imageCards = controller.getOnlinePhotoCards();
		if (imageCards.size() <= 1) {
			return;
		}

		if (next) {
			int startPreloadThumbnailIndex = selectedPosition + 1;
			if (startPreloadThumbnailIndex >= imageCards.size()) {
				return;
			}
			int lastPreloadThumbnailIndex = startPreloadThumbnailIndex + PRELOAD_THUMBNAILS_COUNT;
			if (lastPreloadThumbnailIndex >= imageCards.size()) {
				lastPreloadThumbnailIndex = imageCards.size() - 1;
			}
			for (int i = selectedPosition; i < lastPreloadThumbnailIndex; i++) {
				ImageCard card = imageCards.get(i);
				Picasso.get().load(card.getThumbnailUrl()).fetch();
			}
		} else {
			int startPreloadThumbnailIndex = selectedPosition - 1;
			if (startPreloadThumbnailIndex < 0) {
				return;
			}
			int lastPreloadThumbnailIndex = startPreloadThumbnailIndex - PRELOAD_THUMBNAILS_COUNT;
			if (lastPreloadThumbnailIndex < 0) {
				lastPreloadThumbnailIndex = 0;
			}
			for (int i = selectedPosition; i > lastPreloadThumbnailIndex; i--) {
				ImageCard card = imageCards.get(i);
				Picasso.get().load(card.getThumbnailUrl()).fetch();
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	private void updateImageDescriptionRow(@NonNull ImageCard imageCard) {
		if (imageCard instanceof WikiImageCard wikiImageCard) {
			dateView.setVisibility(View.VISIBLE);
			authorView.setVisibility(View.VISIBLE);
			Metadata metadata = wikiImageCard.getWikiImage().getMetadata();
			String date = metadata.getDate();
			String author = metadata.getAuthor();
			String license = metadata.getLicense();
			if (!wikiImageCard.isMetaDataDownloaded() && (Algorithms.isEmpty(date) || date.equals("Unknown")
					|| Algorithms.isEmpty(author) || author.equals("Unknown")
					|| Algorithms.isEmpty(license) || license.equals("Unknown"))) {
				controller.addMetaDataListener(this);
				controller.downloadWikiMetaData(wikiImageCard);
			} else {
				setMetaData(metadata.getAuthor(), metadata.getDate(), metadata.getLicense());
			}
		} else {
			dateView.setVisibility(View.INVISIBLE);
			authorView.setVisibility(View.INVISIBLE);
		}

		Drawable icon = app.getUIUtilities().getIcon(imageCard.getTopIconId());
		if (icon != null) {
			sourceView.setImageDrawable(icon);
		} else {
			sourceView.setVisibility(View.GONE);
		}
	}

	private void setMetaData(@Nullable String author, @Nullable String date, @Nullable String license) {
		String formattedDate = WikiAlgorithms.formatWikiDate(date);

		String fullDate = getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_date), formattedDate != null && !formattedDate.equals("Unknown") ? formattedDate : "");
		dateView.setText(fullDate);

		String fullAuthorString = getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_author), author != null && !author.equals("Unknown") ? author : "");
		authorView.setText(fullAuthorString);

		String licenseString = getString(R.string.ltr_or_rtl_combine_via_colon,
				getString(R.string.shared_string_license), license != null && !license.equals("Unknown") ? license : "");
		licenseView.setText(licenseString);
	}

	private void setupMetadataRow(ViewGroup view) {
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
		backButton.setContentDescription(getString(R.string.shared_string_close));
		backButton.setOnClickListener(v -> onBackPressed());
		setupSelectableBackground(backButton);

		ImageView shareButton = toolbar.findViewById(R.id.share_button);
		shareButton.setOnClickListener(v -> shareImage());
		shareButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_gshare_dark, ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(shareButton);

		ImageView optionsButton = toolbar.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(this::showContextWidgetMenu);
		optionsButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_overflow_menu_white, ColorUtilities.getColor(app, R.color.app_bar_secondary_light)));
		setupSelectableBackground(optionsButton);
	}

	private void shareImage() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT, getSelectedImageCard().getImageHiresUrl());
			Intent chooserIntent = Intent.createChooser(sendIntent, getString(R.string.shared_string_share));

			AndroidUtils.startActivityIfSafe(activity, chooserIntent);
		}
	}

	public void showContextWidgetMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();
		UiUtilities uiUtilities = app.getUIUtilities();
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_details)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_info_outlined, iconColor))
				.setOnClickListener(item -> GalleryDetailsFragment.showInstance(getMapActivity(), selectedPosition))
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_external_link, iconColor))
				.setTitleId(R.string.open_in_browser)
				.setOnClickListener(item -> {
					FragmentActivity activity = getActivity();
					ImageCard card = getSelectedImageCard();
					if (activity != null && card instanceof WikiImageCard wikiImageCard) {
						AndroidUtils.openUrl(activity, wikiImageCard.getWikiImage().getUrlWithCommonAttributions(), nightMode);
					}
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_gsave_dark, iconColor))
				.setTitleId(R.string.shared_string_download)
				.setOnClickListener(item -> {
					ImageCard card = getSelectedImageCard();
					String downloadUrl = card.getImageHiresUrl();
					if (Algorithms.isEmpty(downloadUrl)) {
						downloadUrl = card.getImageUrl();
					}
					downloadImage(downloadUrl);
				})
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void downloadImage(String url) {
		String fileName = url.substring(url.lastIndexOf('/') + 1);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startDownloading(fileName, url);
		} else {
			if (AndroidUtils.hasPermission(getMapActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				startDownloading(fileName, url);
			} else {
				AndroidUtils.hasPermission(getMapActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
				ActivityCompat.requestPermissions(getMapActivity(), new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_PERMISSION);
			}
		}
	}

	private void startDownloading(String fileName, String url) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
				.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
				.setTitle(fileName)
				.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setAllowedOverMetered(true)
				.setAllowedOverRoaming(false)
				.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
		DownloadManager downloadManager = (DownloadManager) getMapActivity().getSystemService(Context.DOWNLOAD_SERVICE);
		downloadManager.enqueue(request);
	}

	private ImageCard getSelectedImageCard() {
		return controller.getOnlinePhotoCards().get(selectedPosition);
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
		AndroidUiHelper.setStatusBarContentColor(getView(), false);
		return R.color.color_transparent;
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.removeMetaDataListener(this);
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
					.addToBackStack(TAG)
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
