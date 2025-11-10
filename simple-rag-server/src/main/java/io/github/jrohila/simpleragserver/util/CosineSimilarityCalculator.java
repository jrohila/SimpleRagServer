package io.github.jrohila.simpleragserver.util;

import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class CosineSimilarityCalculator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CosineSimilarityCalculator.class);

    /**
     * Checks if the vector 'a' is similar to the set of vectors 'vectors',
     * using a buffered min/max range.
     *
     * @param a The vector to compare.
     * @param vectors The set of vectors to compare against.
     * @param errorBuffer The buffer percentage (e.g., 0.5 for 50%).
     * @return true if similarity is within buffered min/max, false otherwise.
     */
    public static boolean isSimilar(List<Float> a, List<List<Float>> vectors, double errorBuffer) {
        if (vectors == null || vectors.isEmpty() || a == null || a.isEmpty()) {
            return false;
        }
        double[] minMax = calculateMinMaxSimilarities(vectors);
        double min = minMax[0];
        double max = minMax[1];
        double range = max - min;
        double buffer = range * errorBuffer;
        double bufferedMin = min - buffer;
        double bufferedMax = max + buffer;
        double avgSim = calculateAverageSimilarity(a, vectors);
        log.info("[CosineSimilarityCalculator] avgSim={}, bufferedMin={}, bufferedMax={}", avgSim, bufferedMin, bufferedMax);
        return avgSim >= bufferedMin && avgSim <= bufferedMax;
    }

    // Cosine similarity between two List<Float>
    public static double cosineSimilarity(List<Float> a, List<Float> b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            float va  = a.get(i);
            float vb = b.get(i);
            dot += va  * vb;
            normA += va  * va;
            normB += vb * vb;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // Calculate min and max cosine similarities as mean ± k * stddev (k=2)
    public static double[] calculateMinMaxSimilarities(List<List<Float>> vectors) {
        int n = vectors.size();
        if (n < 2) {
            return new double[]{0.0, 0.0};
        }
        List<Double> similarities = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Float> vecA = vectors.get(i);
            for (int j = i + 1; j < n; j++) {
                List<Float> vecB = vectors.get(j);
                similarities.add(cosineSimilarity(vecA, vecB));
            }
        }
        if (similarities.isEmpty()) {
            return new double[]{0.0, 0.0};
        }
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (double sim : similarities) {
            stats.addValue(sim);
        }
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;
        double lowerFence = q1 - 1.5 * iqr;
        double upperFence = q3 + 1.5 * iqr;
        // Remove edge cases (outliers)
        List<Double> filtered = new java.util.ArrayList<>();
        for (double sim : similarities) {
            if (sim >= lowerFence && sim <= upperFence) {
                filtered.add(sim);
            }
        }
        if (filtered.isEmpty()) {
            filtered = similarities;
        }
        DescriptiveStatistics filteredStats = new DescriptiveStatistics();
        for (double sim : filtered) {
            filteredStats.addValue(sim);
        }
        double mean = filteredStats.getMean();
        double stddev = filteredStats.getStandardDeviation();
        double min = mean - stddev;
        double max = mean + stddev;
        return new double[]{min, max};
    }

    // Calculate average cosine similarity between a vector and a list of vectors
    public static double calculateAverageSimilarity(List<Float> a, List<List<Float>> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return 0.0;
        }
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (List<Float> vec : vectors) {
            stats.addValue(cosineSimilarity(a, vec));
        }
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;
        double lowerFence = q1 - 1.5 * iqr;
        double upperFence = q3 + 1.5 * iqr;
        // Filter outliers
        DescriptiveStatistics filteredStats = new DescriptiveStatistics();
        for (int i = 0; i < stats.getN(); i++) {
            double sim = stats.getElement(i);
            if (sim >= lowerFence && sim <= upperFence) {
                filteredStats.addValue(sim);
            }
        }
        if (filteredStats.getN() == 0) {
            return stats.getMean();
        }
        return filteredStats.getMean();
    }
}
