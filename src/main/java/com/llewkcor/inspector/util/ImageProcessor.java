package com.llewkcor.inspector.util;

import com.google.common.collect.Lists;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Queue;

public class ImageProcessor {
    private static final int[] DILATION_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DILATION_Y = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] BOUNDING_BOX_X = {-1, 1, 0, 0};
    private static final int[] BOUNDING_BOX_Y = {0, 0, -1, 1};

    public static Rectangle getBoundingBox(BufferedImage input, int x, int y, boolean[][] visited) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final Queue<int[]> queue = Lists.newLinkedList();
        int minX = x, minY = y, maxX = x, maxY = y;

        queue.add(new int[]{x, y});
        visited[x][y] = true;

        while (!queue.isEmpty()) {
            final int[] point = queue.poll();
            final int visitedX = point[0];
            final int visitedY = point[1];

            if (visitedX < minX) minX = visitedX;
            if (visitedY < minY) minY = visitedY;
            if (visitedX > maxX) maxX = visitedX;
            if (visitedY > maxY) maxY = visitedY;

            for (int i = 0; i < 4; i++) {
                final int nx = visitedX + BOUNDING_BOX_X[i];
                final int ny = visitedY + BOUNDING_BOX_Y[i];

                if (
                        nx >= 0
                        && ny >= 0
                        && nx < width
                        && ny < height
                        && !visited[nx][ny]
                        && (input.getRGB(nx, ny) & 0xff) == 0
                ) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public static BufferedImage applyMorphologicalOperation(BufferedImage input, int[][] kernel, boolean erode) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 1; y < (height - 1); y++) {
            for (int x = 1; x < (width - 1); x++) {
                int minMax = (erode ? 255 : 0);

                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        if (kernel[ky+1][kx+1] == 1) {
                            final int px = input.getRGB(x + kx, y + ky) & 0xff; // Blue channel extraction
                            minMax = (erode ? Math.min(minMax, px) : Math.max(minMax, px));
                        }
                    }
                }

                output.setRGB(x, y, (minMax == 255 ? 0xFFFFFF : 0x000000));
            }
        }

        return output;
    }

    public static BufferedImage applyGrayscale(BufferedImage input) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = input.getRGB(x, y);
                int a = (p>>24)&0xcff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;
                int avg = (r+g+b)/3;

                p = (a<<24) | (avg<<16) | (avg<<8) | avg;
                output.setRGB(x, y, p);
            }
        }

        return output;
    }

    public static BufferedImage applyDilation(BufferedImage input) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 1; x < (width - 1); x++) {
            for (int y = 1; y < (height - 1); y++) {
                final int px = input.getRGB(x, y) & 0xff;

                if (px == 0) {
                    for (int i = 0; i < 8; i++) {
                        output.setRGB(
                                (x + DILATION_X[i]),
                                (y + DILATION_Y[i]),
                                0x000
                        );
                    }

                    continue;
                }

                output.setRGB(x, y, 0xFFF);
            }
        }
        return output;
    }

    public static BufferedImage applyErosion(BufferedImage input) {
        return applyMorphologicalOperation(input, new int[][]{
                {0, 1, 0},
                {1, 1, 1},
                {0, 1, 0}
        }, true);
    }

    public static BufferedImage applyMask(BufferedImage input, int threshold) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final BufferedImage grayscale = applyGrayscale(input);
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int px = grayscale.getRGB(x, y) & 0xff; // Extract blue channel
                output.setRGB(x, y, (px > threshold ? 0xFFFFFF : 0x000000));
            }
        }

        return applyDilation(applyErosion(output));
    }

    public static void applyMarker(BufferedImage input, Rectangle bbx) {
        for (int y = bbx.y; y < bbx.y + bbx.height; y++) {
            for (int x = bbx.x; x < bbx.x + bbx.width; x++) {
                input.setRGB(x, y, 0xFF0000); // TODO: Make marker color customizable
            }
        }
    }

    public static BufferedImage applyImperfectionsMap(BufferedImage input, int minSize, int maxSize) {
        final int width = input.getWidth();
        final int height = input.getHeight();
        final BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        boolean[][] visited = new boolean[width][height];
        double totalArea = 0.0;

        for (int x = 1; x < (width - 1); x++) {
            for (int y = 1; y < (height - 1); y++) {
                if (visited[x][y]) {
                    continue;
                }

                final int px = input.getRGB(x, y) & 0xff;

                if (px != 0) {
                    continue;
                }

                final Rectangle bbx = getBoundingBox(input, x, y, visited);
                final int area = bbx.width * bbx.height;

                if (area < minSize || area > maxSize) {
                    continue;
                }

                applyMarker(output, bbx);
                totalArea += area;
            }
        }

        return output;
    }
}
