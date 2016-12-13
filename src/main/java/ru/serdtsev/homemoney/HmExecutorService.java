package ru.serdtsev.homemoney;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class HmExecutorService implements ExecutorService {
  private static Logger log = LoggerFactory.getLogger(HmExecutorService.class);
  private static HmExecutorService instance;
  private ExecutorService executorService;

  private HmExecutorService(ServletConfig config) {
    final String threadsNumParamName = "threads.num";
    String sThreadsNum = config.getInitParameter(threadsNumParamName);
    int nThreadsNum = 10;
    try {
      if (!Strings.isNullOrEmpty(sThreadsNum)) {
        nThreadsNum = Integer.decode(sThreadsNum);
      }
    } catch (NumberFormatException e) {
      log.warn("Incorrect value parameter \"{}\" in web.xml: {}. Used default value {}.", threadsNumParamName, sThreadsNum, nThreadsNum);
    }
    this.executorService = Executors.newFixedThreadPool(nThreadsNum);
  }

  public static HmExecutorService getInstance() {
    return instance;
  }

  public static HmExecutorService initInstance(ServletConfig config) {
    if (instance == null) {
      instance = new HmExecutorService(config);
    }
    return instance;
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return executorService.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(@NotNull Callable<T> task) {
    return executorService.submit(task);
  }

  @Override
  public <T> Future<T> submit(@NotNull Runnable task, T result) {
    return executorService.submit(task, result);
  }

  @Override
  public Future<?> submit(@NotNull Runnable task) {
    return executorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return executorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
    return executorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return executorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(@NotNull Runnable command) {
    executorService.execute(command);
  }
}
