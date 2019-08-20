package de.klaro.reformcloud2.executor.api.common.utility.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Links {

    public static List<String> toLowerCase(List<String> list) {
        List<String> strings = new ArrayList<>();
        list.forEach(new Consumer<String>() {
            @Override
            public void accept(String string) {
                strings.add(string.toLowerCase());
            }
        });

        return strings;
    }

    public static <T> List<T> unmodifiable(List<T> in) {
        return Collections.unmodifiableList(in);
    }

    public static <T> List<T> newList(List<T> in) {
        return new ArrayList<>(in);
    }

    public static <T, F> List<F> apply(List<T> in, Function<T, F> function) {
        List<F> out = new ArrayList<>();
        in.forEach(new Consumer<T>() {
            @Override
            public void accept(T t) {
                out.add(function.apply(t));
            }
        });

        return out;
    }

    public static <T> T filter(List<T> in, Predicate<T> predicate) {
        for (T t : in) {
            if (predicate.test(t)) {
                return t;
            }
        }

        return null;
    }

    public static <T, F> F filterAndApply(List<T> in, Predicate<T> predicate, Function<T, F> function) {
        for (T t : in) {
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }

        return null;
    }

    public static <F, T> List<T> getValues(Map<F, T> in, Predicate<F> predicate) {
        List<T> out = new ArrayList<>();
        in.entrySet().forEach(new Consumer<Map.Entry<F, T>>() {
            @Override
            public void accept(Map.Entry<F, T> ftEntry) {
                if (predicate.test(ftEntry.getKey())) {
                    out.add(ftEntry.getValue());
                }
            }
        });
        return out;
    }

    public static <F, T> void forEachValues(Map<F, T> map, Consumer<T> consumer) {
        map.forEach(new BiConsumer<F, T>() {
            @Override
            public void accept(F f, T t) {
                consumer.accept(t);
            }
        });
    }

    public static <F> void forEach(List<F> list, Consumer<F> consumer) {
        list.forEach(consumer);
    }

    public static <F, T, X> List<X> keyApply(Map<F, T> map, Function<F, X> fxFunction) {
        List<X> out = new ArrayList<>();
        map.keySet().forEach(new Consumer<F>() {
            @Override
            public void accept(F f) {
                out.add(fxFunction.apply(f));
            }
        });

        return out;
    }
}
