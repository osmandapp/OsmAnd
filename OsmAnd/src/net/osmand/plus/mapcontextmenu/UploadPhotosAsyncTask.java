package net.osmand.plus.mapcontextmenu;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.UploadPhotoProgressBottomSheet;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.openplacereviews.OPRConstants;
import net.osmand.plus.openplacereviews.OprStartFragment;
import net.osmand.plus.osmedit.opr.OpenDBAPI;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.openplacereviews.opendb.util.exception.FailedVerificationException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UploadPhotosAsyncTask extends AsyncTask<Void, Integer, Void> {

	private static final Log LOG = PlatformUtil.getLog(UploadPhotosAsyncTask.class);

	private static final int MAX_IMAGE_LENGTH = 2048;

	private final OsmandApplication app;
	private final WeakReference<MapActivity> activityRef;
	private final OpenDBAPI openDBAPI = new OpenDBAPI();
	private final LatLon latLon;
	private final List<Uri> data;
	private final String[] placeId;
	private final Map<String, String> params;
	private final GetImageCardsListener imageCardListener;
	private UploadPhotosListener listener;

	public UploadPhotosAsyncTask(MapActivity activity, List<Uri> data, LatLon latLon, String[] placeId,
								 Map<String, String> params, GetImageCardsListener imageCardListener) {
		app = (OsmandApplication) activity.getApplicationContext();
		activityRef = new WeakReference<>(activity);
		this.data = data;
		this.latLon = latLon;
		this.params = params;
		this.placeId = placeId;
		this.imageCardListener = imageCardListener;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			FragmentManager manager = activity.getSupportFragmentManager();
			listener = UploadPhotoProgressBottomSheet.showInstance(manager, data.size(), new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					cancel(false);
				}
			});
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (listener != null) {
			listener.uploadPhotosProgressUpdate(values[0]);
		}
	}

	protected Void doInBackground(Void... uris) {
		List<Uri> uploadedPhotoUris = new ArrayList<>();
		for (int i = 0; i < data.size(); i++) {
			if (isCancelled()) {
				break;
			}
			Uri uri = data.get(i);
			if (handleSelectedImage(uri)) {
				uploadedPhotoUris.add(uri);
				publishProgress(uploadedPhotoUris.size());
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (listener != null) {
			listener.uploadPhotosFinished();
		}
	}

	private boolean handleSelectedImage(final Uri uri) {
		boolean success = false;
		InputStream inputStream = null;
		try {
			inputStream = app.getContentResolver().openInputStream(uri);
			if (inputStream != null) {
				success = uploadImageToPlace(inputStream);
			}
		} catch (Exception e) {
			LOG.error(e);
			app.showToastMessage(R.string.cannot_upload_image);
		} finally {
			Algorithms.closeStream(inputStream);
		}
		return success;
	}

	private boolean uploadImageToPlace(InputStream image) {
		boolean success = false;
		InputStream serverData = new ByteArrayInputStream(compressImageToJpeg(image));
		String baseUrl = OPRConstants.getBaseUrl(app);
		// all these should be constant
		String url = baseUrl + "api/ipfs/image";
		String response = NetworkUtils.sendPostDataRequest(url, "file", "compressed.jpeg", serverData);
		if (response != null) {
			int res = 0;
			try {
				StringBuilder error = new StringBuilder();
				String privateKey = app.getSettings().OPR_ACCESS_TOKEN.get();
				String name = app.getSettings().OPR_BLOCKCHAIN_NAME.get();
				res = openDBAPI.uploadImage(
						placeId,
						baseUrl,
						privateKey,
						name,
						response, error);
				if (res != 200) {
					app.showToastMessage(error.toString());
				}
			} catch (FailedVerificationException e) {
				LOG.error(e);
				checkTokenAndShowScreen();
			}
			if (res != 200) {
				//image was uploaded but not added to blockchain
				checkTokenAndShowScreen();
			} else {
				success = true;
				String str = app.getString(R.string.successfully_uploaded_pattern, 1, 1);
				app.showToastMessage(str);
				//refresh the image

				MapActivity activity = activityRef.get();
				if (activity != null) {
					MenuBuilder.execute(new GetImageCardsTask(activity, latLon, params, imageCardListener));
				}
			}
		} else {
			checkTokenAndShowScreen();
		}
		return success;
	}

	//This method runs on non main thread
	private void checkTokenAndShowScreen() {
		String baseUrl = OPRConstants.getBaseUrl(app);
		String name = app.getSettings().OPR_USERNAME.get();
		String privateKey = app.getSettings().OPR_ACCESS_TOKEN.get();
		if (openDBAPI.checkPrivateKeyValid(app, baseUrl, name, privateKey)) {
			app.showToastMessage(R.string.cannot_upload_image);
		} else {
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					MapActivity activity = activityRef.get();
					if (activity != null) {
						OprStartFragment.showInstance(activity.getSupportFragmentManager());
					}
				}
			});
		}
	}

	private byte[] compressImageToJpeg(InputStream image) {
		BufferedInputStream bufferedInputStream = new BufferedInputStream(image);
		Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		int h = bmp.getHeight();
		int w = bmp.getWidth();
		boolean scale = false;
		while (w > MAX_IMAGE_LENGTH || h > MAX_IMAGE_LENGTH) {
			w = w / 2;
			h = h / 2;
			scale = true;
		}
		if (scale) {
			Matrix matrix = new Matrix();
			matrix.postScale(w, h);
			Bitmap resizedBitmap = Bitmap.createBitmap(
					bmp, 0, 0, w, h, matrix, false);
			bmp.recycle();
			bmp = resizedBitmap;
		}
		bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
		return os.toByteArray();
	}


	public interface UploadPhotosListener {

		void uploadPhotosProgressUpdate(int progress);

		void uploadPhotosFinished();

	}
}