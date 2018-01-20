package co.ifunny.envoy.api.core;

import java.util.Objects;

public class Key {

    final private Object backedObject;

    public Key(Object backedObject) {
        this.backedObject = backedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key key = (Key) o;

        return Objects.equals(backedObject, key.backedObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backedObject);
    }
}
