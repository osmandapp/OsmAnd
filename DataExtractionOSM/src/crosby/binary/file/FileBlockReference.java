package crosby.binary.file;

import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.ByteString;

/**
 * A FileBlockPosition that remembers what file this is so that it can simply be
 * dereferenced
 */
public class FileBlockReference extends FileBlockPosition {

    /**
     * Convenience cache for storing the input this reference is contained
     * within so that it can be cached
     */
    protected InputStream input;

    protected FileBlockReference(String type, ByteString indexdata) {
        super(type, indexdata);
    }

    public FileBlock read() throws IOException {
        return read(input);
    }

    static FileBlockPosition newInstance(FileBlockBase base, InputStream input,
            long offset, int length) {
        FileBlockReference out = new FileBlockReference(base.type,
                base.indexdata);
        out.datasize = length;
        out.data_offset = offset;
        out.input = input;
        return out;
    }
}
