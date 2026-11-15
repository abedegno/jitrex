package com.humio.jitrex;


public class RegexRuntimeLimitException extends JitrexRuntimeException {
    private final int backtrackCount;

    public RegexRuntimeLimitException(String msg, int backtrackCount) {
        super(msg);
        this.backtrackCount = backtrackCount;
    }

    public int getBacktrackCount() {
        return backtrackCount;
    }
}
