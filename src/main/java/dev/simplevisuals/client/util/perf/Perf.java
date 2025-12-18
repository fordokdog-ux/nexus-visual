package dev.simplevisuals.client.util.perf;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Perf {
    private Perf() {}

    public static final class Sample {
        public final String name;
        public long cpuNs;
        public long gpuNs;
        private long cpuStart;
        private int gpuQueryId;
        private boolean gpuActive;

        Sample(String name) { this.name = name; }

        void startCpu() { cpuStart = System.nanoTime(); }
        void endCpu() { cpuNs += Math.max(0, System.nanoTime() - cpuStart); }

        void startGpu() {
            if (!GPU_SUPPORTED) return;
            if (gpuActive) return;
            if (gpuQueryId == 0) gpuQueryId = GL15.glGenQueries();
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, gpuQueryId);
            gpuActive = true;
        }

        void endGpu() {
            if (!GPU_SUPPORTED || !gpuActive) return;
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            long elapsed;
            long[] tmp = new long[1];
            GL33.glGetQueryObjecti64v(gpuQueryId, GL15.GL_QUERY_RESULT, tmp);
            elapsed = tmp[0];
            gpuNs += Math.max(0, elapsed);
            gpuActive = false;
        }
    }

    private static final Map<String, Sample> current = new ConcurrentHashMap<>();
    private static final Map<String, double[]> ema = new ConcurrentHashMap<>();
    private static long lastPublishMs = 0L;
    private static long lastBeginMs = 0L;

    private static boolean checkedCaps = false;
    private static boolean GPU_SUPPORTED = false;

    private static void ensureCaps() {
        if (checkedCaps) return;
        checkedCaps = true;
        try {
            var caps = GL.getCapabilities();
            GPU_SUPPORTED = caps != null && caps.OpenGL33;
        } catch (Throwable t) {
            GPU_SUPPORTED = false;
        }
    }

    public static void beginFrame() {
        ensureCaps();
        current.values().forEach(s -> { s.cpuNs = 0; s.gpuNs = 0; });
    }

    public static void tryBeginFrame() {
        long now = System.currentTimeMillis();
        if (now - lastBeginMs >= 5) {
            beginFrame();
            lastBeginMs = now;
        }
    }

    public static void endFrame() {
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastPublishMs >= 250) {
            for (var e : current.entrySet()) {
                var s = e.getValue();
                double cpuMs = s.cpuNs / 1_000_000.0;
                double gpuMs = s.gpuNs / 1_000_000.0;
                var arr = ema.computeIfAbsent(e.getKey(), k -> new double[]{0,0});
                arr[0] = arr[0] * 0.8 + cpuMs * 0.2;
                arr[1] = arr[1] * 0.8 + gpuMs * 0.2;
            }
            lastPublishMs = nowMs;
        }
    }

    public static void startCpu(String name) {
        current.computeIfAbsent(name, Sample::new).startCpu();
    }

    public static void endCpu(String name) {
        Sample s = current.get(name);
        if (s != null) s.endCpu();
    }

    public static void startGpu(String name) {
        ensureCaps();
        if (!GPU_SUPPORTED) return;
        current.computeIfAbsent(name, Sample::new).startGpu();
    }

    public static void endGpu(String name) {
        Sample s = current.get(name);
        if (s != null) s.endGpu();
    }

    public static final class CpuScope implements AutoCloseable {
        private final String name;
        public CpuScope(String name) {
            this.name = name;
            Perf.startCpu(name);
        }
        @Override public void close() { Perf.endCpu(name); }
    }

    public static final class GpuScope implements AutoCloseable {
        private final String name;
        public GpuScope(String name) {
            this.name = name;
            Perf.startGpu(name);
        }
        @Override public void close() { Perf.endGpu(name); }
    }

    public static CpuScope scopeCpu(String name) { return new CpuScope(name); }
    public static GpuScope scopeGpu(String name) { return new GpuScope(name); }

    public static List<Row> snapshot() {
        double totalCpu = 0, totalGpu = 0;
        for (var e : current.entrySet()) {
            var s = e.getValue();
            double[] m = ema.get(e.getKey());
            double cpuMs = (m == null ? s.cpuNs / 1_000_000.0 : m[0]);
            double gpuMs = (m == null ? s.gpuNs / 1_000_000.0 : m[1]);
            totalCpu += cpuMs;
            totalGpu += gpuMs;
        }
        List<Row> rows = new ArrayList<>();
        for (var e : current.entrySet()) {
            var s = e.getValue();
            double[] m = ema.get(e.getKey());
            double cpuMs = (m == null ? s.cpuNs / 1_000_000.0 : m[0]);
            double gpuMs = (m == null ? s.gpuNs / 1_000_000.0 : m[1]);
            double cpuPct = totalCpu > 0 ? (cpuMs / totalCpu * 100.0) : 0.0;
            double gpuPct = totalGpu > 0 ? (gpuMs / totalGpu * 100.0) : 0.0;
            rows.add(new Row(e.getKey(), cpuMs, cpuPct, gpuMs, gpuPct));
        }
        rows.sort(Comparator.comparingDouble((Row r) -> -(r.cpuMs + r.gpuMs)));
        return rows;
    }

    public static final class Row {
        public final String name;
        public final double cpuMs;
        public final double cpuPct;
        public final double gpuMs;
        public final double gpuPct;
        public Row(String name, double cpuMs, double cpuPct, double gpuMs, double gpuPct) {
            this.name = name; this.cpuMs = cpuMs; this.cpuPct = cpuPct; this.gpuMs = gpuMs; this.gpuPct = gpuPct;
        }
    }
}
