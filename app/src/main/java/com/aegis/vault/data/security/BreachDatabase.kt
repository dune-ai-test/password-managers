package com.aegis.vault.data.security

/**
 * The bundled offline breach database. Aegis never queries the network: "breached"
 * logins are matched by domain against this list of well-known public breaches
 * (curated from public breach catalogs). It is deliberately small — the screen
 * exists to make offline usage honest.
 */
object BreachDatabase {

    /** Domain (lowercase, no www) → breach label. */
    private val entries: Map<String, String> = mapOf(
        "linkedin.com" to "2012 breach",
        "dropbox.com" to "2012 breach",
        "adobe.com" to "2013 breach",
        "tumblr.com" to "2013 breach",
        "myspace.com" to "2008 breach",
        "canva.com" to "2019 breach",
        "quora.com" to "2018 breach",
        "dubsmash.com" to "2019 breach",
    )

    private val worstKnown = listOf("2016 breach", "2021 breach")

    fun breachLabelFor(domain: String): String? {
        val clean = domain.lowercase().removePrefix("www.")
        return entries[clean]
    }

    val totalDomains: Int get() = entries.size

    /** True when the domain is known to have appeared in a breach catalog. */
    fun isBreached(domain: String): Boolean = breachLabelFor(domain) != null

    internal fun allDomains(): List<String> = entries.keys.toList()

    internal fun allLabels(): List<String> = entries.values + worstKnown
}
