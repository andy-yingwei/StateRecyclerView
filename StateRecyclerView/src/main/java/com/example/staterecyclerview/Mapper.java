package com.example.staterecyclerview;

@FunctionalInterface
public interface Mapper<T, R> {
    R apply(T input);
}
