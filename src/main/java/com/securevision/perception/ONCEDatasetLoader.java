package com.securevision.perception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Loads real data from the ONCE autonomous driving dataset.
 * Reads LiDAR .bin files and annotation JSON files.
 * 
 * The ONCE dataset contains 1 million LiDAR scenes and 7 million camera images
 * from 144 driving hours across various areas, weather conditions, and time periods.
 * 
 * Dataset: https://once-for-auto-driving.github.io
 * Citation: Mao et al. (2021) One Million Scenes for Autonomous Driving: ONCE Dataset. NeurIPS 2021.
 */
public class ONCEDatasetLoader {

    private static final Logger logger = LoggerFactory.getLogger(ONCEDatasetLoader.class);
    private static final int BYTES_PER_POINT = 16; // x, y, z, intensity = 4 floats x 4 bytes

    private final String datasetRootPath;

    public ONCEDatasetLoader(String datasetRootPath) {
        this.datasetRootPath = datasetRootPath;
    }

    /**
     * Load all scene IDs available in the dataset folder.
     * 
     * @return list of scene IDs (folder names like "000000", "000001", etc.)
     */
    public List<String> loadSceneIds() {
        List<String> sceneIds = new ArrayList<>();
        File dataDir = new File(datasetRootPath + "/data");

        if (!dataDir.exists()) {
            logger.error("Dataset folder not found: {}", dataDir.getAbsolutePath());
            return sceneIds;
        }

        File[] files = dataDir.listFiles();
        if (files != null) {
            for (File sceneFolder : files) {
                if (sceneFolder.isDirectory()) {
                    sceneIds.add(sceneFolder.getName());
                }
            }
        }

        Collections.sort(sceneIds);
        logger.info("Found {} scenes in dataset", sceneIds.size());
        return sceneIds;
    }

    /**
     * Load LiDAR point cloud from a .bin file.
     * Each point = [x, y, z, intensity] stored as 4 little-endian floats.
     *
     * @param sceneId    scene folder name e.g. "000000"
     * @param frameId    timestamp string e.g. "1616004157400"
     * @return list of float arrays, each [x, y, z, intensity]
     */
    public List<float[]> loadLiDARFrame(String sceneId, String frameId) {
        String binPath = String.format("%s/data/%s/lidar_roof/%s.bin",
                datasetRootPath, sceneId, frameId);

        List<float[]> points = new ArrayList<>();

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(binPath));
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            while (buffer.remaining() >= BYTES_PER_POINT) {
                float x = buffer.getFloat();
                float y = buffer.getFloat();
                float z = buffer.getFloat();
                float intensity = buffer.getFloat();
                points.add(new float[]{x, y, z, intensity});
            }

            logger.info("Loaded {} LiDAR points from scene {} frame {}",
                    points.size(), sceneId, frameId);

        } catch (IOException e) {
            logger.error("Failed to load LiDAR bin file: {}", binPath, e);
        }

        return points;
    }

    /**
     * Streams a LiDAR frame point by point using NIO FileChannel.
     * I added this method after noticing that loadLiDARFrame() loads the entire
     * file into memory at once. For a single frame this is fine, but when processing
     * thousands of frames in a loop the heap fills up quickly. This streaming version
     * processes 4096 points at a time and keeps memory usage constant regardless
     * of file size.
     *
     * @param sceneId   scene folder name e.g. "000201"
     * @param frameId   timestamp string e.g. "1616004157400"
     * @param consumer  receives each point as float[]{x, y, z, intensity}
     */
    public void streamLiDARFrame(String sceneId, String frameId,
                                  java.util.function.Consumer<float[]> consumer) {

        String binPath = String.format("%s/data/%s/lidar_roof/%s.bin",
                datasetRootPath, sceneId, frameId);

        // Buffer holds 4096 points at a time — balances memory vs I/O calls
        int CHUNK = 4096 * BYTES_PER_POINT;

        try (java.nio.channels.FileChannel channel =
                     java.nio.channels.FileChannel.open(
                         java.nio.file.Paths.get(binPath),
                         java.nio.file.StandardOpenOption.READ)) {

            java.nio.ByteBuffer buffer =
                    java.nio.ByteBuffer.allocateDirect(CHUNK)
                                       .order(java.nio.ByteOrder.LITTLE_ENDIAN);

            while (channel.read(buffer) > 0) {
                buffer.flip();
                while (buffer.remaining() >= BYTES_PER_POINT) {
                    float x = buffer.getFloat();
                    float y = buffer.getFloat();
                    float z = buffer.getFloat();
                    float intensity = buffer.getFloat();
                    consumer.accept(new float[]{x, y, z, intensity});
                }
                buffer.clear();
            }

        } catch (java.io.IOException e) {
            logger.error("Failed to stream LiDAR frame {}/{}: {}", sceneId, frameId, e.getMessage());
        }
    }

    /**
     * List all LiDAR frame timestamps available for a scene.
     * 
     * @param sceneId scene folder name e.g. "000000"
     * @return list of frame IDs (timestamps) sorted chronologically
     */
    public List<String> getLiDARFrameIds(String sceneId) {
        List<String> frameIds = new ArrayList<>();
        String lidarPath = datasetRootPath + "/data/" + sceneId + "/lidar_roof";
        File lidarDir = new File(lidarPath);

        if (!lidarDir.exists()) {
            logger.warn("No lidar_roof folder for scene: {}", sceneId);
            return frameIds;
        }

        File[] files = lidarDir.listFiles();
        if (files != null) {
            for (File binFile : files) {
                if (binFile.getName().endsWith(".bin")) {
                    // Remove .bin extension to get the frame timestamp
                    frameIds.add(binFile.getName().replace(".bin", ""));
                }
            }
        }

        Collections.sort(frameIds); // sort chronologically
        logger.info("Scene {} has {} LiDAR frames", sceneId, frameIds.size());
        return frameIds;
    }

    /**
     * Load camera image path for a given scene, camera, and frame.
     * 
     * @param sceneId   scene folder name e.g. "000000"
     * @param cameraId  camera identifier e.g. "cam01", "cam03", etc.
     * @param frameId   timestamp string e.g. "1616004157400"
     * @return full path to the camera image file
     */
    public String getCameraImagePath(String sceneId, String cameraId, String frameId) {
        return String.format("%s/data/%s/%s/%s.jpg",
                datasetRootPath, sceneId, cameraId, frameId);
    }

    /**
     * Get the annotation JSON file path for a scene.
     * 
     * @param sceneId scene folder name e.g. "000000"
     * @return full path to the annotation JSON file
     */
    public String getAnnotationPath(String sceneId) {
        return String.format("%s/data/%s/%s.json",
                datasetRootPath, sceneId, sceneId);
    }

    /**
     * Check if the dataset is available at the configured path.
     * 
     * @return true if the dataset folder exists and contains scene data
     */
    public boolean isDatasetAvailable() {
        File dataDir = new File(datasetRootPath + "/data");
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return false;
        }
        File[] files = dataDir.listFiles();
        return files != null && files.length > 0;
    }

    /**
     * Print a quick summary of the dataset to the log.
     */
    public void printDatasetSummary() {
        List<String> scenes = loadSceneIds();
        logger.info("=== ONCE Dataset Summary ===");
        logger.info("Total scenes: {}", scenes.size());

        if (!scenes.isEmpty()) {
            String firstScene = scenes.get(0);
            List<String> frames = getLiDARFrameIds(firstScene);
            logger.info("First scene: {} | LiDAR frames: {}", firstScene, frames.size());

            if (!frames.isEmpty()) {
                List<float[]> points = loadLiDARFrame(firstScene, frames.get(0));
                logger.info("First frame point count: {}", points.size());
            }
        }
    }

    /**
     * Get the root path of the dataset.
     * 
     * @return the dataset root path
     */
    public String getDatasetRootPath() {
        return datasetRootPath;
    }
}
