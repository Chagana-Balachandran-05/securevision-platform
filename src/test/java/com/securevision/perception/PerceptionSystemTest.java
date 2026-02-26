package com.securevision.perception;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

// Task 4 - Tests for the large dataset perception system
// I included a performance test (1000 sensor validations under 2 seconds)
// because dataset processing has a time dimension that unit correctness
// tests alone do not capture.

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task 3 - Autonomous Perception System Tests")
public class PerceptionSystemTest {

    private static final Logger logger = LoggerFactory.getLogger(PerceptionSystemTest.class);

    // sensor data validation tests

    @Test
    @Order(1)
    @DisplayName("LiDAR: Valid sensor data should pass validation")
    void testValidLiDARSensorData() {
        logger.info("Testing valid LiDAR sensor data");

        LiDARSensorData lidar = new LiDARSensorData(
                System.currentTimeMillis(),
                "lidar-sensor-001",
                150000,   // 150k points
                80.0      // 80m range
        );

        assertThat(lidar.isValid())
                .as("LiDAR with 150k points and 80m range should be valid")
                .isTrue();

        assertThat(lidar.getQualityScore())
                .as("Quality score should be between 0 and 1")
                .isBetween(0.0, 1.0);
    }

    @Test
    @Order(2)
    @DisplayName("LiDAR: Zero points should be invalid")
    void testInvalidLiDARSensorDataZeroPoints() {
        logger.info("Testing invalid LiDAR sensor data - zero points");

        LiDARSensorData lidar = new LiDARSensorData(
                System.currentTimeMillis(),
                "lidar-sensor-002",
                0,    // no points - invalid
                80.0
        );

        assertThat(lidar.isValid())
                .as("LiDAR with 0 points should be invalid")
                .isFalse();
    }

    @Test
    @Order(3)
    @DisplayName("Camera: Valid sensor data should pass validation")
    void testValidCameraSensorData() {
        logger.info("Testing valid camera sensor data");

        CameraSensorData camera = new CameraSensorData(
                System.currentTimeMillis(),
                "camera-sensor-001",
                1920, 1080,  // Full HD resolution
                128.0         // Good brightness
        );

        assertThat(camera.isValid())
                .as("Camera with 1920x1080 resolution and good brightness should be valid")
                .isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Camera: Zero brightness should be invalid")
    void testInvalidCameraZeroBrightness() {
        logger.info("Testing invalid camera - zero brightness (corrupted image)");

        CameraSensorData camera = new CameraSensorData(
                System.currentTimeMillis(),
                "camera-sensor-002",
                1920, 1080,
                0.0   // zero brightness - corrupted or pitch black
        );

        assertThat(camera.isValid())
                .as("Camera with 0 brightness should be invalid")
                .isFalse();
    }

    // data quality report tests

    @Test
    @Order(5)
    @DisplayName("DataQuality: All valid sensors should give EXCELLENT quality")
    void testExcellentDataQuality() {
        logger.info("Testing data quality with all valid sensors");

        List<SensorData> sensors = Arrays.asList(
                new LiDARSensorData(System.currentTimeMillis(), "lidar-1", 100000, 80.0),
                new CameraSensorData(System.currentTimeMillis(), "cam-1", 1920, 1080, 128.0),
                new LiDARSensorData(System.currentTimeMillis(), "lidar-2", 120000, 75.0)
        );

        DataQualityReport report = DataProcessingUtils.validateDataQuality(sensors);

        assertThat(report.getValidSensorCount())
                .as("All 3 sensors should be valid")
                .isEqualTo(3);

        assertThat(report.getCorruptedSensorCount())
                .as("No corrupted sensors")
                .isEqualTo(0);

        assertThat(report.getOverallQuality())
                .as("All valid sensors should give EXCELLENT quality")
                .isEqualTo(DataQuality.EXCELLENT);
    }

    @Test
    @Order(6)
    @DisplayName("DataQuality: Empty sensor list should give POOR quality")
    void testPoorDataQualityEmptyList() {
        logger.info("Testing data quality with empty sensor list");

        DataQualityReport report = DataProcessingUtils.validateDataQuality(
                Collections.emptyList()
        );

        assertThat(report.getOverallQuality())
                .as("Empty sensor list should give POOR quality")
                .isEqualTo(DataQuality.POOR);
    }

    @Test
    @Order(7)
    @DisplayName("DataQuality: Null sensor list should give POOR quality")
    void testPoorDataQualityNullList() {
        logger.info("Testing data quality with null sensor list");

        DataQualityReport report = DataProcessingUtils.validateDataQuality(null);

        assertThat(report.getOverallQuality())
                .as("Null sensor list should give POOR quality")
                .isEqualTo(DataQuality.POOR);

        assertThat(report.getQualityIssues())
                .as("Should report 'No sensor data provided' issue")
                .contains("No sensor data provided");
    }

    // batch processing tests

    @Test
    @Order(8)
    @DisplayName("BatchProcessing: Should correctly process all items in batches")
    void testBatchProcessing() {
        logger.info("Testing batch processing of large dataset");

        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<Integer> results = DataProcessingUtils.processInBatches(
                data,
                10, // batch size of 10
                batch -> batch // identity processor - returns items unchanged
        );

        assertThat(results)
                .as("All 100 items should be processed")
                .hasSize(100);

        assertThat(results)
                .as("Results should contain all original items")
                .containsExactlyElementsOf(data);
    }

    @Test
    @Order(9)
    @DisplayName("BatchProcessing: Empty list should return empty result")
    void testBatchProcessingEmptyList() {
        logger.info("Testing batch processing with empty list");

        List<String> results = DataProcessingUtils.<String, String>processInBatches(
                Collections.emptyList(),
                10,
                batch -> batch
        );

        assertThat(results)
                .as("Empty input should give empty output")
                .isEmpty();
    }

    @Test
    @Order(10)
    @DisplayName("BatchProcessing: Invalid batch size should throw exception")
    void testBatchProcessingInvalidBatchSize() {
        logger.info("Testing batch processing with invalid batch size");

        List<Integer> data = Arrays.asList(1, 2, 3);

        assertThatThrownBy(() ->
                DataProcessingUtils.processInBatches(data, 0, batch -> batch)
        )
                .as("Batch size of 0 should throw IllegalArgumentException")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be positive");
    }

    // performance tests

    @Test
    @Order(11)
    @DisplayName("Performance: Processing 1000 sensor validations should complete in under 2 seconds")
    void testLargeScaleValidationPerformance() {
        logger.info("Testing performance with large-scale sensor data validation");

        List<SensorData> largeSensorList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeSensorList.add(new LiDARSensorData(
                    System.currentTimeMillis(),
                    "lidar-" + i,
                    50000 + (i * 100),
                    70.0 + (i % 30)
            ));
        }

        long startTime = System.currentTimeMillis();
        DataQualityReport report = DataProcessingUtils.validateDataQuality(largeSensorList);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration)
                .as("Validating 1000 sensors should complete in under 2000ms")
                .isLessThan(2000L);

        assertThat(report.getValidSensorCount())
                .as("All 1000 sensors should be valid")
                .isEqualTo(1000);

        logger.info("Validated 1000 sensors in {}ms - quality: {}",
                duration, report.getOverallQuality());
    }

    @Test
    @Order(12)
    @DisplayName("Streaming: streamLiDARFrame handles missing file gracefully without throwing")
    void testStreamingHandlesMissingFile() {
        ONCEDatasetLoader loader = new ONCEDatasetLoader("once_dataset");
        java.util.concurrent.atomic.AtomicInteger pointCount =
                new java.util.concurrent.atomic.AtomicInteger(0);

        // The once_dataset folder in the ZIP has JSON only, no .bin LiDAR files.
        // The streaming method should log the error and return — not throw an exception.
        assertThatCode(() ->
                loader.streamLiDARFrame("000201", "0000000000000",
                        point -> pointCount.incrementAndGet())
        )
                .as("Streaming a missing file should log the error and not throw")
                .doesNotThrowAnyException();

        assertThat(pointCount.get())
                .as("No points should be counted for a missing file")
                .isEqualTo(0);
    }

    @Test
    @Order(13)
    @DisplayName("Integration: Full perception pipeline processes sensor frame end-to-end")
    void testFullPerceptionPipelineEndToEnd() {
        logger.info("Testing full autonomous perception pipeline integration");

        // Wire up all four pipeline stages with real implementations
        // This tests the coordinator (AutonomousPerceptionSystem)
        // and all four service interfaces together
        AutonomousPerceptionSystem system = new AutonomousPerceptionSystem(
            new ConcreteDataIngestionService(),
            new ConcreteSensorFusionService(),
            new ConcreteDetectionEngine(),
            new ConcreteNavigationService()
        );

        // Create a realistic synthetic LiDAR frame (50,000 points, 80m range)
        LiDARSensorData lidar = new LiDARSensorData(
            System.currentTimeMillis(), "lidar_roof", 50000, 80.0
        );
        SensorDataFrame frame = new SensorDataFrame(List.of(lidar));
        PerceptionInput input = new PerceptionInput(frame);

        // Run the full pipeline
        PerceptionResult result = system.processPerceptionFrame(input);

        assertThat(result)
            .as("Pipeline should return a result object").isNotNull();
        assertThat(result.getProcessingTimeMs())
            .as("Perception pipeline should process a frame in under 5 seconds")
            .isLessThan(5000L);
        assertThat(result.getNavigationGuidance())
            .as("Navigation guidance should always be generated, even with no detections")
            .isNotNull();
        assertThat(result.getNavigationGuidance().getRecommendedAction())
            .as("With no detected objects, guidance should be to proceed normally")
            .isEqualTo("PROCEED_NORMALLY");

        logger.info("Pipeline integration test passed in {}ms", result.getProcessingTimeMs());
    }
}
