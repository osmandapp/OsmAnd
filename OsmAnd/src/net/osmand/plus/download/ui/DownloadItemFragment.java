package net.osmand.plus.download.ui;

import static net.osmand.plus.download.ui.DownloadResourceGroupFragment.REGION_ID_DLG_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.ui.DownloadDescriptionInfo.ActionButton;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.custom.CustomIndexItem;
import net.osmand.plus.plugins.custom.CustomRegion;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.Algorithms;

import java.util.List;

public class DownloadItemFragment extends BaseFullScreenDialogFragment implements DownloadEvents {

	public static final String ITEM_ID_DLG_KEY = "index_item_dialog_key";

	public static final String TAG = DownloadItemFragment.class.getSimpleName();

	private String regionId = "";
	private int itemIndex = -1;

	private BannerAndDownloadFreeVersion banner;
	private DownloadResourceGroup group;

	private Toolbar toolbar;
	private TextView description;
	private ViewPager imagesPager;
	private View descriptionContainer;
	private ViewGroup buttonsContainer;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.item_info_fragment, container, false);

		if (savedInstanceState != null) {
			regionId = savedInstanceState.getString(REGION_ID_DLG_KEY);
			itemIndex = savedInstanceState.getInt(ITEM_ID_DLG_KEY, -1);
		}
		if ((itemIndex == -1 || group == null) && getArguments() != null) {
			regionId = getArguments().getString(REGION_ID_DLG_KEY);
			itemIndex = getArguments().getInt(ITEM_ID_DLG_KEY, -1);
		}

		toolbar = view.findViewById(R.id.toolbar);
		Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) requireActivity(), false);

		description = view.findViewById(R.id.description);
		imagesPager = view.findViewById(R.id.images_pager);
		buttonsContainer = view.findViewById(R.id.buttons_container);
		descriptionContainer = view.findViewById(R.id.description_container);

		reloadData();

		view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = view.getViewTreeObserver();
				obs.removeOnGlobalLayoutListener(this);
				descriptionContainer.setPadding(0, 0, 0, buttonsContainer.getHeight());
			}
		});

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(REGION_ID_DLG_KEY, regionId);
		outState.putInt(ITEM_ID_DLG_KEY, itemIndex);
	}

	@Override
	public void onUpdatedIndexesList() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		reloadData();
	}

	@Override
	public void downloadHasFinished() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		reloadData();
	}

	@Override
	public void downloadInProgress() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
	}

	private void reloadData() {
		DownloadActivity activity = (DownloadActivity) requireActivity();
		DownloadResources indexes = activity.getDownloadThread().getIndexes();
		group = indexes.getGroupById(regionId);
		CustomIndexItem indexItem = (CustomIndexItem) group.getItemByIndex(itemIndex);
		if (indexItem != null) {
			toolbar.setTitle(indexItem.getVisibleName(app, app.getRegions()));

			DownloadDescriptionInfo descriptionInfo = indexItem.getDescriptionInfo();
			if (descriptionInfo != null) {
				updateDescription(app, descriptionInfo, description);
				updateImagesPager(app, descriptionInfo, imagesPager);
				updateActionButtons(activity, descriptionInfo, indexItem, buttonsContainer, R.layout.bottom_buttons, nightMode);
			}
		}
		WorldRegion region = group.getParentGroup().getRegion();
		if (region instanceof CustomRegion) {
			int headerColor = ((CustomRegion) region).getHeaderColor();
			if (headerColor != CustomRegion.INVALID_ID) {
				toolbar.setBackgroundColor(headerColor);
			}
		}
	}

	static void updateActionButtons(@NonNull DownloadActivity activity,
			@NonNull DownloadDescriptionInfo descriptionInfo,
			@Nullable IndexItem indexItem, ViewGroup buttonsContainer,
			@LayoutRes int layoutId, boolean nightMode) {
		buttonsContainer.removeAllViews();

		List<ActionButton> actionButtons = descriptionInfo.getActionButtons(activity);
		if (Algorithms.isEmpty(actionButtons) && indexItem != null && !indexItem.isDownloaded()) {
			actionButtons.add(new ActionButton(ActionButton.DOWNLOAD_ACTION, activity.getString(R.string.shared_string_download), null));
		}

		for (ActionButton actionButton : actionButtons) {
			View buttonView = UiUtilities.getInflater(activity, nightMode).inflate(layoutId, buttonsContainer, false);
			DialogButton button = buttonView.findViewById(R.id.dismiss_button);
			if (button != null) {
				button.setButtonType(DialogButtonType.PRIMARY);
				button.setTitle(actionButton.getName());
			} else {
				TextView buttonText = buttonView.findViewById(R.id.button_text);
				buttonText.setText(actionButton.getName());
			}
			buttonView.setOnClickListener(v -> {
				if (actionButton.getUrl() != null) {
					AndroidUtils.openUrl(activity, actionButton.getUrl(), nightMode);
				} else if (ActionButton.DOWNLOAD_ACTION.equalsIgnoreCase(actionButton.getActionType()) && indexItem != null) {
					boolean isDownloading = activity.getDownloadThread().isDownloading(indexItem);
					if (!isDownloading) {
						activity.startDownload(indexItem);
					}
				} else {
					AndroidUtils.getApp(activity).showShortToastMessage(R.string.download_unsupported_action, actionButton.getActionType());
				}
			});
			buttonsContainer.addView(buttonView);
		}
	}

	static void updateDescription(OsmandApplication app, DownloadDescriptionInfo descriptionInfo, TextView descriptionView) {
		CharSequence descr = descriptionInfo.getLocalizedDescription(app);
		descriptionView.setText(descr);
		AndroidUiHelper.updateVisibility(descriptionView, !Algorithms.isEmpty(descr));
	}

	static void updateImagesPager(OsmandApplication app, DownloadDescriptionInfo descriptionInfo, ViewPager viewPager) {
		if (!Algorithms.isEmpty(descriptionInfo.getImageUrls())) {
			ImagesPagerAdapter adapter = new ImagesPagerAdapter(app, descriptionInfo.getImageUrls());
			viewPager.setAdapter(adapter);
			viewPager.setVisibility(View.VISIBLE);
		} else {
			viewPager.setVisibility(View.GONE);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String regionId, int itemIndex) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(REGION_ID_DLG_KEY, regionId);
			bundle.putInt(ITEM_ID_DLG_KEY, itemIndex);
			DownloadItemFragment fragment = new DownloadItemFragment();
			fragment.setArguments(bundle);
			fragment.show(fragmentManager, TAG);
		}
	}
}