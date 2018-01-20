package co.ifunny.envoy.api.core;

import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Snapshot {

    final private String version;
    final private Map<ResponseType, List<Message>> resources = new HashMap<>();

    public Snapshot(String version) {
        this.version = version;
    }

    public Snapshot addResource(ResponseType responseType, Message resource) {
        resources.computeIfAbsent(responseType, it -> new ArrayList<>()).add(resource);
        return this;
    }

    public  List<Message> getResources(ResponseType responseType) {
        return resources.getOrDefault(responseType, new ArrayList<>());
    }

    public String getVersion() {
        return version;
    }
}
