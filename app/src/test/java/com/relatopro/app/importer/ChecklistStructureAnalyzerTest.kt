package com.relatopro.app.importer

import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.ImportFormat
import com.relatopro.app.importer.parser.*
import org.junit.Assert.*
import org.junit.Test

class ChecklistStructureAnalyzerTest {

    @Test
    fun testTableChecklistAnalysis() {
        val headers = listOf("Nº", "Item / Pergunta de Verificação", "Conforme", "Não Conforme", "Observações")
        val rows = listOf(
            RawRow(listOf(
                RawCell("01"),
                RawCell("Os extintores de incêndio estão dentro do prazo de validade?"),
                RawCell(""),
                RawCell(""),
                RawCell("")
            )),
            RawRow(listOf(
                RawCell("02"),
                RawCell("As saídas de emergência estão desobstruídas e sinalizadas?"),
                RawCell(""),
                RawCell(""),
                RawCell("")
            )),
            RawRow(listOf(
                RawCell("03"),
                RawCell("Existe iluminação de emergência operando corretamente?"),
                RawCell(""),
                RawCell(""),
                RawCell("")
            ))
        )

        val rawDoc = RawDocumentContent(
            suggestedTitle = "Checklist de Combate a Incêndio",
            tables = listOf(RawTable(title = "1. SEGURANÇA CONTRA INCÊNDIO", headers = headers, rows = rows)),
            rawTextLines = emptyList(),
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.PDF, "Checklist_Incendio.pdf")

        assertEquals("Checklist de Combate a Incêndio", parsed.title)
        assertTrue(parsed.sections.isNotEmpty())
        assertEquals(3, parsed.totalItemsCount)
        assertEquals("Os extintores de incêndio estão dentro do prazo de validade?", parsed.sections[0].items[0].label)
        assertEquals("01", parsed.sections[0].items[0].numberPrefix)
    }

    @Test
    fun testTextChecklistWithSectionsAnalysis() {
        val paragraphs = listOf(
            RawParagraph("CHECKLIST DE INSPEÇÃO VEICULAR", isHeading = true),
            RawParagraph("1. DOCUMENTAÇÃO DO VEÍCULO", isHeading = true),
            RawParagraph("1.1 CRLV está em dia e disponível no veículo?"),
            RawParagraph("1.2 Seguro obrigatório e autorização de transporte válidos?"),
            RawParagraph("2. ITENS DE SEGURANÇA", isHeading = true),
            RawParagraph("2.1 Cintos de segurança em bom estado e operando?"),
            RawParagraph("2.2 Pneus em bom estado (sem desgaste excessivo)?")
        )

        val rawDoc = RawDocumentContent(
            suggestedTitle = "CHECKLIST DE INSPEÇÃO VEICULAR",
            paragraphs = paragraphs,
            rawTextLines = paragraphs.map { it.text },
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyze(rawDoc, ImportFormat.WORD, "Checklist_Veicular.docx")

        assertEquals("CHECKLIST DE INSPEÇÃO VEICULAR", parsed.title)
        assertEquals(2, parsed.sections.size)
        assertEquals(4, parsed.totalItemsCount)
        assertTrue(parsed.sections.any { it.name.contains("DOCUMENTAÇÃO") })
        assertTrue(parsed.sections.any { it.name.contains("SEGURANÇA") })
    }
}
