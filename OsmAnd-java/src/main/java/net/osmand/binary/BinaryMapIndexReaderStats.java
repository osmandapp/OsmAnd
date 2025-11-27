package net.osmand.binary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import net.osmand.search.core.SearchResult;

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
	
	public static class SearchStat {
		long lastReq = 0;
		public long totalTime = 0;
		public long totalBytes = 0;
		public Map<String, Map<String, Map<String, Integer>>> wordsByApis = new HashMap<>();
		private int prevResultsSize = 0;
		Map<BinaryMapIndexReaderApiName, StatByAPI> byApis = new HashMap<>();

		public long beginSearchStats(BinaryMapIndexReaderApiName api, BinaryIndexPart part, CodedInputStream codedIS, String extraInfo) {
			lastReq = System.nanoTime();
			codedIS.resetBytesCounter();
			return lastReq;
		}

		public void addWordStats(String api, String word, List<SearchResult> requestResults) {
			if (requestResults.isEmpty())
				return;

            Map<String, Map<String, Integer>> wordsCounts = wordsByApis.computeIfAbsent(api, k -> new HashMap<>());
            Map<String, Integer> mapByType = wordsCounts.computeIfAbsent(word, k -> new HashMap<>());
			if (prevResultsSize < requestResults.size())
	            for (SearchResult r : requestResults.subList(prevResultsSize, requestResults.size())) {
	                String typeName = (r != null && r.objectType != null) ? r.objectType.name() : "Unknown";
		            mapByType.compute(typeName, (k, cnt) -> cnt == null ? 1 : cnt + 1);
	            }
			prevResultsSize = requestResults.size();
        }

		public void endSearchStats(long statReq, BinaryMapIndexReaderApiName api, BinaryIndexPart part,
				CodedInputStream codedIS, String extraInfo) {
			if (statReq != lastReq) {
				System.err.println("ERROR: in stats counting to fix ! " + statReq + " != " + lastReq);
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
			return String.format("Search stat: time %.3f, bytes %,d KB, by apis - %s", totalTime / 1e9, totalBytes / 1024,
					byApis.values());
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