package com.relatopro.app.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class PdfGenerator(private val context: Context) {

    suspend fun generateReportPdf(
        report: ReportEntity,
        fields: List<TemplateFieldEntity>,
        answers: List<ReportAnswerEntity>,
        photos: Map<Long, List<String>> // FieldId -> List of local paths
    ): File? = withContext(Dispatchers.Main) {
        
        val templateHtml = try {
            context.assets.open("pdf_template.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        // --- Process Data ---
        val df = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        val dateOnlyDf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = df.format(Date(report.date))
        val dateOnlyStr = dateOnlyDf.format(Date(report.date))

        var html = templateHtml
            .replace("{{LOCAL_NAME}}", report.location)
            .replace("{{RESPONSIBLE_NAME}}", report.responsible)
            .replace("{{DATE_TIME}}", dateStr)
            .replace("{{DATE_ONLY}}", dateOnlyStr)
            .replace("{{REPORT_ID}}", report.id.toString())

        // --- Process Checklist Rows ---
        val checklistBuilder = StringBuilder()
        var signaturePhotoBase64: String? = null

        val allPhotos = mutableListOf<PhotoData>()

        for (field in fields) {
            val answer = answers.find { it.templateFieldId == field.id }
            val answerValue = answer?.answerValue ?: "N/A"
            val obsText = answer?.observation ?: "Sem observações."

            val badgeHtml = when (answerValue) {
                "C" -> "<span class=\"badge badge-c\">CONFORME</span>"
                "NC" -> "<span class=\"badge badge-nc\">NÃO CONFORME</span>"
                else -> "<span class=\"badge badge-na\">N/A</span>"
            }

            val orderStr = String.format(Locale.getDefault(), "%02d", field.orderIndex)

            checklistBuilder.append("""
                <tr>
                    <td>$orderStr</td>
                    <td>${field.label}</td>
                    <td style="text-align: center;">$badgeHtml</td>
                    <td>$obsText</td>
                </tr>
            """.trimIndent())

            // Process photos for this field
            val itemPhotos = photos[field.id]
            if (itemPhotos != null) {
                for (photoPath in itemPhotos) {
                    val base64 = imageFileToBase64(photoPath)
                    if (base64 != null) {
                        allPhotos.add(PhotoData(
                            base64 = base64,
                            label = "Item $orderStr",
                            description = field.label
                        ))
                    }
                }
            }
        }

        html = html.replace("{{CHECKLIST_ROWS}}", checklistBuilder.toString())

        // Add Signature to photos if exists
        // Wait, signatures are saved separately in a different entity? Or maybe saved inside the photos map with a special ID?
        // In FieldModeViewModel, saveSignature uses fieldId = -1L. Let's check!
        val signaturePhotos = photos[-1L]
        if (!signaturePhotos.isNullOrEmpty()) {
            val sigPath = signaturePhotos.last()
            val base64 = imageFileToBase64(sigPath)
            if (base64 != null) {
                signaturePhotoBase64 = base64
            }
        }

        // --- Process Photo Grid ---
        val photosBuilder = StringBuilder()
        val photosPerRow = 3
        var currentRowCount = 0

        val totalGridItems = ArrayList<PhotoData>(allPhotos)
        
        if (signaturePhotoBase64 != null) {
            totalGridItems.add(PhotoData(
                base64 = signaturePhotoBase64,
                label = "Assinatura do Inspetor",
                description = report.responsible,
                isSignature = true
            ))
        }

        for (i in 0 until totalGridItems.size step photosPerRow) {
            photosBuilder.append("<div class=\"photo-row\">\n")
            
            for (j in 0 until photosPerRow) {
                if (i + j < totalGridItems.size) {
                    val p = totalGridItems[i + j]
                    val imgStyle = if (p.isSignature) "max-height: 120px; border: none; object-fit: contain;" else ""
                    
                    photosBuilder.append("""
                        <div class="photo-card" ${if (p.isSignature) "style=\"vertical-align: middle;\"" else ""}>
                            <img src="${p.base64}" style="$imgStyle" alt="Evidência">
                            <div class="photo-caption" ${if (p.isSignature) "style=\"text-align: center; border-top: 1px solid #cbd5e1; padding-top: 8px;\"" else ""}>
                                <strong>${p.label}</strong>
                                ${p.description}
                            </div>
                        </div>
                    """.trimIndent())
                } else {
                    photosBuilder.append("<div class=\"photo-card\" style=\"border: none; background: transparent;\"></div>\n")
                }
            }
            photosBuilder.append("</div>\n")
        }

        html = html.replace("{{PHOTO_ROWS}}", photosBuilder.toString())

        // --- Render and Print to PDF using WebView ---
        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        // Format Name as requested: RELATO-PRO_Relatorio-Inspecao_NomeLocal_30-08-2026.pdf
        val safeLocation = report.location.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dateForName = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(report.date))
        val outputFile = File(outputDir, "RELATO-PRO_Relatorio-Inspecao_${safeLocation}_${dateForName}_${report.id}.pdf")

        return@withContext suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    
                    val printAdapter = webView.createPrintDocumentAdapter("Relatorio")
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    try {
                        val descriptor = ParcelFileDescriptor.open(
                            outputFile, 
                            ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
                        )
                        
                        printAdapter.onLayout(null, printAttributes, null, object : android.print.PdfPrint.LayoutResultCallbackWrapper() {
                            override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                                printAdapter.onWrite(arrayOf(PageRange.ALL_PAGES), descriptor, CancellationSignal(), object : android.print.PdfPrint.WriteResultCallbackWrapper() {
                                    override fun onWriteFinished(pages: Array<out PageRange>?) {
                                        super.onWriteFinished(pages)
                                        descriptor.close()
                                        if (continuation.isActive) {
                                            continuation.resume(outputFile)
                                        }
                                    }

                                    override fun onWriteFailed(error: CharSequence?) {
                                        super.onWriteFailed(error)
                                        descriptor.close()
                                        if (continuation.isActive) {
                                            continuation.resume(null)
                                        }
                                    }
                                })
                            }

                            override fun onLayoutFailed(error: CharSequence?) {
                                super.onLayoutFailed(error)
                                descriptor.close()
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }
            
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    private fun imageFileToBase64(filePath: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
            val outputStream = ByteArrayOutputStream()
            // Compress significantly to avoid huge base64 strings blocking the WebView
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private data class PhotoData(
        val base64: String,
        val label: String,
        val description: String,
        val isSignature: Boolean = false
    )
}
