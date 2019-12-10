package com.hirpc.protocol;

/**
 * @author mwq0106
 * @date 2019/9/21
 */
public class Response {
    private long id;

    private Object result;

    private Exception exception;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Object getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }
}
