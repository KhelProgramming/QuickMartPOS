package com.quickmart.pos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

final class ProductPhoto extends JPanel {
    private static final int PHOTO_SIZE = 384;
    private final JLabel thumbnail = new JLabel("No photo", SwingConstants.CENTER);
    private final JButton remove = new JButton("Remove");
    private byte[] jpeg;
    private boolean changed;

    ProductPhoto(byte[] jpeg) {
        super(new BorderLayout(6, 6));
        this.jpeg = jpeg;
        thumbnail.setPreferredSize(new Dimension(150, 112));
        thumbnail.setOpaque(true);
        thumbnail.setBackground(Color.WHITE);
        add(thumbnail, BorderLayout.CENTER);

        JButton upload = new JButton("Upload Photo");
        JButton capture = new JButton("Take Photo");
        JPanel controls = new JPanel();
        controls.add(upload);
        controls.add(capture);
        controls.add(remove);
        add(controls, BorderLayout.SOUTH);
        upload.addActionListener(event -> upload());
        capture.addActionListener(event -> takePhoto());
        remove.addActionListener(event -> setPhoto(null));
        refresh();
    }

    byte[] jpeg() { return jpeg; }
    boolean changed() { return changed; }

    private void refresh() {
        ImageIcon icon = icon(jpeg, 110);
        thumbnail.setIcon(icon);
        thumbnail.setText(icon == null ? "No photo" : "");
        remove.setEnabled(jpeg != null);
    }

    private void setPhoto(byte[] jpeg) {
        this.jpeg = jpeg;
        changed = true;
        refresh();
    }

    private void upload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Photos (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        preparePhoto(() -> {
            if (Files.size(file.toPath()) > 20_000_000) throw new IOException("Choose a photo under 20 MB.");
            try (var input = ImageIO.createImageInputStream(file)) {
                if (input == null) throw new IOException("Could not read the selected file.");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("Choose a readable image.");
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int maxSide = Math.max(reader.getWidth(0), reader.getHeight(0));
                    int step = 1 + (maxSide - 1) / 1600;
                    var options = reader.getDefaultReadParam();
                    options.setSourceSubsampling(step, step, 0, 0);
                    return reader.read(0, options);
                } finally {
                    reader.dispose();
                }
            }
        }, false);
    }

    private void takePhoto() {
        while (true) {
            BufferedImage photo = CameraDialog.takePhoto(this);
            if (photo == null || !preparePhoto(() -> photo, true)) return;
        }
    }

    private boolean preparePhoto(PhotoInput input, boolean fromCamera) {
        JDialog progress = new JDialog(SwingUtilities.getWindowAncestor(this), "Preparing Photo",
                Dialog.ModalityType.APPLICATION_MODAL);
        progress.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JLabel progressLabel = new JLabel("  Preparing your product photo…  ");
        progressLabel.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        progress.add(progressLabel);
        progress.pack();
        progress.setLocationRelativeTo(this);
        SwingWorker<ProcessedPhoto, Void> worker = new SwingWorker<>() {
            @Override protected ProcessedPhoto doInBackground() throws Exception {
                BufferedImage source = input.read();
                BufferedImage original = onWhiteCanvas(source);
                try {
                    BufferedImage foreground = PhotoBackground.remove(source);
                    return new ProcessedPhoto(original, foreground, "Preview the white-background result.");
                } catch (Exception | LinkageError ex) {
                    return new ProcessedPhoto(original, null, "Background removal could not finish. You can keep the original.");
                }
            }

            @Override protected void done() {
                progress.dispose();
            }
        };
        worker.execute();
        progress.setVisible(true);
        try {
            ProcessedPhoto result = worker.get();
            boolean cutout = result.foreground() != null;
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            BufferedImage preview = cutout ? result.foreground() : result.original();
            panel.add(new JLabel(new ImageIcon(resize(preview, 230, 230, BufferedImage.TYPE_INT_RGB))),
                    BorderLayout.CENTER);
            panel.add(new JLabel(result.message()), BorderLayout.SOUTH);
            String preferred = cutout ? "Use White Background" : "Keep Original";
            Object[] buttons = cutout
                    ? new Object[]{preferred, "Keep Original", fromCamera ? "Retake" : "Cancel"}
                    : new Object[]{preferred, fromCamera ? "Retake" : "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, panel, "Product Photo Preview",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, buttons, preferred);
            if (choice == buttons.length - 1) return fromCamera;
            if (choice >= 0) {
                setPhoto(toJpeg(choice == 0 ? preview : result.original()));
            }
        } catch (Exception ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            JOptionPane.showMessageDialog(this, "Could not open photo: " + cause.getMessage(),
                    "Product Photo", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    static BufferedImage onWhiteCanvas(BufferedImage image) {
        BufferedImage result = new BufferedImage(PHOTO_SIZE, PHOTO_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = (PHOTO_SIZE - 32.0) / Math.max(image.getWidth(), image.getHeight());
        int width = Math.max(1, (int) Math.round(scale * image.getWidth()));
        int height = Math.max(1, (int) Math.round(scale * image.getHeight()));
        graphics.drawImage(image, (PHOTO_SIZE - width) / 2, (PHOTO_SIZE - height) / 2, width, height, null);
        graphics.dispose();
        return result;
    }

    static ImageIcon icon(byte[] jpeg, int size) {
        if (jpeg == null) return null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
            return image == null ? null : new ImageIcon(resize(image, size, size, BufferedImage.TYPE_INT_RGB));
        } catch (IOException ex) {
            return null;
        }
    }

    static byte[] toJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpeg", output)) throw new IOException("JPEG encoding unavailable.");
        return output.toByteArray();
    }

    static BufferedImage resize(BufferedImage source, int width, int height, int type) {
        BufferedImage result = new BufferedImage(width, height, type);
        Graphics2D graphics = result.createGraphics();
        if (type == BufferedImage.TYPE_INT_RGB) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    @FunctionalInterface
    private interface PhotoInput {
        BufferedImage read() throws IOException;
    }

    private record ProcessedPhoto(BufferedImage original, BufferedImage foreground, String message) {}
}
