package com.ekenya.chamakyc.service.impl.functions;

@FunctionalInterface
public interface QuadFunction<T,U,V,W,Z> {
    Z apply (T t,U u,V v,W w);
}
