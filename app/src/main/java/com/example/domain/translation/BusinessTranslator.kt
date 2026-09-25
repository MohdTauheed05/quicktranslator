package com.example.domain.translation

import java.util.regex.Pattern

data class ProtectedCommercialText(
    val maskedText: String,
    val tokenMap: Map<String, String>,
    val extractedEntities: List<String>
)

object BusinessTranslator {

    private val COMMERCIAL_PATTERNS = listOf(
        // URLs and Emails
        Pattern.compile("""https?://\S+|www\.\S+|\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""),

        // Incoterms (e.g. FOB Ningbo, CIF Dubai, EXW Shenzhen, DDP Riyadh)
        Pattern.compile("""(?i)\b(?:FOB|CIF|EXW|CFR|DDP|DAP|CIP|CPT|FAS|FCA)\s+[A-Za-z0-9\-_]+(?:\s+[A-Za-z0-9\-_]+)?"""),

        // Prices with Currency (e.g. USD 0.075/PC, $500, 120.50 EUR, 500 SAR/KG)
        Pattern.compile("""(?i)(?:USD|EUR|GBP|SAR|AED|INR|CNY|RMB|NPR|CAD|AUD|\$|€|£|¥|₹|﷼)\s*\d+(?:[.,]\d+)?(?:\s*/\s*(?:PC|PCS|KG|SET|SETS|UNIT|UNITS|M|PIECE|PIECES|CARTON|CTN))?"""),
        Pattern.compile("""(?i)\b\d+(?:[.,]\d+)?\s*(?:USD|EUR|GBP|SAR|AED|INR|CNY|RMB|NPR|\$|€|£|¥|₹|﷼)(?:\s*/\s*(?:PC|PCS|KG|SET|SETS|UNIT|UNITS|M|PIECE|PIECES|CARTON|CTN))?"""),

        // MOQ and Quantities (e.g. MOQ 500 PCS, MOQ: 1000 UNITS, 5000 PCS)
        Pattern.compile("""(?i)\b(?:MOQ|QTY|QUANTITY|ORDER)\s*:?\s*\d+(?:[.,]\d+)?(?:\s*(?:PCS|PC|KG|SETS?|UNITS?|CTNS?|CARTONS?|BOXES?|PAIRS?))?\b"""),
        Pattern.compile("""(?i)\b\d+(?:[.,]\d+)?\s*(?:PCS|PC|KG|SETS?|UNITS?|CTNS?|CARTONS?|BOXES?|PAIRS?)\b"""),

        // Commercial References (PO #12345, SKU-9821, Invoice #INV-2026-01, Part No. X-500)
        Pattern.compile("""(?i)\b(?:SKU|PO|P\.O\.|INV|INVOICE|PART|MODEL|REF|ITEM|NO\.)\s*#?:?\s*[A-Z0-9\-_]{2,20}\b"""),

        // Percentages (e.g. 30%, 15.5%)
        Pattern.compile("""\b\d+(?:[.,]\d+)?\s*%"""),

        // Dates (e.g. 2026-10-15, 15/10/2026, 15.10.2026)
        Pattern.compile("""\b\d{1,4}[/\-.]\d{1,2}[/\-.]\d{1,4}\b""")
    )

    fun protectTokens(text: String): ProtectedCommercialText {
        var currentText = text
        val tokenMap = mutableMapOf<String, String>()
        val extractedEntities = mutableListOf<String>()
        var counter = 0

        for (pattern in COMMERCIAL_PATTERNS) {
            val matcher = pattern.matcher(currentText)
            val matches = mutableListOf<String>()
            while (matcher.find()) {
                val match = matcher.group()
                if (!matches.contains(match) && !match.startsWith("___COMM_")) {
                    matches.add(match)
                }
            }

            // Replace longer matches first to avoid partial conflicts
            matches.sortedByDescending { it.length }.forEach { entity ->
                val placeholder = "___COMM_${counter}___"
                tokenMap[placeholder] = entity
                extractedEntities.add(entity)
                currentText = currentText.replace(entity, placeholder)
                counter++
            }
        }

        return ProtectedCommercialText(
            maskedText = currentText,
            tokenMap = tokenMap,
            extractedEntities = extractedEntities
        )
    }

    fun restoreTokens(text: String, tokenMap: Map<String, String>): String {
        var restored = text
        for ((token, originalValue) in tokenMap) {
            restored = restored.replace(token, originalValue)
            // In case a translation engine altered underscores or spaces:
            restored = restored.replace(token.replace("_", " "), originalValue)
            restored = restored.replace(token.replace("___", "__"), originalValue)
        }
        return restored
    }
}
