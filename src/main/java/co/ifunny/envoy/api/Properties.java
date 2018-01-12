package co.ifunny.envoy.api;

public enum Properties {

    SERVER_HOST("co.ifunny.envoy.api.server.host", "0.0.0.0"),
    SERVER_PORT("co.ifunny.envoy.api.server.port", "8080"),

    CONSUL_HOST("co.ifunny.envoy.api.consul.host", "127.0.0.1"),
    CONSUL_PORT("co.ifunny.envoy.api.consul.port", "8500"),

    METRICS_PREFIX("co.ifunny.envoy.api.metrics.prefix", "co.ifunny.envoy.api"),
    STATSD_HOST("co.ifunny.envoy.api.statsd.host", "127.0.0.1"),
    STATSD_PORT("co.ifunny.envoy.api.statsd.port", "8125");

    final private String name;
    final private String defaultValue;

    Properties(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String asString() {
        return System.getProperty(this.name, this.defaultValue);
    }

    public Integer asInteger() {
        return Integer.valueOf(asString());
    }
}
