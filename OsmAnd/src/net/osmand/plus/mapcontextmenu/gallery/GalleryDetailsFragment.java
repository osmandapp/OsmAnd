package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.GalleryPhotoPagerFragment.SELECTED_POSITION_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.wikipedia.WikiImageCard;
import net.osmand.util.Algorithms;

public class GalleryDetailsFragment extends BaseOsmAndFragment {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private GalleryContextHelper galleryContextHelper;
	private Toolbar toolbar;
	private ImageView navigationIcon;
	private int selectedPosition = 0;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.galleryContextHelper = app.getGalleryContextHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.gallery_details_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		Bundle args = getArguments();
		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
		} else if (args != null && args.containsKey(SELECTED_POSITION_KEY)) {
			selectedPosition = args.getInt(SELECTED_POSITION_KEY);
		}

		toolbar = view.findViewById(R.id.toolbar);
		navigationIcon = toolbar.findViewById(R.id.close_button);
		LinearLayout mainContainer = view.findViewById(R.id.container);

		fillContent(mainContainer);

		setupToolbar();

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
		super.onSaveInstanceState(outState);
	}

	private void fillContent(LinearLayout mainContainer) {
		ImageCard card = galleryContextHelper.getOnlinePhotoCards().get(selectedPosition);

		String author = getAuthor();
		if (!Algorithms.isEmpty(author)) {
			buildItem(mainContainer, app.getString(R.string.shared_string_author), author, R.drawable.ic_action_user, true, false);
		}

		String date = getDate();
		if (!Algorithms.isEmpty(date)) {
			buildItem(mainContainer, app.getString(R.string.shared_string_added), date, R.drawable.ic_action_sort_by_date, true, false);
		}

		int iconId = card.getTopIconId();
		buildItem(mainContainer, app.getString(R.string.shared_string_source), getSourceTypeName(card), iconId, false, false);

		String link = !Algorithms.isEmpty(card.getImageHiresUrl()) ? card.getImageHiresUrl() : card.getImageUrl();
		buildItem(mainContainer, app.getString(R.string.shared_string_link), link, R.drawable.ic_action_link, true, true);
	}

	private String getSourceTypeName(@NonNull ImageCard imageCard) {
		String typeName = "";
		if (imageCard instanceof WikiImageCard) {
			typeName = app.getString(R.string.wikimedia);
		}
		return typeName;
	}

	@Nullable
	private String getDate() {
		return null;
	}

	@Nullable
	private String getAuthor() {
		return null;
	}

	private void buildItem(@NonNull ViewGroup container, @NonNull String title, @NonNull String description,
	                       int iconId, boolean defaultColor, boolean isUrl) {
		View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_descr_72dp, null);

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
			ShareMenu.copyToClipboardWithToast(getMapActivity(), description, Toast.LENGTH_SHORT);
			return true;
		});
		if (isUrl) {
			view.setOnClickListener(v -> AndroidUtils.openUrl(getMapActivity(), description, nightMode));
		}

		container.addView(view);
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.shared_string_details);

		updateToolbarNavigationIcon();
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void updateToolbarNavigationIcon() {
		navigationIcon.setOnClickListener(view -> {
			FragmentManager fragmentManager = getMapActivity().getSupportFragmentManager();
			fragmentManager.popBackStack();
		});
	}

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
			GalleryDetailsFragment fragment = new GalleryDetailsFragment();
			fragment.setArguments(bundle);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
