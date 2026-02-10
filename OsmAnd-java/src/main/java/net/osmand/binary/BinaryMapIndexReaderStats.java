package net.osmand.binary;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.util.Algorithms;

public class BinaryMapIndexReaderStats { 
	
	
	public enum BinaryMapIndexReaderApiName {
		ADDRESS_BY_NAME,
		POI_BY_NAME,
		LOAD_STREETS,
		LOAD_CITIES,
		LOAD_BUILDINGS,
		POI_BY_TYPE,
	}
	
	private static class StatByAPI {
		BinaryMapIndexReaderApiName api;
		int calls;
		long time;
		long bytes;
		
		@Override
		public String toString() {
			return String.format("API %s [call %d, time %.2f, %,d KB]", api, calls, time / 1e9, bytes/1024);
		}
	}
	
	public static class WordSearchStat {
		public int results;
		public int apis;
		public Map<String, Integer> resultCounts = new HashMap<>();
		public Map<BinaryMapIndexReaderApiName, Integer> apiCalls = new HashMap<>();
		public String requestWord;
		
		@Override
		public String toString() {
			return String.format("api %s, results %s " , apiCalls, resultCounts);
		}
	}
	
	public static class SearchStat {
		long lastReq = 0, subStart = 0;
		public long totalTime = 0;
		public long totalBytes = 0;
		public int prevResultsSize = 0;
		public String requestWord = "";
		Map<BinaryMapIndexReaderApiName, StatByAPI> byApis = new HashMap<>();
		Map<String, WordSearchStat> wordStats = new HashMap<String, WordSearchStat>();
		final ConcurrentHashMap<String, TimingSummary> subTimings = new ConcurrentHashMap<>();
		
		public Map<String, WordSearchStat> getWordStats() {
			return wordStats;
		}
		
		public long beginSearchStats(BinaryMapIndexReaderApiName api, SearchRequest<?> req, BinaryIndexPart part, CodedInputStream codedIS, String extraInfo) {
			lastReq = System.nanoTime();
			codedIS.resetBytesCounter();
			if (req != null && req.getSearchResults() != null) {
				prevResultsSize = req.getSearchResults().size();
			} else {
				prevResultsSize = 0;
			}
			if (req != null && !Algorithms.isEmpty(req.nameQuery)) {
				requestWord = req.nameQuery; 
			} else {
				requestWord = "";
			}
			return lastReq;
		}

		public void endSearchStats(long statReq, BinaryMapIndexReaderApiName api, List<?> objects, BinaryIndexPart part,
				CodedInputStream codedIS, String extraInfo) {
			if (statReq != lastReq) {
				System.err.println("ERROR: in stats counting to fix ! " + statReq + " != " + lastReq);
			}
			if (requestWord == null) {
				requestWord = "";
			}
			WordSearchStat wordStat = wordStats.get(requestWord);
			if(wordStat == null) {
				wordStat = new WordSearchStat();
				wordStat.requestWord = requestWord; 
				wordStats.put(requestWord, wordStat);
			}
			wordStat.apis++;
			wordStat.results += objects.size();
			wordStat.apiCalls.compute(api, (k, cnt) -> cnt == null ? 1 : cnt + 1);
			for (Object o : objects) {
				wordStat.resultCounts.compute(o.getClass().getSimpleName(), (k, cnt) -> cnt == null ? 1 : cnt + 1);
			}

			long timeCall = (System.nanoTime() - statReq);
			long bytes = codedIS.getBytesCounter();
			totalTime += timeCall;
			totalBytes += bytes;
			StatByAPI statByAPI = byApis.get(api);
			if (statByAPI == null) {
				statByAPI = new StatByAPI();
				statByAPI.api = api;
				byApis.put(api, statByAPI);
			}
			statByAPI.bytes += bytes;
			statByAPI.calls++;
			statByAPI.time += timeCall;
		}

		public enum SubOp {
			ADDRESS_TABLE,
			ADDRESS_ATOM,
			POI_NAMEINDEX,
			POI_BOXES,
			POI_POIDATA
		}

		public static final class TimingSummary {
			private final LongAdder totalNs = new LongAdder();
			private final LongAdder count = new LongAdder();
			public volatile String[] subKey = new String[2]; // 0 - max key, 1 - next key
			public volatile long[] subTime = new long[] {0, 0}; // 0 - max time, 1 - next time

			void add(long elapsedNs) {
				totalNs.add(elapsedNs);
				count.increment();
			}

			void addTotal(long elapsedNs, long occurrences) {
				totalNs.add(elapsedNs);
				count.add(occurrences);
			}

			public long getTotalNs() {
				return totalNs.sum();
			}

			public long getCount() {
				return count.sum();
			}

			void updateMaxMinSub(String maxKey, long maxTotalNs, String afterMaxKey, long afterMaxTotalNs) {
				subKey[0] = maxKey == null ? "" : maxKey;
				subTime[0] = Math.max(0, maxTotalNs);
				subKey[1] = afterMaxKey == null ? "" : afterMaxKey;
				subTime[1] = Math.max(0, afterMaxTotalNs);
			}
		}

		public Map<String, TimingSummary> getTimingSummary() {
			HashMap<String, TimingSummary> grouped = new HashMap<>();
			HashMap<String, HashMap<String, Long>> maxByOpAndSuffix = new HashMap<>();
			for (Map.Entry<String, TimingSummary> e : subTimings.entrySet()) {
				String key = e.getKey(), op, suffix;
				int idx = key == null ? -1 : key.indexOf('|');
				if (idx < 0) {
					op = key;
					suffix = "";
				} else {
					op = key.substring(0, idx);
					suffix = key.substring(idx + 1);
				}
				if (op == null) {
					op = "UNKNOWN";
				}
				TimingSummary sub = e.getValue();
				long subTotalNs = sub.getTotalNs();
				long subCount = sub.getCount();
				grouped.computeIfAbsent(op, k -> new TimingSummary()).addTotal(subTotalNs, subCount);

				HashMap<String, Long> suffixTotals = maxByOpAndSuffix.computeIfAbsent(op, k -> new HashMap<>());
				suffixTotals.merge(suffix, subTotalNs, Long::sum);
			}

			ArrayList<Map.Entry<String, TimingSummary>> sorted = new ArrayList<>(grouped.entrySet());
			sorted.sort((o1, o2) -> {
				long d = o2.getValue().getTotalNs() - o1.getValue().getTotalNs();
				return d == 0 ? 0 : (d > 0 ? 1 : -1);
			});

			LinkedHashMap<String, TimingSummary> result = new LinkedHashMap<>();
			for (Map.Entry<String, TimingSummary> e : sorted) {
				String op = e.getKey();
				TimingSummary summary = e.getValue();
				HashMap<String, Long> suffixTotals = maxByOpAndSuffix.get(op);
				if (suffixTotals != null) {
					ArrayList<Map.Entry<String, Long>> suffixEntries = new ArrayList<>(suffixTotals.entrySet());
					suffixEntries.sort((a, b) -> {
						long av = a.getValue() == null ? 0 : a.getValue();
						long bv = b.getValue() == null ? 0 : b.getValue();
						if (av == bv) {
							String ak = a.getKey() == null ? "" : a.getKey();
							String bk = b.getKey() == null ? "" : b.getKey();
							return ak.compareTo(bk);
						}
						return av < bv ? 1 : -1;
					});

					String bestSuffix = "";
					long bestTotal = 0;
					String afterBestSuffix = "";
					long afterBestTotal = 0;
					if (!suffixEntries.isEmpty()) {
						Map.Entry<String, Long> first = suffixEntries.get(0);
						bestSuffix = first.getKey();
						bestTotal = first.getValue() == null ? 0 : first.getValue();
						if (suffixEntries.size() > 1) {
							Map.Entry<String, Long> second = suffixEntries.get(1);
							afterBestSuffix = second.getKey();
							afterBestTotal = second.getValue() == null ? 0 : second.getValue();
						}
					}
					summary.updateMaxMinSub(bestSuffix, bestTotal, afterBestSuffix, afterBestTotal);
				}
				result.put(op, summary);
			}

			subTimings.clear();
			return result;
		}

		public long beginSubSearchStats() {
			subStart = System.nanoTime();
			return subStart;
		}

		public void endSubSearchStats(long statReq, SubOp op, String obf) {
			if (statReq != subStart) {
				System.err.println("ERROR: in sub search stats counting to fix ! " + statReq + " != " + subStart);
			}
			long timeCall = (System.nanoTime() - statReq);
			String key = op.name() + "|" + obf;
			subTimings.computeIfAbsent(key, k -> new TimingSummary()).add(timeCall);
		}

		@Override
		public String toString() {
			return String.format("Search stat: time %.3f, bytes %,d KB, by apis - %s; words %s", totalTime / 1e9, totalBytes / 1024,
					byApis.values(), wordStats);
		}
	}
	
	public static class MapObjectStat {
		public int lastStringNamesSize;
		public int lastObjectIdSize;
		public int lastObjectHeaderInfo;
		public int lastObjectAdditionalTypes;
		public int lastObjectTypes;
		public int lastObjectCoordinates;
		public int lastObjectLabelCoordinates;

		public int lastObjectSize;
		public int lastBlockStringTableSize;
		public int lastBlockHeaderInfo;

		public void addBlockHeader(int typesFieldNumber, int sizeL) {
			lastBlockHeaderInfo +=
					CodedOutputStream.computeTagSize(typesFieldNumber) +
							CodedOutputStream.computeRawVarint32Size(sizeL);
		}

		public void addTagHeader(int typesFieldNumber, int sizeL) {
			lastObjectHeaderInfo +=
					CodedOutputStream.computeTagSize(typesFieldNumber) +
							CodedOutputStream.computeRawVarint32Size(sizeL);
		}

		public void clearObjectStats() {
			lastStringNamesSize = 0;
			lastObjectIdSize = 0;
			lastObjectHeaderInfo = 0;
			lastObjectAdditionalTypes = 0;
			lastObjectTypes = 0;
			lastObjectCoordinates = 0;
			lastObjectLabelCoordinates = 0;
		}
	}
	

}