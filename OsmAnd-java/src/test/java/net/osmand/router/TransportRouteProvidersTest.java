package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.osmand.data.LatLon;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportRouteProvidersTest {

	private static final LatLon START = new LatLon(40.0, -74.0);
	private static final LatLon END = new LatLon(40.1, -74.1);
	private static final long NOW = 1_700_000_000L;

	private final TransportRoutingConfiguration cfg = new TransportRoutingConfiguration(null, new HashMap<>());
	private final RouteCalculationProgress progress = new RouteCalculationProgress();
	private final TransportRouteProviders providers = new TransportRouteProviders(() -> NOW);

	/** Records its calls and answers with a fixed value. */
	private static class StubProvider implements TransportRouteProvider {
		final List<TransportRouteResult> answer;
		final List<TransportRouteRequest> requests = new ArrayList<>();

		StubProvider(List<TransportRouteResult> answer) {
			this.answer = answer;
		}

		@Override
		public List<TransportRouteResult> calculateRoutes(TransportRouteRequest request) {
			requests.add(request);
			return answer;
		}
	}

	private TransportRouteResult result() {
		return new TransportRouteResult(cfg);
	}

	private List<TransportRouteResult> calculate(Long departure) throws Exception {
		return providers.calculate(START, END, departure, new HashMap<String, String>(), cfg, progress);
	}

	@Test
	public void noProviderMeansNoAnswerAndIsEmpty() throws Exception {
		assertTrue(providers.isEmpty());
		assertNull(calculate(null));
	}

	@Test
	public void providerResultIsUsed() throws Exception {
		List<TransportRouteResult> answer = Collections.singletonList(result());
		StubProvider p = new StubProvider(answer);
		providers.add(p);
		assertSame(answer, calculate(null));
		assertEquals(1, p.requests.size());
	}

	@Test
	public void nullAndEmptyFallThrough() throws Exception {
		StubProvider returnsNull = new StubProvider(null);
		StubProvider returnsEmpty = new StubProvider(new ArrayList<TransportRouteResult>());
		providers.add(returnsNull);
		providers.add(returnsEmpty);
		assertNull(calculate(null));
		assertEquals(1, returnsNull.requests.size());
		assertEquals(1, returnsEmpty.requests.size());
	}

	@Test
	public void firstNonEmptyAnswerWinsAndLaterProvidersAreNotAsked() throws Exception {
		List<TransportRouteResult> answer = Arrays.asList(result(), result());
		StubProvider none = new StubProvider(null);
		StubProvider first = new StubProvider(answer);
		StubProvider later = new StubProvider(Collections.singletonList(result()));
		providers.add(none);
		providers.add(first);
		providers.add(later);
		assertSame(answer, calculate(null));
		assertEquals(0, later.requests.size());
	}

	@Test
	public void requestCarriesInputsAndResolvesDepartureNow() throws Exception {
		StubProvider p = new StubProvider(null);
		providers.add(p);
		Map<String, String> params = new HashMap<>();
		params.put("a", "b");
		providers.calculate(START, END, null, params, cfg, progress);
		providers.calculate(START, END, 12345L, params, cfg, progress);
		TransportRouteRequest now = p.requests.get(0);
		TransportRouteRequest fixed = p.requests.get(1);
		assertSame(START, now.getStart());
		assertSame(END, now.getEnd());
		assertSame(cfg, now.getConfig());
		assertSame(progress, now.getProgress());
		assertEquals("b", now.getParams().get("a"));
		assertEquals(NOW, now.getDepartureTimeSec());
		assertEquals(12345L, fixed.getDepartureTimeSec());
	}

	@Test
	public void sameResolvedDepartureReachesEveryProvider() throws Exception {
		StubProvider first = new StubProvider(null);
		StubProvider second = new StubProvider(null);
		providers.add(first);
		providers.add(second);
		calculate(null);
		calculate(0L);
		assertEquals(NOW, first.requests.get(0).getDepartureTimeSec());
		assertEquals(NOW, second.requests.get(0).getDepartureTimeSec());
		// 0 is a real time (the epoch), not a synonym of "now"
		assertEquals(0L, first.requests.get(1).getDepartureTimeSec());
		assertEquals(0L, second.requests.get(1).getDepartureTimeSec());
	}

	@Test
	public void requestParamsAreReadOnly() throws Exception {
		StubProvider p = new StubProvider(null);
		providers.add(p);
		calculate(null);
		try {
			p.requests.get(0).getParams().put("x", "y");
			fail("expected UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {
			// ok
		}
	}

	@Test
	public void cancelledCalculationAsksNobodyElse() throws Exception {
		StubProvider first = new StubProvider(null) {
			@Override
			public List<TransportRouteResult> calculateRoutes(TransportRouteRequest request) {
				request.getProgress().isCancelled = true;
				return super.calculateRoutes(request);
			}
		};
		StubProvider second = new StubProvider(Collections.singletonList(result()));
		providers.add(first);
		providers.add(second);
		assertNull(calculate(null));
		assertEquals(0, second.requests.size());
	}

	@Test
	public void exceptionsPropagate() throws Exception {
		providers.add(request -> {
			throw new IOException("index broken");
		});
		StubProvider after = new StubProvider(Collections.singletonList(result()));
		providers.add(after);
		try {
			calculate(null);
			fail("expected IOException");
		} catch (IOException expected) {
			assertEquals("index broken", expected.getMessage());
		}
		assertEquals(0, after.requests.size());
	}

	@Test
	public void addIgnoresNullAndDuplicatesAndRemoveWorks() throws Exception {
		StubProvider p = new StubProvider(null);
		providers.add(null);
		assertTrue(providers.isEmpty());
		providers.add(p);
		providers.add(p);
		calculate(null);
		assertEquals(1, p.requests.size());
		providers.remove(p);
		assertTrue(providers.isEmpty());
		assertNull(calculate(null));
		assertEquals(1, p.requests.size());
	}
}
