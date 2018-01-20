package co.ifunny.envoy.api.api;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import envoy.api.v2.AddressOuterClass.Address;
import envoy.api.v2.AddressOuterClass.SocketAddress;
import envoy.api.v2.ConfigSourceOuterClass.ApiConfigSource;
import envoy.api.v2.ConfigSourceOuterClass.ApiConfigSource.ApiType;
import envoy.api.v2.ConfigSourceOuterClass.ConfigSource;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import envoy.api.v2.Lds.Filter;
import envoy.api.v2.Lds.FilterChain;
import envoy.api.v2.Lds.Listener;
import envoy.api.v2.ListenerDiscoveryServiceGrpc;
import envoy.api.v2.filter.network.HttpConnectionManagerOuterClass.HttpConnectionManager;
import envoy.api.v2.filter.network.HttpConnectionManagerOuterClass.HttpConnectionManager.CodecType;
import envoy.api.v2.filter.network.HttpConnectionManagerOuterClass.HttpFilter;
import envoy.api.v2.filter.network.HttpConnectionManagerOuterClass.Rds;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static co.ifunny.envoy.api.util.Protobuf.messageToStruct;

public class ListenerDiscoveryService extends ListenerDiscoveryServiceGrpc.ListenerDiscoveryServiceImplBase {

    final private static Logger logger = LoggerFactory.getLogger(ListenerDiscoveryService.class);

    final private static String ENVOY_HTTP_FILTER_NAME = "envoy.http_connection_manager";
    final private static String XDS_CLUSTER_NAME = "xds_cluster";
    final private static String ROUTER_NAME = "envoy.router";
    final private static AtomicLong streamNonce = new AtomicLong();

    final private CatalogClient consulCatalog;
    final private KeyValueClient consulKeyValue;

    public ListenerDiscoveryService(CatalogClient consulCatalog, KeyValueClient consulKeyValue) {
        this.consulCatalog = consulCatalog;
        this.consulKeyValue = consulKeyValue;
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamListeners(StreamObserver<DiscoveryResponse> responseObserver) {
        return new StreamObserver<DiscoveryRequest>() {
            @Override
            public void onNext(DiscoveryRequest request) {
                responseObserver.onNext(buildListenersResponse(request));
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Encountered error in streamListeners", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void fetchListeners(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        responseObserver.onNext(buildListenersResponse(request));
        responseObserver.onCompleted();
        //responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is unimplemented").asException());
    }

    private DiscoveryResponse buildListenersResponse(DiscoveryRequest request) {
        try {
            logger.info("Request: {}", JsonFormat.printer().print(request));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Request", e);
        }

        final String typeUrl = request.getTypeUrl();
        final String listenerName = "listener";
        final String versionInfo = "0";
        final String ingressHttpStatPrefix = "ingress_http";
        final String ingressListenerHost = "0.0.0.0";
        final int ingressListenerPort = 9090;

        HttpConnectionManager httpConnectionManager = HttpConnectionManager.newBuilder()
                .setStatPrefix(ingressHttpStatPrefix)
                .setCodecType(CodecType.HTTP2)
                .setRds(Rds.newBuilder()
                        .setRouteConfigName("local_route")
                        .setConfigSource(ConfigSource.newBuilder()
                                .setApiConfigSource(ApiConfigSource.newBuilder()
                                        .setApiType(ApiType.GRPC)
                                        .addClusterName(XDS_CLUSTER_NAME))))
                .addHttpFilters(HttpFilter.newBuilder().setName(ROUTER_NAME))
                .build();

        Listener listener = Listener.newBuilder()
                .setName(listenerName)
                .setAddress(Address.newBuilder()
                        .setSocketAddress(SocketAddress.newBuilder()
                                .setProtocol(SocketAddress.Protocol.TCP)
                                .setAddress(ingressListenerHost)
                                .setPortValue(ingressListenerPort)))
                .addFilterChains(FilterChain.newBuilder()
                        .addFilters(Filter.newBuilder()
                                .setName(ENVOY_HTTP_FILTER_NAME)
                                .setConfig(messageToStruct(httpConnectionManager))))
                .build();

        DiscoveryResponse response = DiscoveryResponse.newBuilder()
                .setTypeUrl(typeUrl)
                .setVersionInfo(versionInfo)
                .setCanary(false)
                .setNonce(String.valueOf(streamNonce.incrementAndGet()))
                .addResources(Any.pack(listener))
                .build();

        try {
            logger.info("Response: {}", JsonFormat.printer()
                    .usingTypeRegistry(TypeRegistry.newBuilder().add(Listener.getDescriptor()).build())
                    .print(response));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Response", e);
        }

        return response;
    }
}
