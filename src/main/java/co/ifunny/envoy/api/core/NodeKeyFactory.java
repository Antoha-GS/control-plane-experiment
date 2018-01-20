package co.ifunny.envoy.api.core;

import envoy.api.v2.Base;

public class NodeKeyFactory implements KeyFactory<Base.Node> {

    @Override
    public Key create(Base.Node node) {
        return new Key(node);
    }
}
