package com.hirpc.protocol.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mwq0106
 * @date 2019/9/23
 */
public class KryoSerializer {
    public static byte[] serialize(Object obj) {

        Kryo kryo = kryoLocal.get();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Output output = new Output(byteArrayOutputStream);//<1>

        kryo.writeClassAndObject(output, obj);//<2>

        output.close();

        return byteArrayOutputStream.toByteArray();

    }


    public static <T> T deserialize(byte[] bytes) {

        Kryo kryo = kryoLocal.get();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

        Input input = new Input(byteArrayInputStream);

        input.close();

        return (T) kryo.readClassAndObject(input);

    }


    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {//<3>

        @Override
        protected Kryo initialValue(){

            Kryo kryo = new Kryo();

            kryo.setReferences(true);//默认值为true,强调作用

            kryo.setRegistrationRequired(false);//默认值为false,强调作用

            return kryo;

        }

    };
}
