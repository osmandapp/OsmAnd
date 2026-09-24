package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;

import net.osmand.shared.util.collections.KTIntArrayList;
import net.osmand.shared.util.collections.KTroveHashOrder;

import org.junit.Test;

import java.util.Random;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * {@link KTroveHashOrder} against the trove OsmAnd-java runs on: maps filled with random keys, with
 * repeats, negative keys and sizes that make the table grow several times, walked by trove's own
 * iterator.
 */
public class TroveHashOrderCompatTest {

	@Test
	public void keysComeOutInTroveOrder() {
		Random random = new Random(24092026L);
		for (int round = 0; round < 3000; round++) {
			int size = random.nextInt(round < 2000 ? 400 : 5000);
			int range = 1 << (1 + random.nextInt(30));
			boolean negative = random.nextInt(4) == 0;
			TIntObjectHashMap<String> map = new TIntObjectHashMap<>();
			KTIntArrayList order = new KTIntArrayList();
			for (int i = 0; i < size; i++) {
				int key = random.nextInt(range) - (negative ? range / 2 : 0);
				map.put(key, "");
				order.add(key);
			}
			int[] trove = new int[map.size()];
			TIntObjectIterator<String> it = map.iterator();
			int n = 0;
			while (it.hasNext()) {
				it.advance();
				trove[n++] = it.key();
			}
			assertArrayEquals("round " + round, trove, KTroveHashOrder.INSTANCE.intObjectMapKeys(order));
		}
	}
}
