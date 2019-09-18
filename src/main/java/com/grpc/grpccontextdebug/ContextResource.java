package com.grpc.grpccontextdebug;

import static com.grpc.example.ContextExampleGrpc.ContextExampleImplBase;

import com.grpc.example.FireAndForgetRequest;
import com.grpc.example.FireAndForgetResponse;
import com.grpc.example.FirstRequest;
import com.grpc.example.FirstResponse;
import com.grpc.example.RunTestRequest;
import com.grpc.example.RunTestResponse;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextResource extends ContextExampleImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(ContextResource.class);
  private final ContextClient contextClient;

  public ContextResource(final ContextClient contextClient) {
    this.contextClient = contextClient;
  }

  @Override
  public void runTest(final RunTestRequest request,
                      final StreamObserver<RunTestResponse> responseObserver) {
    executeTest(request).whenComplete(respond(responseObserver));
  }

  @Override
  public void first(final FirstRequest request,
                    final StreamObserver<FirstResponse> responseObserver) {
    final FirstResponse response = FirstResponse.newBuilder().setResponse("FirstResponse").build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void fireAndForget(final FireAndForgetRequest request,
                            final StreamObserver<FireAndForgetResponse> responseObserver) {
    final FireAndForgetResponse response =
        FireAndForgetResponse.newBuilder().setResponse("FireAndForgetResponse").build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private CompletionStage<RunTestResponse> executeTest(final RunTestRequest request) {
    /*
     * {@link ContextClient.first} is to simulated an RPC that should propagate {@link io.grpc.Context}.
     */
    return contextClient.first(request.getRunAsync())
        .thenApply(str -> RunTestResponse.newBuilder().setResponse(str).build())
        .whenComplete(
            (str, throwable) -> {
              /* When the {@link ContextClient.first} RPC finish we want to fire and forget an additional RPC.
               * This could simulate a remote logging API that uses gRPC as the transport.
               * This will get cancelled due to gRPC auto-cancellation.
               */
              contextClient
                  .fireAndForgetWithListenableFuture()
                  .exceptionally(
                      ex -> {
                        LOG.error("fireAndForget RPC failed: {}", ex, ex);
                        return null;
                      });
            });
  }

  private static <T> BiConsumer<T, Throwable> respond(final StreamObserver<T> responseObserver) {
    return (response, throwable) -> {
      if (throwable != null) {
        responseObserver.onError(throwable);
      } else {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    };
  }
}
