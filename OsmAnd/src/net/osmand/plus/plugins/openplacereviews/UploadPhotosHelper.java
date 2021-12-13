package net.osmand.plus.plugins.openplacereviews;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.openplacereviews.OprAuthHelper.CheckOprAuthTask;
import net.osmand.plus.plugins.openplacereviews.UploadPhotosAsyncTask.UploadPhotosListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UploadPhotosHelper {
	private static final Log LOG = PlatformUtil.getLog(UploadPhotosHelper.class);

	private static final int PICK_IMAGE = 1231;

	private final WeakReference<MapActivity> mapActivityRef;
	private final OsmandApplication app;
	private final UploadPhotosHelperListener listener;

	public interface UploadPhotosHelperListener {
		void imageCardCreated(@NonNull ImageCard imageCard);
	}

	public UploadPhotosHelper(@NonNull MapActivity activity, @Nullable UploadPhotosHelperListener listener) {
		app = (OsmandApplication) activity.getApplicationContext();
		mapActivityRef = new WeakReference<>(activity);
		this.listener = listener;
	}

	@Nullable
	private MapActivity getMapActivity() {
		MapActivity mapActivity = mapActivityRef.get();
		return AndroidUtils.isActivityNotDestroyed(mapActivity) ? mapActivity : null;
	}

	public void chooseAndUploadPhoto(@NonNull String[] placeId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		registerResultListener(placeId);

		String userName = app.getSettings().OPR_USERNAME.get();
		String token = app.getSettings().OPR_ACCESS_TOKEN.get();
		if (Algorithms.isBlank(token) || Algorithms.isBlank(userName)) {
			OprStartFragment.showInstance(mapActivity.getSupportFragmentManager());
			return;
		}
		CheckOprAuthTask checkOprAuthTask = new CheckOprAuthTask(app, token, userName, authorized -> {
			MapActivity activity = getMapActivity();
			if (activity != null) {
				if (authorized) {
					Intent intent = new Intent().setAction(Intent.ACTION_GET_CONTENT).setType("image/*");
					intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					Intent chooserIntent = Intent.createChooser(intent, app.getString(R.string.select_picture));
					AndroidUtils.startActivityForResultIfSafe(activity, chooserIntent, PICK_IMAGE);
				} else {
					OprStartFragment.showInstance(activity.getSupportFragmentManager());
				}
			}
		});
		checkOprAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private void registerResultListener(@NonNull String[] placeId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		mapActivity.registerActivityResultListener(new ActivityResultListener(PICK_IMAGE, (resultCode, resultData) -> {
			MapActivity activity = getMapActivity();
			if (activity != null && resultData != null) {
				List<Uri> imagesUri = new ArrayList<>();
				Uri data = resultData.getData();
				if (data != null) {
					imagesUri.add(data);
				}
				ClipData clipData = resultData.getClipData();
				if (clipData != null) {
					for (int i = 0; i < clipData.getItemCount(); i++) {
						Uri uri = resultData.getClipData().getItemAt(i).getUri();
						if (uri != null) {
							imagesUri.add(uri);
						}
					}
				}
				UploadPhotosListener listener = response -> app.runInUIThread(() -> {
					if (getMapActivity() != null) {
						try {
							ImageCardsHolder holder = new ImageCardsHolder();
							if (OsmandPlugin.createImageCardForJson(holder, new JSONObject(response))) {
								ImageCard imageCard = holder.getFirstItem();
								if (imageCard != null && UploadPhotosHelper.this.listener != null) {
									UploadPhotosHelper.this.listener.imageCardCreated(imageCard);
								}
							}
						} catch (JSONException e) {
							LOG.error(e);
						}
					}
				});
				UploadPhotosAsyncTask uploadTask = new UploadPhotosAsyncTask(activity, imagesUri, placeId, listener);
				uploadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
			}
		}));
	}
}
