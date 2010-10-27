package crosby.binary.file;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import crosby.binary.Fileformat;

/**
 * Stores the position in the stream of a fileblock so that it can be easily
 * read in a random-access fashion.
 * 
 * We can turn this into a 'real' block by appropriately seeking into the file
 * and doing a 'read'.
 * 
 * */
public class FileBlockPosition extends FileBlockBase {
    protected FileBlockPosition(String type, ByteString indexdata) {
        super(type, indexdata);
    }

    /** Parse out and decompress the data part of a fileblock helper function. */
    FileBlock parseData(byte buf[]) throws InvalidProtocolBufferException {
        FileBlock out = FileBlock.newInstance(type, null, indexdata);
        Fileformat.Blob blob = Fileformat.Blob.parseFrom(buf);
        if (blob.hasRaw()) {
            out.data = blob.getRaw();
        } else if (blob.hasZlibData()) {
            byte buf2[] = new byte[blob.getRawSize()];
            Inflater decompresser = new Inflater();
            decompresser.setInput(blob.getZlibData().toByteArray());
            // decompresser.getRemaining();
            try {
                decompresser.inflate(buf2);
            } catch (DataFormatException e) {
                e.printStackTrace();
                throw new Error(e);
            }
            assert (decompresser.finished());
            decompresser.end();
            out.data = ByteString.copyFrom(buf2);
        }
        return out;
    }

    public int getDatasize() {
        return datasize;
    }

    /*
     * Given any form of fileblock and an offset/length value, return a
     * reference that can be used to dereference and read the contents.
     */
    static FileBlockPosition newInstance(FileBlockBase base, long offset,
            int length) {
        FileBlockPosition out = new FileBlockPosition(base.type, base.indexdata);
        out.datasize = length;
        out.data_offset = offset;
        return out;
    }

    public FileBlock read(InputStream input) throws IOException {
        if (input instanceof FileInputStream) {
            ((FileInputStream) input).getChannel().position(data_offset);
            byte buf[] = new byte[getDatasize()];
            (new DataInputStream(input)).readFully(buf);
            return parseData(buf);
        } else {
            throw new Error("Random access binary reads require seekability");
        }
    }

    /**
     * TODO: Convert this reference into a serialized representation that can be
     * stored.
     */
    public ByteString serialize() {
        throw new Error("TODO");
    }

    /** TODO: Parse a serialized representation of this block reference */
    static FileBlockPosition parseFrom(ByteString b) {
      throw new Error("TODO");
    }

    protected int datasize;
    /** Offset into the file of the data part of the block */
    long data_offset;
}
