/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codahale.gprcproxy;

import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

class StatsTracerFactory extends ServerStreamTracer.Factory {

  private final LongAdder requests = new LongAdder();
  private final LongAdder responseTime = new LongAdder();
  private final AtomicLong timestamp = new AtomicLong();
  private final Recorder latency = new Recorder(3);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Override
  public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {
    requests.increment();

    return new ServerStreamTracer() {
      final long start = System.nanoTime();

      @Override
      public void streamClosed(Status status) {
        final long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
        responseTime.add(duration);
        latency.recordValue(duration);
      }
    };
  }

  void start() {
    timestamp.set(System.nanoTime());
    executor.scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);
  }

  void stop() {
    executor.shutdown();
  }

  private void report() {
    final double t = (System.nanoTime() - timestamp.getAndSet(System.nanoTime())) * 1e-9;
    final Histogram h = latency.getIntervalHistogram();
    final double x = requests.sumThenReset() / t;
    final double r, n;
    if (x == 0) {
      r = n = 0;
    } else {
      r = (double) responseTime.sumThenReset() / x * 1e-6;
      n = x * r;
    }
    System.out.printf("Stats at %s ==============\n", Instant.now());
    System.out.printf("  Throughput: %2.2f req/sec\n", x);
    System.out.printf("  Avg Response Time: %2.4fs\n", r);
    System.out.printf("  Avg Concurrent Clients: %2.4fs\n", n);
    System.out.printf("  p50:  %2.2fms\n", h.getValueAtPercentile(50) * 1e-3);
    System.out.printf("  p75:  %2.2fms\n", h.getValueAtPercentile(75) * 1e-3);
    System.out.printf("  p95:  %2.2fms\n", h.getValueAtPercentile(95) * 1e-3);
    System.out.printf("  p99:  %2.2fms\n", h.getValueAtPercentile(99) * 1e-3);
    System.out.printf("  p999: %2.2fms\n", h.getValueAtPercentile(99.9) * 1e-3);
  }
}
