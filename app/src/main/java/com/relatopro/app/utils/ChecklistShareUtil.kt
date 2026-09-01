package com.relatopro.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64

object ChecklistShareUtil {

    private const val PREFIX_QR = "RELATOPRO:CHK:1:"

    data class ExportedItem(
        val label: String,
        val type: String,
        val isRequired: Boolean,
        val orderIndex: Int
    )

    data class ExportedCategory(
        val name: String,
        val items: List<ExportedItem>
    )

    data class ChecklistPackage(
        val name: String,
        val description: String,
        val category: String,
        val categories: List<ExportedCategory>,
        val totalQuestions: Int
    )

    /**
     * Converte um checklist e seus campos em uma estrutura JSON completa.
     */
    fun serializeChecklistToJson(template: TemplateEntity, fields: List<TemplateFieldEntity>): String {
        val root = JSONObject()
        root.put("schema", "relatopro_checklist_v1")
        root.put("name", template.name)
        root.put("description", template.description)
        root.put("category", template.category)
        root.put("version", template.version)
        root.put("createdAt", System.currentTimeMillis())

        val categoriesArray = JSONArray()
        val grouped: Map<String, List<TemplateFieldEntity>> = fields.groupBy { it.category.ifBlank { "GERAL" } }

        for ((catName, fieldList) in grouped) {
            val catObj = JSONObject()
            catObj.put("name", catName)

            val itemsArray = JSONArray()
            for (f in fieldList) {
                val itemObj = JSONObject()
                itemObj.put("label", f.label)
                itemObj.put("type", f.type)
                itemObj.put("isRequired", f.isRequired)
                itemObj.put("orderIndex", f.orderIndex)
                itemsArray.put(itemObj)
            }
            catObj.put("items", itemsArray)
            categoriesArray.put(catObj)
        }

        root.put("categories", categoriesArray)
        return root.toString(2)
    }

    /**
     * Compacta o JSON do checklist com GZIP + Base64 para gerar um QR Code ultracompacto.
     */
    fun encodeChecklistToQrPayload(jsonStr: String): String {
        val minified = JSONObject(jsonStr).toString()
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(minified.toByteArray(StandardCharsets.UTF_8))
        }
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        return PREFIX_QR + base64
    }

    /**
     * Decodifica um payload de QR Code (ou texto colado) de volta para o pacote de checklist.
     */
    fun decodeChecklistPayload(payload: String): ChecklistPackage? {
        val clean = payload.trim()
        val jsonStr = try {
            if (clean.startsWith(PREFIX_QR)) {
                val base64Data = clean.substring(PREFIX_QR.length)
                val compressedBytes = Base64.decode(base64Data, Base64.NO_WRAP or Base64.URL_SAFE)
                val bais = ByteArrayInputStream(compressedBytes)
                GZIPInputStream(bais).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            } else {
                clean
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return parseChecklistJson(jsonStr)
    }

    /**
     * Decodifica um Bitmap contendo uma imagem de QR Code usando ZXing nativo.
     */
    fun decodeQrBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Realiza o parser e validação do JSON de um checklist.
     */
    fun parseChecklistJson(jsonStr: String): ChecklistPackage? {
        return try {
            val root = JSONObject(jsonStr)
            val name = root.optString("name", "Checklist Importado")
            val description = root.optString("description", "")
            val category = root.optString("category", "Personalizados")

            val categoriesList = mutableListOf<ExportedCategory>()
            var questionCount = 0

            val categoriesArray = root.optJSONArray("categories")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    val catObj = categoriesArray.getJSONObject(i)
                    val catName = catObj.optString("name", "GERAL")
                    val itemsArray = catObj.optJSONArray("items") ?: JSONArray()
                    val itemsList = mutableListOf<ExportedItem>()

                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        val label = itemObj.optString("label", "")
                        if (label.isNotBlank()) {
                            itemsList.add(
                                ExportedItem(
                                    label = label,
                                    type = itemObj.optString("type", "C_NC_NA"),
                                    isRequired = itemObj.optBoolean("isRequired", true),
                                    orderIndex = itemObj.optInt("orderIndex", j)
                                )
                            )
                            questionCount++
                        }
                    }
                    if (itemsList.isNotEmpty()) {
                        categoriesList.add(ExportedCategory(catName, itemsList))
                    }
                }
            }

            if (categoriesList.isEmpty()) null else ChecklistPackage(
                name = name,
                description = description,
                category = category,
                categories = categoriesList,
                totalQuestions = questionCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Gera um Bitmap do QR Code usando o ZXing QRCodeWriter nativo.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exporta o checklist como um arquivo .relatopro e prepara o Intent para compartilhamento.
     */
    suspend fun exportChecklistToFile(
        context: Context,
        template: TemplateEntity,
        fields: List<TemplateFieldEntity>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val jsonContent = serializeChecklistToJson(template, fields)
            val exportDir = File(context.cacheDir, "shared_checklists").apply { mkdirs() }
            val safeName = template.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(exportDir, "checklist_${safeName}.relatopro")

            file.writeText(jsonContent, StandardCharsets.UTF_8)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Dispara o menu nativo de compartilhamento do Android para enviar o arquivo .relatopro.
     */
    fun shareChecklistFile(context: Context, file: File, templateName: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Checklist Relato Pro: $templateName")
                putExtra(Intent.EXTRA_TEXT, "Compartilhando o formulário/checklist '$templateName' do Relato Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Checklist"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lê e faz o parser de um arquivo .relatopro ou .json selecionado pelo usuário.
     */
    suspend fun parseChecklistFromFileUri(context: Context, uri: Uri): ChecklistPackage? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            decodeChecklistPayload(content) ?: parseChecklistJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
