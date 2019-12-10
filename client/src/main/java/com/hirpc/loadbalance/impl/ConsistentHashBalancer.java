package com.hirpc.loadbalance.impl;

import com.hirpc.handler.RpcClientHandler;
import com.hirpc.loadbalance.Balancer;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * todo 一致性哈希负载均衡，正确性有待验证
 * @author mwq0106
 * @date 2019/12/2
 */
public class ConsistentHashBalancer implements Balancer {

    private int VIRTUAL_NODE_NUM = 5;

    /**
     * get hash code on 2^32 ring (md5散列的方式计算hash值)
     * @param key
     * @return
     */
    private long hash(String key) {

        // md5 byte
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        }

        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

    public RpcClientHandler doRoute(String serviceKey, List<RpcClientHandler> handlers) {

        // ------A1------A2-------A3------
        // -----------J1------------------
        TreeMap<Long, RpcClientHandler> addressRing = new TreeMap<>();
        for (RpcClientHandler handler: handlers) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("SHARD-" + handler.getServerAddress() + "-NODE-" + i);
                addressRing.put(addressHash, handler);
            }
        }

        long jobHash = hash(serviceKey);
        SortedMap<Long, RpcClientHandler> lastRing = addressRing.tailMap(jobHash);
        if (!lastRing.isEmpty()) {
            return lastRing.get(lastRing.firstKey());
        }
        return addressRing.firstEntry().getValue();
    }

    @Override
    public RpcClientHandler elect(String service, List<RpcClientHandler> candidates) {
        return doRoute(service, candidates);
    }

}
