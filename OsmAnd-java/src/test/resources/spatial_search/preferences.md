# Search preferences

`preferences.jsonl` records what a person said about search results, so the judgement is not
lost the next time the ranking changes.

## Why this is not another golden file

The corpus in `resources/test-resources/spatial_search/*.json` stores an **outcome** — the whole
ordered result list, with the comparator key and the distance baked into every line:

```
"Home Run Apple [[1, POI artwork;wiki_place, t1+0-w1-oth0-10elo-tp0, 11.84 km, Q29096356 [...]]]"
```

Any ranking change rewrites all of it. When the scoring ranking was tried, 49 of 74 of those
cases changed — not because 49 behaviours regressed, but because 49 snapshots moved, and nobody
had judged a single one of them. A corpus like that cannot say whether a change was good.

A preference stores a **judgement** instead:

> for query Q asked from position L, object B should come before object A

That sentence stays true no matter how the ranker is rewritten. A new preference *adds* a
constraint; it never invalidates an existing one.

## Record format

One JSON object per line, append-only. Two kinds:

```jsonc
// order: which of two objects should come first
{"schema":1,"id":"pref-0023","kind":"order","query":"asciano","lang":"",
 "map":"Italy_toscana_europe_2.obf","lat":43.25,"lon":11.65,
 "a":{"osmId":12161593649,"name":"Asciano Sante Marie Di Sotto",
      "type":"POI Public transport platform","lat":43.2426,"lon":11.5871},
 "b":{"osmId":2444176950,"name":"Asciano","type":"POI Railway station",
      "lat":43.2356,"lon":11.5797},
 "prefer":"b","source":"human:victor","confidence":1.0,"reviewedAt":"2026-09-08",
 "detector":"micro_above_landmark","note":"","conflictsWith":[],"supersedes":null}

// merge: several rows that are one physical place
{"schema":1,"id":"pref-0016","kind":"merge","query":"bölschestr.", ...,
 "objects":[{"osmId":...}, ...],"verdict":"one_row", ...}
```

Rules that keep the file usable:

- **`osmId` is the identity.** Not the rank, not the name, not the position in a list — the only
  handle that survives re-ranking, deduplication and a new map edition. `name`, `type` and the
  coordinates are kept so a record can still be matched, and flagged stale, if an id changes
  upstream.
- **`prefer` is about objects, not about the order they had on review day.** The review UI showed
  "A above B"; the record says "prefer this object", so it does not depend on what the list
  looked like that day.
- **Append-only.** Changed your mind? Add a line with `supersedes` set to the old id. Never edit
  in place — the history of a judgement is worth as much as the judgement.
- **Contradictions are kept, not resolved.** `pref-0037` and `pref-0057` are the same pair
  (`castellina in chianti`) judged both ways by the same person on the same day. They are
  cross-linked through `conflictsWith`, and the test asserts neither side. That is the measured
  noise floor of a human judge, and pretending it away would only hide it.
- **`source`** says who judged: `human:<name>`, `llm:<model>`, `click` (revealed from logs). Keep
  `confidence` for weighting when sources disagree.
- **`detector`** is which automatic rule proposed the pair. Useful for auditing how precise each
  detector is; never treated as a judgement itself.

## Running the check

Maps are not in the repository, so the test skips unless it can find them.

```bash
./gradlew :OsmAnd-java:test --tests "*SpatialSearchPreferencesTest*" \
    -Dosmand.maps.dir=$HOME/osmand/maps
```

It prints one number, comparable across ranker versions:

```
ranking: score
preferences: 25 satisfied, 21 violated, 1 not applicable, 13 not asserted
```

- **satisfied / violated** — the human's statement holds, or the engine contradicts it.
- **not applicable** — one of the objects is not returned at all. A recall problem, not a
  ranking one; counting it here would hide both.
- **not asserted** — `prefer: "either"`, `verdict: "as_is"`, or a contradicted pair.

`-Dosmand.spatial.scoreRanking=false` runs the same preferences against the old lexicographic
comparator, which is what makes the number meaningful:

| ranking | satisfied | violated |
|---|---|---|
| ladder (today) | 28 | 41 |
| score, first weights | 35 | 26 |
| score, weights fitted on the reviews | 52 | 25 |
| + deduplication by name and distance | 56 | 11 |
| + the merge records re-judged against it | 57 | 5 |

(The second row was measured on the 76 records that existed when those weights were written; the
rest on the whole file.) The merge records that stayed violated turned out to be records whose
object list had outgrown the judgement: for `bundesplatz berlin` the stop nodes are now one row, and
what is still separate is a railway station, a park, a tunnel and two streets that happen to share
the name. Re-judged on 2026-09-09, each was replaced (`supersedes`) by one record per group the
engine actually produces, and the answer to the street question came with it: *"we do not merge
[a street with its stops], we merge only stops with each other"*. Two more record kinds of
housekeeping followed: a record replaced by a later one is no longer asserted, and an object that
deduplication absorbed into another row is reported as `absorbed` rather than judged - the
complaint it recorded cannot be evaluated against a row that now stands for something else.

The test gates on `MIN_SATISFIED`, a ratchet: an unrelated reordering cannot break the build,
only contradicting a recorded judgement can. Raise it when a change earns more.

## Where the current records came from

60 judgements made on 2026-09-08 over 300 real queries from the web search log, replayed on
`Germany_berlin_europe_2.obf` and `Italy_toscana_europe_2.obf` from the position each query was
typed from. 38 order records, 22 merge records. The 13 merge records still violated are the
duplicate floods — one stop stored as up to twelve OSM nodes — which the scoring change does not
address; that needs the deduplication fix, which is a separate, exact rule.

16 more on 2026-09-09 (`"batch": "01"`), this time over nine regions — Kyiv, Minsk, Amsterdam,
Berlin, Munich, Paris, Praha, Egypt, Toscana — each query replayed from the position it was
really typed from, so the distance tier means something. They are the round that produced the
current weights, and they say something the first 60 could not:

- between two ordinary POIs the NEARER one wins. An exact name (`Mini Supermarkt` over
  `Supermarkt`), a matching category (`Омега-Київ [clinic]` over `DENIS [hospital]`) and a higher
  elo (a restaurant 24 km away over one 8 km away) all lost to distance.
- a node that only describes a place still loses to the place from 5.5 km closer
  (`Asciano [motorway junction]` 0.4 km against `Asciano [railway station]` 5.9 km).
- merge is about metres, not kinds: rows 9 m and 26 m apart were one place (a wiki place and a
  theatre among them), rows 218 m apart were not.

The one preference the weights cannot satisfy is `supermarkt`, and it names the missing signal:
the engine cannot tell a word for a KIND of object from a proper name, so it cannot know that
carrying "supermarkt" in the name is worth nothing.

10 more on 2026-09-09 (`"batch": "02"`), chosen where the fitted score was least sure. They
settled the rating weight and the merge threshold:

- fame is worth about one point, which is the gap between 3 km and 30 km. It sorts two prominent
  objects of the same kind (`Palazzo Pubblico`, elo 2998 at 80 km, over `Castello di Punta Ala`,
  2329 at 32 km) and does not rescue `Cattedrale di Santa Maria del Fiore` (elo 4049) at 80 km
  from an ordinary church 800 m away. In the reviewer's words: *"it would be different at 20 km,
  but not 75"*. `wRating` 2.0 -> 1.0.
- merge is about metres and not about kinds: 129 m and 147 m (a village and its platform) are one
  row, 218 m are two, 107 m was called either way.

`camping` is the second preference the weights cannot satisfy, and it says the infrastructure
penalty is applied too widely: R1 is a statement about a node AND THE PLACE IT BELONGS TO, but
`typeScore` charges it against every comparison, so a guidepost named "Camping" 8 km away lost to
an office 12 km away.

10 more on 2026-09-09 (`"batch": "03"`), drawn from New York, Los Angeles, San Francisco, Utrecht
and Rotterdam — an address grammar and a POI mix the earlier records had barely seen — plus the
"<street> <city>" shape that the Dutch stop naming breaks. All seven order records were already
satisfied when they were written down, which is what the tier fix was for. Three of them are worth
quoting as rules rather than as records:

- *"city more important than station in same distance threshold 3-7 km"* (`pijnacker`),
- *"closer"* for two streets of the same name 300 m apart (`4th avenue`, `high street`) — the same
  R6 that governs POIs, now for streets,
- and three verdicts that are not about ranking at all: `la amiga`, `rotterdam camping` and
  `amodo lodge` were skipped as *"bad search"* — a recall and a matching problem behind the
  ranking one.

## A third kind of record: `note`

A reviewer may refuse a case, and the reason is worth more than the refusal:

> *"none is good, la probably should not be indexed"* — `la amiga`
> *"bad search: we are not in Rotterdam and they do not match t2"* — `rotterdam camping`

Those are statements about recall and matching, not about order. They are stored as
`"kind": "note"` with `"verdict": "not_a_ranking_question"`, asserted by nothing, and they are the
only record that the case was ever looked at - without them the same pair comes back in the next
sample and costs a minute again.
