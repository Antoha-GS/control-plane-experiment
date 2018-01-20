package co.ifunny.envoy.api.core;

public interface KeyFactory<T> {

    Key create(T object);
}
