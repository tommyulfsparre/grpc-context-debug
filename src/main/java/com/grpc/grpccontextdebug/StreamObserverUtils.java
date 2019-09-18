package com.grpc.grpccontextdebug;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

public class StreamObserverUtils {

  static <T> StreamObserver<T> forCompletableFuture(CompletableFuture<T> completableFuture) {
    return new StreamObserver<>() {

      private T value;

      @Override
      public void onNext(T value) {
        this.value = value;
      }

      @Override
      public void onError(Throwable t) {
        completableFuture.completeExceptionally(t);
      }

      @Override
      public void onCompleted() {
        completableFuture.complete(this.value);
      }
    };
  }
}
