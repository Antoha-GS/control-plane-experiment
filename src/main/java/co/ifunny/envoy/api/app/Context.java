package co.ifunny.envoy.api.app;

import co.ifunny.envoy.api.api.ClusterDiscoveryService;
import co.ifunny.envoy.api.api.EndpointDiscoveryService;
import co.ifunny.envoy.api.api.ListenerDiscoveryService;
import co.ifunny.envoy.api.api.RouteDiscoveryService;
import co.ifunny.envoy.api.metrics.StatsDReporter;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import me.dinowernli.grpc.prometheus.Configuration;

import java.net.InetSocketAddress;

import static co.ifunny.envoy.api.Properties.*;

public class Context {

    private InetSocketAddress serverSocketAddress;
    private StatsDClient statsDClient;
    private Configuration prometheusConfiguration = Configuration.allMetrics();
    private StatsDReporter statsDReporter;
    private Consul consul;
    private ClusterDiscoveryService clusterDiscoveryService;
    private EndpointDiscoveryService endpointDiscoveryService;
    private ListenerDiscoveryService listenerDiscoveryService;
    private RouteDiscoveryService routeDiscoveryService;

    public void boot() {
        serverSocketAddress = new InetSocketAddress(SERVER_HOST.asString(), SERVER_PORT.asInteger());

        statsDClient = new NonBlockingStatsDClient(METRICS_PREFIX.asString(), STATSD_HOST.asString(), STATSD_PORT.asInteger());

        statsDReporter = new StatsDReporter(statsDClient);
        statsDReporter.start(prometheusConfiguration.getCollectorRegistry());

        consul = Consul.builder()
                .withHostAndPort(HostAndPort.fromParts(CONSUL_HOST.asString(), CONSUL_PORT.asInteger()))
                .build();
        final CatalogClient consulCatalog = consul.catalogClient();
        final KeyValueClient consulKeyValue = consul.keyValueClient();

        clusterDiscoveryService = new ClusterDiscoveryService(consulCatalog, consulKeyValue);
        endpointDiscoveryService = new EndpointDiscoveryService(consulCatalog, consulKeyValue);
        listenerDiscoveryService = new ListenerDiscoveryService(consulCatalog, consulKeyValue);
        routeDiscoveryService = new RouteDiscoveryService(consulCatalog, consulKeyValue);
    }

    public void cleanup() {
        if (statsDReporter != null) {
            statsDReporter.stop();
        }
        if (statsDClient != null) {
            statsDClient.stop();
        }
        consul.destroy();
    }

    public InetSocketAddress getServerSocketAddress() {
        return serverSocketAddress;
    }

    public Configuration getPrometheusConfiguration() {
        return prometheusConfiguration;
    }

    public ClusterDiscoveryService getClusterDiscoveryService() {
        return clusterDiscoveryService;
    }

    public EndpointDiscoveryService getEndpointDiscoveryService() {
        return endpointDiscoveryService;
    }

    public ListenerDiscoveryService getListenerDiscoveryService() {
        return listenerDiscoveryService;
    }

    public RouteDiscoveryService getRouteDiscoveryService() {
        return routeDiscoveryService;
    }
}
