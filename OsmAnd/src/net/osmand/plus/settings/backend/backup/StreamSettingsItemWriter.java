package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.backup.items.StreamSettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StreamSettingsItemWriter extends SettingsItemWriter<StreamSettingsItem> {

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

	private void streamCopy(@NonNull InputStream in, @NonNull OutputStream out, @Nullable IProgress pg, int bytesDivisor) throws IOException {
		MessageDigest digest = getMessageDigest();
		Algorithms.streamCopy(in, out, pg, bytesDivisor, digest);

		if (digest != null) {
			byte[] md5sum = digest.digest();
			getItem().setMd5Digest(new String(Hex.encodeHex(md5sum)));
		}
	}

	@Nullable
	private MessageDigest getMessageDigest() {
		if (getItem().needMd5Digest()) {
			try {
				return MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// ignore
			}
		}
		return null;
	}
}
