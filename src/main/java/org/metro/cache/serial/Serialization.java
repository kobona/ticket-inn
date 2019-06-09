package org.metro.cache.serial;


import io.protostuff.ProtobufIOUtil;
import org.metro.cache.alloc.Allocator.Space;
import org.metro.cache.alloc.Memory;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Serialization {

    private static final ThreadLocal<SpaceOutput> OUTPUT =
            ThreadLocal.withInitial(SpaceOutput::new);

    private static final ThreadLocal<SpaceInput> INPUT =
            ThreadLocal.withInitial(SpaceInput::new);

    private static final ConcurrentMap<Class, Schema>
        SCHEMA = new ConcurrentHashMap<>();

    private final static <T> Schema<T> parseClazz(Class<T> clazz) {
        Schema schema = SCHEMA.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clazz);
            SCHEMA.putIfAbsent(clazz, schema);
        }
        return schema;
    }

    public static <T> SpaceWrapper write(T message, Memory memory) throws Exception {
        Class<T> clazz = (Class<T>) message.getClass();
        Schema<T> schema = parseClazz(clazz);
        SpaceOutput output = OUTPUT.get();
        LinkedBuffer buffer = output.buffer();
        int length = ProtobufIOUtil.writeTo(buffer, message, schema);
        SpaceWrapper space = (SpaceWrapper) memory.acquire(length);
        if (space != null) {
            LinkedBuffer.writeTo(output.wrap(space), buffer);
            space.clazz = clazz;
            space.padding = (int) space.length() - length;
        }
        buffer.clear();
        return space;
    }

    public static <T> T read(SpaceWrapper<T> space, Class<T> clazz) throws Exception {
        Schema<T> schema = parseClazz(clazz);
        T message = schema.newMessage();
        synchronized (space) {
            if (space.free()) {
                return null;
            }
            schema.mergeFrom(INPUT.get().wrap(space).input(), message);
        }
        return message;
    }

}