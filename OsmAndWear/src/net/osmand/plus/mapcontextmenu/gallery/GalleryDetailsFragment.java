package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.gallery.GalleryController.DownloadMetadataListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.wikipedia.WikiAlgorithms;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;
import net.osmand.wiki.Metadata;

import org.jetbrains.annotations.NotNull;

public class GalleryDetailsFragment extends BaseOsmAndFragment implements DownloadMetadataListener {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private GalleryController controller;

	private int selectedPosition;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), !nightMode);
		return ColorUtilities.getAppBarColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return !nightMode;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = (GalleryController) app.getDialogManager().findController(GalleryController.PROCESS_ID);
		if (controller != null) {
			controller.addMetaDataListener(this);
		}

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
		View view = themedInflater.inflate(R.layout.gallery_details_fragment, container, false);
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

	private ImageCard getSelectedCard() {
		return controller.getOnlinePhotoCards().get(selectedPosition);
	}

	private void updateContent(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.container);
		container.removeAllViews();

		ImageCard card = getSelectedCard();
		Metadata metadata = card instanceof WikiImageCard ? ((WikiImageCard) card).getWikiImage().getMetadata() : null;

		String author = metadata != null ? metadata.getAuthor() : null;
		if (!Algorithms.isEmpty(author)) {
			buildItem(container, getString(R.string.shared_string_author), author, R.drawable.ic_action_user, true, false);
		}

		String date = metadata != null ? metadata.getDate() : null;
		String formattedDate = WikiAlgorithms.formatWikiDate(date);
		if (Algorithms.isEmpty(formattedDate)) {
			formattedDate = date;
		}
		if (!Algorithms.isEmpty(formattedDate)) {
			buildItem(container, getString(R.string.shared_string_added), formattedDate, R.drawable.ic_action_sort_by_date, true, false);
		}

		int iconId = card.getTopIconId();
		buildItem(container, getString(R.string.shared_string_source), getSourceTypeName(card), iconId, false, false);

		String license = metadata != null ? metadata.getLicense() : null;
		if (!Algorithms.isEmpty(license)) {
			buildItem(container, getString(R.string.shared_string_license), license, R.drawable.ic_action_copyright, true, false);
		}

		String link;
		if (card instanceof WikiImageCard wikiImageCard) {
			link = wikiImageCard.getWikiImage().getUrlWithCommonAttributions();
		} else {
			link = !Algorithms.isEmpty(card.getImageHiresUrl()) ? card.getImageHiresUrl() : card.getImageUrl();
		}
		buildItem(container, getString(R.string.shared_string_link), link, R.drawable.ic_action_link, true, true);
	}

	@NonNull
	private String getSourceTypeName(@NonNull ImageCard imageCard) {
		return imageCard instanceof WikiImageCard ? getString(R.string.wikimedia) : "";
	}

	private void buildItem(@NonNull ViewGroup container, @NonNull String title, @NonNull String description,
	                       int iconId, boolean defaultColor, boolean isUrl) {
		View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_descr_72dp, container, false);

		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		ImageView iconView = view.findViewById(R.id.icon);
		Drawable drawable = !defaultColor ? app.getUIUtilities().getIcon(iconId) : app.getUIUtilities().getPaintedIcon(iconId, defaultIconColor);
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
			ShareMenu.copyToClipboardWithToast(app, description, Toast.LENGTH_SHORT);
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

	@Override
	public void onMetadataDownloaded(@NonNull @NotNull WikiImageCard imageCard) {
		View view = getView();
		if (view != null && Algorithms.stringsEqual(imageCard.getImageUrl(), getSelectedCard().getImageUrl())) {
			updateContent(view);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (controller != null) {
			controller.removeMetaDataListener(this);
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return (MapActivity) requireActivity();
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
