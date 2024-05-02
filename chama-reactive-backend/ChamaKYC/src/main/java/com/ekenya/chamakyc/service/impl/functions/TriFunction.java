package com.ekenya.chamakyc.service.impl.functions;

/**
 * @author Alex Maina
 * @created 25/02/2022
 **/
@FunctionalInterface
public interface TriFunction<T,U,V,W> {
    W apply(T t, U u, V v);
}
