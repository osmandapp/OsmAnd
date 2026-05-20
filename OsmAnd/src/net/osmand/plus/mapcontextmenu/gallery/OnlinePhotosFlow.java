package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.gallery.online.OnlinePhotosGroup.WIKIMEDIA;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.gallery.controller.GalleryController;
import net.osmand.plus.gallery.model.GalleryItem;
import net.osmand.plus.gallery.online.OnlinePhotosHolder;
import net.osmand.plus.gallery.online.cache.PhotoCacheManager;
import net.osmand.plus.gallery.online.tasks.CacheReadTask;
import net.osmand.plus.gallery.online.tasks.CacheWriteTask;
import net.osmand.plus.gallery.online.tasks.GetOnlineImagesTask;
import net.osmand.plus.gallery.online.tasks.GetOnlineImagesTask.GetImageCardsListener;
import net.osmand.shared.media.RemoteMediaFactory;
import net.osmand.shared.wiki.WikiCoreHelper;
import net.osmand.shared.wiki.WikiHelper;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnlinePhotosFlow {

	private final OsmandApplication app;
	private final OnlinePhotosFlowListener listener;

	private GalleryController galleryController;
	private GetOnlineImagesTask getOnlineImagesTask;
	private List<GalleryItem> currentGalleryItems;
	private boolean loading;

	public OnlinePhotosFlow(@NonNull OsmandApplication app,
	                        @NonNull OnlinePhotosFlowListener listener) {
		this.app = app;
		this.listener = listener;
		this.galleryController = findController(app);
	}

	private final GetImageCardsListener imageCardListener = new GetImageCardsListener() {
		@Override
		public void onTaskStarted() {
			loading = true;
			listener.onPhotosLoadStarted();
		}

		@Override
		public void onFinish(OnlinePhotosHolder mediaHolder) {
			loading = false;
			setCurrentGalleryItems(mediaHolder.getOrderedGalleryItems());
			if (galleryController != null) {
				galleryController.setItemsHolder(mediaHolder);
			}
			listener.onPhotosLoadFinished(mediaHolder);
		}
	};

	private void setCurrentGalleryItems(@NonNull List<GalleryItem> onlinePhotoItems) {
		List<GalleryItem> items = new ArrayList<>(onlinePhotoItems);
		if (onlinePhotoItems.isEmpty()) {
			items.add(new GalleryItem.NoMedia());
		}
		currentGalleryItems = items;
	}

	public void startLoadingImages() {
		startLoadingImagesTask();
	}

	private void startLoadingImagesTask() {
		if (galleryController == null) {
			return;
		}

		LatLon latLon = listener.getLatLon();
		Map<String, String> params = listener.getAdditionalImageParams();

		PhotoCacheManager cacheManager = new PhotoCacheManager(app);
		WikiHelper.WikiTagData wikiTagData = WikiHelper.INSTANCE.extractWikiTagData(params);
		String wikidataId = wikiTagData.getWikidataId();
		String wikiCategory = wikiTagData.getWikiCategory();
		String wikiTitle = wikiTagData.getWikiTitle();
		String rawKey = PhotoCacheManager.buildRawKey(wikidataId, wikiCategory, wikiTitle);

		OnlinePhotosHolder holder = galleryController.getItemsHolder();
		if (holder != null && galleryController.isCurrentHolderEquals(latLon, params)) {
			imageCardListener.onFinish(holder);
		} else if (!app.getSettings().isInternetConnectionAvailable()){
			loadFromCache(cacheManager, rawKey, params, wikiTagData, latLon);
		} else {
			stopLoadingImagesTask();
			galleryController.clearHolder();
			getOnlineImagesTask = new GetOnlineImagesTask(app, listener.getLatLon(),
					listener.getAdditionalImageParams(), imageCardListener,
					response -> savePhotoListToCache(cacheManager, rawKey, response));
			OsmAndTaskManager.executeTask(getOnlineImagesTask);
		}
	}

	private void savePhotoListToCache(@NonNull PhotoCacheManager cacheManager, @NonNull String rawKey, @NonNull String response){
		if (!Algorithms.isEmpty(response)) {
			CacheWriteTask cacheWriteTask = new CacheWriteTask(cacheManager, rawKey, response);
			OsmAndTaskManager.executeTask(cacheWriteTask);
		}
	}

	private void loadFromCache(@NonNull PhotoCacheManager cacheManager,
	                           @NonNull String rawKey,
	                           @NonNull Map<String, String> params,
	                           @NonNull WikiHelper.WikiTagData wikiTagData,
	                           @NonNull LatLon latLon) {
		OnlinePhotosHolder holder = new OnlinePhotosHolder(latLon, params);

		if (!cacheManager.exists(rawKey)) {
			imageCardListener.onFinish(holder);
			return;
		}

		imageCardListener.onTaskStarted();

		CacheReadTask cacheReadTask = new CacheReadTask(cacheManager, rawKey, json -> {
			if (!Algorithms.isEmpty(json)) {
				List<WikiImage> wikimediaImageList = WikiCoreHelper.INSTANCE.getImagesFromJson(
						json,
						wikiTagData.getWikiImages()
				);
				for (WikiImage wikiImage : wikimediaImageList) {
					holder.addMediaItem(WIKIMEDIA, RemoteMediaFactory.fromWikiImage(wikiImage));
				}
			}
			imageCardListener.onFinish(holder);
			return true;
		});
		OsmAndTaskManager.executeTask(cacheReadTask);
	}

	public void stopLoadingImagesTask() {
		if (getOnlineImagesTask != null && getOnlineImagesTask.getStatus() == AsyncTask.Status.RUNNING) {
			getOnlineImagesTask.cancel(false);
		}
		getOnlineImagesTask = null;
	}

	public void clear() {
		stopLoadingImagesTask();
		currentGalleryItems = null;
		loading = false;
		if (galleryController != null) {
			galleryController.clearHolder();
		}
	}

	@Nullable
	public GalleryController getGalleryController() {
		if (galleryController == null) {
			galleryController = findController(app);
		}
		return galleryController;
	}

	@Nullable
	public List<GalleryItem> getCurrentGalleryItems() {
		return currentGalleryItems;
	}

	public boolean hasCurrentGalleryItems() {
		return currentGalleryItems != null;
	}

	public boolean isLoading() {
		return loading;
	}

	@Nullable
	public static GalleryController findController(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (GalleryController) dialogManager.findController(GalleryController.PROCESS_ID);
	}
}
