package com.rs.booklet7;




import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.RelativeLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import android.content.res.AssetManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookletActivity extends AppCompatActivity {

    private static final int SAVE_REQUEST_CODE = 102;
    private ActivityResultLauncher<String> filePicker;
    private File inputFile;
    private ExecutorService executorService;

    // Progress UI Components
    private RelativeLayout progressContainer;
    private ProgressBar circularProgress;
    private TextView progressPercentage;
    private TextView progressText;

    // ✅ HIGHEST QUALITY SETTINGS (No Compromise)
    private static final int BITMAP_SCALE_FACTOR = 4;
    private static final int OUTPUT_DPI = 300;
    private static final boolean USE_ULTRA_QUALITY = true;
    private static final float A4_WIDTH_300DPI = 2480f;
    private static final float A4_HEIGHT_300DPI = 3508f;
    private static final int MAX_BITMAP_DIMENSION = 8192;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotate);

        // ✅ Initialize optimized thread pool
        executorService = Executors.newFixedThreadPool(1);

        // Initialize progress UI
        initializeProgressUI();

        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                showProgress("Loading PDF...", 0);
                final Uri finalUri = uri;
                executorService.execute(() -> {
                    File resultFile = uriToFileOptimized(finalUri);
                    runOnUiThread(() -> {
                        hideProgress();
                        if (resultFile != null) {
                            inputFile = resultFile;
                            long fileSizeMB = inputFile.length() / (1024 * 1024);

                            String defaultFileName = "UltraQuality_Booklet_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".pdf";
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
                            startActivityForResult(intent, SAVE_REQUEST_CODE);
                        } else {
                            Toast.makeText(BookletActivity.this, "Failed to process PDF", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                Toast.makeText(this, "No PDF selected", Toast.LENGTH_SHORT).show();
            }
        });

     //   loadPdfPreviewFromAssets();

        Button selectButton = findViewById(R.id.select_pdf);
        selectButton.setOnClickListener(v -> filePicker.launch("application/pdf"));
    }

    // ✅ INITIALIZE PROGRESS UI
    private void initializeProgressUI() {
        progressContainer = findViewById(R.id.progress_container);
        circularProgress = findViewById(R.id.circular_progress);
        progressPercentage = findViewById(R.id.progress_percentage);
        progressText = findViewById(R.id.progress_text);

        // Set circular progress properties
        circularProgress.setMax(100);
        circularProgress.setProgress(0);
    }

    // ✅ SHOW PROGRESS WITH PERCENTAGE
    private void showProgress(String message, int progress) {
        runOnUiThread(() -> {
            progressText.setText(message);
            circularProgress.setProgress(progress);
            progressPercentage.setText(progress + "%");
            progressContainer.setVisibility(RelativeLayout.VISIBLE);
        });
    }

    // ✅ UPDATE PROGRESS
    private void updateProgress(String message, int progress) {
        runOnUiThread(() -> {
            progressText.setText(message);
            circularProgress.setProgress(progress);
            progressPercentage.setText(progress + "%");
        });
    }

    // ✅ HIDE PROGRESS
    private void hideProgress() {
        runOnUiThread(() -> {
            progressContainer.setVisibility(RelativeLayout.GONE);
        });
    }

    // ✅ SHOW SUCCESS MESSAGE
    private void showSuccess(String message) {
        runOnUiThread(() -> {
            Toast.makeText(BookletActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    // ✅ OPTIMIZED FILE COPY FOR LARGE FILES
    private File uriToFileOptimized(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to open PDF stream", Toast.LENGTH_LONG).show());
                return null;
            }

            File file = new File(getCacheDir(), "temp_ultra_quality.pdf");
            outputStream = new FileOutputStream(file);

            // Get total file size for progress calculation
            long totalFileSize = getFileSize(uri);
            byte[] buffer = new byte[64 * 1024];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Update progress for file copying
                if (totalFileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / totalFileSize);
                    updateProgress("Loading PDF...", progress);
                }
            }

            outputStream.flush();
            return file;

        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(BookletActivity.this, "Error copying file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            });
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ✅ GET FILE SIZE FROM URI
    private long getFileSize(Uri uri) {
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    return size;
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                showProgress("Processing PDF...", 0);
                final Uri finalOutputUri = uri;
                executorService.execute(() -> {
                    processPdfUltraQuality(finalOutputUri);
                });
            }
        }
    }

    // ✅ ULTRA QUALITY PDF PROCESSING WITH PROGRESS UPDATES
    private void processPdfUltraQuality(Uri outputUri) {
        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;
        PdfDocument outputDoc = null;
        FileOutputStream fos = null;

        try {
            if (inputFile == null || !inputFile.exists()) {
                throw new IOException("Input PDF file is missing");
            }

            pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();

            int totalPages = pageCount;
            if (totalPages % 4 != 0) {
                totalPages += (4 - totalPages % 4);
            }

            outputDoc = new PdfDocument();

            float a4Width = A4_WIDTH_300DPI;
            float a4Height = A4_HEIGHT_300DPI;

            float margin = 20f;
            float verticalGap = 220f;
            float partWidth = a4Width - 2 * margin;
            float partHeight = (a4Height - 2 * margin - verticalGap) / 2f;

            Paint borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(0xFF000000);
            borderPaint.setStrokeWidth(1f);
            borderPaint.setAntiAlias(true);

            int left = 1;
            int right = totalPages;

            int pageCounter = 0;
            final int totalOutputPages = (totalPages + 3) / 2;

            while (left < right) {
                processSingleSpread(renderer, outputDoc, left, right, a4Width, a4Height,
                        partWidth, partHeight, margin, verticalGap, borderPaint, pageCount);


                pageCounter += 2;
                left += 2;
                right -= 2;

                // ✅ Update progress percentage
                final int currentProgress = pageCounter;
                int progress = (currentProgress * 100) / totalOutputPages;
                updateProgress("Creating booklet...", progress);

                System.gc();
            }

            fos = (FileOutputStream) getContentResolver().openOutputStream(outputUri);
            outputDoc.writeTo(fos);
            fos.flush();

            hideProgress();
            showSuccess("✅ Ultra Quality PDF booklet created successfully! (" + pageCounter + " pages)");

        } catch (Exception e) {
            hideProgress();
            runOnUiThread(() -> {
                Toast.makeText(BookletActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            });
        } finally {
            try {
                if (renderer != null) renderer.close();
                if (pfd != null) pfd.close();
                if (outputDoc != null) outputDoc.close();
                if (fos != null) fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.gc();
        }
    }

    // ✅ PROCESS SINGLE SPREAD WITH FIXED ROTATION (ODD = 270°, EVEN = 90°)
    private void processSingleSpread(PdfRenderer renderer, PdfDocument outputDoc,
                                     int left, int right, float a4Width, float a4Height,
                                     float partWidth, float partHeight, float margin,
                                     float verticalGap, Paint borderPaint, int realPageCount) throws IOException {

        // ✅ Odd sheet hamesha 270°
        drawUltraQualityBookletPage(
                new int[]{right, left},                 // pages ka order
                new int[]{right, left},                 // original page numbers
                renderer, outputDoc,
                a4Width, a4Height, partWidth, partHeight, margin, verticalGap,
                borderPaint, realPageCount, 270f
        );

        // ✅ Even sheet hamesha 90°
        drawUltraQualityBookletPage(
                new int[]{left + 1, right - 1},         // pages ka order
                new int[]{left + 1, right - 1},         // original page numbers
                renderer, outputDoc,
                a4Width, a4Height, partWidth, partHeight, margin, verticalGap,
                borderPaint, realPageCount, 90f
        );
    }

    // ✅ ULTRA QUALITY BOOKLET PAGE DRAWING
    private void drawUltraQualityBookletPage(int[] pageIndices, int[] originalPageNumbers, PdfRenderer renderer, PdfDocument outputDoc,
                                             float a4Width, float a4Height, float partWidth, float partHeight,
                                             float margin, float verticalGap, Paint borderPaint, int realPageCount, float rotateAngle) throws IOException {

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder((int) a4Width, (int) a4Height,
                outputDoc.getPages().size() + 1).create();
        PdfDocument.Page outputPage = outputDoc.startPage(pageInfo);
        Canvas canvas = outputPage.getCanvas();

        Paint ultraQualityPaint = new Paint();
        ultraQualityPaint.setFilterBitmap(true);
        ultraQualityPaint.setAntiAlias(true);
        ultraQualityPaint.setDither(true);
        ultraQualityPaint.setFlags(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        Paint pageNumberPaint = new Paint();
        pageNumberPaint.setColor(0xFF000000);
        pageNumberPaint.setTextSize(36f);
        pageNumberPaint.setAntiAlias(true);
        pageNumberPaint.setTextAlign(Paint.Align.CENTER);

        for (int j = 0; j < 2; j++) {
            int pageIndex = pageIndices[j] - 1;
            int originalPageNumber = originalPageNumbers[j];

            float left = margin;
            float top = margin + j * (partHeight + verticalGap);

            if (pageIndex >= 0 && pageIndex < realPageCount) {
                PdfRenderer.Page page = renderer.openPage(pageIndex);

                try {
                    Bitmap originalBitmap = renderUltraHighQualityPage(page);

                    if (originalBitmap != null) {
                        float scale = Math.min(partHeight / originalBitmap.getWidth(), partWidth / originalBitmap.getHeight());

                        canvas.save();
                        if (rotateAngle == 270f) {
                            canvas.translate(left, top + partHeight);
                        } else if (rotateAngle == 90f) {
                            canvas.translate(left + partWidth, top);
                        }
                        canvas.rotate(rotateAngle);
                        canvas.scale(scale, scale);

                        canvas.drawBitmap(originalBitmap, 0, 0, ultraQualityPaint);
                        canvas.restore();

                        if (!originalBitmap.isRecycled()) {
                            originalBitmap.recycle();
                        }
                    }
                } finally {
                    page.close();
                }

                drawPageNumber(canvas, left, top, partWidth, partHeight, originalPageNumber, rotateAngle, pageNumberPaint);

                // ✅ QR Code draw karo based on position (left/right)
                drawQRCodeAtVerticalCenter(canvas, left, top, partWidth, partHeight, rotateAngle, j);

            } else {
                Paint textPaint = new Paint();
                textPaint.setTextSize(24f);
                textPaint.setColor(0xFF999999);
                textPaint.setAntiAlias(true);
                canvas.drawText(" ", left + partWidth / 2 - 30, top + partHeight / 2, textPaint);
            }

            canvas.drawRect(left, top, left + partWidth, top + partHeight, borderPaint);
        }

        outputDoc.finishPage(outputPage);
    }

    // ✅ PAGE NUMBER DRAWING
    private void drawPageNumber(Canvas canvas, float left, float top, float width, float height,
                                int pageNumber, float rotation, Paint paint) {

        canvas.save();

        float centerX = left + width / 2;
        float centerY = top + height / 2;

        if (rotation == 270f) {
            canvas.translate(left + width - 40, centerY);
            canvas.rotate(-90);
        } else if (rotation == 90f) {
            canvas.translate(left + 40, centerY);
            canvas.rotate(90);
        }

        canvas.drawText(String.valueOf(pageNumber), 0, 0, paint);
        canvas.restore();
    }



    // ✅ QR CODE DRAWING AT VERTICAL CENTER
    private void drawQRCodeAtVerticalCenter(Canvas canvas, float left, float top, float width, float height, float rotation, int positionIndex) {
        Bitmap qrBitmap = loadQRCodeFromAssets();
        if (qrBitmap != null) {
            int qrSize = 100; // QR code ka size pixels
            Bitmap scaledQR = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true);

            float qrX, qrY;




            // Vertical center calculate karo
            float verticalCenter = top + height / 2 - qrSize / 2 -120;

            float horizontalGap = 1f; // kinare se gap, yaha 10 → 20

            if (rotation == 90f) {
                // Left page (90 degree rotation) - QR code LEFT side pe
                qrX = left + horizontalGap;
                qrY = verticalCenter;
            } else if (rotation == 270f) {
                // Right page (270 degree rotation) - QR code RIGHT side pe
                qrX = left + width - qrSize - horizontalGap;
                qrY = verticalCenter;
            } else {
                // Default case (agar koi rotation nahi hai)
                if (positionIndex == 0) {
                    qrX = left + horizontalGap;
                } else {
                    qrX = left + width - qrSize - horizontalGap;
                }
                qrY = verticalCenter;
            }

            canvas.drawBitmap(scaledQR, qrX, qrY, null);

            if (!scaledQR.isRecycled()) {
                scaledQR.recycle();
            }
        }
    }


    // ✅ QR CODE DRAWING AT VERTICAL CENTER
    private void FdrawQRCodeAtVerticalCenter(Canvas canvas, float left, float top, float width, float height, float rotation, int positionIndex) {
        Bitmap qrBitmap = loadQRCodeFromAssets();
        if (qrBitmap != null) {
            int qrSize = 60; // QR code ka size pixels
            Bitmap scaledQR = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true);

            float qrX, qrY;

            // Vertical center calculate karo
            float verticalCenter = top + height / 2 - qrSize / 2;

            if (rotation == 90f) {
                // Left page (90 degree rotation) - QR code LEFT side pe
                qrX = left + 10; // Left edge se 10px gap
                qrY = verticalCenter;
            } else if (rotation == 270f) {
                // Right page (270 degree rotation) - QR code RIGHT side pe
                qrX = left + width - qrSize - 10; // Right edge se 10px gap
                qrY = verticalCenter;
            } else {
                // Default case (agar koi rotation nahi hai)
                if (positionIndex == 0) {
                    // Left side
                    qrX = left + 10;
                } else {
                    // Right side
                    qrX = left + width - qrSize - 10;
                }
                qrY = verticalCenter;
            }

            canvas.drawBitmap(scaledQR, qrX, qrY, null);

            // Scaled bitmap ko recycle karo
            if (!scaledQR.isRecycled()) {
                scaledQR.recycle();
            }
        }
    }

    // ✅ ULTRA QUALITY RENDERING
    private Bitmap renderUltraHighQualityPage(PdfRenderer.Page page) {
        Bitmap bitmap = null;
        try {
            int originalWidth = page.getWidth();
            int originalHeight = page.getHeight();

            int ultraWidth = originalWidth * BITMAP_SCALE_FACTOR;
            int ultraHeight = originalHeight * BITMAP_SCALE_FACTOR;

            if (ultraWidth > MAX_BITMAP_DIMENSION || ultraHeight > MAX_BITMAP_DIMENSION) {
                float scale = Math.min(MAX_BITMAP_DIMENSION / (float) ultraWidth,
                        MAX_BITMAP_DIMENSION / (float) ultraHeight);
                ultraWidth = (int) (ultraWidth * scale);
                ultraHeight = (int) (ultraHeight * scale);
            }

            bitmap = Bitmap.createBitmap(ultraWidth, ultraHeight, Bitmap.Config.ARGB_8888);

            if (USE_ULTRA_QUALITY) {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            } else {
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            }

            return bitmap;

        } catch (OutOfMemoryError e) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            System.gc();
            return renderUltraHighQualityPage(page);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ FALLBACK RENDERER
    private Bitmap renderHighQualityPageFallback(PdfRenderer.Page page) {
        try {
            int originalWidth = page.getWidth();
            int originalHeight = page.getHeight();

            int highWidth = originalWidth * 2;
            int highHeight = originalHeight * 2;

            Bitmap bitmap = Bitmap.createBitmap(highWidth, highHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ QR CODE LOADING FROM ASSETS

    private Bitmap loadQRCodeFromAssets() {
        try {
            InputStream is = getAssets().open("qr_code.png");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            return bitmap;
        } catch (IOException e) {
           // e.printStackTrace();
            return null;
        }
    }



    // ✅ FIXED PDF PREVIEW LOADING METHOD

    private void loadPdfPreviewFromAssets() {
        LinearLayout container = findViewById(R.id.pdf_pages_container);

        try {
            // Clear previous views
            container.removeAllViews();

            // AssetManager se PDF open karna
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("booklet.pdf"); // assets folder me file

            // Temporary file me copy karna kyunki PdfRenderer ko FileDescriptor chahiye
            File tempFile = new File(getCacheDir(), "temp_pdf.pdf");
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            inputStream.close();

            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);

            int pageCount = renderer.getPageCount();
            if (pageCount == 0) {
                Toast.makeText(this, "PDF has no pages", Toast.LENGTH_SHORT).show();
                renderer.close();
                pfd.close();
                return;
            }

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 8, 0, 8);

                container.addView(imageView);

                page.close();
            }

            renderer.close();
            pfd.close();

        } catch (Exception e) {
           // e.printStackTrace();
            Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show();
        }
    }



    // ✅ MEMORY MANAGEMENT
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (inputFile != null && inputFile.exists()) {
            inputFile.delete();
        }
    }
}