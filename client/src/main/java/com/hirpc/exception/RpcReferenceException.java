package com.hirpc.exception;

import org.springframework.beans.BeansException;

public class RpcReferenceException extends BeansException {
    public RpcReferenceException(String msg) {
        super(msg);
    }

    public RpcReferenceException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
