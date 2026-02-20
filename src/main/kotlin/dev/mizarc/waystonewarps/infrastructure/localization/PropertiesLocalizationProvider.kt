package dev.mizarc.waystonewarps.infrastructure.localization

import dev.mizarc.waystonewarps.application.services.ConfigService
import dev.mizarc.waystonewarps.application.services.PlayerLocaleService
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import java.io.File
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import java.util.UUID

class PropertiesLocalizationProvider(
    private val config: ConfigService,
    private val dataFolder: File,
    private val playerLocaleService: PlayerLocaleService
) : LocalizationProvider {
    private val languages: MutableMap<String, Properties> = mutableMapOf()
    private val baseDefaultLanguageCode = "en"

    init {
        loadLayeredProperties()
    }

    override fun get(playerId: UUID, key: String, vararg args: Any?): String {
        val clientLocale = normalizeLocaleCode(playerLocaleService.getLocale(playerId))
        return fetchMessageString(clientLocale, key, *args)
    }

    override fun getConsole(key: String, vararg args: Any?): String {
        val configuredLocale = normalizeLocaleCode(config.getPluginLanguage())
        return fetchMessageString(configuredLocale, key, *args)
    }

    private fun fetchMessageString(requestedLocale: String, key: String, vararg args: Any?): String {
        for (candidateLocale in buildFallbackLocales(requestedLocale)) {
            languages[candidateLocale]?.getProperty(key)?.let { pattern ->
                return formatPattern(pattern, key, *args)
            }

            // If this is a base language (e.g. "zh"), use an existing variant like "zh_cn" as fallback.
            if (!candidateLocale.contains("_")) {
                val sameLanguageVariant = findLanguageVariant(candidateLocale) ?: continue
                languages[sameLanguageVariant]?.getProperty(key)?.let { pattern ->
                    return formatPattern(pattern, key, *args)
                }
            }
        }

        return key
    }

    private fun buildFallbackLocales(requestedLocale: String): List<String> {
        val locales = linkedSetOf<String>()

        if (requestedLocale.isNotBlank()) {
            locales.add(requestedLocale)
            locales.add(requestedLocale.substringBefore('_'))
        }

        val configuredDefault = normalizeLocaleCode(config.getPluginLanguage())
        if (configuredDefault.isNotBlank()) {
            locales.add(configuredDefault)
            locales.add(configuredDefault.substringBefore('_'))
        }

        locales.add(baseDefaultLanguageCode)
        return locales.filter { it.isNotBlank() }
    }

    private fun formatPattern(pattern: String, key: String, vararg args: Any?): String {
        return try {
            if (args.isNotEmpty()) {
                MessageFormat.format(pattern, *args)
            } else {
                pattern
            }
        } catch (_: IllegalArgumentException) {
            println("Failed to format localization key '$key' with arguments: ${args.joinToString()}")
            pattern
        } catch (e: Exception) {
            println(
                "An unexpected error occurred while formatting localization with arguments: " +
                        "${args.joinToString()} - ${e.message}"
            )
            pattern
        }
    }

    private fun loadLayeredProperties() {
        val langFolder = File(dataFolder, "lang")
        val defaultsFolder = File(langFolder, "defaults")
        val overridesFolder = File(langFolder, "overrides")

        val availableLanguages = findAvailableLanguages(defaultsFolder, overridesFolder)

        availableLanguages.forEach { locale ->
            val properties = Properties()
            val baseLanguage = locale.substringBefore('_')

            // 1. Base language defaults, e.g. "en.properties"
            loadInto(File(defaultsFolder, "$baseLanguage.properties"), properties)
            // 2. Region-specific defaults, e.g. "en_us.properties"
            if (locale != baseLanguage) {
                loadInto(File(defaultsFolder, "$locale.properties"), properties)
            }

            // 3. Base language overrides
            loadInto(File(overridesFolder, "$baseLanguage.properties"), properties)
            // 4. Region-specific overrides
            if (locale != baseLanguage) {
                loadInto(File(overridesFolder, "$locale.properties"), properties)
            }

            languages[locale] = properties
        }
    }

    private fun loadInto(file: File, target: Properties) {
        if (!file.exists()) return
        try {
            file.reader(Charsets.UTF_8).use { target.load(it) }
        } catch (_: Exception) {
            println("Failed to load language file: ${file.path}")
        }
    }

    private fun findAvailableLanguages(defaultsFolder: File, overridesFolder: File): Set<String> {
        val codes = mutableSetOf<String>()

        defaultsFolder.listFiles { file -> file.isFile && file.extension == "properties" }?.forEach { file ->
            val code = normalizeLocaleCode(file.nameWithoutExtension)
            if (code.isNotBlank()) {
                codes.add(code)
            }
        }

        overridesFolder.listFiles { file -> file.isFile && file.extension == "properties" }?.forEach { file ->
            val code = normalizeLocaleCode(file.nameWithoutExtension)
            if (code.isNotBlank()) {
                codes.add(code)
            }
        }

        return codes
    }

    private fun normalizeLocaleCode(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.trim()
            .replace('-', '_')
            .lowercase(Locale.ROOT)
    }

    private fun findLanguageVariant(language: String): String? {
        if (language.isBlank()) return null
        return languages.keys.asSequence()
            .filter { it.startsWith("${language}_") }
            .sorted()
            .firstOrNull()
    }
}
