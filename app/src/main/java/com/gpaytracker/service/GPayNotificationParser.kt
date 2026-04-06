package com.gpaytracker.service

import com.gpaytracker.data.Expense
import com.gpaytracker.data.ExpenseCategory

/**
 * Parses Google Pay notification text to extract transaction details.
 *
 * Google Pay notifications follow several patterns, e.g.:
 *  - "₹349 paid to Swiggy"
 *  - "You paid ₹1,299 to Amazon"
 *  - "Paid ₹187 to Uber via UPI"
 *  - "₹876 sent to bigbasket@okaxis"
 *  - "Payment of ₹649 to Netflix successful"
 */
object GPayNotificationParser {

    // All known GPay package identifiers
    val GPAY_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user",  // Google Pay India
        "com.google.android.apps.walletnfcrel"      // Google Pay global
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("""₹([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""Rs\.?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""INR\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""paid to (.+?)(?:\s+via|\s+successfully|\s+using|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""payment (?:of .+? )?to (.+?)(?:\s+successful|\s+via|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""sent to ([a-z0-9.\-_@]+)""", RegexOption.IGNORE_CASE),
        Regex("""to (.+?)(?:\s+is|\s+was|\s+via|\s*$)""", RegexOption.IGNORE_CASE)
    )

    data class ParseResult(
        val amount: Double,
        val merchant: String,
        val category: ExpenseCategory,
        val upiId: String = ""
    )

    fun parse(title: String, body: String): ParseResult? {
        val fullText = "$title $body"

        // Must look like a payment notification
        if (!looksLikePayment(fullText)) return null

        val amount = extractAmount(fullText) ?: return null
        val merchant = extractMerchant(fullText) ?: return null
        val upiId = extractUpiId(fullText) ?: ""
        val category = categorize(merchant, upiId)

        return ParseResult(
            amount = amount,
            merchant = cleanMerchant(merchant),
            category = category,
            upiId = upiId
        )
    }

    private fun looksLikePayment(text: String): Boolean {
        val keywords = listOf("paid", "payment", "sent", "debited", "₹", "rs.", "inr")
        val lowerText = text.lowercase()
        return keywords.any { lowerText.contains(it) }
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        // First try UPI ID (e.g. swiggy@okaxis → "Swiggy")
        val upiId = extractUpiId(text)
        if (upiId != null) {
            val handle = upiId.substringBefore("@").trim()
            if (handle.length > 2) return handle
        }
        // Then try named merchant patterns
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val merchant = match.groupValues[1].trim()
            if (merchant.length > 1) return merchant
        }
        return null
    }

    private fun extractUpiId(text: String): String? {
        val upiPattern = Regex("""[\w.\-]+@[\w]+""")
        return upiPattern.find(text)?.value
    }

    private fun cleanMerchant(raw: String): String {
        return raw
            .replace(Regex("@[\\w]+$"), "")   // strip UPI suffix
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
    }

    /**
     * Rule-based merchant → category mapping.
     * Falls back to OTHER for unknown merchants.
     */
    fun categorize(merchant: String, upiId: String = ""): ExpenseCategory {
        val key = "$merchant $upiId".lowercase()

        return when {
            key.containsAny("swiggy", "zomato", "dominos", "mcdonalds", "kfc", "burger",
                "pizza", "restaurant", "cafe", "dunzo", "blinkit") -> ExpenseCategory.FOOD

            key.containsAny("bigbasket", "grofers", "zepto", "milkbasket", "grocery",
                "supermart", "dmart", "reliance fresh") -> ExpenseCategory.GROCERIES

            key.containsAny("uber", "ola", "rapido", "blablacar", "yulu", "bounce",
                "metro", "irctc", "redbus", "makemytrip", "ixigo") -> ExpenseCategory.TRANSPORT

            key.containsAny("amazon", "flipkart", "myntra", "meesho", "snapdeal",
                "ajio", "nykaa", "shoppers", "mall", "store") -> ExpenseCategory.SHOPPING

            key.containsAny("netflix", "spotify", "hotstar", "primevideo", "youtube",
                "zee5", "sony", "bookmyshow", "pvr", "inox", "game") -> ExpenseCategory.ENTERTAINMENT

            key.containsAny("pharmacy", "pharmeasy", "1mg", "apollo", "netmeds",
                "medlife", "doctor", "clinic", "hospital", "lab", "diagnostic") -> ExpenseCategory.HEALTH

            key.containsAny("electricity", "water", "gas", "jio", "airtel", "bsnl",
                "internet", "broadband", "dth", "tata sky", "d2h") -> ExpenseCategory.UTILITIES

            else -> ExpenseCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
