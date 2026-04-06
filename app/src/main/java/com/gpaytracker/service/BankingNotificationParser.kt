package com.gpaytracker.service

/**
 * Detects and parses deposit/credit notifications from Indian banking apps.
 *
 * Supported banks (package names):
 *   HDFC, SBI, ICICI, Axis, Kotak, YES, IndusInd, PNB, BOB, Canara,
 *   Federal, IDFC First, AU Small Finance, and generic UPI credit alerts.
 */
object BankingNotificationParser {

    val BANKING_PACKAGES = setOf(
        "com.snapwork.hdfc",                    // HDFC MobileBanking
        "com.htcinc.hdfcbank",
        "com.sbi.SBIFreedomPlus",              // SBI YONO
        "com.sbi.upi",
        "com.csam.icici.bank.imobile",         // ICICI iMobile
        "com.axis.mobile",                      // Axis Mobile
        "com.kotak.mahindra.kotak811",         // Kotak 811
        "com.kotak.mahindra.kotakbanking",
        "com.yesbank",                          // YES Bank
        "com.indusind.mobile",                  // IndusInd
        "com.pnb.mbanking",                    // PNB
        "com.baroda.mpassbook",                // Bank of Baroda
        "com.freecharge.android",              // Freecharge
        "com.idfcfirstbank.mobileapp",         // IDFC First
        "com.aubank.mobile",                   // AU Small Finance
        "com.fampay.in",                       // FamPay
        "in.jupiter.app",                      // Jupiter
        "com.niyo.global",                     // Niyo
        "com.fi.money",                        // Fi Money
        "com.google.android.apps.nbu.paisa.user", // GPay (also sends credit alerts)
        "com.phonepe.app",                     // PhonePe
        "net.one97.paytm"                      // Paytm
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "deposit",
        "added to your account", "money received", "transferred to your",
        "inward", "neft credit", "imps credit", "upi credit",
        "salary", "refund credited", "cashback"
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "paid", "payment", "withdrawn", "purchase",
        "spent", "charged", "sent"
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:INR|Rs\.?|₹)""", RegexOption.IGNORE_CASE),
        Regex("""credited.*?(?:INR|Rs\.?|₹)\s*([\d,]+)""", RegexOption.IGNORE_CASE)
    )

    private val SOURCE_PATTERNS = listOf(
        Regex("""(?:from|by|sender[:\s]+)\s*([A-Za-z0-9 .&'-]{2,30})""", RegexOption.IGNORE_CASE),
        Regex("""(?:salary|transfer)\s+from\s+([A-Za-z0-9 .&'-]{2,30})""", RegexOption.IGNORE_CASE),
        Regex("""([A-Za-z0-9 .&'-]{2,20})\s+(?:has sent|transferred|paid)""", RegexOption.IGNORE_CASE)
    )

    data class IncomeParseResult(
        val amount: Double,
        val source: String,
        val bankName: String
    )

    fun parse(packageName: String, title: String, body: String): IncomeParseResult? {
        val fullText = "$title $body"
        val lower = fullText.lowercase()

        // Must contain a credit keyword
        if (CREDIT_KEYWORDS.none { lower.contains(it) }) return null

        // Must NOT be purely a debit notification
        val debitScore = DEBIT_KEYWORDS.count { lower.contains(it) }
        val creditScore = CREDIT_KEYWORDS.count { lower.contains(it) }
        if (debitScore > creditScore) return null

        val amount = extractAmount(fullText) ?: return null
        if (amount <= 0) return null

        val source = extractSource(fullText) ?: inferSource(lower)
        val bank = bankNameFromPackage(packageName)

        return IncomeParseResult(
            amount = amount,
            source = source,
            bankName = bank
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val value = raw.toDoubleOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }

    private fun extractSource(text: String): String? {
        for (pattern in SOURCE_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val src = match.groupValues[1].trim()
            if (src.length > 2 && !src.lowercase().contains("account")) {
                return src.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
            }
        }
        return null
    }

    private fun inferSource(lower: String): String = when {
        lower.contains("salary")  -> "Salary"
        lower.contains("refund")  -> "Refund"
        lower.contains("cashback")-> "Cashback"
        lower.contains("neft")    -> "NEFT Transfer"
        lower.contains("imps")    -> "IMPS Transfer"
        lower.contains("upi")     -> "UPI Transfer"
        else -> "Bank Transfer"
    }

    private fun bankNameFromPackage(pkg: String): String = when {
        pkg.contains("hdfc")      -> "HDFC Bank"
        pkg.contains("sbi")       -> "SBI"
        pkg.contains("icici")     -> "ICICI Bank"
        pkg.contains("axis")      -> "Axis Bank"
        pkg.contains("kotak")     -> "Kotak Bank"
        pkg.contains("yesbank")   -> "YES Bank"
        pkg.contains("indusind")  -> "IndusInd Bank"
        pkg.contains("pnb")       -> "PNB"
        pkg.contains("baroda")    -> "Bank of Baroda"
        pkg.contains("idfc")      -> "IDFC First Bank"
        pkg.contains("aubank")    -> "AU Small Finance"
        pkg.contains("jupiter")   -> "Jupiter"
        pkg.contains("fi.money")  -> "Fi Money"
        pkg.contains("paisa")     -> "Google Pay"
        pkg.contains("phonepe")   -> "PhonePe"
        pkg.contains("paytm")     -> "Paytm"
        else -> "Bank"
    }
}
