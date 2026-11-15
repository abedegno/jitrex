/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.jvm;

import com.humio.jitrex.RegexCancelledException;
import com.humio.jitrex.RegexRuntimeLimitException;
import com.humio.jitrex.util.Regex;

import java.util.Enumeration;
import java.util.Hashtable;
public abstract class JavaClassRegexStub extends Regex {
    public JavaClassRegexStub() {}

    abstract protected boolean nextMatchInt(MatcherState state);

    abstract protected Hashtable<String,int[]> getVars();

    /**
     * Debugging method to print jitrex state. Called if internal error happens.
     */
    public void dumpForks(MatcherState state) {
        state.dumpForks(getVars());
    }

    //------------------ Regexp implementation


    public int getVariableHandle(String var, boolean begin) {
        int[] entry = (int[]) getVars().get(var);
        if (entry == null)
            return -1;
        return entry[begin ? 0 : 1];
    }


    public int getExtVariableHandle(String var) {
        int[] entry = (int[]) getVars().get(var);
        if (entry == null)
            return -1;
        return entry[2];
    }


    public Enumeration<String> variables() {
        return getVars().keys();
    }


    public boolean searchAgain(MatcherState state) {
        state.failCount = 0;
        state.searching = true;
        while (true) {
            while (state.headStart > state.maxStart) {
                return false;
            }
            if (nextMatchInt(state)) // modifies headStart on failure
            {
                state.headStart = state.matchStart + 1; // so when we fail next time, we start at an offset
                return true;
            }
            state.forkPtr = 0; // it should be 0 anyway?
        }
    }


    public boolean search(MatcherState state, int from) {
        if (from > state.end)
            throw new IndexOutOfBoundsException();
        state.start = from;
        state.headStart = from;
        return search(state);
    }


    public boolean search(MatcherState state) {
        state.failCount = 0;
        state.searching = true;
        while (true) {
            while (state.headStart > state.maxStart) {
                return false;
            }
            int oldHeadStart = state.headStart;
            if (nextMatchInt(state)) // modifies headStart on failure
            {
                state.matchStart = state.headStart;
                // next time start after the end of just matched substring
                if (state.matchStart < state.matchEnd && state.matchEnd < state.end)
                    state.headStart = state.matchEnd;
                else
                    state.headStart = state.matchEnd + 1;
                state.forkPtr = 0;
                return true;
            }
            if (state.headStart == oldHeadStart)
                return false;
            state.forkPtr = 0;
        }
    }


    public boolean matchWhole(MatcherState state) {
        state.failCount = 0;
        state.searching = false;
        int lastMatchEnd = -1;
        while (nextMatchInt(state)) {
            if (state.matchEnd == state.end)
                return true;
            if (state.matchEnd == lastMatchEnd)
                return false;
            lastMatchEnd = state.matchEnd;
            if (state.forkPtr == 0) // no alternatives left
                break;
        }
        return false;
    }


    public boolean match(MatcherState state) {
        state.failCount = 0;
        state.searching = false;
        return nextMatchInt(state);
    }

    protected static char charAt(CharSequence cs, int i) {
        return cs.charAt(i);
    }

    /**
     * Returns true if the character at the i'th position in the input has the
     * expected value. The index is guaranteed to be within the bounds of the
     * string.
     */
    protected static boolean charMatches(CharSequence input, int i, char expected) {
        return input.charAt(i) == expected;
    }

    protected static char lowerCaseCharAt(CharSequence cs, int i) {
        char ch = cs.charAt(i);
        if (ch < 128) {
            if (ch >= 'A' && ch <= 'Z') {
                return (char) ((int)ch + ('a' - 'A'));
            } else {
                return ch;
            }
        } else {
            return Character.toLowerCase(ch);
        }
    }

    /**
     * Returns true if the character at the i'th position in the input has the
     * expected value, independent of case. The index is guaranteed to be within
     * the bounds of the string and the expected character is guaranteed to be
     * lower case if it is alphabetical.
     */
    protected static boolean charMatchesInsensitive(CharSequence input, int i, char expected) {
        char ch = input.charAt(i);
        if (ch == expected)
            return true;
        if (ch < 128) {
            if (ch >= 'A' && ch <= 'Z') {
                char lower = (char) ((int)ch + ('a' - 'A'));
                return lower == expected;
            } else {
                return false;
            }
        } else {
            return Character.toLowerCase(ch) == expected;
        }
    }

    /**
     * Returns true if the substring starting at the given offset in the input
     * is the given string. It is guaranteed that the input has at least the
     * string's length characters left after the offset.
     */
    protected static boolean substringMatches(CharSequence input, int offset, String expected) {
        int length = expected.length();
        for (int i = 0; i < length; i++) {
            if (!charMatches(input, offset + i, expected.charAt(i)))
                return false;
        }
        return true;
    }

    /**
     * Returns true if the substring starting at the given offset in the input
     * is the given string, case insensitive. It is guaranteed that the input
     * has at least the string's length characters left after the offset.
     */
    protected static boolean substringMatchesInsensitive(CharSequence input, int offset, String expected) {
        int length = expected.length();
        for (int i = 0; i < length; i++) {
            if (!charMatchesInsensitive(input, offset + i, expected.charAt(i)))
                return false;
        }
        return true;
    }

    protected static int cmpUnixLineTerminator(char c) {
        return c - '\n';
    }
    protected static int cmpLineTerminator(char c) {
        switch (c) {
            case '\n':
            case '\r':
            case '\u0085':
            case '\u2028':
            case '\u2029':
                return 0;

            default:
                return 1;
        }
    }

    /** Called by the generated code on each backtrack; enforces the backtrack limit and the cancelled flag. */
    public static void onBackTrack(MatcherState state) {
        if (++state.failCount > state.failCountMax)
            throw new RegexRuntimeLimitException("regex backtrack limit reached", state.failCountMax);
        if (state.cancelledFlag.get())
            throw new RegexCancelledException();
    }
}
