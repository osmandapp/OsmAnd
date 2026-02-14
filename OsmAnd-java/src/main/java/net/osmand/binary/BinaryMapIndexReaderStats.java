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
		POI_NAME_OBJECTS
	}
	
	public static class SubStatByAPI {
		public final BinaryMapIndexReaderApiName api;
		public final BinaryMapIndexReaderSubApiName subApi;
		public final String mapName;
		private long time = 0, size = 0, count = 0, bytes = 0;

		SubStatByAPI(BinaryMapIndexReaderApiName api, BinaryMapIndexReaderSubApiName subApi, String mapName) {
			this.api = api;
			this.subApi = subApi;
			this.mapName = mapName;
		}

		void add(long timeNs, long size, long bytes) {
			this.time += timeNs;
			this.size += size;
			this.bytes += bytes;
			count++;
		}

		public long getTime() {
			return time;
		}

		public long getBytes() {
			return bytes;
		}

		public long getCount() {
			return count;
		}

		public long getSize() {
			return size;
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

		public List<SubStatByAPI> getSummary() {
			Map<String, SubStatByAPI> grouped = new HashMap<>();
			for (SubStatByAPI st : subApis.values()) {
				if (st == null || st.mapName == null) {
					continue;
				}
				String key = st.subApi.name() + '|' + st.mapName;
				SubStatByAPI agg = grouped.computeIfAbsent(key, k -> new SubStatByAPI(api, st.subApi, st.mapName));
				agg.time += st.time;
				agg.size += st.size;
				agg.bytes += st.bytes;
				agg.count += st.count;
			}
			List<SubStatByAPI> result = new ArrayList<>(grouped.values());
			result.sort((a, b) -> Long.compare(b.time, a.time));
			return result;
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
					agg.size += st.size;
					agg.bytes += st.bytes;
					agg.count += st.count;
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
			subStart = System.nanoTime();
			subSize = size;
			return subStart;
		}

		public void endSubSearchStats(long statReq, BinaryMapIndexReaderApiName api, BinaryMapIndexReaderSubApiName op, String obf, int size, long bytes) {
			if (statReq != subStart) {
				System.err.println("ERROR: in sub search stats counting to fix ! " + statReq + " != " + subStart);
			}
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