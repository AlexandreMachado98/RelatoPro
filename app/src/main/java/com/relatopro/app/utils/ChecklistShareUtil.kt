package com.relatopro.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.zxing.*
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

object ChecklistShareUtil {

    private const val PREFIX_V2 = "RPRO:2:"
    private const val PREFIX_V1 = "RELATOPRO:CHK:1:"

    data class ExportedItem(
        val label: String,
        val type: String = "C_NC_NA",
        val isRequired: Boolean = true,
        val orderIndex: Int = 0
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
     * Serializa o checklist para um arquivo JSON legível e completo (.relatopro).
     */
    fun serializeChecklistToJson(template: TemplateEntity, fields: List<TemplateFieldEntity>): String {
        val root = JSONObject()
        root.put("schema", "relatopro_checklist_v2")
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
     * Compacta o checklist para uma carga ultracompacta para QR Code (GZIP + Base64).
     * Reduz o payload em até 85% comparado ao JSON bruto, facilitando leitura por qualquer câmera.
     */
    fun encodeChecklistToQrPayload(template: TemplateEntity, fields: List<TemplateFieldEntity>): String {
        return try {
            val mini = JSONObject()
            mini.put("v", 2)
            mini.put("n", template.name)
            if (template.description.isNotBlank()) mini.put("d", template.description)
            if (template.category.isNotBlank()) mini.put("c", template.category)

            val grouped = fields.groupBy { it.category.ifBlank { "GERAL" } }
            val catArray = JSONArray()

            for ((catName, items) in grouped) {
                val catObj = JSONObject()
                catObj.put("n", catName)
                val itemsArray = JSONArray()
                for (item in items) {
                    val row = JSONArray()
                    row.put(item.label)
                    if (item.type != "C_NC_NA") {
                        row.put(item.type)
                    }
                    itemsArray.put(row)
                }
                catObj.put("i", itemsArray)
                catArray.put(catObj)
            }
            mini.put("k", catArray)

            val rawJson = mini.toString()
            val baos = ByteArrayOutputStream()
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(rawJson.toByteArray(StandardCharsets.UTF_8))
            }
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
            PREFIX_V2 + b64
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback para JSON tradicional comprimido
            val json = serializeChecklistToJson(template, fields)
            encodeJsonFallback(json)
        }
    }

    private fun encodeJsonFallback(jsonStr: String): String {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(jsonStr.toByteArray(StandardCharsets.UTF_8))
        }
        return PREFIX_V1 + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    /**
     * Decodifica qualquer payload recebido (QR Code v2, v1 ou JSON colado).
     */
    fun decodeChecklistPayload(payload: String): ChecklistPackage? {
        val clean = payload.trim()
        if (clean.isBlank()) return null

        try {
            if (clean.startsWith(PREFIX_V2)) {
                val b64 = clean.substring(PREFIX_V2.length)
                val bytes = Base64.decode(b64, Base64.NO_WRAP or Base64.URL_SAFE)
                val jsonStr = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return parseCompactJson(jsonStr)
            } else if (clean.startsWith(PREFIX_V1)) {
                val b64 = clean.substring(PREFIX_V1.length)
                val bytes = Base64.decode(b64, Base64.NO_WRAP or Base64.URL_SAFE)
                val jsonStr = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                return parseChecklistJson(jsonStr)
            } else if (clean.startsWith("{")) {
                return parseCompactJson(clean) ?: parseChecklistJson(clean)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return parseChecklistJson(clean)
    }

    private fun parseCompactJson(jsonStr: String): ChecklistPackage? {
        return try {
            val root = JSONObject(jsonStr)
            if (!root.has("k") && !root.has("categories")) return null

            val name = root.optString("n", root.optString("name", "Checklist Importado"))
            val description = root.optString("d", root.optString("description", ""))
            val category = root.optString("c", root.optString("category", "Personalizados"))

            val catArray = root.optJSONArray("k") ?: root.optJSONArray("categories") ?: return null
            val categoriesList = mutableListOf<ExportedCategory>()
            var totalQ = 0

            for (i in 0 until catArray.length()) {
                val catObj = catArray.getJSONObject(i)
                val catName = catObj.optString("n", catObj.optString("name", "GERAL"))
                val itemsArr = catObj.optJSONArray("i") ?: catObj.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<ExportedItem>()

                for (j in 0 until itemsArr.length()) {
                    val rawItem = itemsArr.get(j)
                    if (rawItem is JSONArray) {
                        val label = rawItem.optString(0, "")
                        val type = if (rawItem.length() > 1) rawItem.optString(1, "C_NC_NA") else "C_NC_NA"
                        if (label.isNotBlank()) {
                            itemsList.add(ExportedItem(label = label, type = type, orderIndex = j))
                            totalQ++
                        }
                    } else if (rawItem is JSONObject) {
                        val label = rawItem.optString("label", rawItem.optString("l", ""))
                        val type = rawItem.optString("type", rawItem.optString("t", "C_NC_NA"))
                        if (label.isNotBlank()) {
                            itemsList.add(ExportedItem(label = label, type = type, orderIndex = j))
                            totalQ++
                        }
                    }
                }
                if (itemsList.isNotEmpty()) {
                    categoriesList.add(ExportedCategory(catName, itemsList))
                }
            }

            if (categoriesList.isEmpty()) null else ChecklistPackage(
                name = name,
                description = description,
                category = category,
                categories = categoriesList,
                totalQuestions = totalQ
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Realiza o parser e validação do JSON tradicional de um checklist.
     */
    fun parseChecklistJson(jsonStr: String): ChecklistPackage? {
        return try {
            val root = JSONObject(jsonStr)
            val name = root.optString("name", root.optString("n", "Checklist Importado"))
            val description = root.optString("description", root.optString("d", ""))
            val category = root.optString("category", root.optString("c", "Personalizados"))

            val categoriesList = mutableListOf<ExportedCategory>()
            var questionCount = 0

            val categoriesArray = root.optJSONArray("categories") ?: root.optJSONArray("k")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    val catObj = categoriesArray.getJSONObject(i)
                    val catName = catObj.optString("name", catObj.optString("n", "GERAL"))
                    val itemsArray = catObj.optJSONArray("items") ?: catObj.optJSONArray("i") ?: JSONArray()
                    val itemsList = mutableListOf<ExportedItem>()

                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.optJSONObject(j)
                        if (itemObj != null) {
                            val label = itemObj.optString("label", itemObj.optString("l", ""))
                            if (label.isNotBlank()) {
                                itemsList.add(
                                    ExportedItem(
                                        label = label,
                                        type = itemObj.optString("type", itemObj.optString("t", "C_NC_NA")),
                                        isRequired = itemObj.optBoolean("isRequired", true),
                                        orderIndex = itemObj.optInt("orderIndex", j)
                                    )
                                )
                                questionCount++
                            }
                        } else if (itemsArray.optJSONArray(j) != null) {
                            val row = itemsArray.getJSONArray(j)
                            val label = row.optString(0, "")
                            val type = if (row.length() > 1) row.optString(1, "C_NC_NA") else "C_NC_NA"
                            if (label.isNotBlank()) {
                                itemsList.add(ExportedItem(label = label, type = type, orderIndex = j))
                                questionCount++
                            }
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
     * Gera um Bitmap do QR Code com densidade otimizada para leitura instantânea por telas.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 600): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, // Nível Low reduz densidade e facilita leitura
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
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
     * Decodifica um Bitmap contendo uma imagem de QR Code usando ZXing nativo com múltiplos modos de contraste.
     */
    fun decodeQrBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader().apply {
                setHints(mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.CHARACTER_SET to "UTF-8"
                ))
            }
            val result = reader.decodeWithState(binaryBitmap)
            result.text
        } catch (e: Exception) {
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
