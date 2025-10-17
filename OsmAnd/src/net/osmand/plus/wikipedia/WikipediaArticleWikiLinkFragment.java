package net.osmand.plus.wikipedia;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescriptionTwoIcons;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.search.AmenitySearcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class WikipediaArticleWikiLinkFragment extends MenuBottomSheetDialogFragment implements DownloadIndexesThread.DownloadEvents {

	public static final String TAG = WikivoyageArticleWikiLinkFragment.class.getSimpleName();

	public static final String ARTICLE_URL_KEY = "article_url";
	public static final String ARTICLE_LATITUDES_KEY = "article_latitudes_key";
	public static final String ARTICLE_LONGITUDES_KEY = "article_longitudes_key";
	//	private static final Log log = LogFactory.getLog(WikipediaArticleWikiLinkFragment.class);
	private final Log log = PlatformUtil.getLog(WikipediaArticleWikiLinkFragment.class);

	private String articleUrl;
	private BottomSheetItemWithDescriptionTwoIcons downloadWikiItem;
	private DownloadIndexesThread downloadThread;
	private List<LatLon> locations = new ArrayList<>();
	private IndexItem wikiLocationIndexItem;


//	boolean isDownloading = true;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		downloadThread = app.getDownloadThread();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Bundle args = getArguments();
		if (args != null) {
			articleUrl = args.getString(ARTICLE_URL_KEY);
			double[] latitudes = args.getDoubleArray(ARTICLE_LATITUDES_KEY);
			double[] longitudes = args.getDoubleArray(ARTICLE_LONGITUDES_KEY);
			if (latitudes != null && longitudes != null && latitudes.length == longitudes.length) {
				for (int i = 0; i < latitudes.length; i++) {
					locations.add(new LatLon(latitudes[i], longitudes[i]));
				}
			}
		}

		BaseBottomSheetItem wikiLinkitem = new BottomSheetItemWithDescription.Builder()
				.setDescription(articleUrl)
				.setTitle(getString(R.string.how_to_open_link))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr)
				.create();
		items.add(new TitleDividerItem(ctx));
		items.add(wikiLinkitem);

		if (Version.isPaidVersion(app)) {
			Drawable osmandLiveIcon = getContentIcon(R.drawable.ic_action_wikipedia);
			Drawable downloadIcon = getActiveIcon(R.drawable.ic_action_download);

			boolean showProgress = false;
			if (downloadThread.shouldDownloadIndexes()) {
				showProgress = true;
				downloadThread.runReloadIndexFiles();
			}

			BottomSheetItemWithDescriptionTwoIcons.Builder builder = (BottomSheetItemWithDescriptionTwoIcons.Builder) new BottomSheetItemWithDescriptionTwoIcons.Builder()
					.setRightIcon(downloadIcon)
					.setIcon(osmandLiveIcon)
					.setTitle(getString(R.string.download_wikipedia_label))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_icons_left_right)
					.setOnClickListener(v -> {
						MapActivity mapActivity = getMapActivity();
						if (wikiLocationIndexItem != null && mapActivity != null) {
							if (downloadThread.isDownloading(wikiLocationIndexItem)) {
								downloadThread.cancelDownload(wikiLocationIndexItem);
								updateIndexFiles();
							} else {
								new DownloadValidationManager(app).startDownload(mapActivity, wikiLocationIndexItem);
								downloadWikiItem.setIsDownloading(true);
							}
						}
					});
			downloadWikiItem = builder.create();
			downloadWikiItem.setIsDownloading(showProgress);
			updateIndexFiles();
			items.add(downloadWikiItem);
		} else {
			Drawable osmandLiveIcon = getIcon(R.drawable.ic_action_wikipedia_download_colored_day, 0);
			BaseBottomSheetItem wikiArticleOfflineItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(getString(R.string.save_and_access_articles_offline))
					.setIcon(osmandLiveIcon)
					.setTitle(getString(R.string.read_wikipedia_offline))
					.setLayoutId(R.layout.bottom_sheet_item_in_frame_with_descr_and_icon)
					.setOnClickListener(v -> {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							ChoosePlanFragment.showInstance(activity, OsmAndFeature.WIKIVOYAGE);
						}
						dismiss();
					})
					.create();
			items.add(wikiArticleOfflineItem);
		}
		items.add(new DividerHalfItem(ctx));
		Drawable viewOnlineIcon = getContentIcon(R.drawable.ic_world_globe_dark);

		BaseBottomSheetItem wikiArticleOnlineItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.requires_internet_connection))
				.setIcon(viewOnlineIcon)
				.setTitle(getString(R.string.open_in_browser))
				.setLayoutId(R.layout.bottom_sheet_item_in_frame_with_descr_and_icon)
				.setOnClickListener(v -> {
					AndroidUtils.openUrl(ctx, articleUrl, nightMode);
					dismiss();
				})
				.create();
		items.add(wikiArticleOnlineItem);
	}

	@Override
	public void downloadingError(@NonNull String error) {
		DownloadIndexesThread.DownloadEvents.super.downloadingError(error);
		dismiss();
	}

	@Override
	public void downloadHasFinished() {
		DownloadIndexesThread.DownloadEvents.super.downloadHasFinished();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu contextMenu = mapActivity.getContextMenu();
			Object menuObject = mapActivity.getContextMenu().getObject();
			if (menuObject instanceof BaseDetailsObject baseDetailsObject) {
				List<NativeLibrary.RenderedObject> renderedObjects = baseDetailsObject.getRenderedObjects();
				if (!Algorithms.isEmpty(renderedObjects)) {
					NativeLibrary.RenderedObject renderedObject = renderedObjects.get(0);
					AmenitySearcher searcher = app.getResourceManager().getAmenitySearcher();
					AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();
					searcher.searchBaseDetailedObjectAsync(renderedObject, settings, detailsObject -> {
						app.runInUIThread(() -> {
							if (detailsObject != null) {
								LatLon latLon = baseDetailsObject.getLocation();
								MapLayers mapLayers = mapActivity.getMapLayers();
								PointDescription description = mapLayers.getPoiMapLayer().getObjectName(detailsObject);
								contextMenu.update(latLon, description, detailsObject);
								WikipediaDialogFragment.showInstance(mapActivity, detailsObject.getSyntheticAmenity(), null);
							}
						});
						dismiss();
						return true;
					});
				}
			}
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		DownloadIndexesThread.DownloadEvents.super.downloadHasFinished();
		updateIndexFiles();
	}

	private void updateIndexFiles() {
		if (downloadWikiItem != null) {
			MapActivity mapActivity = getMapActivity();
			if (!downloadThread.shouldDownloadIndexes() && mapActivity != null && !locations.isEmpty()) {
				downloadWikiItem.setIsDownloading(false);
				try {
					List<IndexItem> wikiIndexes = DownloadResources.findIndexItemsAt(
							app, locations.get(0), DownloadActivityType.WIKIPEDIA_FILE,
							false, -1, true);
					if (!Algorithms.isEmpty(wikiIndexes)) {
						wikiLocationIndexItem = wikiIndexes.get(0);
						if (!Algorithms.isEmpty(wikiIndexes)) {
							downloadWikiItem.setDescription(wikiLocationIndexItem.getVisibleName(app, app.getRegions(), false) + " • " + wikiLocationIndexItem.getSizeDescription(app));
						}
					}
				} catch (IOException ignored) {
				}
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.list_background_color_light;
	}

	public static boolean showInstance(@NonNull FragmentManager fm, @NonNull String articleUrl, @NonNull List<LatLon> locations) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(ARTICLE_URL_KEY, articleUrl);
			List<Double> latitudes = locations.stream().map(LatLon::getLatitude).collect(Collectors.toUnmodifiableList());
			List<Double> longitudes = locations.stream().map(LatLon::getLongitude).collect(Collectors.toUnmodifiableList());
			args.putDoubleArray(ARTICLE_LATITUDES_KEY, latitudes.stream().mapToDouble(Double::doubleValue).toArray());
			args.putDoubleArray(ARTICLE_LONGITUDES_KEY, longitudes.stream().mapToDouble(Double::doubleValue).toArray());

			WikipediaArticleWikiLinkFragment fragment = new WikipediaArticleWikiLinkFragment();
			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		}
		return false;
	}
}