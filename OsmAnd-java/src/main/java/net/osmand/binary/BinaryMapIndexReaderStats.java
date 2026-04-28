package net.osmand.binary;

import java.util.*;

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
		POI_NAME_OBJECTS,
		POI_NAME_GROUPS_BBOXES
	}


	public static class PoiReadMetricSet {
		long payloadBytesParsed;
		long decodeTimeNs;
		long matcherTimeNs;
		long blocksLoaded;
		long objectsLoaded;
		long matchedObjectsLoaded;
		long maxObjectsPerBlock;
		private long objectsLoadedBefore, payloadBytesParsedBefore;
		
		void beginLoadObject(CodedInputStream stream) {
			objectsLoadedBefore = objectsLoaded;
			payloadBytesParsedBefore = stream.getTotalBytesRead();
		}

		void endLoadObject(CodedInputStream stream) {
			blocksLoaded++;

			long objectsInBlock = objectsLoaded - objectsLoadedBefore;
			maxObjectsPerBlock = Math.max(maxObjectsPerBlock, objectsInBlock);
			payloadBytesParsed += stream.getTotalBytesRead() - payloadBytesParsedBefore;
		}
	}
	
	public static class SubStatByAPI {
		public final BinaryMapIndexReaderApiName api;
		public final BinaryMapIndexReaderSubApiName subApi;
		public final String mapName;
		private long time = 0, count = 0, calls = 0, bytes = 0;
		private long payloadBytesParsed = 0, decodeTime = 0, matcherTime = 0;
		private long blocksLoaded = 0, objectsLoaded = 0, matchedObjects = 0, maxObjectsPerBlock = 0;

		SubStatByAPI(BinaryMapIndexReaderApiName api, BinaryMapIndexReaderSubApiName subApi, String mapName) {
			this.api = api;
			this.subApi = subApi;
			this.mapName = mapName;
		}

		void add(long timeNs, long count, long bytes) {
			this.time += timeNs;
			this.count += count;
			this.bytes += bytes;
			calls++;
		}

		void add(long timeNs, long count, long bytes, PoiReadMetricSet metrics) {
			this.time += timeNs;
			this.count += count;
			this.bytes += bytes;
			this.payloadBytesParsed +=  metrics.payloadBytesParsed;
			this.decodeTime +=  metrics.decodeTimeNs;
			this.matcherTime +=  metrics.matcherTimeNs;
			this.blocksLoaded += metrics.blocksLoaded;
			this.objectsLoaded += metrics.objectsLoaded;
			this.matchedObjects += metrics.matchedObjectsLoaded;
			this.maxObjectsPerBlock = Math.max(this.maxObjectsPerBlock,  metrics.maxObjectsPerBlock);
			calls++;
		}

		public long getTime() {
			return time;
		}

		public long getBytes() {
			return bytes;
		}

		public long getCalls() {
			return calls;
		}

		public long getCount() {
			return count;
		}

		public long getPayloadBytesParsed() {
			return payloadBytesParsed;
		}

		public long getDecodeTime() {
			return decodeTime;
		}

		public long getMatcherTime() {
			return matcherTime;
		}

		public long getBlocksLoaded() {
			return blocksLoaded;
		}

		public long getObjectsLoaded() {
			return objectsLoaded;
		}

		public long getMatchedObjects() {
			return matchedObjects;
		}

		public long getMaxObjectsPerBlock() {
			return maxObjectsPerBlock;
		}
	}
	
	public static class StatByAPI {
		BinaryMapIndexReaderApiName api;
		public int calls;
		public long time;
		public long bytes;
		// int objects; // if we can count 
		Map<String, SubStatByAPI> subApis = new HashMap<>();
		
		public SubStatByAPI getSubApi(BinaryMapIndexReaderSubApiName subApi, String mapName) {
			String key = subApi.name() + '|' + mapName;
			SubStatByAPI subStat = subApis.get(key);
			if (!subApis.containsKey(key)) {
				subStat = new SubStatByAPI(api, subApi, mapName);
				subApis.put(key, subStat);
			}
			return subStat;
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
		private static final boolean TO_DETAILED_STRING = false;
		private static final int DEFAULT_TOP_K_OBF = 5;
		long lastReq = 0, subSize = 0;
		public long totalTime = 0;
		public long totalBytes = 0;
		public int prevResultsSize = 0;
		public String requestWord = "";
		Map<BinaryMapIndexReaderApiName, StatByAPI> byApis = new HashMap<>();
		Map<String, WordSearchStat> wordStats = new HashMap<String, WordSearchStat>();

		public Map<String, WordSearchStat> getWordStats() {
			return wordStats;
		}

		public Map<BinaryMapIndexReaderApiName, StatByAPI> getByApis() {
			return byApis;
		}

		public List<SubStatByAPI> getSubStatsSummary() {
			Map<String, SubStatByAPI> grouped = new HashMap<>();
			for (StatByAPI apiStats : byApis.values()) {
				if (apiStats == null || apiStats.subApis == null) {
					continue;
				}
				for (SubStatByAPI st : apiStats.subApis.values()) {
					if (st == null) {
						continue;
					}
					String mapName = st.mapName == null ? "" : st.mapName;
					String key = st.api.name() + '|' + st.subApi.name() + '|' + mapName;
					SubStatByAPI agg = grouped.computeIfAbsent(key, k -> new SubStatByAPI(st.api, st.subApi, st.mapName));
					agg.time += st.time;
					agg.count += st.count;
					agg.bytes += st.bytes;
					agg.payloadBytesParsed += st.payloadBytesParsed;
					agg.decodeTime += st.decodeTime;
					agg.matcherTime += st.matcherTime;
					agg.blocksLoaded += st.blocksLoaded;
					agg.objectsLoaded += st.objectsLoaded;
					agg.matchedObjects += st.matchedObjects;
					agg.maxObjectsPerBlock = Math.max(agg.maxObjectsPerBlock, st.maxObjectsPerBlock);
					agg.calls += st.calls;
				}
			}
			List<SubStatByAPI> result = new ArrayList<>(grouped.values());
			result.sort((a, b) -> Long.compare(b.bytes, a.bytes));
			return result;
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

		public long beginSubSearchStats(int size) {
			long subStart = System.nanoTime();
			subSize = size;
			return subStart;
		}

		public void endSubSearchStats(long statReq, BinaryMapIndexReaderApiName api, BinaryMapIndexReaderSubApiName op, String obf, int size, long bytes) {
			long timeCall = (System.nanoTime() - statReq);
			StatByAPI statByAPI = byApis.get(api);
			if (statByAPI == null) {
				statByAPI = new StatByAPI();
				statByAPI.api = api;
				byApis.put(api, statByAPI);
			}
			SubStatByAPI subStatByAPI = statByAPI.getSubApi(op, obf);
			subStatByAPI.add(timeCall, size - subSize, bytes);
		}

		public void endSubSearchStats(long statReq, BinaryMapIndexReaderApiName api, BinaryMapIndexReaderSubApiName op,
				String obf, int size, long bytes, PoiReadMetricSet metrics) {
			long timeCall = System.nanoTime() - statReq;
			StatByAPI statByAPI = byApis.get(api);
			if (statByAPI == null) {
				statByAPI = new StatByAPI();
				statByAPI.api = api;
				byApis.put(api, statByAPI);
			}
			SubStatByAPI subStatByAPI = statByAPI.getSubApi(op, obf);
			subStatByAPI.add(timeCall, size - subSize, bytes, metrics);
		}

		@Override
		public String toString() {
			if (TO_DETAILED_STRING) {
				return toDetailedString();
			}
			return String.format("Search stat: time %.3f, bytes %,d KB, by apis - %s; words %s", totalTime / 1e9, totalBytes / 1024,
					byApis.values(), wordStats);
		}

		public String toDetailedString() {
			return toDetailedString(DEFAULT_TOP_K_OBF);
		}

		public String toDetailedString(int topKObf) {
			DetailedStringData data = buildDetailedStringData(Math.max(0, topKObf));
			return data.renderDetailedString(wordStats, totalTime, totalBytes, Math.max(0, topKObf));
		}

		private record DetailedStringData(String c1, String c2, String c3, String c4, String c5, String c6,
		                                  String c9, String c10, String c11, String c12, String c13, String c14, String c15,
		                                  Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, List<SubStatByAPI>>> rows,
		                                  Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, SubStatByAPI>> totalsByApiSubApi,
		                                  Map<BinaryMapIndexReaderApiName, SubStatByAPI> totalsByApi,
		                                  List<BinaryMapIndexReaderApiName> apis,
		                                  List<String> obfNames,
		                                  Map<String, SubStatByAPI> totalsByApiObf,
		                                  int subApiCount) {

			private String renderDetailedString(Map<String, WordSearchStat>  wordStats, long totalTime, long totalBytes, int topKObf) {
				int w1 = c1.length(), w2 = c2.length(), w3 = c3.length(), w4 = c4.length(), w5 = c5.length(), w6 = c6.length(),
						w9 = c9.length(), w10 = c10.length(), w11 = c11.length(),
						w12 = c12.length(), w13 = c13.length(), w14 = c14.length(), w15 = c15.length();
				for (BinaryMapIndexReaderApiName api : apis) {
					if (api == null) {
						continue;
					}
					w1 = Math.max(w1, api.name().length());
					Map<BinaryMapIndexReaderSubApiName, SubStatByAPI> bySub = totalsByApiSubApi.get(api);
					if (bySub == null) {
						continue;
					}
					List<BinaryMapIndexReaderSubApiName> subApis = new ArrayList<>(bySub.keySet());
					subApis.sort((a, b) -> Long.compare(bySub.get(b).bytes, bySub.get(a).bytes));
					for (BinaryMapIndexReaderSubApiName subApi : subApis) {
						if (subApi == null) {
							continue;
						}
						w2 = Math.max(w2, subApi.name().length());
						List<SubStatByAPI> detail = rows.getOrDefault(api, Collections.emptyMap()).getOrDefault(subApi, Collections.emptyList());
						detail.sort((a, b) -> Long.compare(b.bytes, a.bytes));
					}
				}
				List<SubStatByAPI> orderedObfTotals = new ArrayList<>(totalsByApiObf.values());
				orderedObfTotals.sort((a, b) -> {
					int compareApi = compareApiForMetricsSummary(a.api, b.api);
					if (compareApi != 0) {
						return compareApi;
					}
					int compareLoaded = Long.compare(b.bytes, a.bytes);
					if (compareLoaded != 0) {
						return compareLoaded;
					}
					int compareTime = Long.compare(b.time, a.time);
					if (compareTime != 0) {
						return compareTime;
					}
					return a.mapName.compareTo(b.mapName);
				});

				StringBuilder sb = new StringBuilder();
				sb.append(String.format(Locale.US, "Search stat: time %.3f, bytes % d KB, by APIs:",
						totalTime / 1e9, totalBytes / 1024));
				sb.append("\n");
				sb.append(padRight(c1, w1)).append(", ")
						.append(padRight(c2, w2)).append(", ").append(padRight(c3, w3)).append(", ")
						.append(padRight(c4, w4)).append(", ").append(padRight(c5, w5)).append(", ")
						.append(padRight(c6, w6)).append(", ").append(padRight(c9, w9)).append(", ")
						.append(padRight(c10, w10)).append(", ").append(padRight(c11, w11)).append(", ")
						.append(padRight(c12, w12)).append(", ").append(padRight(c13, w13)).append(", ")
						.append(padRight(c14, w14)).append(", ").append(padRight(c15, w15));

				for (BinaryMapIndexReaderApiName api : apis) {
					if (api == null) {
						continue;
					}
					SubStatByAPI apiTotal = totalsByApi.get(api);
					if (apiTotal == null) {
						continue;
					}
					sb.append("\n");
					sb.append(padRight(api.name(), w1)).append(", ")
							.append(padRight("", w2)).append(", ")
							.append(padLeft(String.format(Locale.US, "%.2f", apiTotal.time / 1e9), w3)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.count), w4)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.bytes / 1024), w5)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.calls), w6)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.payloadBytesParsed / 1024), w9)).append(", ")
							.append(padLeft(String.format(Locale.US, "%.2f", apiTotal.decodeTime / 1e6), w10)).append(", ")
							.append(padLeft(String.format(Locale.US, "%.2f", apiTotal.matcherTime / 1e6), w11)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.blocksLoaded), w12)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.objectsLoaded), w13)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.matchedObjects), w14)).append(", ")
							.append(padLeft(String.format(Locale.US, "% d", apiTotal.maxObjectsPerBlock), w15));

					Map<BinaryMapIndexReaderSubApiName, SubStatByAPI> bySub = totalsByApiSubApi.get(api);
					if (bySub == null) {
						continue;
					}
					List<BinaryMapIndexReaderSubApiName> subApis = new ArrayList<>(bySub.keySet());
					subApis.sort((a, b) -> Long.compare(bySub.get(b).bytes, bySub.get(a).bytes));
					for (BinaryMapIndexReaderSubApiName subApi : subApis) {
						if (subApi == null) {
							continue;
						}
						SubStatByAPI subTotal = bySub.get(subApi);
						if (subTotal == null) {
							continue;
						}
						sb.append("\n");
						sb.append(padRight("", w1)).append(", ")
								.append(padRight(subApi.name(), w2)).append(", ")
								.append(padLeft(String.format(Locale.US, "%.2f", subTotal.time / 1e9), w3)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.count), w4)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.bytes / 1024), w5)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.calls), w6)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.payloadBytesParsed / 1024), w9)).append(", ")
								.append(padLeft(String.format(Locale.US, "%.2f", subTotal.decodeTime / 1e6), w10)).append(", ")
								.append(padLeft(String.format(Locale.US, "%.2f", subTotal.matcherTime / 1e6), w11)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.blocksLoaded), w12)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.objectsLoaded), w13)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.matchedObjects), w14)).append(", ")
								.append(padLeft(String.format(Locale.US, "% d", subTotal.maxObjectsPerBlock), w15));

						List<SubStatByAPI> detail = rows.getOrDefault(api, Collections.emptyMap()).getOrDefault(subApi, Collections.emptyList());
						detail.sort((a, b) -> Long.compare(b.bytes, a.bytes));
						for (int i = 0; i < detail.size() && i < topKObf; i++) {
							SubStatByAPI st = detail.get(i);
							if (st == null) {
								continue;
							}
							sb.append("\n");
							sb.append(padRight("", w1)).append(", ")
									.append(padRight(st.mapName == null ? "" : st.mapName, w2)).append(", ")
									.append(padLeft(String.format(Locale.US, "%.2f", st.time / 1e9), w3)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.count), w4)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.bytes / 1024), w5)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.calls), w6)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.payloadBytesParsed / 1024), w9)).append(", ")
									.append(padLeft(String.format(Locale.US, "%.2f", st.decodeTime / 1e6), w10)).append(", ")
									.append(padLeft(String.format(Locale.US, "%.2f", st.matcherTime / 1e6), w11)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.blocksLoaded), w12)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.objectsLoaded), w13)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.matchedObjects), w14)).append(", ")
									.append(padLeft(String.format(Locale.US, "% d", st.maxObjectsPerBlock), w15));
						}
					}
				}

				sb.append("\nWords ").append(wordStats);
				sb.append("\nBreadth: APIs=").append(apis.size())
						.append(", Sub-APIs=").append(subApiCount)
						.append(", OBFs=").append(obfNames.size())
						.append(", Word entries=").append(wordStats == null ? 0 : wordStats.size());
				sb.append("\nOBF count: ").append(obfNames.size());
				sb.append("\ncategory, name, Time (s), Payload (KB), Blocks, Objects, Matched");
				for (SubStatByAPI obfTotal : orderedObfTotals) {
					if (obfTotal == null || obfTotal.api == null || obfTotal.mapName == null || obfTotal.mapName.isEmpty()) {
						continue;
					}
					if (obfTotal.api != BinaryMapIndexReaderApiName.POI_BY_NAME && obfTotal.api != BinaryMapIndexReaderApiName.ADDRESS_BY_NAME) {
						continue;
					}
					sb.append("\n")
							.append(obfTotal.api.name())
							.append(", ")
							.append(obfTotal.mapName)
							.append(", ").append(String.format(Locale.US, "%.2f", obfTotal.time / 1e9))
							.append(",").append(String.format(Locale.US, "% d", obfTotal.payloadBytesParsed / 1024))
							.append(", ").append(String.format(Locale.US, "%d", obfTotal.blocksLoaded))
							.append(", ").append(String.format(Locale.US, "%d", obfTotal.objectsLoaded))
							.append(", ").append(String.format(Locale.US, "%d", obfTotal.matchedObjects));
				}
				return sb.toString();
			}

			private static int compareApiForMetricsSummary(BinaryMapIndexReaderApiName left, BinaryMapIndexReaderApiName right) {
				return Integer.compare(getApiMetricsSummaryOrder(left), getApiMetricsSummaryOrder(right));
			}

			private static int getApiMetricsSummaryOrder(BinaryMapIndexReaderApiName api) {
				if (api == BinaryMapIndexReaderApiName.POI_BY_NAME) {
					return 0;
				}
				if (api == BinaryMapIndexReaderApiName.ADDRESS_BY_NAME) {
					return 1;
				}
				return 2;
			}
		}

		private DetailedStringData buildDetailedStringData(int topKObf) {
			String c1 = "API       ", c2 = String.format(Locale.US, "Sub-API / Top-%d OBF                             ", topKObf), c3 = "Time (s)",
					c4 = "Count (O)", c5 = "Volume (KB)", c6 = "Calls",
					c9 = "Payload (KB)", c10 = "Decode (ms)", c11 = "Matcher (ms)",
					c12 = "Blocks", c13 = "Objects", c14 = "Matched", c15 = "Max Obj/Block";

			Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, List<SubStatByAPI>>> rows = new HashMap<>();
			Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, SubStatByAPI>> totalsByApiSubApi = new HashMap<>();
			Map<BinaryMapIndexReaderApiName, SubStatByAPI> totalsByApi = new HashMap<>();
			Map<String, SubStatByAPI> totalsByApiObf = new HashMap<>();
			Set<String> obfNames = new TreeSet<>();
			Set<BinaryMapIndexReaderSubApiName> usedSubApis = new HashSet<>();
			for (StatByAPI apiStats : byApis.values()) {
				if (apiStats == null || apiStats.api == null || apiStats.subApis == null) {
					continue;
				}
				if (apiStats.subApis.isEmpty()) {
					SubStatByAPI apiTotal = totalsByApi.computeIfAbsent(apiStats.api,
							k -> new SubStatByAPI(apiStats.api, null, ""));
					apiTotal.time += apiStats.time;
					apiTotal.bytes += apiStats.bytes;
					apiTotal.calls += apiStats.calls;
					rows.computeIfAbsent(apiStats.api, k -> new HashMap<>());
					continue;
				}
				for (SubStatByAPI st : apiStats.subApis.values()) {
						if (st == null || st.subApi == null) {
							continue;
						}
						usedSubApis.add(st.subApi);
						if (st.mapName != null && !st.mapName.isEmpty()) {
							obfNames.add(st.mapName);
							String apiObfKey = st.api.name() + '|' + st.mapName;
							SubStatByAPI obfTotal = totalsByApiObf.computeIfAbsent(apiObfKey,
									k -> new SubStatByAPI(st.api, st.subApi, st.mapName));
							obfTotal.time += st.time;
							obfTotal.count += st.count;
							obfTotal.bytes += st.bytes;
							obfTotal.payloadBytesParsed += st.payloadBytesParsed;
							obfTotal.decodeTime += st.decodeTime;
							obfTotal.matcherTime += st.matcherTime;
							obfTotal.blocksLoaded += st.blocksLoaded;
							obfTotal.objectsLoaded += st.objectsLoaded;
							obfTotal.matchedObjects += st.matchedObjects;
							obfTotal.maxObjectsPerBlock = Math.max(obfTotal.maxObjectsPerBlock, st.maxObjectsPerBlock);
							obfTotal.calls += st.calls;
						}
						rows.computeIfAbsent(apiStats.api, k -> new HashMap<>())
								.computeIfAbsent(st.subApi, k -> new ArrayList<>())
								.add(st);

						totalsByApiSubApi.computeIfAbsent(apiStats.api, k -> new HashMap<>());
						SubStatByAPI subTotal = totalsByApiSubApi.get(apiStats.api).computeIfAbsent(st.subApi,
								k -> new SubStatByAPI(apiStats.api, st.subApi, ""));
						subTotal.time += st.time;
						subTotal.count += st.count;
						subTotal.bytes += st.bytes;
						subTotal.payloadBytesParsed += st.payloadBytesParsed;
						subTotal.decodeTime += st.decodeTime;
						subTotal.matcherTime += st.matcherTime;
						subTotal.blocksLoaded += st.blocksLoaded;
						subTotal.objectsLoaded += st.objectsLoaded;
						subTotal.matchedObjects += st.matchedObjects;
						subTotal.maxObjectsPerBlock = Math.max(subTotal.maxObjectsPerBlock, st.maxObjectsPerBlock);
						subTotal.calls += st.calls;

						SubStatByAPI apiTotal = totalsByApi.computeIfAbsent(apiStats.api, k ->
								new SubStatByAPI(apiStats.api, st.subApi, ""));
						apiTotal.time += st.time;
						apiTotal.count += st.count;
						apiTotal.bytes += st.bytes;
						apiTotal.payloadBytesParsed += st.payloadBytesParsed;
						apiTotal.decodeTime += st.decodeTime;
						apiTotal.matcherTime += st.matcherTime;
						apiTotal.blocksLoaded += st.blocksLoaded;
						apiTotal.objectsLoaded += st.objectsLoaded;
						apiTotal.matchedObjects += st.matchedObjects;
						apiTotal.maxObjectsPerBlock = Math.max(apiTotal.maxObjectsPerBlock, st.maxObjectsPerBlock);
						apiTotal.calls += st.calls;
				}
			}

			List<BinaryMapIndexReaderApiName> apis = new ArrayList<>(totalsByApi.keySet());
			apis.sort((a, b) -> Long.compare(totalsByApi.get(b).time, totalsByApi.get(a).time));
			return new DetailedStringData(c1, c2, c3, c4, c5, c6, c9, c10, c11, c12, c13, c14, c15,
					rows, totalsByApiSubApi, totalsByApi, apis, new ArrayList<>(obfNames), totalsByApiObf, usedSubApis.size());
		}

		private static String padRight(String value, int width) {
			return String.format("%-" + width + "s", value == null ? "" : value);
		}

		private static String padLeft(String value, int width) {
			return String.format("%" + width + "s", value == null ? "" : value);
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
