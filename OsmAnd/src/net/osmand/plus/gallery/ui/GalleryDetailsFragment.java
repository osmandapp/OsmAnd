package net.osmand.plus.gallery.ui;

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
import net.osmand.plus.gallery.controller.GalleryPagerController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.shared.media.MediaUriResolver;
import net.osmand.shared.media.domain.MediaDetails;
import net.osmand.shared.media.domain.MediaItem;
import net.osmand.shared.media.domain.MediaOrigin;
import net.osmand.util.Algorithms;

import java.util.List;

public class GalleryDetailsFragment extends BaseFullScreenFragment {

	public static final String TAG = GalleryDetailsFragment.class.getSimpleName();

	private static final String SELECTED_ITEM_ID_KEY = "selected_item_id";

	private String selectedItemId;

	private GalleryPagerController controller;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), true);
		return ColorUtilities.getAppBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_ITEM_ID_KEY)) {
			selectedItemId = savedInstanceState.getString(SELECTED_ITEM_ID_KEY);
		} else if (args != null && args.containsKey(SELECTED_ITEM_ID_KEY)) {
			selectedItemId = args.getString(SELECTED_ITEM_ID_KEY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();

		controller = GalleryPagerController.getExistingInstance(app);
		if (controller == null || Algorithms.isEmpty(selectedItemId)) {
			return null;
		}

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
	private GalleryItem.Media getSelectedGalleryItem() {
		if (controller == null || Algorithms.isEmpty(selectedItemId)) {
			return null;
		}

		List<GalleryItem.Media> items = controller.getPhotoItems();
		for (GalleryItem.Media item : items) {
			if (Algorithms.stringsEqual(item.getMediaItem().getId(), selectedItemId)) {
				return item;
			}
		}
		return null;
	}

	private void updateContent(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.container);
		container.removeAllViews();

		GalleryItem.Media media = getSelectedGalleryItem();
		if (media == null) {
			return;
		}

		MediaItem mediaItem = media.getMediaItem();
		MediaDetails details = mediaItem.getDetails();

		String description = details != null ? details.getDescription(app.getLanguage()) : null;
		if (!Algorithms.isEmpty(description)) {
			buildDescriptionItem(container, description);
		}

		String author = details != null ? details.getAuthor() : null;
		if (!Algorithms.isEmpty(author)) {
			buildItem(container, getString(R.string.shared_string_author), author, R.drawable.ic_action_user, true, false);
		}

		String date = details != null ? details.getDate() : null;
		String formattedDate = WikiAlgorithms.formatWikiDate(date);
		if (Algorithms.isEmpty(formattedDate)) {
			formattedDate = date;
		}
		if (!Algorithms.isEmpty(formattedDate)) {
			buildItem(container, getString(R.string.shared_string_added), formattedDate, R.drawable.ic_action_sort_by_date, true, false);
		}

		MediaOrigin mediaOrigin = mediaItem.getOrigin();
		String titleKey = mediaOrigin.getTitleKey();
		String source = titleKey != null ? getString(titleKey) : null;
		int iconId = getDrawableId(mediaOrigin.getIconName());
		if (!Algorithms.isEmpty(source) || iconId != 0) {
			buildItem(container, getString(R.string.shared_string_source), source, iconId, false, false);
		}

		String license = details != null ? details.getLicense() : null;
		if (!Algorithms.isEmpty(license)) {
			buildItem(container, getString(R.string.shared_string_license), license, R.drawable.ic_action_copyright, true, false);
		}

		String link = MediaUriResolver.getDetailsLink(mediaItem);
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
		if (!Algorithms.isEmpty(selectedItemId)) {
			outState.putString(SELECTED_ITEM_ID_KEY, selectedItemId);
		}
		super.onSaveInstanceState(outState);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String selectedItemId) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(SELECTED_ITEM_ID_KEY, selectedItemId);
			GalleryDetailsFragment fragment = new GalleryDetailsFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}