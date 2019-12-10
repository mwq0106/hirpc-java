package com.hirpc.protocol;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author mwq0106
 * @date 2019/9/21
 */
public class Request {
    private static final AtomicLong REQUEST_ID = new AtomicLong(0);

    private long id;

    private boolean isOneWay = false;

    private boolean isHeartBeat = false;

    private boolean isEvent = false;

    private String data;

    private String servicePath;

    private String serviceName;

    private Class<?>[] parameterType;

    private Object[] parameterValue;

    private Map<String,String> header;

    private String serviceVersion;

    private static long newRequestId() {
        return REQUEST_ID.getAndIncrement();
    }

    public Request(){
        id = newRequestId();
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isOneWay() {
        return isOneWay;
    }

    public boolean isHeartBeat() {
        return isHeartBeat;
    }

    public boolean isEvent() {
        return isEvent;
    }

    public void setOneWay(boolean oneWay) {
        isOneWay = oneWay;
    }

    public void setHeartBeat(boolean heartBeat) {
        isHeartBeat = heartBeat;
    }

    public void setEvent(boolean event) {
        isEvent = event;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setParameterType(Class<?>[] parameterType) {
        this.parameterType = parameterType;
    }

    public void setParameterValue(Object[] parameterValue) {
        this.parameterValue = parameterValue;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public long getId() {
        return id;
    }

    public String getServicePath() {
        return servicePath;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Class<?>[] getParameterType() {
        return parameterType;
    }

    public Object[] getParameterValue() {
        return parameterValue;
    }

    public Map<String, String> getHeader() {
        return header;
    }
}
