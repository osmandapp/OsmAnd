package net.osmand;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {
	private final OutputStream out;
	private final IProgress progress;
	private int bytesDivisor = 1024;

	private int counter = 0;

	public ProgressOutputStream(OutputStream out, IProgress progress) {
		this.out = out;
		this.progress = progress;
	}

	public ProgressOutputStream(OutputStream out, IProgress progress, int bytesDivisor) {
		this.out = out;
		this.progress = progress;
		this.bytesDivisor = bytesDivisor;
	}

	private void submitProgress(int length) {
		counter += length;
		if (progress != null) {
			if (counter > bytesDivisor) {
				progress.progress(counter / bytesDivisor);
				counter = counter % bytesDivisor;
			}
		}
	}

	public void write(byte[] b, int offset, int length) throws IOException {
		this.out.write(b, offset, length);
		submitProgress(length);
	}

	public void write(byte[] b) throws IOException {
		this.out.write(b);
		submitProgress(b.length);
	}

	public void write(int b) throws IOException {
		this.out.write(b);
		submitProgress(1);
	}
}
