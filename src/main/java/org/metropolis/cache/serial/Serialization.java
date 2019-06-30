package org.metropolis.cache.serial;


import io.protostuff.ProtobufIOUtil;
import org.metropolis.cache.alloc.Memory;
import io.protostuff.LinkedBuffer;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Serialization {

    private static final ThreadLocal<SpaceOutput> OUTPUT =
            ThreadLocal.withInitial(SpaceOutput::new);

    private static final ThreadLocal<SpaceInput> INPUT =
            ThreadLocal.withInitial(SpaceInput::new);

    private static final ConcurrentMap<Class, Schema>
        SCHEMA = new ConcurrentHashMap<>();

    private static final Schema
        VOID = RuntimeSchema.createFrom(Void.class);

    private final static <T> Schema<T> parseClazz(Class<T> clazz) {
        Schema schema = SCHEMA.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clazz);
            if (((RuntimeSchema) schema).getFields().isEmpty()) {
                if (! Serializable.class.isAssignableFrom(clazz)) {
                    throw new UnsupportedOperationException();
                }
                schema = VOID;
            }
            SCHEMA.putIfAbsent(clazz, schema);
        }
        return schema;
    }

    private static <T> SpaceWrapper writeObject(T message, Memory memory, Class<T> clazz) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64);
        try {
            ObjectOutputStream stream = new ObjectOutputStream(buffer);
            stream.writeObject(message);
        } catch (Exception e) {
            throw new SerialFailure(e);
        }
        SpaceWrapper space = (SpaceWrapper) memory.acquire(buffer.size());
        if (space == null) {
            throw new NoEnoughSpace();
        }
        try {
            SpaceOutput output = OUTPUT.get();
            output.wrap(space);
            buffer.writeTo(output);
            space.clazz = clazz;
            space.padding = (int) space.length() - buffer.size();
            return space;
        } catch (Exception e) {
            throw new SerialFailure(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T readObject(SpaceWrapper<T> space, Class<T> clazz) {
        try {
            ObjectInputStream stream = new ObjectInputStream(INPUT.get().wrap(space).buffer());
            return (T) stream.readObject();
        } catch (Exception e) {
            throw new SerialFailure(e);
        }
    }

    public static <T> SpaceWrapper write(T message, Memory memory) {
        Class<T> clazz = (Class<T>) message.getClass();
        Schema<T> schema = parseClazz(clazz);
        if (schema == VOID) {
            return writeObject(message, memory, clazz);
        }
        SpaceOutput output = OUTPUT.get();
        LinkedBuffer buffer = output.buffer();
        int length = ProtobufIOUtil.writeTo(buffer, message, schema);
        SpaceWrapper space = (SpaceWrapper) memory.acquire(length);
        if (space == null) {
            throw new NoEnoughSpace();
        }
        try {
            LinkedBuffer.writeTo(output.wrap(space), buffer);
        } catch (Exception e) {
            memory.release(space);
            throw new SerialFailure(e);
        }
        space.clazz = clazz;
        space.padding = (int) space.length() - length;
        buffer.clear();
        return space;
    }

    public static <T> T read(SpaceWrapper<T> space, Class<T> clazz) {
        Schema<T> schema = parseClazz(clazz);
        if (schema == VOID) {
            return readObject(space, clazz);
        }
        T message = schema.newMessage();
        try {
            synchronized (space) {
                if (space.free()) {
                    return null;
                }
                schema.mergeFrom(INPUT.get().wrap(space).input(), message);
            }
        } catch (Exception e) {
            throw new SerialFailure(e);
        }
        return message;
    }


    public static class NoEnoughSpace extends RuntimeException {

    }

    public static class SerialFailure extends RuntimeException {
        public SerialFailure(Exception e) {
            super(e);
        }
    }

}