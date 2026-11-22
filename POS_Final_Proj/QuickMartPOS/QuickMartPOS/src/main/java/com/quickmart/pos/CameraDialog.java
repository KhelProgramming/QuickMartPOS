package com.quickmart.pos;

import com.github.sarxos.webcam.Webcam;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.swing.*;

final class CameraDialog extends JDialog {
    // One worker owns the camera across both dialogs, including opening and closing.
    private static final ScheduledExecutorService CAMERA = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "quickmart-camera");
        thread.setDaemon(true);
        return thread;
    });
    private static final long FRAME_PERIOD_NS = 1_000_000_000L / 60;
    private static final long SCAN_PERIOD_NS = TimeUnit.MILLISECONDS.toNanos(250);
    private static final long BARCODE_RESET_NS = TimeUnit.MILLISECONDS.toNanos(1500);
    private final JComboBox<Webcam> devices = new JComboBox<>();
    private final JComboBox<Dimension> resolutions = new JComboBox<>();
    private final JButton start = new JButton("Start");
    private final JButton takePhoto = new JButton("Take Photo");
    private final JLabel status = new JLabel("Finding cameras...");
    private final JLabel frameRate = new JLabel("Waiting for frames");
    private final Function<String, String> onBarcode;
    private final Timer previewTimer = new Timer(16, event -> showLatestFrame());
    private final JPanel preview = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (displayed == null) return;
            BufferedImage image = displayed.image();
            double scale = Math.min((double) getWidth() / image.getWidth(),
                    (double) getHeight() / image.getHeight());
            int width = (int) (image.getWidth() * scale);
            int height = (int) (image.getHeight() * scale);
            graphics.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2,
                    width, height, null);
        }
    };

    private Stream stream;
    private Frame displayed;
    private BufferedImage photo;
    private SwingWorker<String, Void> decoder;
    private long nextScan;
    private long lastRead;
    private long nextFpsUpdate;
    private String lastBarcode = "";

    static BufferedImage takePhoto(Component owner) {
        CameraDialog dialog = new CameraDialog(owner, null);
        dialog.showCamera();
        return dialog.photo;
    }

    static void scan(Component owner, Function<String, String> onBarcode) {
        new CameraDialog(owner, onBarcode).showCamera();
    }

    private CameraDialog(Component owner, Function<String, String> onBarcode) {
        super(SwingUtilities.getWindowAncestor(owner),
                onBarcode == null ? "Product Camera" : "Barcode Scanner", ModalityType.APPLICATION_MODAL);
        this.onBarcode = onBarcode;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel();
        controls.add(devices);
        controls.add(resolutions);
        controls.add(start);
        if (onBarcode == null) controls.add(takePhoto);
        JButton close = new JButton("Close");
        controls.add(close);
        add(controls, BorderLayout.NORTH);
        preview.setBackground(new Color(20, 43, 54));
        preview.setPreferredSize(new Dimension(640, 360));
        add(preview, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        footer.add(status, BorderLayout.CENTER);
        footer.add(frameRate, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        devices.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Webcam camera ? camera.getName() : "No camera");
                return this;
            }
        });
        resolutions.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value instanceof Dimension size ? size.width + " × " + size.height : "Resolution");
                return this;
            }
        });
        devices.addActionListener(event -> loadResolutions());
        start.setEnabled(false);
        start.addActionListener(event -> {
            if (stream == null) startStream();
            else stopStream();
        });
        takePhoto.setEnabled(false);
        takePhoto.addActionListener(event -> {
            if (displayed != null) {
                photo = displayed.image();
                dispose();
            }
        });
        close.addActionListener(event -> dispose());
        pack();
        setLocationRelativeTo(owner);
    }

    @Override public void dispose() {
        stopStream();
        super.dispose();
    }

    private void showCamera() {
        CAMERA.execute(() -> {
            try {
                Webcam[] cameras = Webcam.getWebcams().toArray(Webcam[]::new);
                SwingUtilities.invokeLater(() -> {
                    if (!isDisplayable()) return;
                    devices.setModel(new DefaultComboBoxModel<>(cameras));
                    loadResolutions();
                    status.setText(cameras.length == 0 ? "No camera detected." : "Choose a resolution and click Start.");
                });
            } catch (RuntimeException | LinkageError ex) {
                reportError("Camera detection failed", ex);
            }
        });
        setVisible(true);
    }

    private void loadResolutions() {
        Webcam selected = (Webcam) devices.getSelectedItem();
        start.setEnabled(false);
        resolutions.removeAllItems();
        if (selected == null) return;
        CAMERA.execute(() -> {
            try {
                Dimension[] sizes = selected.getViewSizes();
                SwingUtilities.invokeLater(() -> {
                    if (!isDisplayable() || devices.getSelectedItem() != selected) return;
                    resolutions.setModel(new DefaultComboBoxModel<>(sizes));
                    for (Dimension size : sizes) {
                        if (size.width == 640 && size.height == 480) {
                            resolutions.setSelectedItem(size);
                            break;
                        }
                    }
                    start.setEnabled(sizes.length > 0);
                });
            } catch (RuntimeException | LinkageError ex) {
                reportError("Could not read camera resolutions", ex);
            }
        });
    }

    private void startStream() {
        Webcam selected = (Webcam) devices.getSelectedItem();
        Dimension size = (Dimension) resolutions.getSelectedItem();
        if (selected == null || size == null) return;
        Stream active = new Stream(selected, size);
        stream = active;
        devices.setEnabled(false);
        resolutions.setEnabled(false);
        start.setText("Stop");
        status.setText("Opening camera...");
        CAMERA.execute(() -> {
            try {
                if (active.closed) return;
                active.camera.setViewSize(active.size);
                if (!active.camera.open()) throw new IllegalStateException("Camera did not open.");
                if (active.closed) return;
                active.capture = CAMERA.scheduleAtFixedRate(() -> readFrame(active), 0,
                        FRAME_PERIOD_NS, TimeUnit.NANOSECONDS);
                SwingUtilities.invokeLater(() -> {
                    if (stream == active && isDisplayable()) {
                        status.setText(onBarcode == null ? "Center one product in the frame."
                                : "Hold a barcode in front of the camera.");
                    }
                });
            } catch (RuntimeException | LinkageError ex) {
                streamError(active, ex);
            }
        });
        previewTimer.start();
    }

    private void readFrame(Stream active) {
        if (active.closed) return;
        try {
            BufferedImage image = active.camera.getImage();
            if (image != null) active.latest = new Frame(image, active.camera.getFPS());
        } catch (RuntimeException | LinkageError ex) {
            active.capture.cancel(false);
            streamError(active, ex);
        }
    }

    private void showLatestFrame() {
        if (stream == null || stream.latest == null) return;
        Frame latest = stream.latest;
        long now = System.nanoTime();
        if (latest != displayed) {
            displayed = latest;
            preview.repaint();
            takePhoto.setEnabled(onBarcode == null);
        }
        if (nextFpsUpdate == 0 || now - nextFpsUpdate >= 0) {
            frameRate.setText(String.format(Locale.ROOT, "Capture %.1f FPS", latest.fps()));
            nextFpsUpdate = now + TimeUnit.MILLISECONDS.toNanos(500);
        }
        if (onBarcode != null && decoder == null && (nextScan == 0 || now - nextScan >= 0)) {
            nextScan = now + SCAN_PERIOD_NS;
            decodeFrame(stream, latest.image());
        }
    }

    private void decodeFrame(Stream active, BufferedImage image) {
        decoder = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return BarcodeReader.decode(image);
            }

            @Override protected void done() {
                if (decoder != this) return;
                decoder = null;
                if (stream != active || !isDisplayable()) return;
                try {
                    String barcode = get();
                    long now = System.nanoTime();
                    if (barcode == null) {
                        if (now - lastRead > BARCODE_RESET_NS) lastBarcode = "";
                    } else {
                        if (!barcode.equals(lastBarcode)) status.setText(onBarcode.apply(barcode));
                        lastBarcode = barcode;
                        lastRead = now;
                    }
                } catch (Exception ex) {
                    status.setText("Could not read this frame.");
                }
            }
        };
        decoder.execute();
    }

    private void stopStream() {
        previewTimer.stop();
        Stream active = stream;
        stream = null;
        if (active != null) {
            active.closed = true;
            CAMERA.execute(() -> {
                if (active.capture != null) active.capture.cancel(false);
                if (active.camera.isOpen()) active.camera.close();
            });
        }
        if (decoder != null) {
            decoder.cancel(false);
            decoder = null;
        }
        displayed = null;
        nextScan = 0;
        nextFpsUpdate = 0;
        lastBarcode = "";
        takePhoto.setEnabled(false);
        devices.setEnabled(true);
        resolutions.setEnabled(true);
        start.setText("Start");
        frameRate.setText("Waiting for frames");
        status.setText("Camera stopped.");
        preview.repaint();
    }

    private void streamError(Stream active, Throwable error) {
        SwingUtilities.invokeLater(() -> {
            if (stream != active || !isDisplayable()) return;
            stopStream();
            status.setText("Camera error: " + error.getMessage());
        });
    }

    private void reportError(String message, Throwable error) {
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) status.setText(message + ": " + error.getMessage());
        });
    }

    private record Frame(BufferedImage image, double fps) {}

    private static final class Stream {
        private final Webcam camera;
        private final Dimension size;
        private volatile Frame latest;
        private volatile boolean closed;
        private ScheduledFuture<?> capture;

        private Stream(Webcam camera, Dimension size) {
            this.camera = camera;
            this.size = size;
        }
    }
}
