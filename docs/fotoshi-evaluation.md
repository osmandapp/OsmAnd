# Fotoshi gallery integration

Selecting an OSM-backed POI adds up to five Fotoshi photos to the existing online-photo row, grid and full-screen viewer. No new dependencies, screens, map layers or OBF attributes are required.

## Request and attribution

`MenuBuilder` uses `ObfConstants` to decode an Amenity into its original typed OSM ID. Other object classes are excluded because their IDs do not necessarily represent original OSM objects. There is no radius fallback or inferred association.

```http
GET https://api.fotoshi.com/v1/data/photos?osm_id=w204781910&limit=5&photo_size=thumbnail,1280
Authorization: Bearer <public OsmAnd integration key>
```

The Android adapter makes one HTTPS request, in parallel with existing photo providers. Existing-provider results can display before Fotoshi finishes; a final snapshot merges the results. Cancellation suppresses further UI delivery. Connect and read timeouts are five seconds each, redirects are disabled, and errors leave other providers available. Headers and response bodies are not logged.

The shared parser produces existing `MediaItem.Remote` objects in the `OTHER` group. It preserves creator, uploader, source institution, license and capture date when supplied, deduplicating identical credits. Rows without credit, license or required HTTPS assets are skipped. Details and browser actions open the canonical Fotoshi photo page; downloads use the rendition asset. Date-only values are not assigned invented timestamps.

## Public integration credential

The adapter includes a dedicated public OsmAnd integration key. It is intentionally distributable in source and APKs and is not an administrative secret. No developer account login is required by app users. A repository secret would not hide a credential included in a distributed APK.

Fotoshi enforces the following server-side policy:

- Exact typed OSM-ID photo lookup only, with thumbnail and 1280px renditions.
- At most 30 photos per request; this client requests and displays five.
- No pagination, spatial searches, bulk filters or broader Data/STAC access.
- Independent integration-wide quotas, initially 300 requests/minute, 10,000 requests/UTC day and 50,000 returned photos/UTC day. These limits are shared by all OsmAnd installations, not per device.
- The integration can be disabled, restricted or have its quotas adjusted server-side. Credential rotation requires updating the client.

The key identifies the integration; it does not authenticate an individual installation or prevent other clients from copying it. Exhausted limits or a disabled/revoked key cause Fotoshi photos to be unavailable while other providers continue working. No account, administrative credentials or backend implementation code is included in this patch.

As with other online providers, fetching photos sends a request to an external service. Fotoshi receives the selected OSM ID and the connection's IP address. This adapter adds no account, email, device identifier or GPS coordinates to its request. Existing online-photo enablement and connectivity controls remain applicable. Maintainers should review provider enablement and these shared limits before release.

## Verification

- Eight shared parser tests cover typed identity, malformed responses, required credit/assets, deduplication and the five-photo cap, capture dates, combined attribution and canonical details/browser links.
- Ten gallery-loader tests cover existing behavior, incremental/final results and cancellation.
- The public-key adapter passed the eighteen parser/gallery-loader tests and the external request harness: invalid IDs make no request; five-photo parsing, Authorization headers, redirect refusal, timeouts and HTTP/offline failures passed. Live calls through this adapter returned five museum photos, one library photo and zero for wrong-type/nonexistent IDs. Incremental delivery and cancelled-listener checks also passed.
- The final public-key adapter passed Android SDK 35 compilation and full app Kotlin/Java compile tasks for `nightlyFreeOpenglArm64Debug` (`BUILD SUCCESSFUL in 3m 51s`). ARM64 debug APK assembly and the physical-device run passed for the prior private-key adapter. The latest change was not rerun on a device.
- A physical Pixel 8 running Android 14 loaded the official Azores offline map. An actual map tap on the museum POI opened the normal place menu, gallery and viewer, with five live Fotoshi photos. Its real OBF identity decoded to way 204781910. No POI identity or photo response was seeded in this final device run. This device run preceded the public-key adapter change.
- Live server policy checks returned five photos for a five-photo request and all sixteen available for a thirty-photo request. Over-limit, pagination, bulk-filter, larger-rendition and broader endpoint requests were denied.

Reproduce shared tests in a configured checkout:

```sh
./gradlew :OsmAnd-shared:jvmTest --tests '*FotoshiMediaFactoryTest'
```

Manual check: open [Museu de Angra do Heroismo](https://www.openstreetmap.org/way/204781910), expand Online photos, open a thumbnail and inspect its credit and browser action.

## Validation limits

The broad upstream `OsmAnd-java` suite failed in the evaluation checkout (473 failures, including missing map fixtures and routing/locale assertions). Those failures were not fully diagnosed or counted as passing; the successful app build excluded that task. iOS is not wired up. Pagination is not implemented. Added on-device latency has not been benchmarked.
