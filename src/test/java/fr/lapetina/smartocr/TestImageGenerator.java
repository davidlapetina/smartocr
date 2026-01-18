package fr.lapetina.smartocr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility for generating test images with text for OCR testing.
 */
public final class TestImageGenerator {

    static {
        // Enable headless mode for environments without display
        System.setProperty("java.awt.headless", "true");
    }

    private TestImageGenerator() {
    }

    /**
     * Creates a simple image containing the specified text.
     *
     * @param text the text to render in the image
     * @return PNG image as byte array
     */
    public static byte[] createImageWithText(String text) {
        return createImageWithText(text, 400, 200);
    }

    /**
     * Creates an image with the specified text and dimensions.
     *
     * @param text   the text to render
     * @param width  image width
     * @param height image height
     * @return PNG image as byte array
     */
    public static byte[] createImageWithText(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Enable anti-aliasing for better text rendering
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Black text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));

        // Draw text with word wrapping
        drawWrappedText(g2d, text, 20, 40, width - 40);

        g2d.dispose();

        return toPngBytes(image);
    }

    /**
     * Creates an invoice-style document image.
     *
     * @param invoiceNumber the invoice number
     * @param date          the invoice date
     * @param amount        the total amount
     * @param vendorName    the vendor name
     * @return PNG image as byte array
     */
    public static byte[] createInvoiceImage(String invoiceNumber, String date, String amount, String vendorName) {
        int width = 500;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Header
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2d.drawString("INVOICE", 200, 50);

        // Vendor name
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString(vendorName, 30, 100);

        // Invoice details
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));

        int y = 150;
        g2d.drawString("Invoice Number: " + invoiceNumber, 30, y);
        y += 30;
        g2d.drawString("Date: " + date, 30, y);
        y += 30;

        // Line separator
        g2d.drawLine(30, y, width - 30, y);
        y += 40;

        // Total
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("Total Amount: " + amount, 30, y);

        g2d.dispose();

        return toPngBytes(image);
    }

    /**
     * Creates a receipt-style image.
     *
     * @param storeName store name
     * @param items     array of item descriptions
     * @param total     total amount
     * @param date      receipt date
     * @return PNG image as byte array
     */
    public static byte[] createReceiptImage(String storeName, String[] items, String total, String date) {
        int width = 350;
        int height = 100 + (items.length * 25) + 100;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.BLACK);

        // Store name
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString(storeName, 20, 40);

        // Date
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2d.drawString(date, 20, 60);

        // Separator
        g2d.drawString("--------------------------------", 20, 80);

        // Items
        int y = 100;
        for (String item : items) {
            g2d.drawString(item, 20, y);
            y += 25;
        }

        // Separator and total
        g2d.drawString("--------------------------------", 20, y);
        y += 25;
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.drawString("TOTAL: " + total, 20, y);

        g2d.dispose();

        return toPngBytes(image);
    }

    private static void drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] lines = text.split("\n");

        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (fm.stringWidth(testLine) <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (!currentLine.isEmpty()) {
                        g2d.drawString(currentLine.toString(), x, y);
                        y += fm.getHeight();
                    }
                    currentLine = new StringBuilder(word);
                }
            }
            if (!currentLine.isEmpty()) {
                g2d.drawString(currentLine.toString(), x, y);
                y += fm.getHeight();
            }
        }
    }

    private static byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create PNG image", e);
        }
    }
}
