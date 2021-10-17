package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.backup.items.StreamSettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StreamSettingsItemWriter extends SettingsItemWriter<StreamSettingsItem> {

	private static final int BUFFER_SIZE = 1024;

	public StreamSettingsItemWriter(StreamSettingsItem item) {
		super(item);
	}

	@Override
	public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
		int bytesDivisor = 1024;
		StreamSettingsItem item = getItem();
		if (progress != null) {
			progress.startWork((int) (item.getSize() / bytesDivisor));
		}
		InputStream inputStream = item.getInputStream();
		if (inputStream != null) {
			try {
				streamCopy(inputStream, outputStream, progress, bytesDivisor);
			} finally {
				Algorithms.closeStream(inputStream);
			}
		}
		if (progress != null) {
			progress.finishTask();
		}
	}

	private void streamCopy(InputStream in, OutputStream out, IProgress pg, int bytesDivisor) throws IOException {
		byte[] b = new byte[BUFFER_SIZE];
		int read;
		int cp = 0;
		StreamSettingsItem item = getItem();
		MessageDigest digest = null;
		if (item.needMd5Digest()) {
			try {
				digest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// ignore
			}
		}
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
			if (digest != null) {
				digest.update(b, 0, read);
			}
			cp += read;
			if (pg != null && cp > bytesDivisor) {
				pg.progress(cp / bytesDivisor);
				cp = cp % bytesDivisor;
				if (pg.isInterrupted()) {
					throw new InterruptedIOException();
				}
			}
		}
		if (digest != null) {
			byte[] md5sum = digest.digest();
			item.setMd5Digest(new String(Hex.encodeHex(md5sum)));
		}
	}
}
