/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

/**
 *
 * @author Jukka
 */
import java.util.List;
import org.apache.commons.lang.ArrayUtils;

public class DistanceCalculator {


    // Euclidean distance between two List<Float>
    public static double euclideanDistance(List<Float> a, List<Float> b) {
        double sum = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }


    // Calculate min and max distances from a list of List<Float> vectors
    public static double[] calculateMinMaxDistances(List<List<Float>> vectors) {
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;

        int n = vectors.size();

        // Compare each pair once (i < j to avoid repeats and self)
        for (int i = 0; i < n; i++) {
            List<Float> vecA = vectors.get(i);
            for (int j = i + 1; j < n; j++) {
                List<Float> vecB = vectors.get(j);
                double dist = euclideanDistance(vecA, vecB);
                if (dist < minDistance) {
                    minDistance = dist;
                }
                if (dist > maxDistance) {
                    maxDistance = dist;
                }
            }
        }
        return new double[]{minDistance, maxDistance};
    }


    public static double calculateAverageDistance(List<Float> a, List<List<Float>> vectors) {
        double totalDistance = 0.0;
        for (List<Float> vec : vectors) {
            totalDistance += euclideanDistance(a, vec);
        }
        return vectors.isEmpty() ? 0.0 : totalDistance / vectors.size();
    }


    // Optionally, keep this if you need to convert to float[] elsewhere
    public static float[] convertListToFloatArray(List<Float> floatList) {
        Float[] floatObjects = floatList.toArray(new Float[0]);
        return ArrayUtils.toPrimitive(floatObjects, 0.0f);
    }

}
