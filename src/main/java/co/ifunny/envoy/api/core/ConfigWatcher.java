package co.ifunny.envoy.api.core;

import envoy.api.v2.Base;

import java.util.List;

public interface ConfigWatcher {

    Watch watch(ResponseType responseType, Base.Node envoyNode, String version, List<String> names);
}
