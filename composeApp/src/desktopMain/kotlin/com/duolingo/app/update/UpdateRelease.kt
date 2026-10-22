package com.duolingo.app.update

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal const val UPDATE_REPO = "https://github.com/Dreamsigner/duolingo-kmp-learning-app"
internal const val LATEST_RELEASE = "https://api.github.com/repos/Dreamsigner/duolingo-kmp-learning-app/releases/latest"
internal const val MAX_INSTALLER_BYTES = 200L * 1024 * 1024

internal data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion) = compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)
    override fun toString() = "$major.$minor.$patch"
    companion object {
        fun parse(value: String): AppVersion? {
            if (!Regex("(?:0|[1-9][0-9]{0,3})\\.(?:0|[1-9][0-9]{0,3})\\.(?:0|[1-9][0-9]{0,3})").matches(value)) return null
            return value.split('.').map(String::toInt).let { AppVersion(it[0], it[1], it[2]) }
        }
    }
}

internal data class UpdateRelease(val version: AppVersion, val fileName: String, val installerUrl: String, val checksumUrl: String, val size: Long)
internal fun parseUpdateRelease(text: String, current: AppVersion): UpdateRelease? {
    val root = Json.parseToJsonElement(text).jsonObject
    require(root["draft"]?.jsonPrimitive?.booleanOrNull == false && root["prerelease"]?.jsonPrimitive?.booleanOrNull == false)
    val tag = root["tag_name"]!!.jsonPrimitive.content
    require(tag.startsWith("v"))
    val version = requireNotNull(AppVersion.parse(tag.drop(1)))
    if (version <= current) return null
    val name = "AtelierApprentissage-$version-Setup.exe"
    val assets = root["assets"]!!.jsonArray.map { it.jsonObject }
    fun asset(file: String) = assets.filter { it["name"]?.jsonPrimitive?.content == file }.single()
    fun url(asset: JsonObject, file: String): String {
        val value = asset["browser_download_url"]!!.jsonPrimitive.content
        require(value == "$UPDATE_REPO/releases/download/$tag/$file")
        require(asset["state"]?.jsonPrimitive?.content == "uploaded")
        return value
    }
    val exe = asset(name)
    val size = exe["size"]!!.jsonPrimitive.long
    require(size in 1..MAX_INSTALLER_BYTES)
    return UpdateRelease(version, name, url(exe, name), url(asset("SHA256SUMS.txt"), "SHA256SUMS.txt"), size)
}

internal fun installerChecksum(text: String, name: String): String {
    val matches = text.lineSequence().mapNotNull { Regex("([a-fA-F0-9]{64})  " + Regex.escape(name)).matchEntire(it.trim())?.groupValues?.get(1) }.toList()
    require(matches.size == 1) { "Missing or ambiguous checksum" }
    return matches.single().lowercase()
}

internal fun fileSha256(path: Path): String = Files.newInputStream(path).use { input ->
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(65536)
    while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
    digest.digest().joinToString("") { "%02x".format(it) }
}
