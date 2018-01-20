package co.ifunny.envoy.api.api;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.option.QueryOptions;
import envoy.api.v2.AddressOuterClass.Address;
import envoy.api.v2.AddressOuterClass.SocketAddress;
import envoy.api.v2.AddressOuterClass.SocketAddress.Protocol;
import envoy.api.v2.Base.Endpoint;
import envoy.api.v2.Discovery.DiscoveryRequest;
import envoy.api.v2.Discovery.DiscoveryResponse;
import envoy.api.v2.Eds.*;
import envoy.api.v2.EndpointDiscoveryServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import static envoy.api.v2.EndpointDiscoveryServiceGrpc.METHOD_STREAM_LOAD_STATS;

public class EndpointDiscoveryService extends EndpointDiscoveryServiceGrpc.EndpointDiscoveryServiceImplBase {

    final private static Logger logger = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    final private static AtomicLong streamNonce = new AtomicLong();

    final private CatalogClient consulCatalog;
    final private KeyValueClient consulKeyValue;

    public EndpointDiscoveryService(CatalogClient consulCatalog, KeyValueClient consulKeyValue) {
        this.consulCatalog = consulCatalog;
        this.consulKeyValue = consulKeyValue;
    }

    @Override
    public StreamObserver<DiscoveryRequest> streamEndpoints(StreamObserver<DiscoveryResponse> responseObserver) {
        return new StreamObserver<DiscoveryRequest>() {
            @Override
            public void onNext(DiscoveryRequest request) {
                responseObserver.onNext(buildEndpointsResponse(request));
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("Encountered error in streamEndpoints", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void fetchEndpoints(DiscoveryRequest request, StreamObserver<DiscoveryResponse> responseObserver) {
        responseObserver.onNext(buildEndpointsResponse(request));
        responseObserver.onCompleted();
        //responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is unimplemented").asException());
    }

    @Override
    public StreamObserver<LoadStatsRequest> streamLoadStats(StreamObserver<LoadStatsResponse> responseObserver) {
        logger.info("[{}] WTF method?", METHOD_STREAM_LOAD_STATS);
        return super.streamLoadStats(responseObserver);
    }

    private DiscoveryResponse buildEndpointsResponse(DiscoveryRequest request) {
        try {
            logger.info("Request: {}", JsonFormat.printer().print(request));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Request", e);
        }

        final String typeUrl = request.getTypeUrl();
        final String clusterName = "cluster";
        final String endpointAddress = "127.0.0.1";
        final int endpointPort = 12345;

        ClusterLoadAssignment clusterLoadAssignment = ClusterLoadAssignment.newBuilder()
                .setClusterName(clusterName)
                .addEndpoints(LocalityLbEndpoints.newBuilder()
                        .setLocality(request.getNode().getLocality())
                        .addLbEndpoints(LbEndpoint.newBuilder()
                                .setEndpoint(Endpoint.newBuilder()
                                        .setAddress(Address.newBuilder()
                                                .setSocketAddress(SocketAddress.newBuilder()
                                                        .setProtocol(Protocol.TCP)
                                                        .setAddress(endpointAddress)
                                                        .setPortValue(endpointPort))))))
                .build();

        DiscoveryResponse response = DiscoveryResponse.newBuilder()
                .setTypeUrl(typeUrl)
                .setVersionInfo("0")
                .setCanary(false)
                .setNonce(String.valueOf(streamNonce.incrementAndGet()))
                .addResources(Any.pack(clusterLoadAssignment))
                .build();

        try {
            logger.info("Response: {}", JsonFormat.printer()
                    .usingTypeRegistry(TypeRegistry.newBuilder().add(ClusterLoadAssignment.getDescriptor()).build())
                    .print(response));
        } catch (InvalidProtocolBufferException e) {
            logger.error("Failed to print Response", e);
        }

        return response;
    }
}
