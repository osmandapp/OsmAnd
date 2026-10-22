package net.osmand.binary;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.NameIndexReader.PrefixNameValue;

public class NameIndexReaderTest {

	private static final File MAP = new File("src/test/resources/search/kiev.obf.gz");

	/**
	 * The blocks of a query come in the order of the file, read or cached: the atoms of a word of the spatial search
	 * follow them, and the order of the hashes of their offsets changed its results.
	 */
	@Test
	public void blocksComeInTheOrderOfTheFile() throws IOException {
		File obf = File.createTempFile("name-index", ".obf");
		try {
			try (InputStream in = new GZIPInputStream(new FileInputStream(MAP)); OutputStream out = new FileOutputStream(obf)) {
				in.transferTo(out);
			}
			BinaryMapIndexReader r = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
			try {
				NameIndexReader index = new NameIndexReader(r.getAddressIndexes().get(0)).setCacheRawBlocks(true);
				NameIndexReaderMatcher all = new NameIndexReaderMatcher("all") {
					@Override
					public boolean matchKey(String key) {
						return true;
					}
				};
				List<PrefixNameValue> read = r.readFullNameIndex(index.setQuery("all", all));
				assertTrue("blocks: " + read.size(), read.size() > 10);
				assertInFileOrder("read", read);
				assertInFileOrder("read again", r.readFullNameIndex(index.setQuery("all", all)));
				assertInFileOrder("cached", index.getMatchedPrefixes("all"));
			} finally {
				r.close();
			}
		} finally {
			obf.delete();
		}
	}

	private static void assertInFileOrder(String what, List<PrefixNameValue> blocks) {
		for (int i = 1; i < blocks.size(); i++) {
			assertTrue(what + ": block " + i + " at " + blocks.get(i).shift + " after " + blocks.get(i - 1).shift,
					blocks.get(i - 1).shift < blocks.get(i).shift);
		}
	}
}
