package com.quickmart.pos;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Map;

final class PhotoBackground {
    private static final int MODEL_SIZE = 320;

    private PhotoBackground() {}

    static BufferedImage remove(BufferedImage source, Path model) throws Exception {
        return extractProduct(source, predictMask(source, model));
    }

    private static float[][] predictMask(BufferedImage source, Path model) throws Exception {
        BufferedImage sample = ProductPhoto.resize(source, MODEL_SIZE, MODEL_SIZE, BufferedImage.TYPE_INT_RGB);
        int pixels = MODEL_SIZE * MODEL_SIZE;
        int[] rgb = sample.getRGB(0, 0, MODEL_SIZE, MODEL_SIZE, null, 0, MODEL_SIZE);
        int brightest = 1;
        for (int pixel : rgb) {
            brightest = Math.max(brightest, Math.max((pixel >> 16) & 255,
                    Math.max((pixel >> 8) & 255, pixel & 255)));
        }
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};
        float[] values = new float[pixels * 3];
        for (int channel = 0; channel < 3; channel++) {
            int shift = (2 - channel) * 8;
            for (int i = 0; i < pixels; i++) {
                float value = ((rgb[i] >> shift) & 255) / (float) brightest;
                values[channel * pixels + i] = (value - mean[channel]) / std[channel];
            }
        }
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions();
             OrtSession session = environment.createSession(model.toString(), options);
             OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(values),
                     new long[]{1, 3, MODEL_SIZE, MODEL_SIZE});
             OrtSession.Result result = session.run(Map.of(session.getInputNames().iterator().next(), tensor))) {
            return ((float[][][][]) result.get(0).getValue())[0][0];
        }
    }

    static BufferedImage extractProduct(BufferedImage source, float[][] mask) throws IOException {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float[] row : mask) {
            for (float value : row) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        if (!Float.isFinite(min) || !Float.isFinite(max) || max - min < 0.001f) {
            throw new IOException("No product found in the photo.");
        }
        BufferedImage smallMask = new BufferedImage(mask[0].length, mask.length, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = smallMask.getRaster();
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                raster.setSample(x, y, 0, Math.round(255 * (mask[y][x] - min) / (max - min)));
            }
        }
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage scaled = ProductPhoto.resize(smallMask, width, height, BufferedImage.TYPE_BYTE_GRAY);
        int[] alpha = scaled.getRaster().getSamples(0, 0, width, height, 0, (int[]) null);
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (alpha[y * width + x] >= 128) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left || bottom < top) throw new IOException("No product found in the photo.");
        int margin = Math.max(1, Math.max(right - left, bottom - top) / 40);
        left = Math.max(0, left - margin);
        top = Math.max(0, top - margin);
        right = Math.min(width - 1, right + margin);
        bottom = Math.min(height - 1, bottom + margin);
        BufferedImage cropped = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                int opacity = alpha[y * width + x];
                int rgb = source.getRGB(x, y);
                int combined = opacity * (rgb >>> 24) / 255;
                cropped.setRGB(x - left, y - top, (combined << 24) | (rgb & 0xffffff));
            }
        }
        return ProductPhoto.onWhiteCanvas(cropped);
    }

}
