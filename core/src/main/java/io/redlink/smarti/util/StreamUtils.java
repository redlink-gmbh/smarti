/*
 * Copyright (c) 2016 Redlink GmbH
 */
package io.redlink.smarti.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 */
public class StreamUtils {

    public static <T> Predicate<T> distinctBy(Function<? super T, Object> key) {
        final Map<Object,Boolean> history = new ConcurrentHashMap<>();
        return t -> history.putIfAbsent(key.apply(t), Boolean.TRUE) == null;
    }

    private StreamUtils() {
        throw new AssertionError("No io.redlink.reisebuddy.util.StreamUtils instances for you!");
    }
}
