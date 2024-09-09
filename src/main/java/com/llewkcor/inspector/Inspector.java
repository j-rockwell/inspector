package com.llewkcor.inspector;

import com.google.common.collect.Lists;
import com.llewkcor.inspector.util.ImageProcessor;
import com.llewkcor.inspector.util.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Inspector {
    private final InspectorConfig config;
    private final ExecutorService executorService;
    private final List<String> filenames;
    private final Logger logger;

    public Inspector(String filename, InspectorConfig config) {
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(config.getProcessorThreadCount());
        this.filenames = Lists.newArrayList(filename);
        this.logger = new Logger("Inspector", config.isDebugMode());
    }

    public Inspector(List<String> filenames, InspectorConfig config) {
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(config.getProcessorThreadCount());
        this.filenames = filenames;
        this.logger = new Logger("Inspector", config.isDebugMode());

    }

    public void start() {
        if (filenames == null || filenames.isEmpty()) {
            shutdown();
            logger.severe("No files were provided");
            return;
        }

        setupOutputDirectory();
        setupProcessorTasks();
        shutdown(); // Awaits tasks to be finished
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private void setupOutputDirectory() {
        final File dir = new File("out");

        if (dir.exists()) {
            logger.debug("Successfully found output directory");
            return;
        }

        try {
            if (dir.mkdirs()) {
                logger.info("Created output directory successfully");
            } else {
                logger.severe("Failed to create output directory");
            }
        } catch (SecurityException e) {
            logger.severe("Failed to create output directory due to a security exception", e);
        }
    }

    private void setupProcessorTasks() {
        if (filenames == null || filenames.isEmpty()) {
            logger.severe("No files were provided while setting up processor");
            return;
        }

        filenames.forEach(filename -> {
            final String cleanFileName = filename.replaceAll(".png", "").replaceAll(".jpg", "");

            executorService.submit(() -> {
                final long startTimestamp = System.currentTimeMillis();

                BufferedImage toProcess = null;
                try {
                    toProcess = ImageIO.read(new File("src/main/resources/" + filename));
                } catch (IOException e) {
                    logger.severe("Failed to read image for " + filename, e);
                    return;
                }

                toProcess = ImageProcessor.applyMask(toProcess, config.getMaskThreshold());
                if (config.isDebugMode()) {
                    writeImageToOutput(toProcess, cleanFileName + "_mask.png", true);
                }

                toProcess = ImageProcessor.applyDilation(toProcess);
                if (config.isDebugMode()) {
                    writeImageToOutput(toProcess, cleanFileName + "_dilated.png", true);
                }

                toProcess = ImageProcessor.applyImperfectionsMap(toProcess, config.getMinImperfectionThreshold(), config.getMaxImperfectionThreshold());
                if (config.isDebugMode()) {
                    writeImageToOutput(toProcess, cleanFileName + "_imperfection.png", true);
                }

                writeImageToOutput(toProcess, cleanFileName + "_imperfection.png", false);

                final long endTimestamp = System.currentTimeMillis();
                final long processTime = endTimestamp - startTimestamp;
                logger.info("Processed " + filename + " (took " + processTime + "ms)");
            });
        });
    }

    private void writeImageToOutput(BufferedImage image, String filename, boolean asExample) {
        final String pathPrefix = (asExample) ? "examples/" : "out/";

        try {
            ImageIO.write(image, "PNG", new File(pathPrefix + filename));
        } catch (IOException e) {
            logger.severe("Failed to write imperfections file for " + filename, e);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static final class InspectorConfig {
        public int processorThreadCount;
        public int minImperfectionThreshold;
        public int maxImperfectionThreshold;
        public int maskThreshold;
        public boolean debugMode;

        public InspectorConfig() {
            processorThreadCount = 4;
            minImperfectionThreshold = 50;
            maxImperfectionThreshold = 100;
            maskThreshold = 100;
            debugMode = true;
        }
    }
}
