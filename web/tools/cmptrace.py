#!/usr/bin/env python3
"""Compares two traces numerically, with a tolerance.

The JVM and a browser cannot be made bit-identical: Math.pow is allowed ~1 ulp
of error and the JVM and V8 land on different sides of it. Cloud.getLift raises
base proximity to the 1000th power, and altitude feeds back into that, so a
1-ulp seed compounds. The flights stay the same flight; the digits drift.

So compare like a physicist, not like diff.

Usage: cmptrace.py <a.trace> <b.trace> [tolerance]
"""
import sys


def load(path):
    rows, header = [], None
    with open(path) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if header is None and not line[0].isdigit():
                header = line
                continue
            parts = line.split()
            rows.append((int(parts[0]), [float(x) for x in parts[1:]]))
    return header, rows


def main():
    tol = float(sys.argv[3]) if len(sys.argv) > 3 else 0.05
    ha, a = load(sys.argv[1])
    hb, b = load(sys.argv[2])

    if ha != hb:
        print("FAIL: different runs\n  %s\n  %s" % (ha, hb))
        return 1
    if len(a) != len(b):
        print("FAIL: %d samples vs %d" % (len(a), len(b)))
        return 1

    cols = "x y z vx vy sink lift".split()
    worst = [(0.0, None, None)] * len(cols)
    for (fa, va), (fb, vb) in zip(a, b):
        if fa != fb:
            print("FAIL: frame %d vs %d" % (fa, fb))
            return 1
        for i, (x, y) in enumerate(zip(va, vb)):
            d = abs(x - y)
            if d > worst[i][0]:
                worst[i] = (d, fa, (x, y))

    print(ha)
    print("%-6s %-12s %s" % ("field", "max delta", "worst frame"))
    bad = False
    for i, c in enumerate(cols):
        d, frame, vals = worst[i]
        flag = ""
        if d > tol:
            flag = "  <-- over tolerance %g" % tol
            bad = True
        detail = "" if frame is None else "frame %d (%.4f vs %.4f)" % (frame, vals[0], vals[1])
        print("%-6s %-12.6f %s%s" % (c, d, detail, flag))
    print("\n%s (tolerance %g over %d samples)" % ("FAIL" if bad else "PASS", tol, len(a)))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
