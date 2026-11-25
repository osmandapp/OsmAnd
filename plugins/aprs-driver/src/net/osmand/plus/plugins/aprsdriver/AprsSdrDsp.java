package net.osmand.plus.plugins.aprsdriver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Streaming FM + Bell 202 + AX.25 frame extractor for APRS IQ samples.
 * All demodulator state persists between {@link #processInterleavedIq} calls.
 */
public class AprsSdrDsp {

	private static final Log LOG = PlatformUtil.getLog(AprsSdrDsp.class);

	public static final int DEFAULT_RF_RATE = 192_000;
	public static final int AUDIO_RATE = 48_000;
	public static final int IF_OFFSET_HZ = 100_000;
	private static final int BAUD = 1200;
	private static final int SAMPLES_PER_BIT = AUDIO_RATE / BAUD;
	private static final int RAW_BITS_KEEP = 512;

	public interface FrameListener {
		void onAx25Frame(@NonNull byte[] frame);
	}

	private final FrameListener listener;
	private final int ifOffsetHz;
	private final int audioRate;

	private double dcI;
	private double dcQ;
	private double mixPhase;
	private double decimAcc;
	private int audioSampleIndex;
	private double prevMi;
	private double prevMq;
	private boolean havePrevMix;
	private double discDcIn;
	private double discDc;

	private int bitSampleCount;
	private final double[] bitBuffer = new double[SAMPLES_PER_BIT];
	private int lastToneLevel = 1;
	private boolean toneInvert;
	private boolean demodCalibrated;
	private int lockedBitPhase;
	private int phaseSamplesToSkip;
	private final List<Integer> rawBits = new ArrayList<>();
	private final Set<Integer> emittedFrameHashes = new HashSet<>();
	private int emittedFrameCount;

	public AprsSdrDsp(@NonNull FrameListener listener) {
		this(listener, IF_OFFSET_HZ);
	}

	public AprsSdrDsp(@NonNull FrameListener listener, int ifOffsetHz) {
		this.listener = listener;
		this.ifOffsetHz = ifOffsetHz;
		this.audioRate = AUDIO_RATE;
	}

	public void resetForReplay() {
		resetStreamState();
		emittedFrameHashes.clear();
		emittedFrameCount = 0;
		demodCalibrated = false;
		toneInvert = false;
		lockedBitPhase = 0;
	}

	public void resetStreamState() {
		dcI = 0;
		dcQ = 0;
		mixPhase = 0;
		decimAcc = 0;
		audioSampleIndex = 0;
		prevMi = 0;
		prevMq = 0;
		havePrevMix = false;
		discDcIn = 0;
		discDc = 0;
		bitSampleCount = 0;
		lastToneLevel = 1;
		rawBits.clear();
		phaseSamplesToSkip = lockedBitPhase;
	}

	public int getEmittedFrameCount() {
		return emittedFrameCount;
	}

	public static int aliasIf(int ifHz, int rfSampleRate) {
		int f = ifHz % rfSampleRate;
		if (f > rfSampleRate / 2) {
			f -= rfSampleRate;
		}
		if (f < -rfSampleRate / 2) {
			f += rfSampleRate;
		}
		return f;
	}

	public void calibrateFromInterleavedIq(@NonNull short[] samples, int sampleCount, int rfSampleRate) {
		int pairCount = sampleCount / 2;
		if (pairCount < SAMPLES_PER_BIT * 32) {
			return;
		}
		double[] audio = iqToAudio(samples, pairCount, rfSampleRate);
		int bestFrames = -1;
		boolean bestInvert = false;
		int bestPhase = 0;
		for (int phase = 0; phase < SAMPLES_PER_BIT; phase++) {
			for (boolean invert : new boolean[] {false, true}) {
				int[] bits = demodulateBitsArray(audio, phase, invert);
				int frames = countValidFramesArray(bits);
				if (frames > bestFrames) {
					bestFrames = frames;
					bestInvert = invert;
					bestPhase = phase;
				}
			}
		}
		toneInvert = bestInvert;
		lockedBitPhase = bestPhase;
		demodCalibrated = true;
		LOG.info("APRS demod calibrated invert=" + toneInvert + " phase=" + lockedBitPhase
				+ " previewFrames=" + bestFrames);
	}

	public void processInterleavedIq(@NonNull short[] samples, int sampleCount, int rfSampleRate) {
		int pairs = sampleCount / 2;
		int aliasedIf = aliasIf(ifOffsetHz, rfSampleRate);
		double mixInc = -2.0 * Math.PI * aliasedIf / rfSampleRate;
		double decim = (double) rfSampleRate / audioRate;

		for (int n = 0; n < pairs; n++) {
			double i = samples[n * 2];
			double q = samples[n * 2 + 1];
			dcI = 0.995 * dcI + 0.005 * i;
			dcQ = 0.995 * dcQ + 0.005 * q;
			i -= dcI;
			q -= dcQ;
			mixPhase += mixInc;
			if (mixPhase > Math.PI * 2.0) {
				mixPhase -= Math.PI * 2.0;
			} else if (mixPhase < -Math.PI * 2.0) {
				mixPhase += Math.PI * 2.0;
			}
			double loI = Math.cos(mixPhase);
			double loQ = Math.sin(mixPhase);
			double mi = i * loI - q * loQ;
			double mq = i * loQ + q * loI;

			double disc = 0;
			if (havePrevMix) {
				disc = mi * prevMq - mq * prevMi;
			}
			prevMi = mi;
			prevMq = mq;
			havePrevMix = true;

			decimAcc += 1.0;
			if (decimAcc < decim) {
				continue;
			}
			decimAcc -= decim;
			processAudioSample(disc);
		}
	}

	public void extractNewFramesIfReady(boolean finalPass) {
		if (rawBits.size() < 8 * 16) {
			return;
		}
		extractAndEmitNewFrames(finalPass);
	}

	public void flush() {
		if (bitSampleCount > SAMPLES_PER_BIT / 2) {
			commitBit();
		}
		extractAndEmitNewFrames(true);
		LOG.info("APRS streaming demod finished emitted=" + emittedFrameCount
				+ " invert=" + toneInvert + " phase=" + lockedBitPhase
				+ " rawBits=" + rawBits.size() + " calibrated=" + demodCalibrated);
	}

	private void processAudioSample(double sample) {
		double blocked = 0.995 * (discDc + sample - discDcIn);
		discDcIn = sample;
		discDc = blocked;
		sample = blocked;

		if (phaseSamplesToSkip > 0) {
			phaseSamplesToSkip--;
			audioSampleIndex++;
			return;
		}
		if (bitSampleCount < SAMPLES_PER_BIT) {
			bitBuffer[bitSampleCount++] = sample;
		}
		audioSampleIndex++;
		if (bitSampleCount >= SAMPLES_PER_BIT) {
			commitBit();
		}
	}

	private void commitBit() {
		int count = bitSampleCount;
		if (count <= 0) {
			return;
		}
		double markE = toneEnergy(bitBuffer, count, 1200);
		double spaceE = toneEnergy(bitBuffer, count, 2200);
		int toneLevel = markE > spaceE ? 1 : 0;
		if (toneInvert) {
			toneLevel = 1 - toneLevel;
		}
		int dataBit = toneLevel != lastToneLevel ? 0 : 1;
		lastToneLevel = toneLevel;
		rawBits.add(dataBit);
		bitSampleCount = 0;
	}

	private double toneEnergy(@NonNull double[] samples, int count, int toneHz) {
		double sinAcc = 0;
		double cosAcc = 0;
		double w = 2.0 * Math.PI * toneHz / audioRate;
		int start = audioSampleIndex - count;
		for (int n = 0; n < count; n++) {
			double phase = w * (start + n);
			phase = phase % (Math.PI * 2.0);
			double v = samples[n];
			sinAcc += v * Math.sin(phase);
			cosAcc += v * Math.cos(phase);
		}
		return sinAcc * sinAcc + cosAcc * cosAcc;
	}

	private void extractAndEmitNewFrames(boolean finalPass) {
		List<Integer> flag = Arrays.asList(0, 1, 1, 1, 1, 1, 1, 0);
		int bestAlign = 0;
		int bestCount = -1;
		for (int align = 0; align < 8; align++) {
			int count = countValidFramesAligned(rawBits, align);
			if (count > bestCount) {
				bestCount = count;
				bestAlign = align;
			}
		}

		int i = bestAlign;
		int lastProcessed = 0;
		while (i + 7 < rawBits.size()) {
			if (match(rawBits, i, flag)) {
				int start = i + 8;
				int j = start;
				while (j + 7 < rawBits.size()) {
					if (match(rawBits, j, flag)) {
						List<Integer> payload = destuff(rawBits.subList(start, j));
						byte[] frame = bitsToBytes(payload);
						if (frame != null && frame.length >= 14 && verifyFcs(frame)) {
							byte[] body = new byte[frame.length - 2];
							System.arraycopy(frame, 0, body, 0, body.length);
							int hash = Arrays.hashCode(body);
							if (emittedFrameHashes.add(hash)) {
								emittedFrameCount++;
								listener.onAx25Frame(body);
							}
						}
						lastProcessed = j + 8;
						i = lastProcessed;
						break;
					}
					j++;
				}
				if (j + 7 >= rawBits.size()) {
					break;
				}
			} else {
				i++;
			}
		}

		if (lastProcessed > 0) {
			int keepFrom = Math.max(0, lastProcessed - RAW_BITS_KEEP);
			if (keepFrom > 0) {
				rawBits.subList(0, keepFrom).clear();
			}
		} else if (!finalPass && rawBits.size() > RAW_BITS_KEEP * 128) {
			rawBits.subList(0, rawBits.size() - RAW_BITS_KEEP * 8).clear();
		}
	}

	@NonNull
	private double[] iqToAudio(@NonNull short[] interleaved, int pairCount, int rfSampleRate) {
		double meanI = 0;
		double meanQ = 0;
		for (int n = 0; n < pairCount; n++) {
			meanI += interleaved[n * 2];
			meanQ += interleaved[n * 2 + 1];
		}
		meanI /= pairCount;
		meanQ /= pairCount;

		int aliasedIf = aliasIf(ifOffsetHz, rfSampleRate);
		double mixInc = -2.0 * Math.PI * aliasedIf / rfSampleRate;
		int decim = Math.max(1, rfSampleRate / audioRate);
		int discLen = Math.max(0, pairCount - 1);
		int audioLen = (discLen + decim - 1) / decim;
		double[] audio = new double[audioLen];

		double localPrevMi = 0;
		double localPrevMq = 0;
		boolean havePrev = false;
		int discIndex = 0;
		int ai = 0;
		for (int n = 0; n < pairCount; n++) {
			double i = interleaved[n * 2] - meanI;
			double q = interleaved[n * 2 + 1] - meanQ;
			double ph = mixInc * n;
			double loI = Math.cos(ph);
			double loQ = Math.sin(ph);
			double mi = i * loI - q * loQ;
			double mq = i * loQ + q * loI;
			if (havePrev) {
				double disc = localPrevMi * mq - localPrevMq * mi;
				if (discIndex % decim == 0 && ai < audioLen) {
					audio[ai++] = disc;
				}
				discIndex++;
			}
			localPrevMi = mi;
			localPrevMq = mq;
			havePrev = true;
		}
		if (ai < audio.length) {
			audio = Arrays.copyOf(audio, ai);
		}

		dcBlockInPlace(audio, 0.995);
		double sum = 0;
		for (double v : audio) {
			sum += v;
		}
		if (audio.length > 0) {
			double mean = sum / audio.length;
			double peak = 0;
			for (int idx = 0; idx < audio.length; idx++) {
				audio[idx] -= mean;
				peak = Math.max(peak, Math.abs(audio[idx]));
			}
			if (peak > 0) {
				for (int idx = 0; idx < audio.length; idx++) {
					audio[idx] /= peak;
				}
			}
		}
		return audio;
	}

	private static void dcBlockInPlace(@NonNull double[] x, double a) {
		double prevIn = 0;
		double prevOut = 0;
		for (int n = 0; n < x.length; n++) {
			double y = a * (prevOut + x[n] - prevIn);
			prevIn = x[n];
			prevOut = y;
			x[n] = y;
		}
	}

	@NonNull
	private int[] demodulateBitsArray(@NonNull double[] audio, int phase, boolean invert) {
		int nBits = (audio.length - phase) / SAMPLES_PER_BIT;
		int[] bits = new int[nBits];
		int prev = 1;
		double wMark = 2.0 * Math.PI * 1200 / audioRate;
		double wSpace = 2.0 * Math.PI * 2200 / audioRate;
		for (int bi = 0; bi < nBits; bi++) {
			int start = phase + bi * SAMPLES_PER_BIT;
			double markSin = 0;
			double markCos = 0;
			double spaceSin = 0;
			double spaceCos = 0;
			for (int n = 0; n < SAMPLES_PER_BIT && start + n < audio.length; n++) {
				double v = audio[start + n];
				double t = start + n;
				markSin += v * Math.sin(wMark * t);
				markCos += v * Math.cos(wMark * t);
				spaceSin += v * Math.sin(wSpace * t);
				spaceCos += v * Math.cos(wSpace * t);
			}
			double markE = markSin * markSin + markCos * markCos;
			double spaceE = spaceSin * spaceSin + spaceCos * spaceCos;
			boolean mark = markE > spaceE;
			if (invert) {
				mark = !mark;
			}
			int level = mark ? 1 : 0;
			bits[bi] = level != prev ? 0 : 1;
			prev = level;
		}
		return bits;
	}

	private int countValidFramesArray(@NonNull int[] bits) {
		int best = 0;
		for (int align = 0; align < 8; align++) {
			best = Math.max(best, countValidFramesArrayAligned(bits, align));
		}
		return best;
	}

	private int countValidFramesArrayAligned(@NonNull int[] bits, int align) {
		int count = 0;
		int i = align;
		while (i + 7 < bits.length) {
			if (matchesFlag(bits, i)) {
				int start = i + 8;
				int j = start;
				while (j + 7 < bits.length) {
					if (matchesFlag(bits, j)) {
						byte[] frame = payloadToFrame(bits, start, j);
						if (frame != null && frame.length >= 14 && verifyFcs(frame)) {
							count++;
						}
						i = j + 8;
						break;
					}
					j++;
				}
				if (j + 7 >= bits.length) {
					break;
				}
			} else {
				i++;
			}
		}
		return count;
	}

	private static boolean matchesFlag(@NonNull int[] bits, int offset) {
		return bits[offset] == 0 && bits[offset + 1] == 1 && bits[offset + 2] == 1
				&& bits[offset + 3] == 1 && bits[offset + 4] == 1 && bits[offset + 5] == 1
				&& bits[offset + 6] == 1 && bits[offset + 7] == 0;
	}

	@Nullable
	private static byte[] payloadToFrame(@NonNull int[] bits, int start, int end) {
		int[] destuffed = destuffArray(bits, start, end);
		return bitsArrayToBytes(destuffed);
	}

	@NonNull
	private static int[] destuffArray(@NonNull int[] bits, int start, int end) {
		int[] out = new int[end - start];
		int outLen = 0;
		int ones = 0;
		for (int idx = start; idx < end; idx++) {
			int b = bits[idx];
			if (ones == 5) {
				if (b == 0) {
					ones = 0;
					continue;
				}
				ones = b == 1 ? 1 : 0;
			} else {
				ones = b == 1 ? ones + 1 : 0;
			}
			out[outLen++] = b;
		}
		if (outLen == out.length) {
			return out;
		}
		return Arrays.copyOf(out, outLen);
	}

	@Nullable
	private static byte[] bitsArrayToBytes(@NonNull int[] bits) {
		if (bits.length < 8) {
			return null;
		}
		byte[] out = new byte[bits.length / 8];
		for (int i = 0; i < out.length; i++) {
			int v = 0;
			for (int j = 0; j < 8; j++) {
				v = (v << 1) | bits[i * 8 + j];
			}
			out[i] = (byte) v;
		}
		return out;
	}

	static boolean verifyFcs(@NonNull byte[] frameWithFcs) {
		int crc = 0xFFFF;
		for (byte b : frameWithFcs) {
			crc ^= b & 0xFF;
			for (int k = 0; k < 8; k++) {
				crc = ((crc & 1) != 0) ? ((crc >> 1) ^ 0x8408) : (crc >> 1);
			}
		}
		return crc == 0xF0B8;
	}

	@Nullable
	static byte[] bitsToBytes(@NonNull List<Integer> bits) {
		if (bits.size() < 8) {
			return null;
		}
		byte[] out = new byte[bits.size() / 8];
		for (int i = 0; i < out.length; i++) {
			int v = 0;
			for (int j = 0; j < 8; j++) {
				v = (v << 1) | bits.get(i * 8 + j);
			}
			out[i] = (byte) v;
		}
		return out;
	}

	@NonNull
	static List<Integer> destuff(@NonNull List<Integer> bits) {
		List<Integer> out = new ArrayList<>();
		int ones = 0;
		for (int b : bits) {
			if (ones == 5) {
				if (b == 0) {
					ones = 0;
					continue;
				}
				ones = b == 1 ? 1 : 0;
			} else {
				ones = b == 1 ? ones + 1 : 0;
			}
			out.add(b);
		}
		return out;
	}

	private int countValidFramesAligned(@NonNull List<Integer> bits, int align) {
		int count = 0;
		List<Integer> flag = Arrays.asList(0, 1, 1, 1, 1, 1, 1, 0);
		int i = align;
		while (i + 7 < bits.size()) {
			if (match(bits, i, flag)) {
				int start = i + 8;
				int j = start;
				while (j + 7 < bits.size()) {
					if (match(bits, j, flag)) {
						List<Integer> payload = destuff(bits.subList(start, j));
						byte[] frame = bitsToBytes(payload);
						if (frame != null && frame.length >= 14 && verifyFcs(frame)) {
							count++;
						}
						i = j + 8;
						break;
					}
					j++;
				}
				if (j + 7 >= bits.size()) {
					break;
				}
			} else {
				i++;
			}
		}
		return count;
	}

	private static boolean match(@NonNull List<Integer> bits, int offset, @NonNull List<Integer> pattern) {
		for (int k = 0; k < pattern.size(); k++) {
			if (bits.get(offset + k) != pattern.get(k)) {
				return false;
			}
		}
		return true;
	}
}
