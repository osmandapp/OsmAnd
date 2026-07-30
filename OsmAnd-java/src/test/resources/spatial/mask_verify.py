# -*- coding: utf-8 -*-
"""
Verification harness for OsmAnd PR #25534 (spatialPipeline) token-mask algebra.

Contains bit-exact ports of the REFERENCE implementation from the PR branch
(SpatialStagePipeline.SpatialObjectRes) and the PROPOSED fixed/new logic.
Runs exhaustive + randomized equivalence tests and scenario tests.

Mask layout (64 bits):
  bits 0-1  atomic header: 00 none, 11 one atomic object, 01 two atomic (saturated)
  bits 2-3  poi header:    00 other, 11 poi, 01 poi category
  bits 4+   2 bits per query token: 00 no match, 01 exact, 10 ambiguous (building/poi ref)
"""

import random
import sys

M64 = (1 << 64) - 1
TOKEN_LOW = 0x5555_5555_5555_5550        # low bit of every 2-bit token field
HDR_TOK = 0x5555_5555_5555_555F          # token low bits + whole header nibble
HEADER_BITS = 4
MAX_TOKENS = 30


def not64(x):
    return (~x) & M64


# =====================================================================
# REFERENCE: bit-exact ports from PR branch SpatialStagePipeline.java
# =====================================================================

def ref_allowed_slow(m1, m2):
    i = m1 & m2 & HDR_TOK
    if (i & 3) == 1:
        return False
    if ((i >> 2) & 3) == 1:
        return False
    return (i >> 4) == 0


def ref_allowed_fast_legacy(m1, m2):
    """PR's allowedFast with the 0x12 lookup constant (currently unused in PR)."""
    i = m1 & m2 & HDR_TOK
    return i < 16 and ((0x12 >> i) & 1) == 0


def ref_combine_loop(m1, m2, total_tokens):
    """PR's combine2BitMasks: per-token loop + header merge."""
    b1 = m1 & 3
    b2 = m2 & 3
    if b1 == 0:
        ca = b2
    elif b2 == 0:
        ca = b1
    else:
        ca = 1 if (b1 == 3 and b2 == 3) else 2   # NB: 2 (=10) is an undefined atomic state
    p1 = (m1 >> 2) & 3
    p2 = (m2 >> 2) & 3
    if p1 == 0:
        cp = p2
    elif p2 == 0:
        cp = p1
    else:
        cp = 3 if (p1 == 3 and p2 == 3) else 1
    res = (cp << 2) + ca
    for i in range(total_tokens):
        s = i * 2 + 4
        s1 = (m1 >> s) & 3
        s2 = (m2 >> s) & 3
        if s1 == 1 or s2 == 1:
            f = 1
        elif s1 == 2 or s2 == 2:
            f = 2
        else:
            f = 0
        res |= f << s
    return res


def ref_count_covered(mask):
    return bin((mask | (mask >> 1)) & TOKEN_LOW).count('1')


# =====================================================================
# PROPOSED: fixed / new logic (mirrors SpatialTokenMask.java deliverable)
# =====================================================================

def build_forbidden_header():
    lookup = 0
    for i in range(16):
        if (i & 3) == 1 or ((i >> 2) & 3) == 1:
            lookup |= 1 << i
    return lookup


FORBIDDEN_HEADER = build_forbidden_header()   # expected 0x22F2


def new_allowed(m1, m2):
    i = m1 & m2 & HDR_TOK
    if (i >> HEADER_BITS) != 0:
        return False
    return ((FORBIDDEN_HEADER >> i) & 1) == 0


def new_combine(m1, m2):
    """Loop-free combine: exact wins over ambiguous wins over no-match."""
    orv = m1 | m2
    exact = orv & TOKEN_LOW
    amb = ((orv >> 1) & TOKEN_LOW) & not64(exact)
    tokens = exact | ((amb << 1) & M64)
    a1 = m1 & 3
    a2 = m2 & 3
    atomic = a2 if a1 == 0 else (a1 if a2 == 0 else 1)   # saturate to ATOMIC_TWO (01)
    p1 = (m1 >> 2) & 3
    p2 = (m2 >> 2) & 3
    poi = p2 if p1 == 0 else (p1 if p2 == 0 else (3 if (p1 == 3 and p2 == 3) else 1))
    return atomic | (poi << 2) | tokens


def set_token(mask, idx, state):
    shift = idx * 2 + HEADER_BITS
    return (mask & not64(3 << shift)) | ((state & 3) << shift)


def get_token(mask, idx):
    return (mask >> (idx * 2 + HEADER_BITS)) & 3


def exact_low_bits(mask):
    return mask & not64(mask >> 1) & TOKEN_LOW


def amb_low_bits(mask):
    return (mask >> 1) & not64(mask) & TOKEN_LOW


def count_covered(mask):
    return bin((mask | (mask >> 1)) & TOKEN_LOW).count('1')


MAX_FORK_TOKENS = 4


def expand_contested(m1, m2, max_fork=MAX_FORK_TOKENS):
    """Per-token ownership fork for tokens ambiguous on BOTH sides.

    Returns list of (resolved1, resolved2). Tokens ambiguous on one side only
    stay where they are (resolved downstream by building calculation).
    """
    contested = amb_low_bits(m1) & amb_low_bits(m2)
    if contested == 0:
        return [(m1, m2)]
    fork_bits = []
    rest = contested
    while rest and len(fork_bits) < max_fork:
        b = rest & (-rest) & M64
        fork_bits.append(b)
        rest &= rest - 1
    out = []
    for v in range(1 << len(fork_bits)):
        drop1 = 0
        drop2 = 0
        for j, b in enumerate(fork_bits):
            if (v >> j) & 1:
                drop1 |= b          # token goes to side 2
            else:
                drop2 |= b          # token goes to side 1
        r1 = m1 & not64(drop1 | (drop1 << 1))
        r2 = m2 & not64(drop2 | (drop2 << 1))
        out.append((r1, r2))
    return out


def duplicate_word_alternatives(mask, duplicate_groups, cap=8):
    """Alternatives keeping exactly one exact match per duplicate-word group.

    Returns list starting with the original mask. Solves the 'Philadelphia
    Philadelphia County' class: without alternatives two objects that each
    matched both duplicate positions can never pass allowed() together.
    """
    variants = [mask]
    for group in duplicate_groups:
        group_bits = 0
        for t in group:
            group_bits |= 1 << (t * 2 + HEADER_BITS)
        size = len(variants)
        for vi in range(size):
            if len(variants) >= cap:
                break
            v = variants[vi]
            matched = exact_low_bits(v) & group_bits
            if bin(matched).count('1') < 2:
                continue
            rest = matched
            while rest and len(variants) < cap:
                keep = rest & (-rest) & M64
                rest &= rest - 1
                drop = matched & not64(keep)
                variants.append(v & not64(drop | (drop << 1)))
    return variants


def best_allowed_combine(variants1, variants2):
    """Max-coverage allowed combination across variants. None if all forbidden."""
    best = None
    best_cov = -1
    for v1 in variants1:
        for v2 in variants2:
            if not new_allowed(v1, v2):
                continue
            c = new_combine(v1, v2)
            cov = count_covered(c)
            if cov > best_cov:
                best_cov = cov
                best = (c, v1, v2)
    return best


# =====================================================================
# Tests
# =====================================================================

FAILURES = []


def check(name, cond, detail=""):
    if cond:
        print(f"  PASS  {name}")
    else:
        print(f"  FAIL  {name}  {detail}")
        FAILURES.append(name)


def rand_mask(rng, ntok, p_exact=0.18, p_amb=0.12):
    atomic = rng.choice([0, 0, 0, 3, 3, 1])
    poi = rng.choice([0, 0, 0, 3, 1])
    m = atomic | (poi << 2)
    for t in range(ntok):
        r = rng.random()
        if r < p_exact:
            st = 1
        elif r < p_exact + p_amb:
            st = 2
        else:
            st = 0
        m |= st << (t * 2 + 4)
    return m


def test_forbidden_header_lookup():
    print("[1] FORBIDDEN_HEADER lookup vs allowedSlow header logic (exhaustive)")
    check("lookup constant == 0x22F2", FORBIDDEN_HEADER == 0x22F2,
          f"got {FORBIDDEN_HEADER:#x}")
    ok = True
    for i in range(16):
        slow_forbidden = (i & 3) == 1 or ((i >> 2) & 3) == 1
        fast_forbidden = ((FORBIDDEN_HEADER >> i) & 1) == 1
        if slow_forbidden != fast_forbidden:
            ok = False
    check("all 16 header intersection values agree", ok)


def test_legacy_fast_is_broken():
    print("[2] Demonstrate the allowedFast 0x12 bug (PR landmine, currently dormant)")
    # occurring header field values: atomic/poi in {00, 01, 11}
    occurring = [0, 1, 3]
    mismatches = []
    for a1 in occurring:
        for p1 in occurring:
            for a2 in occurring:
                for p2 in occurring:
                    m1 = a1 | (p1 << 2)
                    m2 = a2 | (p2 << 2)
                    if ref_allowed_fast_legacy(m1, m2) != ref_allowed_slow(m1, m2):
                        mismatches.append((m1, m2))
    check("legacy allowedFast disagrees with allowedSlow (bug exists)",
          len(mismatches) > 0)
    # practically relevant miss: a stage-3+ pair mask already saturated with
    # 2 atomic objects (atomic field 01) must not combine with another atomic,
    # header intersection i=5 -- legacy 0x12 allows it, slow forbids it
    saturated_pair = 0x1 | (0x1 << 2)   # atomic 01 + poi category 01
    atomic_cat = 0x3 | (0x1 << 2)       # atomic 11 + poi category 01
    check("legacy fast wrongly ALLOWS third atomic into a saturated pair (i=5)",
          ref_allowed_fast_legacy(saturated_pair, atomic_cat)
          and not ref_allowed_slow(saturated_pair, atomic_cat))
    print(f"        {len(mismatches)} mismatching header pairs, e.g. "
          + ", ".join(f"({a:#x},{b:#x})" for a, b in mismatches[:5]))


def test_allowed_equivalence():
    print("[3] new_allowed == allowedSlow")
    # exhaustive: all 16 headers x all token-state pairs on 2 tokens
    states = [0, 1, 2]
    masks = []
    for hdr in range(16):
        for s0 in states:
            for s1 in states:
                masks.append(hdr | (s0 << 4) | (s1 << 6))
    ok = all(new_allowed(m1, m2) == ref_allowed_slow(m1, m2)
             for m1 in masks for m2 in masks)
    check(f"exhaustive small ({len(masks)}^2 pairs)", ok)

    rng = random.Random(42)
    bad = 0
    for _ in range(200_000):
        m1 = rand_mask(rng, 30)
        m2 = rand_mask(rng, 30)
        if new_allowed(m1, m2) != ref_allowed_slow(m1, m2):
            bad += 1
    check("randomized 200k pairs (30 tokens)", bad == 0, f"{bad} mismatches")


def test_combine_equivalence():
    print("[4] new_combine (loop-free) vs PR combine2BitMasks loop")
    rng = random.Random(7)
    token_diff = 0
    full_diff_on_allowed = 0
    invalid_atomic_ref = 0
    for _ in range(200_000):
        m1 = rand_mask(rng, 30)
        m2 = rand_mask(rng, 30)
        ref = ref_combine_loop(m1, m2, 30)
        new = new_combine(m1, m2)
        if (ref ^ new) & not64(0xF):
            token_diff += 1
        if ref_allowed_slow(m1, m2) and ref != new:
            full_diff_on_allowed += 1
        if (ref & 3) == 2:
            invalid_atomic_ref += 1
    check("token area identical on all pairs", token_diff == 0,
          f"{token_diff} diffs")
    check("full mask identical on all allowed() pairs",
          full_diff_on_allowed == 0, f"{full_diff_on_allowed} diffs")
    check("PR loop emits undefined atomic state 10 on forbidden pairs (bug exists)",
          invalid_atomic_ref > 0)
    # new_combine never emits undefined atomic state
    rng2 = random.Random(8)
    ok = True
    for _ in range(50_000):
        c = new_combine(rand_mask(rng2, 30), rand_mask(rng2, 30))
        if (c & 3) == 2:
            ok = False
            break
    check("new_combine atomic header always in {00, 01, 11}", ok)


def test_count_covered():
    print("[5] countCoveredTokens vs naive per-token loop")
    rng = random.Random(11)
    ok = True
    for _ in range(100_000):
        m = rand_mask(rng, 30)
        naive = sum(1 for t in range(MAX_TOKENS) if get_token(m, t) != 0)
        if count_covered(m) != naive or ref_count_covered(m) != naive:
            ok = False
            break
    check("100k random masks", ok)


def test_duplicate_words_philadelphia():
    print("[6] Duplicate query words: 'Philadelphia Philadelphia County'")
    # t0=philadelphia t1=philadelphia t2=county
    city = set_token(set_token(0, 0, 1), 1, 1)            # exact t0, t1
    county = set_token(set_token(set_token(0, 0, 1), 1, 1), 2, 1)  # exact t0,t1,t2

    check("naive main x main pairing is forbidden (the current PR behavior)",
          not new_allowed(city, county))

    city_vars = duplicate_word_alternatives(city, [[0, 1]])
    county_vars = duplicate_word_alternatives(county, [[0, 1]])
    check("city gets 2 alternatives (keep t0 / keep t1)", len(city_vars) == 3,
          f"got {len(city_vars)}")
    best = best_allowed_combine(city_vars, county_vars)
    check("variant pairing exists", best is not None)
    if best:
        combined, v1, v2 = best
        check("variant pairing covers all 3 tokens", count_covered(combined) == 3,
              f"covered {count_covered(combined)}")
        # each duplicate position consumed by exactly one side
        overlap = exact_low_bits(v1) & exact_low_bits(v2)
        check("no token consumed by both sides", overlap == 0)


def test_contested_house_numbers():
    print("[7] Contested ambiguous tokens: '4 8 ave paterson' class")
    # t0='4' t1='8' t2='ave' t3='paterson'
    # side A: street '4th Street' (t0 exact) with building ref 8 (t1 ambiguous)
    side_a = set_token(set_token(0, 0, 1), 1, 2)
    # side B: street 'Paterson Avenue' (t2,t3 exact) with building ref 8 (t1 ambiguous)
    side_b = set_token(set_token(set_token(0, 2, 1), 3, 1), 1, 2)

    check("pair is allowed (ambiguous overlap is not exact overlap)",
          new_allowed(side_a, side_b))
    combined = new_combine(side_a, side_b)
    check("combined covers all 4 tokens", count_covered(combined) == 4)
    check("token '8' stays ambiguous in combined mask", get_token(combined, 1) == 2)

    variants = expand_contested(side_a, side_b)
    check("exactly 2 ownership variants for 1 contested token", len(variants) == 2,
          f"got {len(variants)}")
    owners = set()
    for r1, r2 in variants:
        a_owns = get_token(r1, 1) == 2 and get_token(r2, 1) == 0
        b_owns = get_token(r2, 1) == 2 and get_token(r1, 1) == 0
        check("each variant assigns '8' to exactly one side", a_owns != b_owns)
        owners.add("A" if a_owns else "B")
        # non-contested tokens untouched
        check("non-contested tokens untouched",
              get_token(r1, 0) == 1 and get_token(r2, 2) == 1 and get_token(r2, 3) == 1)
    check("both ownership options produced", owners == {"A", "B"})

    # no contest -> identity
    plain = expand_contested(side_a, set_token(0, 2, 1))
    check("no contested tokens -> single identity variant",
          plain == [(side_a, set_token(0, 2, 1))])

    # cap: 5 contested tokens -> fork 4, leave 1 shared
    m1 = 0
    m2 = 0
    for t in range(5):
        m1 = set_token(m1, t, 2)
        m2 = set_token(m2, t, 2)
    capped = expand_contested(m1, m2)
    check("fork capped at 2^4 variants", len(capped) == 16, f"got {len(capped)}")
    check("uncapped token stays ambiguous on both sides",
          all(get_token(r1, 4) == 2 and get_token(r2, 4) == 2 for r1, r2 in capped))


def test_self_pair():
    print("[8] Self-pair behavior (why stage-2 self-join needs an id skip)")
    exact_obj = set_token(0, 0, 1)
    amb_obj = set_token(0, 0, 2) | 3   # atomic building matched only ambiguously
    check("exact-token object cannot pair with itself", not new_allowed(exact_obj, exact_obj))
    check("ambiguous-only object CAN pair with itself (mask does not prevent it)",
          new_allowed(amb_obj, amb_obj))


def main():
    print("=" * 70)
    print("OsmAnd PR #25534 mask algebra verification")
    print("=" * 70)
    test_forbidden_header_lookup()
    test_legacy_fast_is_broken()
    test_allowed_equivalence()
    test_combine_equivalence()
    test_count_covered()
    test_duplicate_words_philadelphia()
    test_contested_house_numbers()
    test_self_pair()
    print("=" * 70)
    if FAILURES:
        print(f"RESULT: {len(FAILURES)} FAILURES: {FAILURES}")
        sys.exit(1)
    print("RESULT: ALL CHECKS PASSED")


if __name__ == "__main__":
    main()
