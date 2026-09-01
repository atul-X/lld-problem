# DSA Recall Templates — With Explanations

Some algorithms you can **derive** in an interview (BFS, prefix sums). Others you either **recall
or you don't** — the insight took someone a paper to find. This document separates them, and for
each one explains *how it actually works*, so the memorization has something to hang on.

**Tier S** = pure memory. Cannot be re-derived live.
**Tier A** = derivable in principle, but the off-by-ones cost you the round. Memorize verbatim.
**Tier B** = memorize the shape, derive the details.

All code is Java.

---

# TIER S — Pure Recall

## 1. Kadane's — max subarray sum

**The question it answers at every index:** "What is the best subarray *ending exactly here*?"

There are only two candidates: this element alone, or this element appended to the best subarray
ending at `i-1`. Nothing else is possible — any subarray ending at `i` either starts at `i` or
extends one ending at `i-1`. So it's a one-line DP:

```
dp[i] = max(a[i], dp[i-1] + a[i])
```

And since `dp[i]` only needs `dp[i-1]`, you collapse the array to a single variable. The real
intuition is the **discard rule**: the moment your running sum goes negative, it can only hurt
whatever comes next, so you throw it away and restart. That's what `max(a[i], cur + a[i])` encodes —
if `cur` is negative, `a[i]` alone wins. O(n²) brute force becomes O(n) because you never
re-examine a prefix you've already decided to abandon.

```java
int kadane(int[] a) {
    int best = a[0], cur = a[0];
    for (int i = 1; i < a.length; i++) {
        cur  = Math.max(a[i], cur + a[i]);   // extend, or start fresh here
        best = Math.max(best, cur);
    }
    return best;                              // handles all-negative correctly
}
```

**Trap:** initializing `best = 0` implicitly allows the empty subarray, so an all-negative input
returns 0. Start from `a[0]`.

**Max product subarray.** Sum has one accumulator; product needs two. A large *negative* running
product is one negative number away from being the largest positive product, so you can't discard
it. Track both extremes, and when you hit a negative element, the roles swap — the running min
becomes the max candidate:

```java
int maxProduct(int[] a) {
    int best = a[0], curMax = a[0], curMin = a[0];
    for (int i = 1; i < a.length; i++) {
        if (a[i] < 0) { int t = curMax; curMax = curMin; curMin = t; }  // swap on negative
        curMax = Math.max(a[i], curMax * a[i]);
        curMin = Math.min(a[i], curMin * a[i]);
        best = Math.max(best, curMax);
    }
    return best;
}
```

**Circular max subarray.** The answer either doesn't wrap (plain Kadane) or does — and a wrapping
subarray is exactly the complement of a non-wrapping *minimum* subarray. So:
`max(kadane(a), total - minKadane(a))`. If every element is negative, the complement is empty and
that formula returns 0, so special-case it back to `kadane(a)`.

---

## 2. Binary search — all four forms

**Why it works:** you maintain an invariant that the answer lies inside `[lo, hi]`, and every
iteration halves that range while preserving the invariant. The bugs come from mixing two different
conventions (closed vs half-open interval) in the same function. Pick one per form and never improvise.

```java
// (a) Exact match — CLOSED interval [lo, hi]. Loop while the interval is non-empty (lo <= hi).
//     Since mid is definitively ruled out, exclude it: lo = mid+1 or hi = mid-1.
int find(int[] a, int t) {
    int lo = 0, hi = a.length - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;          // never (lo+hi)/2 — overflow
        if (a[mid] == t) return mid;
        if (a[mid] < t) lo = mid + 1; else hi = mid - 1;
    }
    return -1;
}
```

```java
// (b) Lower bound — HALF-OPEN [lo, hi). Invariant: answer is in [lo, hi].
//     mid might BE the answer, so hi = mid (not mid-1). Loop while lo < hi; they converge.
int lowerBound(int[] a, int t) {              // first index with a[i] >= t
    int lo = 0, hi = a.length;
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (a[mid] < t) lo = mid + 1; else hi = mid;
    }
    return lo;                                 // == a.length means "all elements < t"
}
```

Termination is guaranteed because `mid` rounds *down*, so `lo = mid + 1` always advances and
`hi = mid` always shrinks. (If you ever write a form where `lo = mid`, you must round `mid` *up* or
it loops forever — that's the single most common infinite-loop bug.)

**Upper bound** is the same code with `a[mid] <= t`. Together they give you
`count(t) = upperBound(t) - lowerBound(t)` and the insertion point for a new element.

```java
// (d) Binary search on the ANSWER — the form that actually shows up in interviews
int minFeasible(int lo, int hi) {              // predicate must be monotonic: F,F,F,T,T,T
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (feasible(mid)) hi = mid; else lo = mid + 1;
    }
    return lo;                                 // the boundary between F and T
}
```

**How to recognize it:** you're not searching an array, you're searching the *space of possible
answers*. It applies whenever "if X works, then anything larger also works" — that monotonicity is
what makes the answer space look like a sorted boolean array `FFFFTTTT`, and binary search finds the
flip point. The checking function is usually a simple O(n) greedy loop. Phrases that signal it:
"minimize the maximum", "maximum minimum", "smallest capacity / speed / days such that…".
Koko eating bananas, ship packages in D days, split array largest sum, aggressive cows.

**Rotated sorted array.** A rotated array is two sorted runs. At any `mid`, at least one of
`[lo, mid]` and `[mid, hi]` is fully sorted — compare `a[mid]` with `a[lo]` to find which. If the
target falls inside that sorted half's range, search there; otherwise search the other half. You're
still halving each step, so O(log n) holds.

---

## 3. Floyd's cycle detection (tortoise & hare)

**Phase 1 — do they meet?** Fast moves 2, slow moves 1, so the gap between them changes by exactly
1 each step. Once both are inside the cycle, that gap decreases by 1 per step and must eventually hit
0 — they cannot jump past each other. So a cycle guarantees a meeting.

**Phase 2 — why resetting to head finds the entry.** Let `a` = head → cycle entry, `b` = entry →
meeting point, `c` = cycle length. Slow travelled `a + b`; fast travelled twice that, and its extra
distance is whole laps: `a + b + kc = 2(a + b)`, so `a + b = kc`, i.e. **`a = kc - b`**. Starting one
pointer at the head and leaving the other at the meeting point, both moving one step at a time: after
`a` steps the head pointer reaches the entry, and the other one covers `kc - b` from the meeting
point — which is exactly `k` full laps back to the entry. They collide there.

```java
ListNode detectCycleStart(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next; fast = fast.next.next;
        if (slow == fast) {                    // meeting point
            slow = head;
            while (slow != fast) { slow = slow.next; fast = fast.next; }
            return slow;                       // cycle entry
        }
    }
    return null;
}
```

**Find the duplicate number** in `[1..n]` with `n+1` elements: treat `i -> a[i]` as a linked list.
Because values are in `[1..n]` you can never step out of bounds, and because two indices share a
value, two nodes point to the same successor — a cycle whose *entry* is the duplicate. O(n) time,
O(1) space, input array untouched, which is exactly the constraint the problem imposes.

---

## 4. Boyer–Moore majority vote

**The mechanism is cancellation.** Think of it as pairing off one majority element against one
non-majority element and deleting both. If some value occurs more than `n/2` times, there simply
aren't enough other elements to cancel it all — at least one copy survives every possible pairing.
The counter implements this: `count` is "how many unmatched copies of `cand` I'm currently holding",
and when it hits 0, all held copies have been cancelled and you're free to adopt the next element.

```java
int majority(int[] a) {
    int cand = 0, count = 0;
    for (int x : a) {
        if (count == 0) cand = x;
        count += (x == cand) ? 1 : -1;
    }
    return cand;   // valid only if a majority (> n/2) is guaranteed; else verify in a 2nd pass
}
```

**Why a second pass may be needed:** the algorithm always returns *something*. It only guarantees
correctness if a strict majority exists. If that isn't promised, count occurrences of `cand` to confirm.

**n/3 variant:** at most two elements can exceed `n/3`, so you run two independent candidate/counter
pairs and verify both at the end. Generalizes: `n/k` needs `k-1` counters.

---

## 5. Dutch National Flag (3-way partition)

**The invariant is four regions**, and the whole algorithm is about shrinking the unknown one:

```
[0, low)      → all 0s
[low, mid)    → all 1s
[mid, high]   → UNKNOWN  ← every iteration shrinks this by one
(high, n-1]   → all 2s
```

Look at `a[mid]`. If it's 1, it's already in the right region — just widen the 1s by advancing `mid`.
If it's 0, swap it to the boundary of the 0s region; the element you swap *in* came from the 1s
region so it's known to be a 1, and you can safely advance both pointers. If it's 2, swap it to the
2s region — but the element you swap *in* came from the unknown region and has never been examined,
so `mid` must **not** advance.

```java
void sortColors(int[] a) {
    int low = 0, mid = 0, high = a.length - 1;
    while (mid <= high) {
        if (a[mid] == 0)      swap(a, low++, mid++);
        else if (a[mid] == 1) mid++;
        else                  swap(a, mid, high--);   // do NOT advance mid here
    }
}
```

One pass, O(1) space. That asymmetry between the 0-case and the 2-case is the entire trick and the
only thing to memorize.

---

## 6. Cyclic sort — the `[1..n]` family

**The premise:** when values are a permutation of `1..n`, value `v` has a *known* home — index `v-1`.
So instead of comparing elements, you keep throwing each element to where it belongs.

**Why it's O(n) despite the nested-looking loop:** every swap places at least one element into its
final correct position permanently. That can happen at most `n` times. Every other iteration advances
`i`. So total work is bounded by `2n`.

```java
void cyclicSort(int[] a) {
    int i = 0;
    while (i < a.length) {
        int j = a[i] - 1;                                  // where a[i] belongs
        if (a[i] > 0 && a[i] <= a.length && a[i] != a[j]) swap(a, i, j);
        else i++;
    }
}
```

**The guard matters:** comparing `a[i] != a[j]` rather than `i != j` is what makes duplicates
terminate — if the home slot already holds the same value, swapping would loop forever, so you move on.

After the sort, a single scan for `a[i] != i + 1` yields: the missing number, all missing numbers,
the duplicate, all duplicates, or the first missing positive. All O(n) time, O(1) space — which is
usually the constraint that rules out a HashSet and forces this pattern.

---

## 7. Quickselect — k-th element in O(n) average

**The insight:** sorting gives you every rank, but you only asked for one. A partition step tells
you the pivot's *final* rank for free. If that rank is `k`, you're done. If not, the answer lies
strictly on one side — and unlike quicksort, you recurse into **only that side**.

That one-sided recursion is where the complexity comes from: `n + n/2 + n/4 + … = 2n`, so O(n)
expected instead of O(n log n).

```java
int quickSelect(int[] a, int k) {              // k is 0-indexed
    int lo = 0, hi = a.length - 1;
    while (true) {
        int p = partition(a, lo, hi);
        if (p == k) return a[p];
        if (p < k) lo = p + 1; else hi = p - 1;
    }
}
int partition(int[] a, int lo, int hi) {       // Lomuto: i marks the end of the "< pivot" zone
    int pivot = a[hi], i = lo;
    for (int j = lo; j < hi; j++) if (a[j] < pivot) swap(a, i++, j);
    swap(a, i, hi);                            // drop the pivot into its final place
    return i;
}
```

**Worst case O(n²)** when the pivot is always extreme (already-sorted input hits this). Randomize the
pivot — `swap(a, hi, lo + rnd.nextInt(hi - lo + 1))` before partitioning — or the interviewer will
ask. Compare with a size-K heap: heap is O(n log k) but works on streams; quickselect is O(n) but
needs the whole array in memory and mutates it.

---

## 8. LIS in O(n log n) — patience sorting

**What `tails` actually holds:** `tails[i]` = the **smallest possible tail value** of any increasing
subsequence of length `i+1` seen so far. Not the subsequence — just the best ending value.

**Why smallest-tail is the right greedy:** a subsequence ending in 3 can be extended by more future
elements than one ending in 90. Since only the tail matters for extendability, you always want each
length's tail to be as small as possible. Replacing a tail never destroys anything — the length is
already recorded — it only makes future extension easier.

**Why binary search is valid:** `tails` is always sorted ascending (a longer subsequence must end
higher than a shorter prefix of it). So finding where `x` slots in is O(log n). If `x` is bigger than
everything, it extends the longest run by one. Otherwise it overwrites the first tail `>= x`,
improving that length's tail.

```java
int lis(int[] a) {
    int[] tails = new int[a.length];
    int len = 0;
    for (int x : a) {
        int i = Arrays.binarySearch(tails, 0, len, x);
        if (i < 0) i = -(i + 1);               // insertion point = lower bound
        tails[i] = x;
        if (i == len) len++;                   // x extended the longest run
    }
    return len;
}
```

**Critical:** `tails` is **not** the actual LIS — it can end up as a sequence that never existed in
the input. Only `len` is correct. To reconstruct the real subsequence you need a parent-index array.

Strictly increasing → lower bound. Non-decreasing → upper bound.

---

## 9. Next permutation

**Goal:** the next arrangement in lexicographic order, in place, O(n).

The key observation: a suffix that is **non-increasing** is already the largest arrangement of its
elements — nothing after it can be improved. So scan from the right past that suffix to find the
first position `i` that can actually be bumped up (`a[i] < a[i+1]`). That's the **pivot**.

To make the smallest possible increase: swap the pivot with the **smallest value in the suffix that
is still larger than it** — and since the suffix is non-increasing, scanning from the right finds
exactly that value first. After the swap the suffix is still non-increasing, i.e. still maximal, but
now you want it *minimal* — so reverse it. Reversing a non-increasing sequence gives you sorted
ascending, which is the smallest arrangement.

```java
void nextPermutation(int[] a) {
    int n = a.length, i = n - 2;
    while (i >= 0 && a[i] >= a[i + 1]) i--;              // 1. find pivot from the right
    if (i >= 0) {
        int j = n - 1;
        while (a[j] <= a[i]) j--;                        // 2. rightmost value > pivot
        swap(a, i, j);
    }
    reverse(a, i + 1, n - 1);                            // 3. suffix becomes smallest
}
```

If no pivot exists (`i < 0`) the whole array is descending — the last permutation — and reversing
wraps it around to the first. That case falls out of the code for free.

---

## 10. KMP failure function (LPS)

**The problem with naive matching:** on a mismatch you restart the pattern and slide the text pointer
back by one, re-comparing characters you already know match. That's O(n·m).

**The fix:** you already know what the last `len` characters of the text were — they're the pattern's
own prefix. So on a mismatch you never need to touch the text again; you only need to know how much
of the pattern's matched prefix is *also a suffix* of itself. That's a **border**, and `lps[i]` stores
the longest one for `p[0..i]`.

**Building it is the pattern matching itself against itself.** `len` is the current border length. On
a match, the border grows. On a mismatch, you can't drop to 0 — a shorter border might still work, and
the longest such candidate is the border *of the border*, which is `lps[len-1]`. That fallback line is
the whole algorithm, and it's why `i` does not advance there.

```java
int[] buildLPS(String p) {
    int m = p.length(); int[] lps = new int[m];
    int len = 0, i = 1;                                  // lps[0] is always 0
    while (i < m) {
        if (p.charAt(i) == p.charAt(len)) lps[i++] = ++len;
        else if (len > 0) len = lps[len - 1];            // fall back to the border of the border
        else lps[i++] = 0;
    }
    return lps;
}
```

Total O(m): `len` increases at most `m` times, so it can decrease at most `m` times.

Also solves: shortest palindrome, and repeated-substring-pattern via
`n % (n - lps[n-1]) == 0` — because `n - lps[n-1]` is the shortest period of the string.

**Rabin–Karp** trades exactness for simplicity: hash the pattern once, roll a hash across the text so
each window costs O(1) (`hash = (hash*BASE + c) % MOD`, and rolling out the left char subtracts
`old * BASE^(m-1)`). Hash collisions are possible, so **always verify** on a hash match. Its real
advantage is multi-pattern search and 2-D matching.

**Manacher** (all palindromic substrings in O(n)) and the **Z-function** — know what they solve.
Interviewers rarely require the code; expand-around-center O(n²) is an accepted fallback.

---

## 11. Binary exponentiation + modular inverse

**Why it's log:** write the exponent in binary. `b^13 = b^8 · b^4 · b^1` because `13 = 1101₂`. You
generate `b, b², b⁴, b⁸…` by repeated squaring — one multiplication each — and fold a term into the
result only when the corresponding bit is set. That's `log₂(e)` squarings instead of `e` multiplications.

```java
long power(long b, long e, long mod) {
    long r = 1; b %= mod;
    while (e > 0) {
        if ((e & 1) == 1) r = r * b % mod;   // this bit is set → include this power
        b = b * b % mod;                     // advance to the next power of two
        e >>= 1;
    }
    return r;
}
```

Take the mod at every step or you overflow immediately. Use `long` throughout — `int * int` silently
wraps in Java.

**Modular inverse.** Division doesn't exist in modular arithmetic; you multiply by the inverse
instead. By Fermat's little theorem, when `mod` is prime, `a^(mod-1) ≡ 1`, so `a^(mod-2)` is the
inverse: `inv(a) = power(a, mod - 2, mod)`. Needed for nCr under `10⁹+7`.

**Euclid:** `gcd(a,b) = gcd(b, a % b)` — because any common divisor of `a` and `b` also divides
`a % b`, so the set of common divisors is preserved while the numbers shrink fast.
`lcm(a,b) = a / gcd(a,b) * b` — divide **first** or you overflow.

---

## 12. Sieve of Eratosthenes + smallest prime factor

**Why it's near-linear:** every composite number has at least one prime factor, so if you cross out
every multiple of every prime, only primes survive. The work is
`n/2 + n/3 + n/5 + …` which sums to about `n log log n` — effectively linear.

```java
int[] spf = new int[n + 1];
for (int i = 2; i <= n; i++) {
    if (spf[i] == 0)                                    // i was never marked → i is prime
        for (int j = i; j <= n; j += i)
            if (spf[j] == 0) spf[j] = i;                // first marker = smallest prime factor
}
```

Storing *who marked it first* (rather than a boolean) gives you the smallest prime factor, which
factorizes any number in O(log x): `while (x > 1) { int p = spf[x]; ...; x /= p; }`. That upgrade
costs nothing and turns the sieve into a factorization table.

For a plain boolean sieve, the inner loop can start at `i*i` — smaller multiples already had a smaller
factor cross them out. For a single primality test, trial-divide while `i*i <= n`: if `n` had a factor
above its square root, it would need a matching one below.

---

## 13. Reservoir sampling & Fisher–Yates

**Reservoir sampling** solves: pick one item uniformly from a stream whose length you don't know in
advance (and can't store).

**Why 1/i works:** when the i-th item arrives, keep it with probability `1/i`. An item picked at
position `i` survives only if every later item declines to replace it:
`1/i × i/(i+1) × (i+1)/(i+2) × … × (n-1)/n`. The terms telescope to `1/n`. Every item ends up with
identical probability, and you held exactly one item at a time.

```java
T pick(Iterator<T> stream) {
    T res = null; int i = 0;
    while (stream.hasNext()) { T x = stream.next(); if (rnd.nextInt(++i) == 0) res = x; }
    return res;
}
```

**Fisher–Yates** shuffle: walk backward; at each position pick uniformly from the *unshuffled prefix*
`[0..i]` and swap it into place. There are exactly `n!` distinct sequences of random choices and each
produces a distinct permutation, so every permutation is equally likely.

```java
for (int i = a.length - 1; i > 0; i--) swap(a, i, rnd.nextInt(i + 1));
```

**Trap:** using `rnd.nextInt(n)` at every position gives `n^n` equally likely choice-sequences, which
can't distribute evenly over `n!` permutations — a biased shuffle. This is a favourite follow-up.

---

## 14. Bit tricks worth having memorized

```java
n & (n - 1)          // clears the lowest set bit
n & -n               // isolates the lowest set bit
(n & (n - 1)) == 0   // power of two (for n > 0)
```

**Why `n & (n-1)` clears the lowest set bit:** subtracting 1 flips that bit to 0 and turns every
0 below it into 1. AND-ing keeps only the bits both share — everything above is unchanged,
everything at and below is wiped. Looping `n &= n-1` counts set bits in O(popcount) rather than
O(32) — Brian Kernighan's method.

**Why `n & -n` isolates it:** `-n` is `~n + 1` in two's complement, which leaves the lowest set bit
alone and inverts everything above it. The AND leaves exactly that one bit. This is the arithmetic
that makes Fenwick trees work.

**XOR:** `x ^ x = 0` and `x ^ 0 = x`, and XOR is commutative — so XOR-ing an array where every
element appears twice cancels all pairs and leaves the single one.

**Two single numbers:** XOR everything → you get `x ^ y`. Any set bit in that result is a position
where `x` and `y` differ, so pick the lowest with `diff & -diff`, split the array into "has that bit"
and "doesn't", and XOR each group separately. Each single number lands in a different group.

**Enumerate all submasks of a mask** (needed for bitmask DP):
```java
for (int s = mask; s > 0; s = (s - 1) & mask) { /* s is a non-empty submask */ }
```
Subtracting 1 borrows through the lowest set bit; AND-ing with `mask` snaps back to only the bits
you're allowed to use, producing the next smaller submask in descending order.

---

## 15. Morris inorder traversal — O(1) space

**The problem a stack solves:** after descending into a left subtree, you need a way back to the
parent. Morris's answer is to *build that way back into the tree itself* — temporarily.

The inorder predecessor of a node is the rightmost node of its left subtree, and its right pointer is
null (that's what makes it rightmost). So you park a **thread** there pointing back at the current
node. When the left subtree finishes, following that right pointer lands you back on the parent
exactly when you should visit it. Finding a thread already in place is the signal that the left
subtree is done — so you remove it (restoring the tree), visit, and go right.

```java
void morris(TreeNode root) {
    TreeNode cur = root;
    while (cur != null) {
        if (cur.left == null) { visit(cur); cur = cur.right; }
        else {
            TreeNode pre = cur.left;
            while (pre.right != null && pre.right != cur) pre = pre.right;
            if (pre.right == null) { pre.right = cur; cur = cur.left; }   // thread
            else { pre.right = null; visit(cur); cur = cur.right; }       // unthread + visit
        }
    }
}
```

Still O(n): each edge is traversed at most three times. The tree is fully restored at the end, but it
is mutated *during* traversal — which makes it unsafe under concurrent reads, a good thing to mention.
Only ever asked as a follow-up: "can you do it without a stack?"

---

# TIER A — Memorize the template verbatim

## 16. Sliding window (variable size)

**Why two pointers suffice:** the property you're maintaining is monotone in window size — if a
window is invalid, growing it further keeps it invalid. So once `lo` has advanced past a position, it
never needs to come back. Both pointers only move forward, giving O(n) total even though the code
looks nested.

```java
int lo = 0, best = 0;
for (int hi = 0; hi < n; hi++) {
    add(a[hi]);
    while (windowInvalid()) { remove(a[lo]); lo++; }   // while, not if
    best = Math.max(best, hi - lo + 1);
}
```

**`while`, not `if`:** one new element can force multiple removals (e.g. a character whose count now
exceeds the limit may need several shrinks). An `if` silently produces wrong answers on ~30% of inputs.

**The "at most K" trick.** "Exactly K distinct" is *not* monotone — growing a window can push you
from exactly-K to exactly-(K+1) and back — so a plain window can't track it. "At most K" *is*
monotone. So compute both and subtract:

```
exactly(K) = atMost(K) - atMost(K - 1)
```

Every window counted by `atMost(K)` but not by `atMost(K-1)` has exactly K. Used for: subarrays with
exactly K distinct integers, exactly K odd numbers, binary subarrays with sum K.

**Fixed-size window:** add `a[hi]`, and once `hi >= k` remove `a[hi - k]`. No inner loop needed.

---

## 17. Monotonic stack

**What it is really computing:** for each element, the nearest element to the left/right that is
greater/smaller. The stack holds elements that are *still waiting* for their answer, and it stays
sorted because the moment a bigger element arrives, everything smaller below it can never be the
"next greater" for anything further right — it's blocked. So you pop them, and popping is precisely
the moment their answer is known.

Each index is pushed once and popped once → O(n) amortized, despite the inner `while`.

```java
int[] nge = new int[n]; Arrays.fill(nge, -1);
Deque<Integer> st = new ArrayDeque<>();          // indices, values decreasing
for (int i = 0; i < n; i++) {
    while (!st.isEmpty() && a[st.peek()] < a[i]) nge[st.pop()] = a[i];
    st.push(i);
}
```

**Largest rectangle in histogram** — the canonical hard version. For each bar, the largest rectangle
*with that bar as its height* extends left and right until it hits something shorter. When you pop a
bar, both boundaries are known at once: the current index `i` is the first shorter bar on the right,
and the new stack top is the first shorter bar on the left (everything between them was popped
earlier, so it was all taller).

```java
int largestRectangle(int[] h) {
    Deque<Integer> st = new ArrayDeque<>();
    int best = 0, n = h.length;
    for (int i = 0; i <= n; i++) {
        int cur = (i == n) ? 0 : h[i];                       // sentinel flushes the stack
        while (!st.isEmpty() && h[st.peek()] >= cur) {
            int height = h[st.pop()];
            int width  = st.isEmpty() ? i : i - st.peek() - 1;
            best = Math.max(best, height * width);
        }
        st.push(i);
    }
    return best;
}
```

The `width` line is where everyone breaks. `st.isEmpty()` means nothing shorter was ever found on the
left, so the bar extends all the way to index 0 → width `i`. Otherwise the left boundary is
`st.peek()`, exclusive, hence `i - st.peek() - 1`. The `i == n` sentinel of height 0 forces every
remaining bar to pop, so you don't need cleanup code after the loop.

**Maximal rectangle in a binary matrix** = build a histogram of consecutive 1s per row and run this
on each row.

---

## 18. Monotonic deque — sliding window maximum

**Why a deque and not a heap:** a heap can't cheaply remove the element that just fell out of the
window. A deque can, because you only ever need the front.

Two rules maintain it. **Expire:** if the front index has slid out of the window, drop it. **Dominate:**
a new element that's larger than the ones at the back makes them permanently irrelevant — they're both
smaller *and* older, so they'll leave the window first and can never be the maximum while the new one
is present. Drop them. What's left is decreasing, so the front is the window max.

```java
Deque<Integer> dq = new ArrayDeque<>();          // indices, values decreasing
for (int i = 0; i < n; i++) {
    while (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();   // expire
    while (!dq.isEmpty() && a[dq.peekLast()] <= a[i]) dq.pollLast();   // dominate
    dq.offerLast(i);
    if (i >= k - 1) out[i - k + 1] = a[dq.peekFirst()];
}
```

Store **indices**, not values, or you can't tell when something expires.

---

## 19. Prefix sums + hashmap

**The identity:** `sum(l..r) = P[r] - P[l-1]`. So asking "does a subarray ending at `r` sum to `k`?"
becomes "have I previously seen a prefix sum equal to `P[r] - k`?" — a hash lookup. That converts an
O(n²) scan over all `(l, r)` pairs into one pass.

```java
Map<Long, Integer> seen = new HashMap<>();
seen.put(0L, 1);                                  // the empty prefix — always forgotten
long sum = 0; int count = 0;
for (int x : a) {
    sum += x;
    count += seen.getOrDefault(sum - k, 0);
    seen.merge(sum, 1, Integer::sum);
}
```

**Why seed `(0, 1)`:** a subarray starting at index 0 needs `P[l-1] = P[-1] = 0` to exist in the map.
Without it you silently miss every prefix that itself sums to `k`.

**Why not a sliding window here:** windows require monotonicity, which negative numbers destroy —
growing a window can lower the sum. Prefix-sum-plus-map has no such requirement, which is exactly why
it's the tool for "may contain negatives".

Same shape with **prefix XOR** (look up `sum ^ k`), with `sum % k` for "divisible by k" (normalize
with `((sum % k) + k) % k` since Java's `%` keeps the sign), and with a *first-occurrence* map when
you want the **longest** such subarray rather than the count.

**Difference array** — the inverse tool, for many range updates and one final read:
`d[l] += v; d[r+1] -= v;` then a single prefix-sum pass materializes the array. O(1) per update
instead of O(range).

---

## 20. Union-Find (DSU)

**The model:** each set is a tree, and the root is the set's identity. Two elements are connected iff
they have the same root, so `find` is the entire query.

**Path compression** — on the way back up from `find`, point every node directly at the root, so the
next query is O(1). **Union by rank** — always hang the shorter tree under the taller one, so depth
grows only when two equal-height trees merge. Either alone is decent; together they give amortized
near-O(1) (inverse Ackermann, effectively ≤ 4 for any real input).

```java
int[] parent, rank;
int find(int x) { return parent[x] == x ? x : (parent[x] = find(parent[x])); }  // compress
boolean union(int a, int b) {
    int ra = find(a), rb = find(b);
    if (ra == rb) return false;                    // already connected → this edge closes a cycle
    if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
    parent[rb] = ra;
    if (rank[ra] == rank[rb]) rank[ra]++;
    return true;
}
```

Always union the **roots**, never the raw nodes. The `false` return is not a failure — it's how you
detect cycles in an undirected graph and how Kruskal rejects edges.

Uses: connected components, Kruskal's MST, cycle detection, accounts-merge, number-of-islands with
streaming updates. **Limitation to state out loud:** plain DSU cannot undo a union, so it doesn't
handle edge deletion.

---

## 21. Graph traversals

**Kahn's topological sort** is just repeated "remove anything with no remaining dependencies". A node
becomes available the instant its last incoming edge is consumed, which is why you decrement and test
in the same expression.

```java
int[] indeg = new int[n];
for (int u = 0; u < n; u++) for (int v : adj[u]) indeg[v]++;
Queue<Integer> q = new ArrayDeque<>();
for (int i = 0; i < n; i++) if (indeg[i] == 0) q.add(i);
List<Integer> order = new ArrayList<>();
while (!q.isEmpty()) {
    int u = q.poll(); order.add(u);
    for (int v : adj[u]) if (--indeg[v] == 0) q.add(v);
}
boolean hasCycle = order.size() != n;
```

**Why the size check detects cycles:** every node in a cycle has an incoming edge from another node in
that cycle, so none of them ever reaches indegree 0. They're simply never emitted. This makes Kahn's
the natural fit for course-schedule and build-order problems, where you need both the order and the
cycle check.

**Cycle detection, directed, DFS:** three colors — 0 unvisited, 1 in the current recursion stack,
2 fully done. Reaching a node colored 1 means you've looped back onto your own path. A node colored 2
is fine — it's a cross edge to an already-finished branch, not a cycle. Using a plain visited boolean
here is the classic bug.

**Cycle detection, undirected, DFS:** a visited neighbour that isn't your parent means a cycle. You
must skip the parent explicitly, otherwise every single edge looks like a 2-cycle.

**Bipartite check:** BFS while 2-coloring; if you ever reach an already-colored node with the same
color as the current one, an odd cycle exists and the graph isn't bipartite.

---

## 22. Shortest paths

**Why Dijkstra's greed is valid:** pop the smallest tentative distance in the frontier. Because all
edge weights are non-negative, any alternative route to that node would have to pass through some
other frontier node whose distance is already ≥ this one, and then add more non-negative weight. So it
can't be shorter — the popped distance is final. **This argument collapses the moment a negative edge
exists**, which is exactly why Dijkstra fails there.

```java
PriorityQueue<long[]> pq = new PriorityQueue<>((x, y) -> Long.compare(x[1], y[1]));
long[] dist = new long[n]; Arrays.fill(dist, Long.MAX_VALUE);
dist[src] = 0; pq.add(new long[]{src, 0});
while (!pq.isEmpty()) {
    long[] cur = pq.poll();
    int u = (int) cur[0];
    if (cur[1] > dist[u]) continue;                     // stale entry — this line matters
    for (int[] e : adj[u]) {                            // e = {v, w}
        if (dist[u] + e[1] < dist[e[0]]) {
            dist[e[0]] = dist[u] + e[1];
            pq.add(new long[]{e[0], dist[e[0]]});
        }
    }
}
```

**The stale-entry line:** Java's `PriorityQueue` has no decrease-key, so you push duplicates and let
the outdated ones surface later. Skipping them keeps correctness and complexity intact — this is the
standard idiom, not a hack.

**Bellman–Ford** relaxes every edge `V-1` times. Why `V-1`: any shortest path has at most `V-1` edges,
and after round `k` every shortest path using `k` edges is correct. A `V`-th round that still improves
something proves a negative cycle. Rounds also map directly onto "at most K stops" problems — just run
`K+1` rounds over a snapshot of the previous round's distances.

**Floyd–Warshall** is DP over "which vertices am I allowed to route *through*". After iteration `k`,
`d[i][j]` is the best path using only `{0..k}` as intermediates. That's why **k must be the outermost
loop** — swapping the order breaks the DP's meaning and gives wrong answers that look plausible.

```java
for (int k = 0; k < n; k++)
  for (int i = 0; i < n; i++)
    for (int j = 0; j < n; j++)
      d[i][j] = Math.min(d[i][j], d[i][k] + d[k][j]);
```

| Algorithm | Use when | Complexity |
|---|---|---|
| BFS | unweighted (every edge costs 1) | O(V+E) |
| 0-1 BFS (deque: push-front for 0, back for 1) | weights only 0 or 1 | O(V+E) |
| Dijkstra | non-negative weights | O(E log V) |
| Bellman–Ford | negative edges, or "at most K stops" | O(V·E) |
| Floyd–Warshall | all pairs, small V (≲ 400) | O(V³) |

**MST — Kruskal vs Prim.** Kruskal sorts all edges and adds any that doesn't close a cycle (DSU tells
you). Prim grows one connected blob, always taking the cheapest edge leaving it — structurally
Dijkstra, but the priority is the *edge weight* rather than the accumulated path cost. Kruskal suits
sparse/edge-list graphs; Prim suits dense ones.

---

## 23. Backtracking — three shapes, one dedupe rule

**The frame:** you're doing DFS over a decision tree. `cur` is the path from the root; adding is
descending, removing is returning. Everything reduces to what the loop bounds and the recursion index
are.

```java
void subsets(int[] a, int start, List<Integer> cur, List<List<Integer>> out) {
    out.add(new ArrayList<>(cur));                       // every node is an answer for subsets
    for (int i = start; i < a.length; i++) {
        if (i > start && a[i] == a[i - 1]) continue;     // dedupe (requires sorted input)
        cur.add(a[i]);
        subsets(a, i + 1, cur, out);                     // i + 1 → each element used once
        cur.remove(cur.size() - 1);                      //     i → reuse allowed (combination sum)
    }
}
```

**What `start` does:** it forbids looking backward, which is precisely what makes `[1,2]` and `[2,1]`
the same result. Combinations need it; permutations must not have it (they use a `used[]` flag instead,
looping from 0 every time).

**Why `i + 1` vs `i`:** the recursion index decides whether the current element can be picked again.
`i+1` moves past it (subsets, combinations); `i` stays (unbounded reuse, combination sum I).

**The dedupe rule.** After sorting, equal values sit adjacent. At a given tree level, picking the
first of a run of duplicates already generates every branch the later copies would. `i > start` means
"this isn't the first choice at this level", so skip. Note it's `i > start`, not `i > 0` — you're
deduping *siblings*, not the whole array.

For permutations the rule becomes `if (i > 0 && a[i] == a[i-1] && !used[i-1]) continue;` — only use a
duplicate if its identical predecessor is already in the path, which fixes one canonical order among
identical elements.

**Complexity is inherently exponential** (2ⁿ subsets, n! permutations). The interviewer is watching for
*pruning*: sort and break early when the running sum already exceeds the target.

**N-Queens:** place row by row; track `cols`, `diag1 = row + col`, `diag2 = row - col + n` as boolean
arrays, since all cells on a diagonal share those sums/differences. That's O(1) conflict checking.

---

## 24. DP — the classics, with the loop directions that matter

**The 1-D compression.** Knapsack is naturally `dp[item][capacity]`, but each row only reads the row
above, so you can reuse one array. The catch is *what a cell holds mid-update*: not yet overwritten =
previous row (previous item), already overwritten = current row (current item).

That single fact determines the loop direction:

```java
// 0/1 knapsack — each item once — iterate capacity DOWNWARD
for (int i = 0; i < n; i++)
    for (int c = cap; c >= w[i]; c--)
        dp[c] = Math.max(dp[c], dp[c - w[i]] + v[i]);
// going down, dp[c - w[i]] is still the PREVIOUS row → item not yet used → used at most once

// unbounded knapsack — unlimited copies — iterate capacity UPWARD
for (int i = 0; i < n; i++)
    for (int c = w[i]; c <= cap; c++)
        dp[c] = Math.max(dp[c], dp[c - w[i]] + v[i]);
// going up, dp[c - w[i]] is ALREADY the current row → item may already be included → reuse
```

Memorize them as a pair. The direction *is* the semantics.

```java
// coin change — minimum coins
int[] dp = new int[amt + 1]; Arrays.fill(dp, amt + 1); dp[0] = 0;   // amt+1 = "infinity"
for (int c : coins) for (int i = c; i <= amt; i++) dp[i] = Math.min(dp[i], dp[i - c] + 1);
return dp[amt] > amt ? -1 : dp[amt];

// coin change II — count COMBINATIONS: coin loop must be OUTSIDE
for (int c : coins) for (int i = c; i <= amt; i++) dp[i] += dp[i - c];
```

**Why the loop order flips the meaning:** with coins outside, each coin is offered once in a fixed
order, so `{1,2}` is only ever built as 1-then-2 — combinations. With amount outside, every coin is
available at every amount, so `{1,2}` and `{2,1}` both count — permutations. Same three lines, two
different problems.

**Grid DPs** are all `dp[i][j]` over prefixes, where the recurrence branches on whether the current
characters match:

```java
// LCS — if the chars match they must both be in the LCS; else drop one and take the better
dp[i][j] = (s.charAt(i-1) == t.charAt(j-1))
         ? dp[i-1][j-1] + 1
         : Math.max(dp[i-1][j], dp[i][j-1]);

// Edit distance — on a mismatch, pay 1 and take the cheapest of the three edits
dp[i][j] = (s.charAt(i-1) == t.charAt(j-1))
         ? dp[i-1][j-1]
         : 1 + min(dp[i-1][j-1] /*replace*/, dp[i-1][j] /*delete*/, dp[i][j-1] /*insert*/);
```

The three neighbours map to the three operations: diagonal = substitute (consume both), up = delete
from `s`, left = insert into `s`. Base row/column = the cost of turning a string into the empty
string = its length.

**Bitmask DP / TSP:** `dp[mask][i]` = min cost having visited exactly `mask`, currently standing at
`i`. The mask *is* the memo key for "which subset is done", replacing an exponential set of visited-lists
with an integer. `2ⁿ · n` states — viable only up to n ≈ 20.

**Interval DP (matrix chain, burst balloons):** loop by increasing interval **length** first, because
a longer interval's answer is composed of shorter ones that must already be solved. Inside, iterate the
start, then the split point `k`.

---

## 25. Heap patterns

**Top K largest → min-heap of size K** (the inversion everyone gets backwards). The heap holds the K
best so far, and its root is the *weakest* of them — precisely the element to evict when something
better arrives. A max-heap would put the strongest at the root, which you never want to remove.
O(n log k), and it streams — you never hold more than K items.

**K-way merge:** hold one node per list in the heap, tagged with its list index. Pop the global
minimum, then push that list's successor. The heap size is the number of lists, so memory stays O(k)
even for huge lists. O(N log k).

**Median of a stream:** a max-heap for the lower half and a min-heap for the upper half. The two roots
sit adjacent to the median, so you read it in O(1). Rebalance after every insert so the sizes differ by
at most one — always push to one heap then move its root across, which keeps the halves correctly
separated.

**Meeting rooms II:** sort by start; keep a min-heap of *end* times. For each meeting, pop every room
whose end time is `<= start` (freed up), then push this one. The peak heap size is the number of rooms
needed — the heap is really a running count of overlapping intervals.

---

## 26. Fenwick tree (BIT) — point update, prefix query

**The idea:** `i & -i` (the lowest set bit) is the *length of the range* that `tree[i]` is responsible
for, ending at `i`. So the array is implicitly a set of overlapping-free power-of-two blocks.

**Query** walks down by stripping the lowest set bit — each step jumps to the block covering the next
chunk of the prefix, so a prefix decomposes into at most `log n` blocks (its binary representation).
**Update** walks up by adding the lowest set bit — visiting exactly the blocks that contain index `i`.

```java
int[] t = new int[n + 1];                       // 1-indexed; index 0 has no lowest set bit
void update(int i, int v) { for (; i <= n; i += i & -i) t[i] += v; }
int  query(int i)         { int s = 0; for (; i > 0; i -= i & -i) s += t[i]; return s; }
// range [l, r] = query(r) - query(l - 1)
```

Must be 1-indexed — `0 & -0 == 0` would loop forever.

Fifteen lines against a segment tree's eighty, and a smaller constant factor. Reach for Fenwick
whenever the query is a prefix aggregate you can subtract (sum, XOR, count). Use a **segment tree**
when the operation isn't invertible (range min/max) or when you need range updates with lazy
propagation. Also the standard tool for counting inversions: sweep left to right, query how many
larger values you've already seen.

---

# TIER B — Memorize the shape, derive the rest

- **BFS / DFS** on grids and graphs — direction array `{{0,1},{1,0},{0,-1},{-1,0}}`, visited set. Mark
  visited **when enqueuing**, not when dequeuing, or nodes get added multiple times.
- **Tree traversals** — iterative inorder with an explicit stack; level-order with a queue and a
  `size = q.size()` snapshot at the top of each level.
- **LCA** — parent pointers + depth equalization for one-off queries; binary lifting (`up[k][v]` =
  the 2^k-th ancestor) when there are many queries, giving O(log n) each after O(n log n) preprocessing.
- **Trie** — 26-way child array plus an `isEnd` flag. Prefix queries become a walk, not a scan.
- **Merge intervals** — sort by start; if the next start ≤ current end they overlap, so extend the end;
  otherwise push and move on. Sorting is what makes one pass sufficient.
- **Matrix rotate 90° clockwise** — transpose, then reverse each row. Counter-clockwise: transpose,
  then reverse each column.
- **Two pointers** — sorted pair sum (a sum too small can only be fixed by moving `lo` right, which is
  why no candidate is ever missed), container with most water, trapping rain water.
- **Serialize / deserialize a tree** — preorder with explicit null markers; the nulls are what make the
  structure recoverable from a flat sequence.

---

# Edge cases that cost interviews

| Situation | The thing you forget | Why it breaks |
|---|---|---|
| Binary search | `mid = lo + (hi-lo)/2` | `(lo+hi)` overflows int on large ranges |
| Binary search | rounding direction | `lo = mid` with round-down loops forever |
| Kadane | don't init `best = 0` | all-negative array returns 0 |
| Prefix sum + map | seed with `(0, 1)` | misses subarrays starting at index 0 |
| DSU | compare roots, not nodes | `parent[b] = a` corrupts the forest |
| Dijkstra | skip stale PQ entries; use `long` | no decrease-key in Java; distances overflow |
| Sliding window | shrink with `while` | one insert can require several removals |
| Dutch flag | don't advance `mid` after the high swap | the incoming value is unexamined |
| Directed cycle DFS | 3 colors, not a boolean | a finished branch isn't a cycle |
| Undirected cycle DFS | skip the parent | every edge looks like a 2-cycle |
| Linked list | dummy head node | head deletion needs no special case |
| Quickselect | randomize the pivot | sorted input is the O(n²) worst case |
| Integer math | `a / gcd * b` for lcm | multiplying first overflows |
| Fenwick | 1-indexed | `0 & -0 == 0` → infinite loop |
| Recursion | depth ~10⁵ → go iterative | JVM stack is roughly 10⁴ frames |

---

# Drill order (highest interview yield first)

1. Binary search — all four forms, plus search-on-answer
2. Sliding window + the at-most-K trick
3. Prefix sums + hashmap
4. Monotonic stack (next greater → histogram)
5. Heap patterns (top-K, k-way merge, two-heap median)
6. Graphs: BFS/DFS, topo sort, DSU, Dijkstra
7. DP: the knapsack pair, the coin-change pair, LIS, edit distance
8. Backtracking: subsets / permutations / combinations with dedupe
9. Kadane, Dutch flag, cyclic sort, Boyer–Moore, quickselect
10. Fenwick, KMP, Morris, bitmask DP — the "senior follow-up" tier

Items 1–8 cover the overwhelming majority of what an SDE-3 round asks. Items 9–10 are what separates
a clean round from a scrambling one when the interviewer pushes.

**How to use this document:** don't re-read it. Cover the code, read only the explanation, and type
the template from scratch. If you can't, the explanation didn't stick — reread that one paragraph, not
the code. Recall practice beats recognition practice by a wide margin, and recognition is exactly what
re-reading trains.

---
---

# PART 2 — VISUAL REFERENCE

Pictures for the ones where a diagram is the actual unlock. Trace these by hand once; that's worth
three re-reads.

## Binary search — the two interval conventions

```
CLOSED  [lo, hi]                 HALF-OPEN  [lo, hi)
  hi starts at n-1                 hi starts at n
  while (lo <= hi)                 while (lo < hi)
  hi = mid - 1                     hi = mid
  answer returned inside loop      answer = lo after the loop
  used for: exact match            used for: lower/upper bound, search-on-answer
```

`lowerBound([1,3,3,5,8], t=3)`:

```
 idx    0    1    2    3    4   (5)
 val    1    3    3    5    8
        lo ───────────────────── hi
 mid=2  val=3 >= 3  → hi = 2      [lo=0, hi=2)
 mid=1  val=3 >= 3  → hi = 1      [lo=0, hi=1)
 mid=0  val=1 <  3  → lo = 1      lo == hi → STOP
 answer = 1  ← first index with val >= 3
```

Search-on-answer is the same convergence over a boolean space:

```
 candidate answer:  1   2   3   4   5   6   7   8
 feasible(x):       F   F   F   T   T   T   T   T
                            └───┘
                        binary search finds THIS boundary
```

## Kadane — the discard rule in action

```
a    = [-2,  1, -3,  4, -1,  2,  1, -5,  4]
cur  =  -2   1  -2   4   3   5   6   1   5
best =  -2   1   1   4   4   5   6   6   6
             ↑       ↑                   ↑
       cur was -2,   cur was -2,     answer = 6
       so restart    so restart      subarray [4,-1,2,1]
       at 1          at 4
```

Every time `cur` goes negative, the next element starts fresh. That's the whole algorithm.

## Floyd's cycle detection — where the algebra comes from

```
 head ──── a ────► E ──────► ○
                   ▲          ╲
                   │           ╲   b = distance E → M
             cycle │            ▼
             length│    ○ ◄──── M   ← slow and fast meet here
               c   └────╱
```

```
 slow travelled:  a + b
 fast travelled:  2(a + b)   and also   a + b + k·c   (it did k extra laps)
   ⇒ a + b = k·c
   ⇒ a = k·c − b
```

So one pointer walking `a` steps from the head, and one walking `k·c − b` from the meeting point,
both land on `E` at the same time. That is why you reset to head.

## Dutch National Flag — four regions

```
 ┌──────────┬──────────┬────────────────┬──────────┐
 │    0s    │    1s    │    UNKNOWN     │    2s    │
 └──────────┴──────────┴────────────────┴──────────┘
 0        low        mid              high       n-1
                      ▲                  ▲
             examine a[mid]        the 2s grow leftward
```

```
 a[mid] == 0 → swap(low, mid), low++, mid++     (incoming value is a known 1)
 a[mid] == 1 → mid++                            (already correct)
 a[mid] == 2 → swap(mid, high), high--          (incoming value is UNEXAMINED — mid stays)
```

## Sliding window — why both pointers only move right

```
 [ a  a  b  c  b  b ]   longest substring with no repeat
   lo             hi

 hi=0  [a]           ok         len 1
 hi=1  [a a]         dup → shrink: lo→1        [a]        len 1
 hi=2  [a b]         ok                                   len 2
 hi=3  [a b c]       ok                                   len 3  ← best
 hi=4  [a b c b]     dup → shrink: lo→3        [c b]      len 2
```

`lo` never rewinds, so despite the nested `while` the total work is 2n.

## Monotonic stack — the width calculation

```
 h = [2, 1, 5, 6, 2, 3]
                  ┌───┐
            ┌───┐ │ 6 │
            │ 5 │ │   │
   ┌───┐    │   │ │   │       ┌───┐
   │ 2 │    │   │ │   │ ┌───┐ │ 3 │
   │   │┌───┤   │ │   │ │ 2 │ │   │
   └───┘│ 1 │   │ │   │ │   │ │   │
     0    1    2    3    4    5
```

At `i = 4` (height 2) the stack holds indices `[1, 2, 3]`. Bars 6 and 5 must pop:

```
 pop 3 (h=6): right bound = i = 4, left bound = stack top = 2
              width = 4 − 2 − 1 = 1   → area 6 × 1 = 6

 pop 2 (h=5): right bound = i = 4, left bound = stack top = 1
              width = 4 − 1 − 1 = 2   → area 5 × 2 = 10   ← max
```

The left bound is the new stack top because everything between it and the popped bar was already
popped — meaning it was all taller. That is what makes `i - st.peek() - 1` correct.

## Monotonic deque — sliding window maximum

```
 k = 3,  a = [1, 3, -1, -3, 5, 3]
 i=0   push 1              dq=[1]
 i=1   3 > 1 → pop 1       dq=[3]
 i=2   push -1             dq=[3,-1]        window max = 3
 i=3   push -3             dq=[3,-1,-3]     window max = 3
 i=4   front index expired; 5 dominates all → dq=[5]   window max = 5
 i=5   push 3              dq=[5,3]         window max = 5
```

Values in the deque are always decreasing, so the front is always the answer. Anything both
**smaller and older** than a new arrival is permanently useless — it leaves the window first anyway.

## Prefix sum + map — why the (0, 1) seed exists

```
 a = [3, 4],  k = 7

 running sum:  3       7
 seen:      {0:1}   {0:1, 3:1}
 i=0  sum=3  look for 3−7 = −4  → 0 hits
 i=1  sum=7  look for 7−7 =  0  → 1 hit  ← ONLY because of the seed
```

The subarray `[3,4]` starts at index 0, so its "prefix before it" is the empty prefix with sum 0.
Drop the seed and you silently miss every subarray that begins at the start of the array.

## Union-Find — path compression

```
   before find(4)          after find(4)
        1                       1
        │                    ┌──┼──┐
        2                    2  3  4
        │
        3
        │
        4
```

One traversal flattens the whole chain. Every subsequent `find` on those nodes is O(1).

## Fenwick tree — what `i & -i` means

```
 i   binary   lowbit   tree[i] covers
 1    0001      1      [1,1]
 2    0010      2      [1,2]
 3    0011      1      [3,3]
 4    0100      4      [1,4]
 5    0101      1      [5,5]
 6    0110      2      [5,6]
 7    0111      1      [7,7]
 8    1000      8      [1,8]

 query(7):  7 → 6 → 4 → 0     reads tree[7] + tree[6] + tree[4]   (3 blocks = bits of 7)
 update(3): 3 → 4 → 8 → …     writes every block that contains index 3
```

The lowest set bit *is* the block length. Query strips bits, update adds them.

## LIS — what `tails` really is

```
 a = [10, 9, 2, 5, 3, 7, 101, 18]

 10   → [10]                   new length 1
  9   → [9]                    replaced (smaller tail for length 1)
  2   → [2]                    replaced
  5   → [2, 5]                 extended → length 2
  3   → [2, 3]                 replaced 5 (cheaper tail, same length)
  7   → [2, 3, 7]              extended → length 3
 101  → [2, 3, 7, 101]         extended → length 4
 18   → [2, 3, 7, 18]          replaced 101

 answer = 4 (the LENGTH). tails is not guaranteed to be a real subsequence.
```

## Next permutation — the three steps

```
 1  5  8  4  7  6  5  3  1
          ▲  └──────────────┘ suffix is non-increasing = already maximal
        pivot (first a[i] < a[i+1] scanning from the right)

 step 2: rightmost value > 4  is 5
 1  5  8  5  7  6  4  3  1     ← after swap (suffix still non-increasing)

 step 3: reverse the suffix
 1  5  8  5  1  3  4  6  7     ← now smallest possible suffix
```

## KMP — the LPS table

```
 p    a   b   a   b   a   c   a
 i    0   1   2   3   4   5   6
 lps  0   0   1   2   3   0   1
                      ▲
        p[0..4] = "ababa" — its longest border is "aba" (length 3)
```

On a mismatch after matching `len = 3` characters, you don't restart — you fall back to
`lps[2] = 1`, because the "aba" you already matched has its own border "a".

## Edit distance — the grid and what each arrow means

```
          ""   h   o   r   s   e
    ""     0   1   2   3   4   5
     r     1   1   2   2   3   4
     o     2   2   1   2   3   4
     s     3   3   2   2   2  [3]  ← answer: ros → horse costs 3

     ↖ diagonal = replace (or free, if the characters match)
     ↑ up       = delete from the row string
     ← left     = insert into the row string
```

Base row and column are just "cost of turning this prefix into the empty string" = its length.

## Knapsack — why the loop direction is the semantics

```
 0/1 knapsack, capacity loop DOWNWARD:

   dp:  [ prev  prev  prev  prev | CUR  CUR ]
                      ▲c−w   ▲c
          reads LEFT → still the PREVIOUS row → item not yet used → used at most once

 Unbounded knapsack, capacity loop UPWARD:

   dp:  [ CUR  CUR  CUR  CUR | prev  prev ]
             ▲c−w   ▲c
          reads LEFT → already the CURRENT row → item may already be inside → reuse
```

## Two-heap median

```
      maxHeap (lower half)        minHeap (upper half)
          ┌─────────┐                ┌─────────┐
          │ 3  2  1 │                │ 4  5  6 │
          └────▲────┘                └────▲────┘
             top = 3                    top = 4
                └────── median ──────────┘

  odd total  → median = top of the larger heap
  even total → median = (top + top) / 2
  invariant  → sizes differ by at most 1; always push to one heap, then move its root across
```

## Morris traversal — the thread

```
        1                   predecessor of 1 = rightmost node of its left subtree = 5
       / \
      2   3      thread:  5.right ──► 1
     / \
    4   5 ─ ─ ─ ─ ─ ─ ─ ─ ─ ─► (back to 1)
```

Finding the thread already in place on a second visit is the signal that the left subtree is
finished — so you cut it, visit, and move right.

---
---

# PART 3 — THE STUFF THAT ACTUALLY WINS ROUNDS

## Constraint → complexity decoder

The input bound tells you the intended algorithm before you've thought about the problem. This is the
single fastest way to narrow the search space in the first two minutes.

| Constraint on n | Target complexity | What that usually means |
|---|---|---|
| n ≤ 10 | O(n!) | permutations, brute force |
| n ≤ 20 | O(2ⁿ) | subsets, bitmask DP, meet-in-the-middle |
| n ≤ 100 | O(n³) | Floyd–Warshall, interval DP, matrix chain |
| n ≤ 1,000 | O(n²) | grid DP, all pairs, LCS / edit distance |
| n ≤ 10⁵ | O(n log n) | sort, heap, binary search on answer, segment tree |
| n ≤ 10⁶ | O(n) | one pass, two pointers, counting, prefix sums |
| n ≥ 10⁹ | O(log n) or O(1) | math, binary search over the *value* range, matrix power |

If `n ≤ 10⁵` and you're heading toward O(n²), stop — you've picked the wrong tool. Say that out loud;
interviewers grade the reasoning, not just the code.

## Problem phrasing → pattern

| The problem says… | Reach for |
|---|---|
| contiguous subarray, all values positive | sliding window |
| contiguous subarray, values may be negative | prefix sum + hashmap |
| subarray sum / XOR equals K | prefix sum + hashmap |
| "minimize the maximum" / "maximum minimum" / "smallest X such that" | binary search on the answer |
| k-th largest / smallest | heap (streaming) or quickselect (in-memory) |
| top K frequent | bucket sort by frequency, or size-K heap |
| next greater / smaller element, spans, histograms | monotonic stack |
| max / min of every sliding window | monotonic deque |
| shortest path, unweighted | BFS |
| shortest path, weighted non-negative | Dijkstra |
| shortest path with "at most K stops" | Bellman–Ford, K+1 rounds |
| islands, regions, connected components | DFS / BFS / DSU |
| prerequisites, build order, dependencies | topological sort (Kahn) |
| "is it possible to finish" with dependencies | topo sort + cycle check |
| generate all combinations / permutations / partitions | backtracking |
| count the number of ways | DP |
| sorted input | binary search or two pointers — never a hashmap |
| values are a permutation of 1..n, O(1) space | cyclic sort |
| find the duplicate, O(1) space, don't modify input | Floyd's cycle detection |
| linked list, O(1) space, find middle or cycle | fast & slow pointers |
| palindromic substrings | expand around center; Manacher if O(n) is demanded |
| prefix matching, autocomplete, word search | Trie |
| overlapping intervals, meeting rooms, calendar | sort by start + heap or sweep line |
| median of a data stream | two heaps |
| range queries with point updates | Fenwick tree |
| range queries with range updates | segment tree with lazy propagation |
| O(1) get and put with eviction | HashMap + doubly linked list |

## Java gotchas that produce wrong answers, not compile errors

- **`Arrays.sort(int[])` is dual-pivot quicksort** — O(n²) on adversarial input, and some judges ship
  exactly that test. `Arrays.sort(Integer[])` uses TimSort and is safe. Boxing, or shuffling first,
  is the standard defence.
- **`Comparator` must not subtract.** `(a, b) -> a - b` overflows on large or negative values. Use
  `Integer.compare(a, b)`.
- **`ArrayDeque`, never `Stack`.** `Stack` extends `Vector`, is synchronized, and iterates
  bottom-to-top which will silently reverse your traversal. `ArrayDeque` also rejects `null`.
- **`PriorityQueue` has no decrease-key**, and `remove(Object)` is O(n). Push duplicates and skip
  stale entries instead.
- **`List<Integer>.remove(int)` removes by INDEX; `remove(Object)` removes by VALUE.** In
  backtracking, `cur.remove(cur.size() - 1)` is correct; `cur.remove(someInteger)` deletes the wrong
  thing.
- **`Integer` caches −128..127**, so `==` appears to work on small values and breaks on large ones.
  Always `.equals()` or unbox to `int`.
- **`%` keeps the sign of the dividend.** `-7 % 3 == -1` in Java. Normalize with `((x % m) + m) % m`
  before using it as an array index or map key.
- **Overflow is silent.** `mid`, running sums, and `a * b` all need `long`. `int` maxes at ~2.1×10⁹,
  which a sum of 10⁵ elements can exceed easily.
- **Recursion depth ~10⁴ frames** on the default JVM stack. A 10⁵-node linked list or skewed tree
  will `StackOverflowError` — convert to iterative or say why you would.
- **`HashMap<Integer, Integer>` boxes everything.** For bounded keys, an `int[]` is several times
  faster and is what an interviewer expects for a character or small-range count.
- **String concatenation in a loop is O(n²).** `StringBuilder`, always.

## Complexity you should be able to state without pausing

| Structure | Access | Search | Insert | Delete | Note |
|---|---|---|---|---|---|
| Array | O(1) | O(n) | O(n) | O(n) | O(log n) search if sorted |
| ArrayList | O(1) | O(n) | O(1)* | O(n) | *amortized append |
| LinkedList | O(n) | O(n) | O(1) | O(1) | O(1) only with the node in hand |
| HashMap | — | O(1)* | O(1)* | O(1)* | *amortized; O(n) worst case |
| TreeMap | — | O(log n) | O(log n) | O(log n) | sorted, gives floor/ceiling |
| Heap | O(1) peek | O(n) | O(log n) | O(log n) | build from array is O(n) |
| Trie | — | O(L) | O(L) | O(L) | L = key length |
| DSU | — | ~O(1) | ~O(1) | — | no deletion |
| Fenwick | — | — | O(log n) | — | prefix query O(log n) |

Sorting is O(n log n) comparison-based; counting/radix sort is O(n + k) when the value range is small,
which is the answer to "can you beat n log n here?"

## The 6-step protocol — say these out loud

The most common way a strong engineer loses a DSA round is jumping into code before pinning the
problem down. This is the same failure the four-bucket scaffold fixes in system design; here is the
DSA version.

1. **Restate** the problem in one sentence and get agreement. Thirty seconds, catches
   misunderstandings that would otherwise cost twenty minutes.
2. **Constraints.** Size of n. Value ranges (do they fit in `int`?). Negatives? Duplicates? Sorted?
   Empty input? This is where the constraint table above tells you the target complexity.
3. **Walk one example by hand**, including one edge case. Say the expected output before you write
   anything.
4. **State the brute force and its complexity**, then say what's wasteful about it. The optimization
   almost always comes from naming the redundancy — "I'm recomputing the same prefix", "I'm
   re-comparing characters I already matched".
5. **Name the pattern and the target complexity before coding.** "Prefix sums with a hashmap, O(n)
   time O(n) space." Now the interviewer can course-correct you cheaply, which is much better than
   finding out at minute 25.
6. **Code, then dry-run on the edge case** — empty, single element, all-same, all-negative.
   Do not wait to be asked.

Steps 1–5 should take under five minutes. Skipping them to look fast is the trade that loses rounds.

## How to actually drill this

Recall beats recognition by a wide margin, and re-reading only trains recognition. So:

- **Cover the code, read only the explanation, type the template from scratch.** If you can't, reread
  that one paragraph — not the code.
- **Space it.** Same template on day 1, day 3, day 7, day 21. Four short passes beat one long one.
- **Timebox.** 20 minutes per problem. Past that you're not learning the pattern, you're grinding —
  read the solution, then reproduce it from memory the next day.
- **Track the traps you personally hit**, not the problems you solved. A list of ten mistakes you make
  repeatedly is worth more than a count of two hundred problems.
