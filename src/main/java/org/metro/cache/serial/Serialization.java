package org.metro.cache.serial;


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
            ThreadLocal.withInitial(()->new SpaceOutput());

    private static final ThreadLocal<SpaceInput> INPUT =
            ThreadLocal.withInitial(()->new SpaceInput());

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

    public static <T> Space write(T message, Class<T> clazz, Memory memory) throws Exception {
        Schema<T> schema = SCHEMA.get(clazz);
        SpaceOutput output = OUTPUT.get();
        LinkedBuffer buffer = output.buffer();
        int length = ProtostuffIOUtil.writeTo(buffer, message, schema);
        Space space = memory.acquire(length);
        if (space != null) {
            LinkedBuffer.writeTo(output.wrap(space), buffer);
        }
        buffer.clear();
        return space;
    }

    public static <T> T read(Space space, Class<T> clazz) throws Exception {
        Schema<T> schema = SCHEMA.get(clazz);
        T message = schema.newMessage();
        schema.mergeFrom(INPUT.get().wrap(space).input(), message);
        return message;
    }

}