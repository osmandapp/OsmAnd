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
| + ways of one street merged, round 1 re-judged | 59 | 3 |
| + the name term weighted by proximity | 60 | 2 |
| + only the top 10 asserted | 55 | 1 |

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

## What deduplication is allowed to merge

It is a separate question from ranking, and it is answered by a list of pairs that may be united,
not by a distance alone:

| pair | when |
|---|---|
| stop node + stop node (platform, stop position, entrance, junction, bus/tram stop) | same name, within 400 m |
| way + way of one street | same name AND same city, within 2 km - a line's single coordinate says little |
| any other POI + POI | same name, within 30 m |
| street + a bridge, tunnel, viaduct or ford of the same name | within 2 km - it is a piece OF the street, unless it carries a travel rating of its own (the Golden Gate is not road furniture) |
| street + anything else standing on it | never |

The radii are measured, not chosen: 320 m between a camp site and its bus stop and 330 m between
a pass and its platform were both called one place; 218 m between two parcel lockers and 57 m
between two benches were not.

## The rule the fourth round added

> *"they are close to each other so full match is more important"* — twice, on `berlin` and on
> `mugello`.

A per-result score cannot ask whether two candidates are close to EACH OTHER. Multiplying the name
term by proximity has the same effect wherever both are in the same area, and the other half of
the same judgements says it should: `Camping-Freunde Berlin` at 32 km must not beat an unnamed
camping office at 24 km on the strength of the word in its name. So `wName * name * near`, and a
street is worth 0.55 rather than 0.70 - it still stands comfortably above a stop (0.35) and a
platform (0.10), which is the comparison that value exists for.

Two records remain violated and both are worth keeping violated rather than fitted away:

- `berlin` - the artwork literally called "Berlin" against `Gedenkstätte Berliner Mauer` 400 m
  away, elo 2534 against 2722. Every weight that lifts the exact name high enough to beat that
  costs more preferences elsewhere than it wins (0.3 -> 43 satisfied offline, 0.8 -> 39). It needs
  the signal that is still missing: a word that names a KIND against a word that is a name.
- `acqua` - the spring called "Acqua"; the same term, in the other direction.

## Only what a person can see

A judgement is made about two rows on a screen. Asserting it a hundred rows down measures nothing
anybody would ever look at, and lets a deep reshuffle read as a regression - which is how `acqua`
(rows #52 and #102) came to be reported as broken. An order preference is now asserted only when
at least one of its two objects is inside the top 10; the rest are counted as `below the top 10`.
The reviewer's rule, in his words: *"I am only interested in changes in the top 10"*.

## The name term was comparing the query with itself

`liechtenstein` returned the country 22nd, behind three museums, a souvenir shop and a
defibrillator. The cause was not a weight: `nameScore` compared the queried words against
`atom.name`, and an atom carries the matched TOKEN, not the object's name - so
"Liechtensteinisches Landesmuseum Vaduz" scored NAME_EXACT for the query "liechtenstein", exactly
like the country. Every single-word query scored 1.00 for everything, which is why raising the
name weight always made things worse: it was amplifying noise.

With the term reading the object's name, three more things follow:

- an exact whole-name match earns a bonus, but only for an object notable enough to own the name
  (a wikidata article or a travel rating above the floor) - otherwise "Supermarkt" would outrank
  the nearer supermarket, which the reviewer rejected;
- `wiki_place` is a landmark: the type exists because the object has an article;
- a country or a region stored as a POI is a place, not a POI - the map has no address record for
  Liechtenstein at all, only `country;wiki_place`;
- and our own disambiguation suffix ("4th Avenue (Manhattan)", added when a street is united with
  its district) is stripped before comparing, or it would make the street a worse match for its
  own name.

## The one-object tier is a property of a result, never a test on a pair

`objs.size` decides above the score, and it needed two exceptions: a stop named "Amsterdam,
Beethovenstraat" must not take the top row from the street, and "Lombardi's Pizza in New York"
must not lose to every "<x> New York Pizza" (issue #25021). Written as conditions on the PAIR -
"skip this tier if either side is ..." - they make the comparator non-transitive, and
`Collections.sort` threw `IllegalArgumentException: Comparison method violates its general
contract!` on the first list large enough to trip TimSort's check:

    X = "Made in New York Pizza"        one object
    Y = "Lombardi's Pizza" + New York   two, exempt
    Z = "<something> Pizza" + a street  two, not exempt

    X vs Y -> exempt, score decides -> Y first
    Y vs Z -> same count, score decides -> Z first
    X vs Z -> tier applies -> X first        ... and Y > X > Z > Y

Both exceptions are now a single per-result number, `answerParts()`: "<object> in <city>" counts
as one when the city was named by its own name, a subordinate node never counts as one, and the
tier compares two integers. A total order cannot have a cycle.
