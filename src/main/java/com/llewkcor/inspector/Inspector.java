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

        for (String filename : filenames) {
            final long start = System.currentTimeMillis();

            logger.debug("Spawning new process for " + filename);

            executorService.submit(() -> {
                logger.debug("Now processing " + filename);

                BufferedImage input;
                try {
                    input = ImageIO.read(new File("src/main/resources/" + filename));
                } catch (IOException e) {
                    logger.severe("Failed to read file: " + filename);
                    return;
                }

                BufferedImage mask = ImageProcessor.applyMask(input, config.getMaskThreshold());
                try {
                    ImageIO.write(mask, "PNG", new File("out/" + filename + "_mask.png"));
                    logger.info("Wrote " + filename + " mask file successfully");
                } catch (IOException e) {
                    logger.severe("Failed to write mask file: " + filename);
                }

                BufferedImage dilated = ImageProcessor.applyDilation(mask);
                try {
                    ImageIO.write(dilated, "PNG", new File("out/" + filename + "_dilated.png"));
                    logger.info("Wrote " + filename + " dilation file successfully");
                } catch (IOException e) {
                    logger.severe("Failed to write dilation file: " + filename);
                }

                BufferedImage imperfections = ImageProcessor.applyImperfectionsMap(dilated, config.getMinImperfectionThreshold(), config.getMaxImperfectionThreshold());
                try {
                    ImageIO.write(imperfections, "PNG", new File("out/" + filename + "_imperfections.png"));
                    logger.info("Wrote " + filename + " imperfections file successfully");
                } catch (IOException e) {
                    logger.severe("Failed to write imperfections file: " + filename);
                }
            });

            final long end = System.currentTimeMillis();
            final long diff = (end - start);
            logger.debug("Processed " + filename + " (took " + diff + "ms)");
        }

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
            logger.severe("Failed to create output directory due to a security exception");
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
