package crosby.binary.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

enum CompressFlags {
    NONE, DEFLATE
}

public class BlockOutputStream {

    public BlockOutputStream(OutputStream output) {
        this.outwrite = new DataOutputStream(output);
        this.compression = CompressFlags.DEFLATE;
    }

    public void setCompress(CompressFlags flag) {
        compression = flag;
    }

    public void setCompress(String s) {
        if (s.equals("none"))
            compression = CompressFlags.NONE;
        else if (s.equals("deflate"))
            compression = CompressFlags.DEFLATE;
        else
            throw new Error("Unknown compression type: " + s);
    }

    /** Write a block with the stream's default compression flag */
    public void write(FileBlock block) throws IOException {
        this.write(block, compression);
    }

    /** Write a specific block with a specific compression flags */
    public void write(FileBlock block, CompressFlags compression)
            throws IOException {
        FileBlockPosition ref = block.writeTo(outwrite, compression);
        writtenblocks.add(ref);
    }

    public void flush() throws IOException {
        outwrite.flush();
    }

    public void close() throws IOException {
        outwrite.flush();
        outwrite.close();
    }

    OutputStream outwrite;
    List<FileBlockPosition> writtenblocks = new ArrayList<FileBlockPosition>();
    CompressFlags compression;
}
