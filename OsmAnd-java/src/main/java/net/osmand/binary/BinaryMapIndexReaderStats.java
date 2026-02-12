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
	
	public enum BinaryMapIndexReaderSubApiName {
		ADDRESS_NAME_INDEX,
		ADDRESS_NAME_REFERENCES,
		ADDRESS_NAME_OBJECTS,

		POI_NAME_INDEX,
		POI_NAME_REFERENCES, 
		POI_NAME_OBJECTS
	}
	
	private static class SubStatByAPI {
		BinaryMapIndexReaderSubApiName api;
		String mapName;
		int calls;
		long time;
		// long bytes;
	}
	
	private static class StatByAPI {
		BinaryMapIndexReaderApiName api;
		int calls;
		long time;
		long bytes;
		// int objects; // if we can count 
		Map<String, SubStatByAPI> subapis = new HashMap<String, BinaryMapIndexReaderStats.SubStatByAPI>();
		
		public SubStatByAPI getSubApi(String mapName, BinaryMapIndexReaderSubApiName api) {
			String key = api.name() + "_" + mapName;
			SubStatByAPI subapi = subapis.get(key);
			if (!subapis.containsKey(key)) {
				subapi = new SubStatByAPI();
				subapi.mapName = mapName;
				subapi.api = api;
				subapis.put(key, subapi);
			}
			return subapi;
		}
		
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
		long lastReq = 0, subStart = 0, subSize = 0;
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
			ADDRESS_LOAD_CITIES,
			ADDRESS_LOAD_STREETS,
			ADDRESS_LOAD_BUILDINGS,
			POI_NAMEINDEX,
			POI_BOXES,
			POI_POIDATA
		}

		public static final class TimingSummary {
			private final LongAdder totalNs = new LongAdder();
			private final LongAdder count = new LongAdder();
			public volatile Map<String, long[]> subKeys = new LinkedHashMap<>(); // 0 - time, 1 - count

			void add(long elapsedNs, long size) {
				totalNs.add(elapsedNs);
				count.add(size);
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

			void updateSubs(String key1, long time1, long count1, String key2, long time2, long count2) {
				subKeys.clear();
				String normalizedKey1 = key1 == null ? "" : key1;
				String normalizedKey2 = key2 == null ? "" : key2;
				if (normalizedKey1.isEmpty() && normalizedKey2.isEmpty()) {
					return;
				}
				if (normalizedKey1.equals(normalizedKey2)) {
					subKeys.put(normalizedKey1, new long[] {Math.max(0, time1 + time2), Math.max(0, count1 + count2)});
				} else {
					if (!normalizedKey1.isEmpty()) {
						subKeys.put(normalizedKey1, new long[] {Math.max(0, time1), Math.max(0, count1)});
					}
					if (!normalizedKey2.isEmpty()) {
						subKeys.put(normalizedKey2, new long[] {Math.max(0, time2), Math.max(0, count2)});
					}
				}
			}
		}

		public Map<String, TimingSummary> getTimingSummary() {
			HashMap<String, TimingSummary> grouped = new HashMap<>();
			HashMap<String, HashMap<String, long[]>> maxByOpAndSuffix = new HashMap<>();
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

				HashMap<String, long[]> suffixTotals = maxByOpAndSuffix.computeIfAbsent(op, k -> new HashMap<>());
				long[] totals = suffixTotals.computeIfAbsent(suffix, k -> new long[2]);
				totals[0] += subTotalNs;
				totals[1] += subCount;
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
				HashMap<String, long[]> suffixTotals = maxByOpAndSuffix.get(op);
				if (suffixTotals != null) {
					ArrayList<Map.Entry<String, long[]>> suffixEntries = new ArrayList<>(suffixTotals.entrySet());
					suffixEntries.sort((a, b) -> {
						long[] aTotals = a.getValue(), bTotals = b.getValue();
						long av = aTotals == null ? 0 : aTotals[0];
						long bv = bTotals == null ? 0 : bTotals[0];
						if (av == bv) {
							String ak = a.getKey() == null ? "" : a.getKey();
							String bk = b.getKey() == null ? "" : b.getKey();
							return ak.compareTo(bk);
						}
						return av < bv ? 1 : -1;
					});

					String bestKey = "", afterBestKey = "";
					long bestTime = 0, bestCount = 0;
					long afterBestTime = 0, afterBestCount = 0;
					if (!suffixEntries.isEmpty()) {
						Map.Entry<String, long[]> first = suffixEntries.get(0);
						bestKey = first.getKey();
						long[] firstTotals = first.getValue();
						bestTime = firstTotals == null ? 0 : firstTotals[0];
						bestCount = firstTotals == null ? 0 : firstTotals[1];
						if (suffixEntries.size() > 1) {
							Map.Entry<String, long[]> second = suffixEntries.get(1);
							afterBestKey = second.getKey();
							long[] secondTotals = second.getValue();
							afterBestTime = secondTotals == null ? 0 : secondTotals[0];
							afterBestCount = secondTotals == null ? 0 : secondTotals[1];
						}
					}
					summary.updateSubs(bestKey, bestTime, bestCount, afterBestKey, afterBestTime, afterBestCount);
				}
				result.put(op, summary);
			}

			subTimings.clear();
			return result;
		}

		public long beginSubSearchStats(int size) {
			subStart = System.nanoTime();
			subSize = size;
			return subStart;
		}

		public void endSubSearchStats(long statReq, SubOp op, String obf, int size) {
			if (statReq != subStart) {
				System.err.println("ERROR: in sub search stats counting to fix ! " + statReq + " != " + subStart);
			}
			long timeCall = (System.nanoTime() - statReq);
			String key = op.name() + "|" + obf;
			subTimings.computeIfAbsent(key, k -> new TimingSummary()).add(timeCall, size - subSize);
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