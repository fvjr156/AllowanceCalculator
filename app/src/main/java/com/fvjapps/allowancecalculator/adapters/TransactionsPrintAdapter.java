package com.fvjapps.allowancecalculator.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import com.fvjapps.allowancecalculator.entities.TransactionEntity;
import com.fvjapps.allowancecalculator.misc.MillisConv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class TransactionsPrintAdapter extends PrintDocumentAdapter {

    public interface TransactionPrintListener {
        void onSuccess();

        void onFail(String error);
    }

    private final TransactionPrintListener listener;

    private final Context context;
    private final List<TransactionEntity> entityList;
    private PdfDocument pdfDocument;

    public TransactionsPrintAdapter(Context context,
                                    List<TransactionEntity> transactions, TransactionPrintListener listener) {
        this.context = context;
        this.entityList = transactions;
        this.listener = listener;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        pdfDocument = new PdfDocument();

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder(MillisConv.toDate(System.currentTimeMillis(), MillisConv.DateFormat.FILE_BACKUP) + "_transactions.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages,
                        ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setTextSize(12f);

        Paint paint2 = new Paint();
        paint2.setTextSize(16f);
        paint2.setTypeface(Typeface.DEFAULT_BOLD);

        int startX = 40;
        int startY = 40;
        int rowHeight = 20;

        Function<String, String> translateType = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return switch (s) {
                    case "IN" -> "ALLOWANCE";
                    case "OUT" -> "EXPENSE";
                    default -> s;
                };
            }
        };

        String exportDate = MillisConv.toDate(System.currentTimeMillis(), MillisConv.DateFormat.DATABASE_STANDARD);

        canvas.drawText("Allowance Calculator Application — Transactions Export", startX, startY, paint2);
        canvas.drawText("Exported at: " + exportDate, startX, startY + 25, paint);

        int y = startY + 60;

        canvas.drawText("ID", startX, y, paint);
        canvas.drawText("Type", startX + 50, y, paint);
        canvas.drawText("Amount", startX + 160, y, paint);
        canvas.drawText("Date", startX + 240, y, paint);
        canvas.drawText("Label", startX + 410, y, paint);

        y += rowHeight;

        for (TransactionEntity t : entityList) {

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                pdfDocument.close();
                pdfDocument = null;
                return;
            }

            canvas.drawText(String.valueOf(t.transactionId), startX, y, paint);
            canvas.drawText(translateType.apply(t.type), startX + 50, y, paint);
            canvas.drawText(String.valueOf(t.amount), startX + 160, y, paint);
            canvas.drawText(MillisConv.toDate(t.createdAt, MillisConv.DateFormat.DATABASE_STANDARD), startX + 240, y, paint);
            canvas.drawText(
                    t.label != null ? t.label : "",
                    startX + 410,
                    y,
                    paint
            );

            y += rowHeight;

            if (y > 800) {
                pdfDocument.finishPage(page);

                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = startY;
            }
        }

        pdfDocument.finishPage(page);

        try (FileOutputStream out =
                     new FileOutputStream(destination.getFileDescriptor())) {

            pdfDocument.writeTo(out);
            listener.onSuccess();

        } catch (IOException e) {
            callback.onWriteFailed(e.getMessage());
            listener.onFail(e.getMessage());
            return;
        } finally {
            pdfDocument.close();
            pdfDocument = null;
        }

        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
    }
}
