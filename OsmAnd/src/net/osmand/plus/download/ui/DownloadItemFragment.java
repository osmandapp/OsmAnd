package net.osmand.plus.download.ui;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.AndroidUtils;
import net.osmand.PicassoUtils;
import net.osmand.map.WorldRegion;
import net.osmand.plus.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.download.CustomIndexItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import static net.osmand.plus.download.ui.DownloadResourceGroupFragment.REGION_ID_DLG_KEY;

public class DownloadItemFragment extends DialogFragment {

	public static final String ITEM_ID_DLG_KEY = "index_item_dialog_key";

	public static final String TAG = DownloadItemFragment.class.getSimpleName();

	private String regionId = "";
	private int itemIndex = -1;

	private DownloadResourceGroup group;
	private CustomIndexItem indexItem;

	private View view;
	private Toolbar toolbar;
	private ImageView image;
	private TextView description;
	private TextView buttonTextView;

	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = !getMyApplication().getSettings().isLightContent();
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		setStyle(STYLE_NO_FRAME, themeId);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.item_info_fragment, container, false);

		if (savedInstanceState != null) {
			regionId = savedInstanceState.getString(REGION_ID_DLG_KEY);
			itemIndex = savedInstanceState.getInt(ITEM_ID_DLG_KEY, -1);
		}
		if ((itemIndex == -1 || group == null) && getArguments() != null) {
			regionId = getArguments().getString(REGION_ID_DLG_KEY);
			itemIndex = getArguments().getInt(ITEM_ID_DLG_KEY, -1);
		}

		toolbar = view.findViewById(R.id.toolbar);
		Drawable icBack = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(requireContext()));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		description = view.findViewById(R.id.item_description);
		image = view.findViewById(R.id.item_image);

		View dismissButton = view.findViewById(R.id.dismiss_button);
		dismissButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (indexItem != null && !Algorithms.isEmpty(indexItem.getWebUrl())) {
					WikipediaDialogFragment.showFullArticle(v.getContext(), Uri.parse(indexItem.getWebUrl()), nightMode);
				}
			}
		});
		UiUtilities.setupDialogButton(nightMode, dismissButton, UiUtilities.DialogButtonType.PRIMARY, "");
		buttonTextView = (TextView) dismissButton.findViewById(R.id.button_text);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadData();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(REGION_ID_DLG_KEY, regionId);
		outState.putInt(ITEM_ID_DLG_KEY, itemIndex);
	}

	private void reloadData() {
		DownloadActivity downloadActivity = getDownloadActivity();
		if (downloadActivity != null) {
			OsmandApplication app = downloadActivity.getMyApplication();
			DownloadResources indexes = getDownloadActivity().getDownloadThread().getIndexes();
			group = indexes.getGroupById(regionId);
			indexItem = (CustomIndexItem) group.getItemByIndex(itemIndex);
			if (indexItem != null) {
				toolbar.setTitle(indexItem.getVisibleName(app, app.getRegions()));
				WorldRegion region = group.getRegion();
				if (region instanceof CustomRegion) {
					CustomRegion customRegion = (CustomRegion) region;
					int color = customRegion.getHeaderColor();
					if (color != -1) {
						toolbar.setBackgroundColor(color);
					}
				}

				description.setText(indexItem.getLocalizedDescription(app));
				buttonTextView.setText(indexItem.getWebButtonText(app));

				final PicassoUtils picassoUtils = PicassoUtils.getPicasso(app);
				Picasso picasso = Picasso.get();
				for (final String imageUrl : indexItem.getDescriptionImageUrl()) {
					RequestCreator rc = picasso.load(imageUrl);
					rc.into(image, new Callback() {
						@Override
						public void onSuccess() {
							image.setVisibility(View.VISIBLE);
							picassoUtils.setResultLoaded(imageUrl, true);
						}

						@Override
						public void onError(Exception e) {
							image.setVisibility(View.GONE);
							picassoUtils.setResultLoaded(imageUrl, false);
						}
					});
				}
			}
		}
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static DownloadItemFragment createInstance(String regionId, int itemIndex) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_DLG_KEY, regionId);
		bundle.putInt(ITEM_ID_DLG_KEY, itemIndex);
		DownloadItemFragment fragment = new DownloadItemFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}