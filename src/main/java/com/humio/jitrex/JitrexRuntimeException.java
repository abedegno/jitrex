/*
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.
*/
package com.humio.jitrex;

public class JitrexRuntimeException extends RuntimeException {
    public JitrexRuntimeException() {
    }

    public JitrexRuntimeException(String message) {
        super(message);
    }
}
