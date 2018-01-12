package co.ifunny.envoy.api;

import co.ifunny.envoy.api.app.Context;
import io.grpc.BindableService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {

    final private static Logger logger = LoggerFactory.getLogger(Server.class);

    final private Context applicationContext;
    private io.grpc.Server server;

    private Server() {
        this.applicationContext = new Context();
        Runtime.getRuntime().addShutdownHook(new Thread(Server.this::stop));
    }

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

    private void start() throws IOException, InterruptedException {
        applicationContext.boot();

        server = buildServer();
        server.start();
        logger.info("Server started, listening on " + applicationContext.getServerSocketAddress());

        server.awaitTermination();
    }

    private void stop() {
        logger.info("Shutting down server since JVM is shutting down");
        if (server != null) {
            server.shutdown();
        }
        applicationContext.cleanup();
        logger.info("Server shut down");
    }

    private io.grpc.Server buildServer() {
        return NettyServerBuilder.forAddress(applicationContext.getServerSocketAddress())
                .addService(getInstrumented(applicationContext.getClusterDiscoveryService()))
                .addService(getInstrumented(applicationContext.getEndpointDiscoveryService()))
                .addService(getInstrumented(applicationContext.getListenerDiscoveryService()))
                .addService(getInstrumented(applicationContext.getRouteDiscoveryService()))
                // It is used by the gRPC command line tool to introspect server protos and send/receive test RPCs
                .addService(ProtoReflectionService.newInstance())
                .build();
    }

    private ServerServiceDefinition getInstrumented(BindableService service) {
        final MonitoringServerInterceptor monitoringServerInterceptor
                = MonitoringServerInterceptor.create(applicationContext.getPrometheusConfiguration());

        return ServerInterceptors.intercept(service, monitoringServerInterceptor);
    }
}
