package com.securevision.perception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Function;

// Large dataset processing utilities
// The batch processing method here was something I needed after running
// out of memory trying to validate all sensor frames at once.

enum DataQuality { EXCELLENT, GOOD, FAIR, POOR }

class DataQualityReport {
    private final DataQuality overallQuality;
    private final int validSensorCount;
    private final int corruptedSensorCount;
    private final int missingSensorCount;
    private final List<String> qualityIssues;

    public DataQualityReport(DataQuality overallQuality,
                              int validSensorCount,
                              int corruptedSensorCount,
                              int missingSensorCount,
                              List<String> qualityIssues) {
        this.overallQuality = overallQuality;
        this.validSensorCount = validSensorCount;
        this.corruptedSensorCount = corruptedSensorCount;
        this.missingSensorCount = missingSensorCount;
        this.qualityIssues = qualityIssues;
    }

    public DataQuality getOverallQuality() { return overallQuality; }
    public int getValidSensorCount() { return validSensorCount; }
    public int getCorruptedSensorCount() { return corruptedSensorCount; }
    public List<String> getQualityIssues() { return qualityIssues; }

    @Override
    public String toString() {
        return String.format("DataQualityReport{quality=%s, valid=%d, corrupted=%d, missing=%d}",
                overallQuality, validSensorCount, corruptedSensorCount, missingSensorCount);
    }
}

public final class DataProcessingUtils {
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingUtils.class);

    private DataProcessingUtils() {
        // Utility class - prevent instantiation
        throw new IllegalStateException("Utility class");
    }

    /**
     * Process large dataset in batches to manage memory efficiently
     * Clean code: clear method name, single responsibility
     */
    public static <T, R> List<R> processInBatches(List<T> data,
                                                    int batchSize,
                                                    Function<List<T>, List<R>> processor) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        List<R> results = new ArrayList<>();
        int totalBatches = (data.size() + batchSize - 1) / batchSize;

        logger.info("Processing {} items in {} batches of size {}",
                data.size(), totalBatches, batchSize);

        for (int i = 0; i < data.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, data.size());
            List<T> batch = data.subList(i, endIndex);

            try {
                List<R> batchResults = processor.apply(batch);
                results.addAll(batchResults);
                logger.debug("Processed batch {}/{} with {} results",
                        (i / batchSize) + 1, totalBatches, batchResults.size());
            } catch (Exception e) {
                logger.error("Failed to process batch {}/{}: {}",
                        (i / batchSize) + 1, totalBatches, e.getMessage());
                throw new RuntimeException("Batch processing failed", e);
            }
        }

        logger.info("Completed processing: {} results from {} input items",
                results.size(), data.size());

        return results;
    }

    /**
     * Validate data quality with comprehensive checks
     * Clean code: descriptive method name, clear validation logic
     */
    public static DataQualityReport validateDataQuality(List<SensorData> sensorDataList) {

        if (sensorDataList == null || sensorDataList.isEmpty()) {
            return new DataQualityReport(
                    DataQuality.POOR, 0, 0, 0,
                    Collections.singletonList("No sensor data provided")
            );
        }

        int validSensors = 0;
        int corruptedSensors = 0;
        int missingSensors = 0;
        List<String> qualityIssues = new ArrayList<>();

        for (SensorData sensorData : sensorDataList) {
            if (sensorData == null) {
                missingSensors++;
                qualityIssues.add("Null sensor data detected");
                continue;
            }

            if (!sensorData.isValid()) {
                corruptedSensors++;
                qualityIssues.add("Invalid sensor data: " + sensorData.getSensorId());
                continue;
            }

            double qualityScore = sensorData.getQualityScore();
            if (qualityScore < 0.7) {
                qualityIssues.add("Low quality sensor: " + sensorData.getSensorId() +
                        " (score: " + String.format("%.2f", qualityScore) + ")");
            }

            validSensors++;
        }

        // Determine overall data quality
        double validRatio = (double) validSensors / sensorDataList.size();
        DataQuality overallQuality;

        if (validRatio >= 0.9) {
            overallQuality = DataQuality.EXCELLENT;
        } else if (validRatio >= 0.7) {
            overallQuality = DataQuality.GOOD;
        } else if (validRatio >= 0.5) {
            overallQuality = DataQuality.FAIR;
        } else {
            overallQuality = DataQuality.POOR;
        }

        return new DataQualityReport(
                overallQuality, validSensors, corruptedSensors, missingSensors, qualityIssues
        );
    }

    /**
     * Memory management check during large dataset processing
     */
    public static void performMemoryManagement(int batchCount) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / runtime.maxMemory() * 100;

        logger.debug("Memory usage after batch {}: {:.2f}% ({} MB used)",
                batchCount, memoryUsagePercent, usedMemory / 1024 / 1024);

        if (memoryUsagePercent > 80) {
            logger.info("High memory usage detected ({}%), forcing garbage collection",
                    String.format("%.1f", memoryUsagePercent));
            System.gc();
            long newUsedMemory = runtime.totalMemory() - runtime.freeMemory();
            logger.info("Memory after GC: {} MB (freed {} MB)",
                    newUsedMemory / 1024 / 1024,
                    (usedMemory - newUsedMemory) / 1024 / 1024);
        }
    }
}
