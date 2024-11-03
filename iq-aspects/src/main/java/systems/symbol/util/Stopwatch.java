package systems.symbol.util;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
public class Stopwatch {
    protected long start = 0, current = 0, laps = 0;

    public Stopwatch() {
        reset();
    }

    public void reset() {
        current = System.currentTimeMillis();
        start = current;
    }

    public long getStartTimestamp() {
        return start;
    }

    public long elapsed() {
        long elapsed = System.currentTimeMillis() - current;
        current = System.currentTimeMillis();
        return elapsed;
    }

    public long getTotalTime() {
        return System.currentTimeMillis() - start;
    }

    public long mark() {
        long time = getTotalTime();
        laps++;
        return (time / laps); // average
    }

    public String now() {
        return (int) (getTotalTime()) + "ms";
    }

    public String toString() {
        return (int) (elapsed()) + "ms";
    }

    public String summary() {
        long totalTime = getTotalTime();
        long averageTime = laps > 0 ? totalTime / laps : 0;
        return "Elapsed: " + getTotalTime() + "ms, Laps: " + laps + ", Average: " + averageTime + "ms";
    }
}
