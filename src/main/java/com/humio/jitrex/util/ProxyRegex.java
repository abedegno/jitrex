/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.util;

import com.humio.jitrex.jvm.MatcherState;

import java.util.Enumeration;

public class ProxyRegex extends Regex {

    Regex regex;

    public ProxyRegex(Regex orig) {
        regex = orig;
    }

    @Override
    public int getVariableHandle(String var, boolean begin) {
        return regex.getVariableHandle(var, begin);
    }

    @Override
    public int getExtVariableHandle(String var) {
        return regex.getExtVariableHandle(var);
    }

    @Override
    public Enumeration<String> variables() {
        return regex.variables();
    }

    @Override
    public void init(MatcherState state, CharSequence subject, int off, int len) {
        regex.init(state, subject, off, len);
    }

    @Override
    public int getCellCount() {
        return regex.getCellCount();
    }

    @Override
    public boolean searchAgain(MatcherState state) {
        return regex.searchAgain(state);
    }

    @Override
    public boolean search(MatcherState state) {
        return regex.search(state);
    }

    @Override
    public boolean search(MatcherState state, int from) {
        return regex.search(state, from);
    }

    @Override
    public boolean match(MatcherState state) {
        return regex.match(state);
    }

    @Override
    public boolean matchWhole(MatcherState state) {
        return regex.matchWhole(state);
    }

    @Override
    public String toString() {
        return regex.toString();
    }

}
