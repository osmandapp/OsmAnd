package crosby.binary.file;

import java.io.IOException;
import java.io.InputStream;

public class BlockInputStream {
    // TODO: Should be seekable input stream!
    public BlockInputStream(InputStream input, BlockReaderAdapter adaptor) {
        this.input = input;
        this.adaptor = adaptor;
    }

    public void process() throws IOException {
        while (input.available() > 0) {
            FileBlock.process(input, adaptor);
        }
        adaptor.complete();
    }

    public void close() throws IOException {
        input.close();
    }

    InputStream input;
    BlockReaderAdapter adaptor;
}
