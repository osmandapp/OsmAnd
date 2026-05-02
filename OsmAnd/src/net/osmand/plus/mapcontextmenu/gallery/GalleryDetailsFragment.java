package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.gallery.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.shared.media.MediaUrlResolver;
import net.osmand.shared.media.domain.MediaDetails;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.util.Algorithms;

public class GalleryDetailsFragment extends BaseFullScreenFragment {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private GalleryController controller;

	private int selectedPosition;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), true);
		return ColorUtilities.getAppBarColorId(nightMode);
	}

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
		View view = inflate(R.layout.gallery_details_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		updateContent(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(R.string.shared_string_details);

		toolbar.findViewById(R.id.close_button).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.getSupportFragmentManager().popBackStack();
			}
		});
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	@Nullable
	private GalleryItem getSelectedGalleryItem() {
		return controller.getOnlinePhotoItems().get(selectedPosition);
	}

	private void updateContent(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.container);
		container.removeAllViews();

		GalleryItem galleryItem = getSelectedGalleryItem();
		if (!(galleryItem instanceof GalleryItem.Media media)) {
			return;
		}

		MediaItem mediaItem = media.getMediaItem();
		MediaDetails details = mediaItem.getDetails();

		String description = details.getDescription();
		if (!Algorithms.isEmpty(description)) {
			buildDescriptionItem(container, description);
		}

		String author = details.getAuthor();
		if (!Algorithms.isEmpty(author)) {
			buildItem(container, getString(R.string.shared_string_author), author, R.drawable.ic_action_user, true, false);
		}

		String date = details.getDate();
		String formattedDate = WikiAlgorithms.formatWikiDate(date);
		if (Algorithms.isEmpty(formattedDate)) {
			formattedDate = date;
		}
		if (!Algorithms.isEmpty(formattedDate)) {
			buildItem(container, getString(R.string.shared_string_added), formattedDate, R.drawable.ic_action_sort_by_date, true, false);
		}

		String source = mediaItem.getOrigin().getTitle();
		String iconName = mediaItem.getOrigin().getIconName();
		int iconId = getIconId(iconName);
		if (!Algorithms.isEmpty(source) || iconId != 0) {
			buildItem(container, getString(R.string.shared_string_source), source, iconId, false, false);
		}

		String license = details.getLicense();
		if (!Algorithms.isEmpty(license)) {
			buildItem(container, getString(R.string.shared_string_license), license, R.drawable.ic_action_copyright, true, false);
		}

		String link = MediaUrlResolver.getDisplayLink(mediaItem);
		if (!Algorithms.isEmpty(link)) {
			buildItem(container, getString(R.string.shared_string_link), link, R.drawable.ic_action_link, true, true);
		}
	}

	private void buildItem(@NonNull ViewGroup container, @NonNull String title, @NonNull String description,
	                       int iconId, boolean defaultColor, boolean isUrl) {
		View view = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container, false);

		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		ImageView iconView = view.findViewById(R.id.icon);
		Drawable drawable = !defaultColor ? uiUtilities.getIcon(iconId) : uiUtilities.getPaintedIcon(iconId, defaultIconColor);
		iconView.setImageDrawable(drawable);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		titleView.setTextSize(14);
		titleView.setText(title);

		TextView descriptionView = view.findViewById(R.id.description);
		descriptionView.setTextColor(isUrl ? ColorUtilities.getActiveColor(app, nightMode) : ColorUtilities.getPrimaryTextColor(app, nightMode));
		descriptionView.setTextSize(16);
		descriptionView.setText(description);

		view.setOnLongClickListener(v -> {
			ShareMenu.copyToClipboardWithToast(app, description, false);
			return true;
		});
		if (isUrl) {
			view.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AndroidUtils.openUrl(activity, description, nightMode);
				}
			});
		}

		container.addView(view);
	}

	private void buildDescriptionItem(@NonNull ViewGroup container, @NonNull String description) {
		View view = inflate(R.layout.bottom_sheet_item_description_with_padding, container, false);
		view.setMinimumHeight(0);

		TextView descriptionView = view.findViewById(R.id.description);

		descriptionView.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		descriptionView.setTextSize(16);
		descriptionView.setText(description);
		descriptionView.setMinHeight(0);
		descriptionView.setMinimumHeight(0);

		view.setOnLongClickListener(v -> {
			ShareMenu.copyToClipboardWithToast(app, description, false);
			return true;
		});

		container.addView(view);
		View dividerContainer = inflate(R.layout.divider_half_item_with_background, container, false);
		View divider = dividerContainer.findViewById(R.id.divider_half_item);
		FrameLayout.LayoutParams params = new LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		params.setMargins(dpToPx(16), 0, 0, 0);
		divider.setLayoutParams(params);

		container.addView(dividerContainer);
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

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	private int getIconId(@Nullable String iconName) {
		return iconName != null ? AndroidUtils.getDrawableId(app, iconName) : 0;
	}

	public static void showInstance(@NonNull FragmentActivity activity, int selectedPosition) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putInt(SELECTED_POSITION_KEY, selectedPosition);
			GalleryDetailsFragment fragment = new GalleryDetailsFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
