package com.grpc.grpccontextdebug;

import io.grpc.Context;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutorUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutorUtils.class);

  public static Executor forkedContextExecutor(final Executor e) {
    final class ForkedContextExecutor implements Executor {

      @Override
      public void execute(Runnable r) {
        LOG.info("execute");
        e.execute(Context.current().fork().wrap(r));
      }
    }

    return new ForkedContextExecutor();
  }

  public static ThreadFactory newForkedContextThreadFactory(ThreadFactory delegate) {
    return new ForkedContextThreadFactory(delegate);
  }

  public static ThreadFactory newForkedContextThreadFactory() {
    return new ForkedContextThreadFactory(Executors.defaultThreadFactory());
  }

  static class ForkedContextThreadFactory implements ThreadFactory {

    private final ThreadFactory delegate;

    ForkedContextThreadFactory(ThreadFactory d) {
      delegate = d;
    }

    @Override
    public Thread newThread(Runnable r) {
      return delegate.newThread(Context.current().fork().wrap(r));
    }
  }
}
