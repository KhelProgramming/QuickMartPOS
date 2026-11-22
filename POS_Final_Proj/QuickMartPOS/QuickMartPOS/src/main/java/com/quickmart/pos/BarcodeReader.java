package com.quickmart.pos;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

final class BarcodeReader {
    private static final Map<DecodeHintType, ?> HINTS = Map.of(
            DecodeHintType.TRY_HARDER, true,
            DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39, BarcodeFormat.ITF, BarcodeFormat.CODABAR));

    private BarcodeReader() {}

    static String decode(BufferedImage image) {
        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(HINTS);
        for (int turn = 0; turn < 4; turn++) {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            Result result = read(reader, new HybridBinarizer(source));
            if (result == null) result = read(reader, new GlobalHistogramBinarizer(source));
            if (result != null) return result.getText();
            if (turn < 3) image = rotateClockwise(image);
        }
        return null;
    }

    private static Result read(MultiFormatReader reader, Binarizer binarizer) {
        try {
            return reader.decodeWithState(new BinaryBitmap(binarizer));
        } catch (NotFoundException ex) {
            return null;
        } finally {
            reader.reset();
        }
    }

    private static BufferedImage rotateClockwise(BufferedImage source) {
        BufferedImage rotated = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        graphics.translate(source.getHeight(), 0);
        graphics.rotate(Math.PI / 2);
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rotated;
    }
}
