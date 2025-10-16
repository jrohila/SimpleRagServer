package io.github.jrohila.simpleragserver.util;

import io.github.jrohila.simpleragserver.dto.SearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Cosine-DBSCAN with auto-epsilon for small N (<= ~1000).
 * - Accepts List<SearchResultDTO> with embedding vectors.
 * - L2-normalizes vectors.
 * - If eps is null, estimates via the 90th percentile of k-distances (k = minPts - 1).
 * - Returns clusters as List<List<SearchResultDTO>>; noise points are dropped.
 */
public final class VectorClustering {

    private static final Logger log = LoggerFactory.getLogger(VectorClustering.class);

    private VectorClustering() {}

    public static List<List<SearchResultDTO>> dbscanAutoCosine(List<SearchResultDTO> items, Integer minPts, Double eps) {
        if (items == null || items.isEmpty()) return List.of();
        try { log.info("VectorClustering: received items={}", items.size()); } catch (Exception ignore) {}

        // Collect normalized vectors and keep mapping
        List<SearchResultDTO> data = new ArrayList<>();
        List<double[]> X = new ArrayList<>();
        for (SearchResultDTO dto : items) {
            double[] v = toArray(dto.getEmbedding());
            if (v.length == 0) continue;
            normalizeInPlace(v);
            data.add(dto);
            X.add(v);
        }
    int n = X.size();
    try { log.info("VectorClustering: usable items with embedding={}", n); } catch (Exception ignore) {}
    if (n == 0) return List.of();

        int mpts = (minPts == null || minPts < 3)
                ? Math.max(5, (int) Math.round(Math.log(n) * 2))
                : minPts;

    double epsUse = (eps != null && eps > 0) ? eps : autoEps(X, Math.max(1, mpts - 1), 0.90);
    try { log.info("VectorClustering: params minPts={} eps={}", mpts, String.format(java.util.Locale.ROOT, "%.4f", epsUse)); } catch (Exception ignore) {}

        // Precompute neighbors within eps (cosine distance)
        List<int[]> neighbors = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Integer> nb = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double d = cosineDistance(X.get(i), X.get(j));
                if (d <= epsUse) nb.add(j);
            }
            neighbors.add(nb.stream().mapToInt(Integer::intValue).toArray());
        }

        int[] labels = new int[n];
        Arrays.fill(labels, -999); // -999=unvisited, -1=noise, >=0 cluster id
        int clusterId = 0;

        for (int i = 0; i < n; i++) {
            if (labels[i] != -999) continue;
            int[] nb = neighbors.get(i);
            if (nb.length + 1 < mpts) { labels[i] = -1; continue; } // not core
            labels[i] = clusterId;
            Deque<Integer> q = new ArrayDeque<>();
            for (int j : nb) q.add(j);

            while (!q.isEmpty()) {
                int p = q.removeFirst();
                if (labels[p] == -1) labels[p] = clusterId;   // border -> cluster
                if (labels[p] != -999) continue;              // visited
                labels[p] = clusterId;

                int[] nbp = neighbors.get(p);
                if (nbp.length + 1 >= mpts) {
                    for (int t : nbp) if (labels[t] == -999) q.addLast(t);
                }
            }
            clusterId++;
        }

        Map<Integer, List<SearchResultDTO>> map = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int lab = labels[i];
            if (lab < 0) continue; // drop noise
            map.computeIfAbsent(lab, k -> new ArrayList<>()).add(data.get(i));
        }
        List<List<SearchResultDTO>> clusters = new ArrayList<>(map.values());
        try {
            List<Integer> sizes = new ArrayList<>();
            int clusterTotal = 0;
            for (List<SearchResultDTO> c : clusters) { sizes.add(c.size()); clusterTotal += c.size(); }
            int noise = n - clusterTotal; // points labeled as noise (-1) are dropped from clusters
            log.info("VectorClustering: clusters={} sizes={} noise={}", clusters.size(), sizes, noise);
        } catch (Exception ignore) {}
        return clusters;
    }

    private static double[] toArray(List<Float> vec) {
        if (vec == null || vec.isEmpty()) return new double[0];
        double[] a = new double[vec.size()];
        for (int i = 0; i < vec.size(); i++) a[i] = vec.get(i) == null ? 0.0 : vec.get(i);
        return a;
    }

    private static void normalizeInPlace(double[] v) {
        double s = 0.0; for (double x : v) s += x * x;
        if (s <= 0) return;
        double inv = 1.0 / Math.sqrt(s);
        for (int i = 0; i < v.length; i++) v[i] *= inv;
    }

    private static double autoEps(List<double[]> X, int k, double quantile) {
        int n = X.size();
        double[] kth = new double[n];
        for (int i = 0; i < n; i++) {
            double[] d = new double[n - 1];
            int p = 0;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                d[p++] = cosineDistance(X.get(i), X.get(j));
            }
            Arrays.sort(d);
            int idx = Math.max(0, Math.min(d.length - 1, k - 1));
            kth[i] = d[idx];
        }
        Arrays.sort(kth);
        int qi = Math.max(0, Math.min(kth.length - 1, (int) Math.round(quantile * (kth.length - 1))));
        return kth[qi];
    }

    /** Cosine distance for unit-normalized vectors is 1 - dot product; here safe for any vectors. */
    private static double cosineDistance(double[] a, double[] b) {
        double dot = 0.0, na = 0.0, nb = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            double ai = a[i], bi = b[i];
            dot += ai * bi;
            na += ai * ai;
            nb += bi * bi;
        }
        if (na == 0.0 || nb == 0.0) return 1.0; // maximally distant if degenerate
        return 1.0 - (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
