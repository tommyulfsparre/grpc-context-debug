package com.grpc.grpccontextdebug;

import static com.spotify.futures.ListenableFuturesExtra.toCompletableFuture;

import com.google.common.base.Functions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.grpc.example.ContextExampleGrpc;
import com.grpc.example.FireAndForgetRequest;
import com.grpc.example.FireAndForgetResponse;
import com.grpc.example.FirstRequest;
import com.grpc.example.FirstResponse;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextClient {

  private static final Logger LOG = LoggerFactory.getLogger(ContextClient.class);
  private final ContextExampleGrpc.ContextExampleFutureStub futureStub;
  private final ContextExampleGrpc.ContextExampleStub asyncStub;
  private final Executor executor;

  public ContextClient(final ContextExampleGrpc.ContextExampleFutureStub futureStub,
                       final ContextExampleGrpc.ContextExampleStub asyncStub) {
    this.futureStub = futureStub;
    this.asyncStub = asyncStub;
    this.executor = ExecutorUtils.forkedContextExecutor(MoreExecutors.directExecutor());
  }

  public CompletableFuture<String> first(final boolean runAsync) {
    LOG.info("Calling first method");

    final FirstRequest req = FirstRequest.newBuilder().build();

    final ListenableFuture<FirstResponse> future =
        futureStub.withDeadlineAfter(2, TimeUnit.SECONDS).first(req);

    if (runAsync) {
      /*
       * Calling {@link java.util.concurrent.thenApplyAsync} will lose the current context
       * as it's not run on a {@link io.grpc.Context} aware executor.
       */
      LOG.info("Calling first with async");
      return toCompletableFuture(future).thenApplyAsync(FirstResponse::getResponse);
    }

    /*
     * If we call {@link java.util.concurrent.thenApply} gRPC auto-cancellation will cancel the
     * {@link ContextClient.fireAndForgetWithListenableFuture} RPC.
     */
    return toCompletableFuture(future).thenApply(FirstResponse::getResponse);
  }

  /*
   *  This work by forking the current {@link io.grpc.Context} to avoid cascading
   *  cancellation.
   */
  public CompletableFuture<String> fireAndForget() {
    LOG.info("Calling fireAndForget method");

    final FireAndForgetRequest req = FireAndForgetRequest.newBuilder().build();

    CompletableFuture<FireAndForgetResponse> completableFuture = new CompletableFuture<>();

    final StreamObserver<FireAndForgetResponse> streamObserver =
        StreamObserverUtils.forCompletableFuture(completableFuture);

    Context.current().fork().run(
        () -> asyncStub.withDeadlineAfter(2, TimeUnit.SECONDS).fireAndForget(req, streamObserver));

    return completableFuture.thenApply(FireAndForgetResponse::getResponse);
  }

  /*
   *  This will fail with an "io.grpc.StatusRuntimeException: CANCELLED: io.grpc.Context was cancelled
   *  without error" the second time the runTest RPC is invoked.
   */
  public CompletableFuture<String> fireAndForgetWithListenableFuture() {
    LOG.info("Calling fireAndForgetWithListenableFuture method");

    final FireAndForgetRequest req = FireAndForgetRequest.newBuilder().build();

    final ListenableFuture<FireAndForgetResponse> future =
        futureStub.withDeadlineAfter(2, TimeUnit.SECONDS).fireAndForget(req);

    final ListenableFuture<FireAndForgetResponse> transform =
        Futures.transform(future, Functions.identity(), executor);

    return toCompletableFuture(transform).thenApply(FireAndForgetResponse::getResponse);
  }

}
