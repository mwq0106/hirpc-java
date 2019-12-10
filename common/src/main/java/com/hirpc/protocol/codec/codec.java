package com.hirpc.protocol.codec;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * @author mwq0106
 * @date 2019/10/9
 */
public interface codec {
    Object decode(ByteBuf buffer) throws IOException;
    void encode(ByteBuf byteBuf, Object message) throws IOException;
}
