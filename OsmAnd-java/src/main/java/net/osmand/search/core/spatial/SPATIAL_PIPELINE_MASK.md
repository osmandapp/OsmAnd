# Spatial pipeline: token mask algebra

Companion to [PR #25534](https://github.com/osmandapp/OsmAnd/pull/25534) (`spatialPipeline` branch).

This branch adds **`SpatialTokenMask`** and fixes **`SpatialStagePipeline`** so multi-object address search can combine text tokens with spatial joins reliably and fast.

## Problem

Given a tokenized query (e.g. `2419 Avenue G Dickinson TX USA`), the pipeline must find OSM objects whose names match tokens **and** intersect geographically, together covering **all** query tokens.

Each candidate object carries a **64-bit mask**:

| Bits | Meaning |
|------|---------|
| 0–1 | Atomic count: `00` none, `11` one POI/building, `01` saturated (two atomics — no more allowed) |
| 2–3 | POI type: `00` other, `01` category, `11` POI |
| 4+ | Per token (2 bits): `00` no match, `01` exact, `10` ambiguous (house number / POI ref) |

Stages: **prepare → single-object hits → pair self-join → partial pairs × area objects** (city, boundary…).

## What changed (vs draft PR)

| Issue | Fix |
|-------|-----|
| Broken `allowedFast` lookup (`0x12`) | Correct lookup `0x22F2` in `SpatialTokenMask.allowed()` |
| Slow / buggy `combine2BitMasks` loop | Loop-free `SpatialTokenMask.combine()` |
| Duplicate query words block pairs (`Philadelphia Philadelphia County`) | `duplicateWordAlternatives()` + `bestAllowedCombine()` in `prepare` / `join` |
| House number ambiguous on both streets (`4 8 ave paterson`) | `expandContestedTokens()` — per-token ownership fork |
| Self-join `(A,A)` for building-only objects | Skip when `objId` equal in stage 2 |
| NPE on building-only bbox | Fallback `mainAtom1` in `SpatialObjectRes` |
| `printTokenTree` wrong token shift | `+ HEADER_BITS` in `SpatialStagePipelineStats.getTokenState` |
| `acceptPairSemantic` disabled | Re-enabled for street×street / POI×POI settings |
| `checkExcluded` concurrent mutation | Iterate snapshot of `masksStats` |

Details: see commit message and `SpatialTokenMask` javadoc.

## How to test

### 1. Unit tests (recommended first)

From repo root:

```bash
./gradlew :OsmAnd-java:test --tests net.osmand.search.core.spatial.SpatialTokenMaskTest
```

Windows:

```bat
gradlew.bat :OsmAnd-java:test --tests net.osmand.search.core.spatial.SpatialTokenMaskTest
```

### 2. Python cross-check (no Java build)

```bash
python OsmAnd-java/src/test/resources/spatial/mask_verify.py
```

Runs exhaustive header checks, 500k random mask pairs, and scenario tests (`Philadelphia`, `paterson`, self-pair).

### 3. Integration / manual queries

Pipeline is on when `SpatialTextSearchSettings.DEV_USE_PIPELINE = true` (default on `spatialPipeline`).

Run `SpatialSearchTestAndDocs.main` with useful flags:

```java
settings.DEV_USE_PIPELINE = true;
settings.MAX_PIPELINE_STAGE_TO_STOP = new int[0]; // do not stop early while debugging
ctx.stats.printLogs = true;
```

Suggested regression queries (uncomment in `SpatialSearchTestAndDocs`):

| Query | Notes |
|-------|-------|
| `4 8 ave paterson` vs `4 ave 8 paterson` | House-number assignment order |
| `2419 Avenue G, Dickinson, 77539 TX USA` | Border / multi-token US address |
| `Philadelphia Philadelphia County Pennsylvania` | Duplicate words |
| `Travessa Santo António Rua Joaquim Ribeiro Carvalho Portugal` | Long multi-street query |

Compare with `DEV_USE_PIPELINE = false` (legacy intersections) if needed.

### 4. Optional: excluded common masks

Very frequent single-token masks (`USA`, `street`…) are dropped from the spatial index when count > `EXCLUDE_MASKS` (8000). To verify nothing is lost:

```java
SpatialStagePipeline.CHECK_EXCLUDED = true;
```

## Files

| File | Role |
|------|------|
| `SpatialTokenMask.java` | Pure mask algebra (allowed, combine, variants) |
| `SpatialStagePipeline.java` | Multi-stage spatial join using masks |
| `SpatialStagePipelineStats.java` | Debug stats (fixed token shift) |
| `SpatialTokenMaskTest.java` | JUnit tests |
| `src/test/resources/spatial/mask_verify.py` | Standalone verifier |

## Merge notes

- Target branch: **`spatialPipeline`** (PR #25534), not `master`.
- Mask header rules subsume part of old `acceptPairSemantic`; street/POI intersection flags remain in Java.
- West/East disambiguation (`57th street` near Central Park) is a **ranking** problem when direction is omitted — pipeline may return both; sort by user location.

## Author

Patch prepared for OsmAnd review — `dmvkmusic@osmand.net`.
