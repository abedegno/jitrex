/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.util;

import com.humio.jitrex.jvm.MatcherState;

import java.util.Enumeration;

public class CaseInsensitiveRegex extends ProxyRegex {

    private int matchOff;

    private CharSequence cMatch;

    private int[] handleToExtHandle;
    private int[] extOff;
    private CharSequence[] extBuf;

    public CaseInsensitiveRegex(String regex) {
        this(createLowerCaseRegex(regex));
    }

    public CaseInsensitiveRegex(Regex lowerCaseRegex) {
        super(lowerCaseRegex);
        int maxHandle = -1;
        int maxExtHandle = -1;
        Enumeration vars = variables();
        while (vars.hasMoreElements()) {
            String name = (String) vars.nextElement();
            int h = getVariableHandle(name, true);
            if (h > maxHandle)
                maxHandle = h;
            h = getVariableHandle(name, false);
            if (h > maxHandle)
                maxHandle = h;
            h = getExtVariableHandle(name);
            if (h > maxExtHandle)
                maxExtHandle = h;
        }
        if (maxHandle >= 0) {
            handleToExtHandle = new int[maxHandle + 1];
            if (maxExtHandle >= 0) {
                extOff = new int[maxExtHandle + 1];
                extBuf = new CharSequence[maxExtHandle + 1];
            }
            vars = variables();
            while (vars.hasMoreElements()) {
                String name = (String) vars.nextElement();
                int eh = getExtVariableHandle(name);
                int h = getVariableHandle(name, true);
                handleToExtHandle[h] = eh;
                h = getVariableHandle(name, false);
                handleToExtHandle[h] = eh;
            }
        }
    }

    public void init(MatcherState state, CharSequence subject, int off, int len) {
        init(state, -1, subject, off, len);
    }

    private void init(MatcherState state, int extHandle, CharSequence arr, int off, int len) {
        int i = off;
        int last = off + len;
        char c;
        while (i < last && (!Character.isLetter(c = arr.charAt(i)) ||
                Character.isLowerCase(c)))
            i++;
        if (extHandle < 0)
            cMatch = arr;
        else
            extBuf[extHandle] = arr;
        if (i == last)
            if (extHandle < 0) {
                matchOff = 0;
                super.init(state, arr, off, len);
            } else {
                extOff[extHandle] = 0;
                state.setExtVariableBuffer(extHandle, arr);
            }
        else {
            CharSequence buf = new LoweCaseCharSequence(arr);
            if (extHandle < 0) {
                matchOff = off;
                super.init(state, buf, 0, len);
            } else {
                extOff[extHandle] = 0;
                state.setExtVariableBuffer(extHandle, buf);
            }
        }
    }

    public CharSequence getCharBuffer(int extHandle) {
        if (extHandle < 0)
            return cMatch;
        return extBuf[extHandle];
    }

    public void setExtVariableBuffer(MatcherState state, int extHandle, CharSequence arr) {
        init(state, extHandle, arr, 0, arr.length());
    }

    public String toString() {
        return super.toString() + "i";
    }
}


class LoweCaseCharSequence implements CharSequence {

    private final CharSequence seq;

    LoweCaseCharSequence(CharSequence seq) {
        this.seq = seq;
    }
    
    public int length() {
        return seq.length();
    }

    public char charAt(int index) {
        char ch = seq.charAt(index);
        if (ch >= 'A' && ch <= 'Z') {
            return (char) (ch - 'A' + 'a');
        } else if (ch > 0x7f){
            return Character.toLowerCase(ch);
        } else {
            return ch;
        }
    }

    public CharSequence subSequence(int start, int end) {
        return seq.subSequence(start, end);
    }
}