package com.relatopro.app.importer

import com.relatopro.app.importer.analyzer.ChecklistStructureAnalyzer
import com.relatopro.app.importer.model.*
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
    fun testMultiLevelHierarchyAnalysis() {
        val richParagraphs = listOf(
            RichParagraph("CHECKLIST DE INSPEÇÃO DE SEGURANÇA", style = ElementStyle(isHeadingStyle = true, headingLevel = 1)),
            RichParagraph("1. DOCUMENTAÇÃO", style = ElementStyle(isHeadingStyle = true, headingLevel = 1)),
            RichParagraph("1.1 Documentos obrigatórios", style = ElementStyle(isHeadingStyle = true, headingLevel = 2)),
            RichParagraph("1.1.1 A documentação está atualizada?"),
            RichParagraph("1.1.2 Os documentos estão disponíveis no local?"),
            RichParagraph("1.2 Registros e Treinamentos", style = ElementStyle(isHeadingStyle = true, headingLevel = 2)),
            RichParagraph("1.2.1 Os registros estão devidamente preenchidos?"),
            RichParagraph("2. EQUIPAMENTOS", style = ElementStyle(isHeadingStyle = true, headingLevel = 1)),
            RichParagraph("2.1 Máquinas", style = ElementStyle(isHeadingStyle = true, headingLevel = 2)),
            RichParagraph("2.1.1 As máquinas possuem proteção adequada?"),
            RichParagraph("2.1.2 As proteções estão em boas condições?")
        )

        val richDoc = RichDocumentContent(
            suggestedTitle = "CHECKLIST DE INSPEÇÃO DE SEGURANÇA",
            paragraphs = richParagraphs,
            rawTextLines = richParagraphs.map { it.text },
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.WORD, "Checklist_Seguranca.docx")

        assertEquals("CHECKLIST DE INSPEÇÃO DE SEGURANÇA", parsed.title)
        assertEquals(2, parsed.sections.size)
        assertEquals(5, parsed.totalItemsCount)

        // Section 1: DOCUMENTAÇÃO
        val sec1 = parsed.sections.find { it.name.contains("DOCUMENTAÇÃO") }
        assertNotNull(sec1)
        assertEquals(2, sec1!!.categories.size)
        assertTrue(sec1.categories.any { it.name.contains("Documentos obrigatórios") })
        assertTrue(sec1.categories.any { it.name.contains("Registros") })

        // Section 2: EQUIPAMENTOS
        val sec2 = parsed.sections.find { it.name.contains("EQUIPAMENTOS") }
        assertNotNull(sec2)
        assertEquals(1, sec2!!.categories.size)
        assertEquals(2, sec2.categories[0].items.size)
    }

    @Test
    fun testAdministrativeNoiseFiltering() {
        val richParagraphs = listOf(
            RichParagraph("EMPRESA EXEMPLO LTDA - CNPJ: 12.345.677/0001-90"),
            RichParagraph("Telefone: (11) 9999-8888 | www.empresaexemplo.com.br"),
            RichParagraph("CHECKLIST DE MANUTENÇÃO", style = ElementStyle(isHeadingStyle = true)),
            RichParagraph("1. ESTRUTURA FÍSICA", style = ElementStyle(isHeadingStyle = true)),
            RichParagraph("1.1 O piso está limpo e desobstruído?"),
            RichParagraph("Página 1 de 4"),
            RichParagraph("Assinatura do Responsável Técnico: ____________________"),
            RichParagraph("Data: 14/09/2026 Hora: 10:30")
        )

        val richDoc = RichDocumentContent(
            suggestedTitle = "CHECKLIST DE MANUTENÇÃO",
            paragraphs = richParagraphs,
            rawTextLines = richParagraphs.map { it.text },
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.PDF, "Checklist_Manutencao.pdf")

        assertEquals("CHECKLIST DE MANUTENÇÃO", parsed.title)
        // Only 1 real question, the CNPJ, Page 1 de 4, Signature, Date must NOT be questions
        assertEquals(1, parsed.totalItemsCount)
        assertEquals("O piso está limpo e desobstruído?", parsed.allItemsFlattened[0].label)
    }

    @Test
    fun testPreFilledAnswersAndConflictDetection() {
        val headers = listOf("Nº", "Item", "C", "NC", "NA", "Observações")
        val rows = listOf(
            RichRow(
                rowIndex = 0,
                cells = listOf(
                    RichCell("01", columnIndex = 0),
                    RichCell("Extintor identificado e pressurizado?", columnIndex = 1),
                    RichCell("X", columnIndex = 2), // Marked as C
                    RichCell("", columnIndex = 3),
                    RichCell("", columnIndex = 4),
                    RichCell("Dentro da validade", columnIndex = 5)
                )
            ),
            RichRow(
                rowIndex = 1,
                cells = listOf(
                    RichCell("02", columnIndex = 0),
                    RichCell("Alarme de emergência testado?", columnIndex = 1),
                    RichCell("X", columnIndex = 2), // Conflicting marking C
                    RichCell("X", columnIndex = 3), // and NC
                    RichCell("", columnIndex = 4),
                    RichCell("Dispositivo travado", columnIndex = 5)
                )
            )
        )

        val table = RichTable(
            title = "1. SISTEMAS DE EMERGÊNCIA",
            headers = headers,
            rows = rows
        )

        val richDoc = RichDocumentContent(
            suggestedTitle = "Checklist de Emergência",
            tables = listOf(table),
            rawTextLines = emptyList(),
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.EXCEL, "Planilha_Emergencia.xlsx")

        val items = parsed.allItemsFlattened
        assertEquals(2, items.size)

        // Item 1: Pre-filled answer C without conflict
        assertNotNull(items[0].preFilledAnswer)
        assertEquals("C", items[0].preFilledAnswer?.statusShortCode)
        assertFalse(items[0].preFilledAnswer!!.hasConflict)

        // Item 2: Conflict detected (C and NC both marked)
        assertNotNull(items[1].preFilledAnswer)
        assertTrue(items[1].preFilledAnswer!!.hasConflict)
        assertEquals(2, items[1].preFilledAnswer?.conflictingCodes?.size)
        assertTrue(parsed.validationAlerts.any { it.title.contains("Conflito") })
    }

    @Test
    fun testInspectionVerbsWithoutQuestionMark() {
        val richParagraphs = listOf(
            RichParagraph("1. PROCEDIMENTOS DE OPERAÇÃO", style = ElementStyle(isHeadingStyle = true)),
            RichParagraph("1.1 Verificar se os dispositivos de emergência estão operacionais"),
            RichParagraph("1.2 Conferir alinhamento das esteiras de transporte"),
            RichParagraph("1.3 Garantir que os operadores utilizem os EPIs adequados")
        )

        val richDoc = RichDocumentContent(
            suggestedTitle = "Checklist Operacional",
            paragraphs = richParagraphs,
            rawTextLines = richParagraphs.map { it.text },
            ocrUsed = false
        )

        val parsed = ChecklistStructureAnalyzer.analyzeRich(richDoc, ImportFormat.WORD, "Checklist_Operacao.docx")

        val items = parsed.allItemsFlattened
        assertEquals(3, items.size)
        assertTrue(items.all { it.confidenceRating == ConfidenceRating.HIGH || it.confidenceRating == ConfidenceRating.MEDIUM })
    }
}
