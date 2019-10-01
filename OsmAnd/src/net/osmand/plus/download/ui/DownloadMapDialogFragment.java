package net.osmand.plus.download.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.SheetDialogType;
import net.osmand.plus.base.TopSheetDialogFragment;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

public class DownloadMapDialogFragment extends TopSheetDialogFragment {

	public static final String TAG = "DownloadMapDialogFragment";
	
	private final static String REGION_NAME = "REGION_NAME";

	protected DownloadValidationManager downloadValidationManager;

	private View btnClose;
	private View btnDownload;
	private TextView tvDescription;
	private TextView tvSize;

	private IndexItem currentIndexItem;
	private String currentRegionName;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		readBundle(getArguments());

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme), R.layout.card_download_detailed_map, null);

		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			mainView.setBackgroundResource(getLandscapeBottomSidesBgResId());
		} else {
			mainView.setBackgroundResource(getPortraitBgResId());
		}

		downloadValidationManager = new DownloadValidationManager(getMyApplication());

		tvDescription = mainView.findViewById(R.id.description);
		tvSize = mainView.findViewById(R.id.fileSize);
		btnClose = mainView.findViewById(R.id.btnClose);
		btnDownload = mainView.findViewById(R.id.btnDownload);

		UiUtilities.setupDialogButton(nightMode, btnClose, UiUtilities.DialogButtonType.SECONDARY, getString(R.string.shared_string_close));
		UiUtilities.setupDialogButton(nightMode, btnDownload, UiUtilities.DialogButtonType.PRIMARY, getString(R.string.shared_string_download));

		refreshView();

		return mainView;
	}

	public void refreshData(final String newRegionName, final IndexItem newIndexItem) {
		getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				currentRegionName = newRegionName;
				currentIndexItem = newIndexItem;
				refreshView();
			}
		});
	}

	private void refreshView() {
		if (currentRegionName != null) {
			String descriptionText = String.format(getString(R.string.download_detaile_map), currentRegionName);
			int startIndex = descriptionText.indexOf(currentRegionName);
			int endIndex = startIndex + currentRegionName.length();

			SpannableStringBuilder description = new SpannableStringBuilder(descriptionText);
			Typeface typeface = FontCache.getRobotoMedium(getMyApplication());
			description.setSpan(new CustomTypefaceSpan(typeface), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			tvDescription.setText(description);
		}

		if (currentIndexItem != null) {
			String size = currentIndexItem.getSizeDescription(getMyApplication()).toLowerCase();
			tvSize.setText(size);

			btnDownload.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					downloadValidationManager.startDownload(getActivity(), currentIndexItem);
					dismiss();
				}
			});
		}

		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	protected void readBundle(Bundle bundle) {
		if (bundle != null) {
			currentRegionName = bundle.getString(REGION_NAME);
		}
	}

	public static DownloadMapDialogFragment newInstance(FragmentManager fragmentManager, IndexItem indexItem, String name, boolean usedOnMap) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_NAME, name);
		DownloadMapDialogFragment fragment = new DownloadMapDialogFragment();
		fragment.setArguments(bundle);
		fragment.setCurrentIndexItem(indexItem);
		fragment.setUsedOnMap(usedOnMap);
		return fragment;
	}

	public static void showInstance(FragmentManager fragmentManager, IndexItem indexItem, String name, boolean usedOnMap) {
		DownloadMapDialogFragment fragment = newInstance(fragmentManager, indexItem, name, usedOnMap);
		fragmentManager.beginTransaction()
				.add(fragment, TAG).commitAllowingStateLoss();
	}

	public static void hideInstance(FragmentManager fragmentManager) {
		DownloadMapDialogFragment fragment = (DownloadMapDialogFragment) fragmentManager.findFragmentByTag(TAG);
		if (fragment != null) {
			fragment.dismiss();
		}
	}

	@Override
	protected SheetDialogType getSheetDialogType() {
		return SheetDialogType.TOP;
	}

	public void setCurrentIndexItem(IndexItem currentIndexItem) {
		this.currentIndexItem = currentIndexItem;
	}

	@DrawableRes
	protected int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_top_menu_dark : R.drawable.bg_top_menu_light;
	}

	@DrawableRes
	protected int getLandscapeBottomSidesBgResId() {
		return nightMode ? R.drawable.bg_top_sheet_bottom_sides_landscape_dark : R.drawable.bg_top_sheet_bottom_sides_landscape_light;
	}

	@Override
	protected boolean getCancelOnTouchOutside() {
		return false;
	}

	@Override
	protected boolean getInteractWithOutside() {
		return true;
	}

	@Override
	protected float getBackgroundDimAmount() {
		return 0;
	}
}
