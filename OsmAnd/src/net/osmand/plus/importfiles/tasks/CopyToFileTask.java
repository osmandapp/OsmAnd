package net.osmand.plus.importfiles.tasks;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyToFileTask extends BaseImportAsyncTask<Void, Void, String> {
	private static final Log LOG = PlatformUtil.getLog(CopyToFileTask.class.getName());

	private final File originalFile;
	private final Uri destinationUri;

	public CopyToFileTask(@NonNull FragmentActivity activity, @NonNull File originalFile,
	                      @NonNull Uri destinationUri) {
		super(activity);
		this.originalFile = originalFile;
		this.destinationUri = destinationUri;
	}

	@Override
	protected String doInBackground(Void... voids) {
		String error = null;
		OutputStream out = null;
		InputStream in = null;
		try {
			out = app.getContentResolver().openOutputStream(destinationUri);
			in = new FileInputStream(originalFile);
			if (out != null) {
				Algorithms.streamCopy(in, out);
			}
		} catch (IOException e) {
			LOG.error(e);
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(out);
			Algorithms.closeStream(in);
		}

		return error;
	}

	@Override
	protected void onPostExecute(String error) {
		hideProgress();
		notifyImportFinished();
		if (error == null) {
			app.showShortToastMessage(R.string.saved_to_device);
		} else {
			app.showShortToastMessage(app.getString(R.string.save_to_device_error) + ": " + error);
		}
	}
}