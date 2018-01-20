package co.ifunny.envoy.api.core;

public interface Cache extends ConfigWatcher {

    void setSnapshot(Key key, Snapshot snapshot);
}
