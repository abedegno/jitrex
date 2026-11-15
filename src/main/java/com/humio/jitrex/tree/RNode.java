/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

import com.humio.jitrex.util.Regex;

import java.util.Dictionary;
import java.util.Stack;

public abstract class RNode {
    public int position; // in the initial jitrex

    public int maxLeft;
    public int minLeft;

    // by default, it can take from 0 to infinity chars
    public int maxLength = Integer.MAX_VALUE;
    public int minLength = 0;

    public CharSet prefix = CharSet.FULL_CHARSET;

    public RNode tail;
    private int flags;

    public int flags() { return flags; }

    protected RNode(int pos) {
        position = pos;
    }

    public static int minTotalLength(RNode p) {
        int t = 0;
        while (p != null) {
            if (t == Integer.MAX_VALUE) return t;
            t = addSaturated(t, p.minLength);
            p = p.tail;
        }
        return t;
    }

    public static int maxTotalLength(RNode p) {
        int t = 0;
        while (p != null) {
            if (t == Integer.MAX_VALUE) return t;
            t = addSaturated(t, p.maxLength);
            p = p.tail;
        }
        return t;
    }

    /**
     * Number of variables to be used by this jitrex. This should be upper bound,
     * but, of cource, it should be as close as possible to what RCompiler will
     * generate.
     */
    public int getNCells() {
        RNode next = nonAtomicTail();
        if (next == null)
            return 0;
        else
            return next.getNCells();
    }

    public void prepare(int addMaxLeft, int addMinLeft) {
        if (addMaxLeft < 0 || addMinLeft < 0)
            throw new RuntimeException("Invalid internal state addMin="+addMinLeft+" addMax="+addMaxLeft);

        if (isAtomic() && tail != null && tail.isAtomic()) {
            // Several atomic nodes in a row. Prepare them iteratively
            // instead of recursing once per node:
            //  - push the atomic nodes that follow this one on a stack,
            //  - prepare the first non-atomic node after them (if any),
            //  - finish the atomic nodes from the last one back to the first,
            //  - then finish this node (below).

            Stack<RNode> finishStack = new Stack<>();
            RNode next;
            finishStack.push(tail);
            next = tail.tail;
            while (next != null && next.isAtomic()) {
                // Collect the rest of the atomic run; next ends up at the
                // first non-atomic node, or null.
                finishStack.push(next);
                next = next.tail;
            }
            if (next != null)
                // prepare the rest of the chain
                next.prepare(addMaxLeft, addMinLeft);

            // Finish in reverse order: finishPrepare reads tail.maxLeft and
            // tail.minLeft, so a node's tail must be finished first.
            while (!finishStack.empty())
                finishStack.pop().finishPrepare(addMaxLeft, addMinLeft);

        } else if (tail != null)
            tail.prepare(addMaxLeft, addMinLeft);

        finishPrepare(addMaxLeft, addMinLeft);
    }

    protected void finishPrepare(int addMaxLeft, int addMinLeft) {
        if (addMaxLeft < 0 || addMinLeft < 0)
            throw new RuntimeException("Invalid internal state addMin="+addMinLeft+" addMax="+addMaxLeft);

        int maxAfter = (tail == null ? addMaxLeft : tail.maxLeft);
        maxLeft = addSaturated(maxLength, maxAfter);

        int minAfter = (tail == null ? addMinLeft : tail.minLeft);
        minLeft = addSaturated(minLength, minAfter);

        if (minLeft < minLength
                || minLength < 0
                || minLeft < 0
                || maxLeft < 0
                || maxLength < 0
                || maxLeft < maxLength)
            throw new RuntimeException("Invalid min/max char counts: " +
                    minLeft + " " + minLength + " " + maxLeft + " " + maxLength + ": " + this);
    }

    /**
     * Applies flags to this node and the rest of the chain: following atomic
     * nodes get processFlagsLocal, the first non-atomic node processFlagsGlobal.
     */
    public void processFlagsGlobal(int flags) {
        processFlagsLocal(flags);
        RNode next = tail;

        // following atomic nodes get only processFlagsLocal
        while (next != null && next.isAtomic()) {
            next.processFlagsLocal(flags);
            next = next.tail;
        }
        if (next != null)
            next.processFlagsGlobal(flags);
    }

    /**
     * Applies flags to this node only.
     */
    public void processFlagsLocal(int flags) {
        this.flags = flags;
        if (isLowercase()) {
            prefix = prefix.toLowerCase();
        }
    }

    /** True if this node's flags include {@link Regex#CASE_INSENSITIVE}. */
    public boolean isLowercase() { return (flags & Regex.CASE_INSENSITIVE) == Regex.CASE_INSENSITIVE; }


    public CharSet findPrefix(CharSet tailPrefix) {
        RNode next = nonAtomicTail();
        if (next != null)
            next.findPrefix(tailPrefix);
        return prefix;
    }


    public boolean isStartAnchored() { return false; }


    public boolean isEndAnchored() {
        RNode next = nonAtomicTail();
        if (next != null)
            return next.isEndAnchored();
        return false;
    }

    public boolean hasPicks() {
        RNode next = nonAtomicTail();
        if (next != null)
            return next.hasPicks();
        return false;
    }

    public boolean hasForks() {
        RNode next = nonAtomicTail();
        if (next != null)
            return next.hasForks();
        return false;
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        RNode next = nonAtomicTail();
        if (next != null)
            next.collectReferences(refList, pickList);
    }


    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        // skip the run of atomic nodes that starts here, if any
        RNode current = this;
        while (current.isAtomic() && current.tail != null && current.tail.isAtomic())
            current = current.tail;

        if (current.tail != null)
            current.tail = current.tail.markReferenced(refList, pickList, collapse);
        return this;
    }

    public abstract Object eval(RContext context);

    public String toString() {
        RPrintContext pc = new RPrintContext();
        eval(pc);
        return pc.toString();
    }


    public boolean is_empty_node() { return false; }


    /** True for nodes that match a single character (RConstNode, RCharClassNode). */
    public boolean isAtomic() { return false; }

    /**
     * Returns the first node after this one that is not atomic, or null if
     * there is none. Methods that walk the chain use it to skip runs of
     * atomic nodes.
     */
    public RNode nonAtomicTail() {
        RNode current = tail;
        while (current != null && current.isAtomic())
            current = current.tail;

        return current;
    }

    /** Returns a + b, or Integer.MAX_VALUE if the sum overflows. Both must be non-negative. */
    public static int addSaturated(int a, int b) {
        if (a < 0 || b < 0)
            throw new RuntimeException("Invalid internal lengths a=" + a + " b=" + b);

        int sum = a + b;
        if (sum < 0) return Integer.MAX_VALUE;
        return sum;
    }

    /** Returns a * b, or Integer.MAX_VALUE if the product overflows. Both must be non-negative. */
    public static int mulSaturated(int a, int b) {
        if (a < 0 || b < 0)
            throw new RuntimeException("Invalid internal lengths a=" + a + " b=" + b);

        long longProduct = (long) a * b;
        int product = (int) longProduct;
        return product == longProduct ? product : Integer.MAX_VALUE;
    }
}
