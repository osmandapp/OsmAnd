package net.osmand.router;

import java.nio.file.FileSystems;

public interface NativeLibraryTest {
    String nativeLibPath = FileSystems.getDefault().getPath("../../core-legacy/binaries/darwin/intel/Release").normalize().toAbsolutePath().toString();
}
