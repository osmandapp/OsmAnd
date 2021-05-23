package net.osmand;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamWriter {
	void write(OutputStream outputStream, IProgress progress) throws IOException;
}
