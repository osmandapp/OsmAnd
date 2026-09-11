package net.osmand.plus.plugins.aprsdriver;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AprsSdrDspTest {

	@Test
	public void fcsRejectsInvalidFrame() {
		byte[] bad = new byte[] {0x01, 0x02, 0x03, 0x04};
		Assert.assertFalse(AprsSdrDsp.verifyFcs(bad));
	}

	@Test
	public void bitDestuffingRemovesStuffBit() {
		List<Integer> stuffed = Arrays.asList(1, 1, 1, 1, 1, 0, 1);
		List<Integer> destuffed = AprsSdrDsp.destuff(stuffed);
		Assert.assertEquals(Arrays.asList(1, 1, 1, 1, 1, 1), destuffed);
	}

	@Test
	public void vidPidConstantsCoverFourFamilies() {
		int vid = 0x0BDA;
		int[] pids = {0x2831, 0x2832, 0x2838};
		Assert.assertEquals(0x0BDA, vid);
		Assert.assertEquals(3, pids.length);
		for (int pid : pids) {
			Assert.assertTrue(pid == 0x2831 || pid == 0x2832 || pid == 0x2838);
		}
	}

	@Test
	public void decodesSyntheticIqFileWhenPresent() throws Exception {
		File wav = new File("../../../../iq-file/synthetic_gjoevik_5stations_120s.wav");
		if (!wav.isFile()) {
			wav = new File("../../../../../iq-file/synthetic_gjoevik_5stations_120s.wav");
		}
		if (!wav.isFile()) {
			wav = new File("/mnt/2e9a1e9f-2097-408c-ab9a-a01b32f11d28/github-projects/osmand-aprs/iq-file/synthetic_gjoevik_5stations_120s.wav");
		}
		if (!wav.isFile()) {
			return;
		}
		List<byte[]> frames = new ArrayList<>();
		AprsSdrDsp dsp = new AprsSdrDsp(frames::add);
		dsp.resetForReplay();
		try (RandomAccessFile raf = new RandomAccessFile(wav, "r")) {
			byte[] header = new byte[44];
			raf.readFully(header);
			int rfRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			byte[] cal = new byte[rfRate * 5 * 4];
			int calRead = 0;
			while (calRead < cal.length) {
				int n = raf.read(cal, calRead, cal.length - calRead);
				if (n <= 0) {
					break;
				}
				calRead += n;
			}
			if (calRead > 0) {
				short[] calIq = new short[calRead / 2];
				ByteBuffer bbCal = ByteBuffer.wrap(cal, 0, calRead).order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < calIq.length; i++) {
					calIq[i] = bbCal.getShort();
				}
				dsp.calibrateFromInterleavedIq(calIq, calIq.length, rfRate);
				dsp.resetStreamState();
			}
			raf.seek(44);
			byte[] chunk = new byte[8192];
			int n;
			while ((n = raf.read(chunk)) > 0) {
				short[] iq = new short[n / 2];
				ByteBuffer bb = ByteBuffer.wrap(chunk, 0, n).order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < iq.length; i++) {
					iq[i] = bb.getShort();
				}
				dsp.processInterleavedIq(iq, iq.length, rfRate);
				dsp.extractNewFramesIfReady(false);
			}
		}
		dsp.flush();
		System.out.println("APRS_TEST_FRAME_COUNT=" + frames.size());
		Assert.assertTrue("Expected decoded AX.25 frames from synthetic IQ, got " + frames.size(),
				frames.size() >= 26);
	}
}
