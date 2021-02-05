package net.osmand.plus.mapcontextmenu;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.UploadPhotoProgressBottomSheet;
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

public class UploadPhotosAsyncTask extends AsyncTask<Void, Integer, Void> {

	private static final Log LOG = PlatformUtil.getLog(UploadPhotosAsyncTask.class);

	private static final int IMAGE_MAX_SIZE = 4096;

	private final OsmandApplication app;
	private final WeakReference<MapActivity> activityRef;
	private final OpenDBAPI openDBAPI = new OpenDBAPI();
	private final List<Uri> data;
	private final String[] placeId;
	private final UploadPhotosListener listener;
	private UploadPhotosProgressListener progressListener;

	public UploadPhotosAsyncTask(MapActivity activity, List<Uri> data, String[] placeId, UploadPhotosListener listener) {
		app = (OsmandApplication) activity.getApplicationContext();
		activityRef = new WeakReference<>(activity);
		this.data = data;
		this.placeId = placeId;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			FragmentManager manager = activity.getSupportFragmentManager();
			progressListener = UploadPhotoProgressBottomSheet.showInstance(manager, data.size(), new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					cancel(false);
				}
			});
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (progressListener != null) {
			progressListener.uploadPhotosProgressUpdate(values[0]);
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
		if (progressListener != null) {
			progressListener.uploadPhotosFinished();
		}
	}

	private boolean handleSelectedImage(final Uri uri) {
		boolean success = false;
		InputStream inputStream = null;
		int[] imageDimensions = null;
		try {
			inputStream = app.getContentResolver().openInputStream(uri);
			if (inputStream != null) {
				imageDimensions = calcImageDimensions(inputStream);
			}
		} catch (Exception e) {
			LOG.error(e);
			app.showToastMessage(R.string.cannot_upload_image);
		} finally {
			Algorithms.closeStream(inputStream);
		}
		if (imageDimensions != null && imageDimensions.length == 2) {
			try {
				inputStream = app.getContentResolver().openInputStream(uri);
				if (inputStream != null) {
					int width = imageDimensions[0];
					int height = imageDimensions[1];
					success = uploadImageToPlace(inputStream, width, height);
				}
			} catch (Exception e) {
				LOG.error(e);
				app.showToastMessage(R.string.cannot_upload_image);
			} finally {
				Algorithms.closeStream(inputStream);
			}
		}
		return success;
	}

	private boolean uploadImageToPlace(InputStream image, int width, int height) {
		boolean success = false;
		byte[] jpegImageBytes = compressImageToJpeg(image, width, height);
		if (jpegImageBytes == null || jpegImageBytes.length == 0) {
			app.showToastMessage(R.string.cannot_upload_image);
			return false;
		}
		InputStream serverData = new ByteArrayInputStream(jpegImageBytes);
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
				//refresh the image
				if (listener != null) {
					listener.uploadPhotosSuccess(response);
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

	private int[] calcImageDimensions(InputStream image) {
		BufferedInputStream bufferedInputStream = new BufferedInputStream(image);
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(bufferedInputStream, null, opts);
		return new int[] { opts.outWidth, opts.outHeight };
	}

	@Nullable
	private byte[] compressImageToJpeg(InputStream image, int width, int height) {
		BufferedInputStream bufferedInputStream = new BufferedInputStream(image);
		int w = width;
		int h = height;
		boolean scale = false;
		int divider = 1;
		while (w > IMAGE_MAX_SIZE || h > IMAGE_MAX_SIZE) {
			w /= 2;
			h /= 2;
			divider *= 2;
			scale = true;
		}
		Bitmap bmp;
		if (scale) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inSampleSize = divider;
			bmp = BitmapFactory.decodeStream(bufferedInputStream, null, opts);
		} else {
			bmp = BitmapFactory.decodeStream(bufferedInputStream);
		}
		if (bmp != null) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.JPEG, 90, os);
			return os.toByteArray();
		} else {
			return null;
		}
	}

	public interface UploadPhotosProgressListener {

		void uploadPhotosProgressUpdate(int progress);

		void uploadPhotosFinished();

	}

	public interface UploadPhotosListener {

		void uploadPhotosSuccess(String response);

	}
}