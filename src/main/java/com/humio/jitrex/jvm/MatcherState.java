/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.
*/
package com.humio.jitrex.jvm;
import com.humio.jitrex.util.Regex;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

/** State of one match or search: input, variable cells, fork stack and counters. */
public final class MatcherState {
    /**
     * Character buffer which stores the string being matched.
     */
    public CharSequence string;

    /**
     * Variable values. Earch variable is given two cells in this
     * array: one stores index of the first character in the buffer
     * and the other stores index of the last character in the buffer plus one.
     * Note that variables that are only referenced (but not "picked") by
     * this jitrex have their own buffers (stored in extCells).
     */
    public int[] cells;

    /**
     * Index (in the buffer) of the first character of the string being matched.
     */
    public int start;

    /**
     * Index (in the buffer) of the last character in the string being matched plus one.
     */
    public int end;

    /**
     * This keeps information about forks in matching. Forks occur when several ways
     * of matching are possible. Matching algorithm picks the first way and tries it
     * out. If it eventually fails, it goes back to the fork point and tries the other
     * way.
     */
    public int[] forks;

    /**
     * Number of elements used in the <i>forks</i> array.
     */
    public int forkPtr;

    /**
     * Number of back track operations done.
     */
    public int failCount;

    /**
     * Maximum number of back track operations allowed.
     */
    public int failCountMax = Integer.MAX_VALUE;

    /**
     * Starting index for the next search/match.
     */
    public int headStart;

    /**
     * On successful match/search - index of the first character in matching string.
     */
    public int matchStart;

    /**
     * On successful match/search - index of the last character in matching string plus one.
     */
    public int matchEnd;

    /**
     * Character buffers for referenced variable values.
     */
    public CharSequence[] extCells;

    /**
     * True if searching is going on, false if matching.
     */
    public boolean searching;

    /**
     * Maximum value for the start of the matching string.
     */
    public int maxStart;

    private static final AtomicBoolean DEFAULT_CANCELLED_FLAG = new AtomicBoolean(false);
    AtomicBoolean cancelledFlag = DEFAULT_CANCELLED_FLAG; // checked by JavaClassRegexStub.onBackTrack

    public int getFailCount() { return failCount; }

    public MatcherState(Regex regex) {
        int cellCount = regex.getCellCount();
        cells = cellCount == 0 ? null : new int[regex.getCellCount()];
    }

    public void init(CharSequence string, int start, int len) {
        this.string = string;
        this.start = start;
        this.end = start + len;
        if (forks == null)
            forks = new int[4];
        else
            Arrays.fill(forks, 0);

        forkPtr = 0;
        failCount = 0;
    }

    /**
     * Debugging method to print jitrex state. Called if internal error happens.
     */
    public void dumpForks(Hashtable vars) {
        System.err.println("**** State dump:");
        System.err.println("\tstart = " + start);
        System.err.println("\tend = " + end);
        System.err.println("\tmatchStart = " + matchStart);
        System.err.println("\tmatchEnd = " + matchEnd);
        System.err.println("\theadStart = " + headStart);
        System.err.print("\tstring = \"");
        if (start >= 0) {
            for (int i = start; i < end && i < string.length() && i < start + 30; i++) {
                char c = string.charAt(i);
                if (c < ' ')
                    System.err.print("\\x" + Integer.toHexString(c));
                else
                    System.err.print(c);
            }
        }
        System.err.println("\"");
        System.err.println("**** Fork stack dump [ptr=" + forkPtr + "]:");
        for (int i = 0; i < forks.length && i < forkPtr + 4; i++)
            System.err.print(" " + forks[i]);
        System.err.println();
        if (vars == null || cells == null)
            return;
        System.err.println("**** Variable dump:");
        Object[] names = new Object[cells.length];
        Enumeration vlist = vars.keys();
        while (vlist.hasMoreElements()) {
            Object key = vlist.nextElement();
            int[] v = (int[]) vars.get(key);
            names[v[0]] = key;
        }
        for (int i = 0; i < cells.length; i++)
            System.err.println("\t" + cells[i] + (names[i] != null ? "\t$" + names[i] : ""));
    }

    /**
     * Assignes external variable buffer by its buffer handle.
     * It is needed to completely specify value of an external variable.
     */
    public void setExtVariableBuffer(int extHandle, CharSequence arr) {
        extCells[extHandle] = arr;
    }

    /**
     * Returns external variable buffer by its buffer handle. If -1 is passed as a handle
     * the input buffer that was set up for matching (using init) is returned.
     */
    public CharSequence getCharBuffer(int extHandle) {
        if (extHandle < 0)
            return string;
        else
            return extCells[extHandle];
    }

    /**
     * After successiful search returns matching substring's initial position in
     * the input buffer (that was set up by init method).
     */
    public int getMatchStart() {
        return matchStart;
    }

    /**
     * After successiful search returns matching substring's final position in
     * the input buffer (that was set up by init method).
     */
    public int getMatchEnd() {
        return matchEnd;
    }

    /**
     * Returns external or internal variable index using its index handle. Every variable
     * has two indices - 'begin' and 'end'. Use 'begin' and 'end' handles to get them.
     * These indices can be used to determine which part of the input buffer (set by init method)
     * corresponding internal variable matched. An external variable 'begin' and 'end' indices
     * point to its own buffer. They are assinged by setIndex and are never changed.
     */
    public int getIndex(int handle) {
        return cells[handle];
    }

    /**
     * Returns internal variable index using its index handle. Every variable
     * has two indices - 'begin' and 'end'. Use 'begin' and 'end' handles to assign them.
     * These indices are needed to completely specify value of an external variable.
     */
    public void setIndex(int handle, int value) {
        cells[handle] = value;
    }

    public void setBackTrackLimit(int i) { failCountMax = i; }
    public int getBackTrackLimit() { return failCountMax; }
    public int getBackTrackCount() { return failCount; }

    public AtomicBoolean setCancelledFlag(AtomicBoolean value) {
        AtomicBoolean result = cancelledFlag;
        cancelledFlag = value;
        return result;
    }
}
