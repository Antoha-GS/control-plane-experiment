package co.ifunny.envoy.api.core;

import com.google.protobuf.Message;

import java.util.List;

public class Response {

    final private String version;
    final private List<Message> resources;
    final private boolean canary;

    public Response(String version, List<Message> resources) {
        this(version, resources, false);
    }

    public Response(String version, List<Message> resources, boolean canary) {
        this.version = version;
        this.resources = resources;
        this.canary = canary;
    }

    public String getVersion() {
        return version;
    }

    public List<Message> getResources() {
        return resources;
    }

    public boolean isCanary() {
        return canary;
    }
}
