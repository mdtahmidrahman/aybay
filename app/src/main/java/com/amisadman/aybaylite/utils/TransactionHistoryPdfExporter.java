package com.amisadman.aybaylite.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.amisadman.aybaylite.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public final class TransactionHistoryPdfExporter {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 24;
    private static final int HEADER_HEIGHT = 28;
    private static final int ROW_HEIGHT = 30;

    private TransactionHistoryPdfExporter() {
    }

    public static String export(Context context, ArrayList<HashMap<String, String>> history) throws IOException {
        PdfDocument pdfDocument = new PdfDocument();
        String fileName = buildOutputFileName(context);

        try {
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(18f);
            titlePaint.setFakeBoldText(true);

            Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            subtitlePaint.setColor(Color.DKGRAY);
            subtitlePaint.setTextSize(10f);

            Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            headerPaint.setColor(Color.WHITE);
            headerPaint.setTextSize(10f);
            headerPaint.setFakeBoldText(true);

            Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(Color.BLACK);
            bodyPaint.setTextSize(9.5f);

            Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(Color.parseColor("#D0D0D0"));
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(1f);

            Paint headerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            headerBackgroundPaint.setColor(Color.parseColor("#2F3A4A"));

            int contentLeft = MARGIN;
            int contentRight = PAGE_WIDTH - MARGIN;
            int contentWidth = contentRight - contentLeft;

            int typeWidth = 72;
            int amountWidth = 110;
            int timeWidth = 150;
            int reasonWidth = contentWidth - typeWidth - amountWidth - timeWidth;

            int typeLeft = contentLeft;
            int reasonLeft = typeLeft + typeWidth;
            int amountLeft = reasonLeft + reasonWidth;
            int timeLeft = amountLeft + amountWidth;

            int y = MARGIN;
            int pageNumber = 1;

            PdfDocument.Page page = pdfDocument.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
            Canvas canvas = page.getCanvas();

            y = drawPageHeader(canvas, titlePaint, subtitlePaint, y, contentLeft, contentRight);
            y += 12;
            y = drawTableHeader(canvas, headerBackgroundPaint, headerPaint, gridPaint, y,
                    typeLeft, reasonLeft, amountLeft, timeLeft, contentRight,
                    typeWidth, reasonWidth, amountWidth, timeWidth);

            if (history == null || history.isEmpty()) {
                y += 24;
                canvas.drawText("No transaction history available.", contentLeft, y, bodyPaint);
            } else {
                for (HashMap<String, String> entry : history) {
                    if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                        pdfDocument.finishPage(page);
                        pageNumber++;
                        page = pdfDocument.startPage(new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create());
                        canvas = page.getCanvas();
                        y = drawPageHeader(canvas, titlePaint, subtitlePaint, MARGIN, contentLeft, contentRight);
                        y += 12;
                        y = drawTableHeader(canvas, headerBackgroundPaint, headerPaint, gridPaint, y,
                                typeLeft, reasonLeft, amountLeft, timeLeft, contentRight,
                                typeWidth, reasonWidth, amountWidth, timeWidth);
                    }

                    drawTableRow(canvas, gridPaint, bodyPaint, y, entry,
                            typeLeft, reasonLeft, amountLeft, timeLeft,
                            typeWidth, reasonWidth, amountWidth, timeWidth);
                    y += ROW_HEIGHT;
                }
            }

            pdfDocument.finishPage(page);
            return writeToDownloads(context, pdfDocument, fileName);
        } finally {
            pdfDocument.close();
        }
    }

    private static int drawPageHeader(Canvas canvas, Paint titlePaint, Paint subtitlePaint, int startY,
                                      int contentLeft, int contentRight) {
        int y = startY;
        canvas.drawText("Transaction History Report", contentLeft, y + 18, titlePaint);
        canvas.drawText("Generated: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()),
                contentLeft, y + 34, subtitlePaint);
        canvas.drawLine(contentLeft, y + 44, contentRight, y + 44, subtitlePaint);
        return y + 48;
    }

    private static int drawTableHeader(Canvas canvas, Paint headerBackgroundPaint, Paint headerPaint, Paint gridPaint,
                                       int top, int typeLeft, int reasonLeft, int amountLeft, int timeLeft,
                                       int contentRight, int typeWidth, int reasonWidth, int amountWidth, int timeWidth) {
        int bottom = top + HEADER_HEIGHT;
        canvas.drawRect(typeLeft, top, contentRight, bottom, headerBackgroundPaint);
        canvas.drawRect(typeLeft, top, contentRight, bottom, gridPaint);

        drawHeaderLabel(canvas, headerPaint, "Type", typeLeft, top, typeWidth, HEADER_HEIGHT);
        drawHeaderLabel(canvas, headerPaint, "Reason", reasonLeft, top, reasonWidth, HEADER_HEIGHT);
        drawHeaderLabel(canvas, headerPaint, "Amount", amountLeft, top, amountWidth, HEADER_HEIGHT);
        drawHeaderLabel(canvas, headerPaint, "Time", timeLeft, top, timeWidth, HEADER_HEIGHT);

        canvas.drawLine(reasonLeft, top, reasonLeft, bottom, gridPaint);
        canvas.drawLine(amountLeft, top, amountLeft, bottom, gridPaint);
        canvas.drawLine(timeLeft, top, timeLeft, bottom, gridPaint);
        return bottom;
    }

    private static void drawHeaderLabel(Canvas canvas, Paint paint, String label, int left, int top, int width, int height) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float x = left + 8;
        float y = top + (height - (metrics.bottom - metrics.top)) / 2f - metrics.top;
        canvas.drawText(label, x, y, paint);
    }

    private static void drawTableRow(Canvas canvas, Paint gridPaint, Paint bodyPaint, int top,
                                     HashMap<String, String> entry, int typeLeft, int reasonLeft,
                                     int amountLeft, int timeLeft, int typeWidth, int reasonWidth,
                                     int amountWidth, int timeWidth) {
        int bottom = top + ROW_HEIGHT;
        int contentRight = timeLeft + timeWidth;

        canvas.drawRect(typeLeft, top, contentRight, bottom, gridPaint);
        canvas.drawLine(reasonLeft, top, reasonLeft, bottom, gridPaint);
        canvas.drawLine(amountLeft, top, amountLeft, bottom, gridPaint);
        canvas.drawLine(timeLeft, top, timeLeft, bottom, gridPaint);

        String type = safeValue(entry.get("type"));
        String reason = safeValue(entry.get("reason"));
        String amount = formatAmount(entry.get("amount"));
        String time = safeValue(entry.get("time"));

        drawCellText(canvas, bodyPaint, type, typeLeft, top, typeWidth, ROW_HEIGHT, false);
        drawCellText(canvas, bodyPaint, reason, reasonLeft, top, reasonWidth, ROW_HEIGHT, false);
        drawCellText(canvas, bodyPaint, amount, amountLeft, top, amountWidth, ROW_HEIGHT, true);
        drawCellText(canvas, bodyPaint, time, timeLeft, top, timeWidth, ROW_HEIGHT, false);
    }

    private static void drawCellText(Canvas canvas, Paint paint, String text, int left, int top, int width,
                                     int height, boolean alignRight) {
        String displayText = fitTextWithEllipsis(text, paint, width - 16);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float y = top + (height - (metrics.bottom - metrics.top)) / 2f - metrics.top;
        float x = alignRight ? left + width - 8 - paint.measureText(displayText) : left + 8;
        canvas.drawText(displayText, x, y, paint);
    }

    private static String fitTextWithEllipsis(String text, Paint paint, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (paint.measureText(text) <= maxWidth) {
            return text;
        }

        final String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            String candidate = text.substring(0, mid);
            if (paint.measureText(candidate) + ellipsisWidth <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return text.substring(0, low) + ellipsis;
    }

    private static String formatAmount(String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            return "৳ 0.00";
        }

        try {
            double amount = Double.parseDouble(amountText);
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            String sign = amount >= 0 ? "+" : "-";
            return "৳ " + sign + decimalFormat.format(Math.abs(amount));
        } catch (NumberFormatException e) {
            return "৳ " + amountText;
        }
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }

    private static String writeToDownloads(Context context, PdfDocument pdfDocument, String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri itemUri = context.getContentResolver().insert(collection, contentValues);
            if (itemUri == null) {
                throw new IOException("Unable to create PDF file in Downloads");
            }

            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(itemUri);
                if (outputStream == null) {
                    throw new IOException("Unable to open output stream for PDF");
                }

                try (OutputStream stream = outputStream) {
                    pdfDocument.writeTo(stream);
                }

                ContentValues completed = new ContentValues();
                completed.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(itemUri, completed, null, null);
                return Environment.DIRECTORY_DOWNLOADS + File.separator + fileName;
            } catch (IOException e) {
                context.getContentResolver().delete(itemUri, null, null);
                throw e;
            }
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create export directory");
        }

        File outputFile = new File(directory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            pdfDocument.writeTo(outputStream);
        }
        return outputFile.getAbsolutePath();
    }

    private static String buildOutputFileName(Context context) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return context.getString(R.string.app_name) + "_Transaction_History_" + timestamp + ".pdf";
    }
}