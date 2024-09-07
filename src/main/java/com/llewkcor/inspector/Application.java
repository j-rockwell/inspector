package com.llewkcor.inspector;

import com.google.common.collect.Lists;

import java.util.List;

public class Application {
    public static void main(String[] args) {
        // TODO: Remove test files and allow direct loading
        List<String> testFileNames = Lists.newArrayList();
        for (int i = 1; i <= 5; i++) {
            testFileNames.add("img" + i + ".png");
        }

        Inspector.InspectorConfig conf = new Inspector.InspectorConfig();
        conf.setProcessorThreadCount(16);
        conf.setMinImperfectionThreshold(30);
        conf.setMaxImperfectionThreshold(100);
        conf.setDebugMode(true);

        Inspector app = new Inspector(testFileNames, conf);
        app.start();
    }
}
