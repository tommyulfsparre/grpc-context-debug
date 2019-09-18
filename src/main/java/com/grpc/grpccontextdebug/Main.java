package com.grpc.grpccontextdebug;

import static com.grpc.example.ContextExampleGrpc.newFutureStub;
import static com.grpc.example.ContextExampleGrpc.newStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Application entry point.
 */
public final class Main {

  private Main() {
  }

  /**
   * Runs the application.
   *
   * @param args command-line arguments
   */
  public static void main(final String... args) throws InterruptedException, IOException {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final int port = 50051;

    final ManagedChannel channelFirst = ManagedChannelBuilder.forAddress("127.0.0.1", port)
        .usePlaintext()
        .build();

    final ContextClient contextClient = new ContextClient(newFutureStub(channelFirst),
                                                          newStub(channelFirst));

    final Server server =
        ServerBuilder.forPort(port)
            .addService(new ContextResource(contextClient))
            .addService(ProtoReflectionService.newInstance())
            .build();

    System.err.println("*** starting gRPC Server");

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.err.println("*** shutting down gRPC Server since JVM is shutting down");
                  server.shutdown();
                  System.err.println("*** Server shut down");
                }));

    server.start();
    server.awaitTermination();
  }
}
