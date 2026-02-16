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
	
	public static class SubStatByAPI {
		public final BinaryMapIndexReaderApiName api;
		public final BinaryMapIndexReaderSubApiName subApi;
		public final String mapName;
		private long time = 0, count = 0, calls = 0, bytes = 0;

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
					agg.calls += st.calls;
				}
			}
			List<SubStatByAPI> result = new ArrayList<>(grouped.values());
			result.sort((a, b) -> Long.compare(b.time, a.time));
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

		@Override
		public String toString() {
			String c1 = "API       ", c2 = "Sub-API / Top-2 OBF                       ", c3 = "Time (s)", c4 = "Size ", c5 = "Volume (KB)", c6 = "Calls";

			Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, List<SubStatByAPI>>> rows = new HashMap<>();
			Map<BinaryMapIndexReaderApiName, Map<BinaryMapIndexReaderSubApiName, SubStatByAPI>> totalsByApiSubApi = new HashMap<>();
			Map<BinaryMapIndexReaderApiName, SubStatByAPI> totalsByApi = new HashMap<>();
			for (StatByAPI apiStats : byApis.values()) {
				if (apiStats == null || apiStats.api == null || apiStats.subApis == null) {
					continue;
				}
				if (apiStats.subApis.isEmpty()) {
					SubStatByAPI apiTotal = totalsByApi.computeIfAbsent(apiStats.api, k -> new SubStatByAPI(apiStats.api, null, ""));
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
						rows.computeIfAbsent(apiStats.api, k -> new HashMap<>())
								.computeIfAbsent(st.subApi, k -> new ArrayList<>())
								.add(st);

						totalsByApiSubApi.computeIfAbsent(apiStats.api, k -> new HashMap<>());
						SubStatByAPI subTotal = totalsByApiSubApi.get(apiStats.api).computeIfAbsent(st.subApi,
								k -> new SubStatByAPI(apiStats.api, st.subApi, ""));
						subTotal.time += st.time;
						subTotal.count += st.count;
						subTotal.bytes += st.bytes;
						subTotal.calls += st.calls;

						SubStatByAPI apiTotal = totalsByApi.computeIfAbsent(apiStats.api, k ->
								new SubStatByAPI(apiStats.api, st.subApi, ""));
						apiTotal.time += st.time;
						apiTotal.count += st.count;
						apiTotal.bytes += st.bytes;
						apiTotal.calls += st.calls;
				}
			}

			List<BinaryMapIndexReaderApiName> apis = new ArrayList<>(totalsByApi.keySet());
			apis.sort((a, b) -> Long.compare(totalsByApi.get(b).time, totalsByApi.get(a).time));

			int w1 = c1.length(), w2 = c2.length(), w3 = c3.length(), w4 = c4.length(), w5 = c5.length(), w6 = c6.length();
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
				subApis.sort((a, b) -> Long.compare(bySub.get(b).time, bySub.get(a).time));
				for (BinaryMapIndexReaderSubApiName subApi : subApis) {
					if (subApi == null) {
						continue;
					}
					w2 = Math.max(w2, subApi.name().length());
					List<SubStatByAPI> detail = rows.getOrDefault(api, Collections.emptyMap()).getOrDefault(subApi, Collections.emptyList());
					detail.sort((a, b) -> Long.compare(b.time, a.time));
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append(String.format(Locale.US, "Search stat: time %.3f, bytes % d KB, by APIs:",
					totalTime / 1e9, totalBytes / 1024));
			sb.append("\n");
			sb.append(padRight(c1, w1)).append(", ")
					.append(padRight(c2, w2)).append(", ")
					.append(padRight(c3, w3)).append(", ")
					.append(padRight(c4, w4)).append(", ")
					.append(padRight(c5, w5)).append(", ")
					.append(padRight(c6, w6));

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
						.append(padLeft(String.format(Locale.US, "% d", apiTotal.calls), w6));

				Map<BinaryMapIndexReaderSubApiName, SubStatByAPI> bySub = totalsByApiSubApi.get(api);
				if (bySub == null) {
					continue;
				}
				List<BinaryMapIndexReaderSubApiName> subApis = new ArrayList<>(bySub.keySet());
				subApis.sort((a, b) -> Long.compare(bySub.get(b).time, bySub.get(a).time));
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
							.append(padLeft(String.format(Locale.US, "% d", subTotal.calls), w6));

					List<SubStatByAPI> detail = rows.getOrDefault(api, Collections.emptyMap()).getOrDefault(subApi, Collections.emptyList());
					detail.sort((a, b) -> Long.compare(b.time, a.time));
					for (int i = 0; i < detail.size() && i < 2; i++) {
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
								.append(padLeft(String.format(Locale.US, "% d", st.calls), w6));
					}
				}
			}

			sb.append("\nWords ").append(wordStats);
			return sb.toString();
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