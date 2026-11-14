package com.orbitdlc.client;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

record SkyImage(int width, int height, int[][] frames, long[] endTimes) {
    static SkyImage read(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > 64L * 1024 * 1024)
            throw new IOException("Choose an image up to 64 MB");

        try (var stream = ImageIO.createImageInputStream(path.toFile())) {
            if (stream == null) throw new IOException("Cannot open image");
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                boolean gif = reader.getFormatName().equalsIgnoreCase("gif");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (gif) {
                    var root = (IIOMetadataNode) reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                    var size = child(root, "LogicalScreenDescriptor");
                    width = number(size, "logicalScreenWidth");
                    height = number(size, "logicalScreenHeight");
                }
                int count = gif ? reader.getNumImages(true) : 1;
                if (width < 1 || height < 1 || width > 8192 || height > 4096 || count < 1 || count > 600
                        || (long) width * height * count > 24_000_000)
                    throw new IOException("Image or animation is too large");

                var frames = new int[count][];
                var times = new long[count];
                var canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                var graphics = canvas.createGraphics();
                try {
                    long time = 0;
                    for (int i = 0; i < count; i++) {
                        int x = 0, y = 0, delay = 100;
                        String disposal = "";
                        if (gif) {
                            var root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                            var position = child(root, "ImageDescriptor");
                            var control = child(root, "GraphicControlExtension");
                            x = number(position, "imageLeftPosition");
                            y = number(position, "imageTopPosition");
                            delay = Math.max(20, number(control, "delayTime") * 10);
                            disposal = control.getAttribute("disposalMethod");
                            if (reader.getWidth(i) > width || reader.getHeight(i) > height)
                                throw new IOException("Invalid GIF frame size");
                        }
                        int[] previous = disposal.equals("restoreToPrevious")
                                ? canvas.getRGB(0, 0, width, height, null, 0, width) : null;
                        var frame = reader.read(i);
                        graphics.drawImage(frame, x, y, null);
                        frames[i] = canvas.getRGB(0, 0, width, height, null, 0, width);
                        times[i] = time += delay;

                        if (previous != null) canvas.setRGB(0, 0, width, height, previous, 0, width);
                        else if (disposal.equals("restoreToBackgroundColor")) {
                            graphics.setComposite(AlphaComposite.Clear);
                            graphics.fillRect(x, y, frame.getWidth(), frame.getHeight());
                            graphics.setComposite(AlphaComposite.SrcOver);
                        }
                    }
                } finally {
                    graphics.dispose();
                }
                return new SkyImage(width, height, frames, times);
            } finally {
                reader.dispose();
            }
        }
    }

    int frameAt(long elapsedMillis) {
        if (frames.length == 1) return 0;
        long time = elapsedMillis % endTimes[endTimes.length - 1];
        int index = java.util.Arrays.binarySearch(endTimes, time + 1);
        return index >= 0 ? index : -index - 1;
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        var node = (IIOMetadataNode) root.getElementsByTagName(name).item(0);
        return node == null ? new IIOMetadataNode(name) : node;
    }

    private static int number(IIOMetadataNode node, String attribute) {
        String value = node.getAttribute(attribute);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }
}
