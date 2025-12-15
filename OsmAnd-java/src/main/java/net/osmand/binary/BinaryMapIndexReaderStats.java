package net.osmand.binary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		long lastReq = 0;
		public long totalTime = 0;
		public long totalBytes = 0;
		public int prevResultsSize = 0;
		public String requestWord = "";
		Map<BinaryMapIndexReaderApiName, StatByAPI> byApis = new HashMap<>();
		Map<String, WordSearchStat> wordStats = new HashMap<String, WordSearchStat>();

		
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