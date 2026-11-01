package com.dreamsigner.wizard.ui.studio

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.EnumSet
import java.util.UUID

internal data class ChatGptModel(val slug: String, val name: String)
internal class ChatGptPlanException(val safeMessage: String) : Exception()

@Serializable
internal data class ChatGptCredentials(
    val hostId: String,
    val clientId: String = "",
    val subject: String = "",
    val email: String = "",
    val idToken: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val scope: String = "",
    val expiresAt: Long = 0,
)

/** The ChatGPT plan is a separate OAuth/Responses route, never a Chat Completions API key. */
internal class ChatGptPlanClient(private val file: Path) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val random = SecureRandom()
    private val lock = Any()
    private var credentials: ChatGptCredentials? = readCredentials()
    private val corruptFile = Files.exists(file) && credentials == null

    val accountEmail: String? get() = synchronized(lock) { credentials?.email?.takeIf { it.isNotBlank() } }
    val connected: Boolean get() = synchronized(lock) { credentials?.let { it.accessToken.isNotBlank() && "chatgpt.tokens.use.direct" in it.scope.split(' ') } == true }
    val storageError: String? get() = if (corruptFile) "Le fichier de connexion ChatGPT est illisible. Il a été conservé." else null

    fun signIn(): String {
        check(!corruptFile) { "Le fichier de connexion ChatGPT est illisible." }
        val prior = synchronized(lock) { credentials }
        val hostId = prior?.hostId ?: "urn:uuid:${UUID.randomUUID()}".also {
            save(ChatGptCredentials(hostId = it))
        }
        val verifier = randomString(32)
        val state = randomString(32)
        val nonce = randomString(32)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )
        ServerSocket().use { listener ->
            listener.bind(InetSocketAddress("127.0.0.1", 0))
            listener.soTimeout = 180_000
            val redirect = "http://127.0.0.1:${listener.localPort}/auth/callback"
            val initial = prior?.clientId.isNullOrBlank()
            val clientId = if (initial) "dynamic_agent_client" else prior!!.clientId
            val params = linkedMapOf(
                "client_id" to clientId,
                "ext_agent_host_id" to hostId,
                "response_type" to "code",
                "redirect_uri" to redirect,
                "scope" to "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
                "resource" to RESOURCE,
                "state" to state,
                "nonce" to nonce,
                "code_challenge_method" to "S256",
                "code_challenge" to challenge,
            )
            if (initial) params["agent_name_hint"] = "Dreamsign Wizard"
            else {
                prior!!.idToken.takeIf { it.isNotBlank() }?.let { params["id_token_hint"] = it }
                prior.email.takeIf { it.isNotBlank() }?.let { params["login_hint"] = it }
            }
            check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                "Le navigateur système est indisponible."
            }
            Desktop.getDesktop().browse(URI("$AUTH_BASE/api/accounts/authorize?${form(params)}"))
            val callback = awaitCallback(listener, state)
            check(callback["state"] == state) { "La connexion a échoué : état de sécurité incorrect." }
            check(callback["error"] == null) { "La connexion ChatGPT a été annulée ou refusée." }
            val code = callback["code"]?.takeIf { it.isNotBlank() } ?: error("Code de connexion manquant.")
            val issuedClientId = if (initial) callback["client_id"]?.takeIf { it.isNotBlank() && it != "dynamic_agent_client" }
                ?: error("Enregistrement ChatGPT incomplet.")
            else prior!!.clientId.also { check(callback["client_id"] == null || callback["client_id"] == it) { "Compte ChatGPT inattendu." } }
            val token = postToken(mapOf(
                "grant_type" to "authorization_code", "client_id" to issuedClientId,
                "code" to code, "code_verifier" to verifier, "redirect_uri" to redirect,
                "resource" to RESOURCE,
            ))
            val idToken = token.string("id_token")
            val identity = verifyIdentity(idToken, issuedClientId, nonce)
            if (!initial) check(identity.first == prior!!.subject) { "Le compte ChatGPT ne correspond pas au compte enregistré." }
            val scope = token.string("scope")
            check("chatgpt.tokens.use.direct" in scope.split(' ')) { "L’utilisation du forfait ChatGPT n’a pas été autorisée." }
            val saved = ChatGptCredentials(
                hostId, issuedClientId, identity.first, identity.second, idToken,
                token.string("access_token"), token.string("refresh_token"), scope,
                Instant.now().epochSecond + token.long("expires_in"),
            )
            save(saved)
            return saved.email
        }
    }

    fun models(): List<ChatGptModel> {
        val response = request("$RESOURCE/models", "GET")
        val root = json.parseToJsonElement(response).jsonObject
        return root["models"]?.jsonArray.orEmpty().mapNotNull { entry ->
            val item = entry.jsonObject
            if (item["visibility"]?.jsonPrimitive?.contentOrNull != "list") return@mapNotNull null
            val slug = item["slug"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            ChatGptModel(slug, item["display_name"]?.jsonPrimitive?.contentOrNull ?: slug)
        }
    }

    fun respond(model: String, goal: String, history: List<StudioMessage>, acceptedPlan: StudioPlan? = null, reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC): String {
        val body = buildChatGptBody(model, goal, history, acceptedPlan, reasoningEffort)
        check(body.length <= MAX_TUTOR_BODY_CHARS) { "La séance dépasse la limite de contexte." }
        val stream = requestStream("$RESOURCE/responses", body, reasoningEffort)
        return parseChatGptStream(stream)
    }

    /** Clears the local token even if remote revocation cannot be confirmed. */
    fun disconnect(): Boolean {
        synchronized(lock) {
            val existing = credentials ?: return true
            val revoked = if (existing.refreshToken.isBlank()) true else runCatching {
                val discovery = requestPublicDocument("$AUTH_BASE/.well-known/openid-configuration")
                val endpoint = json.parseToJsonElement(discovery).jsonObject.getValue("revocation_endpoint").jsonPrimitive.content
                require(URI(endpoint).host == "auth.openai.com")
                val request = HttpRequest.newBuilder(URI(endpoint)).timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(mapOf(
                        "token" to existing.refreshToken, "token_type_hint" to "refresh_token", "client_id" to existing.clientId,
                    )))).build()
                http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200
            }.getOrDefault(false)
            save(existing.copy(idToken = "", accessToken = "", refreshToken = "", scope = "", expiresAt = 0))
            return revoked
        }
    }

    private fun request(url: String, method: String): String {
        val token = validAccessToken()
        val builder = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer $token")
        val response = http.send(builder.method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "Le service ChatGPT a refusé la requête (${response.statusCode()})." }
        return response.body()
    }

    private fun requestStream(url: String, body: String, effort: ReasoningEffort): java.io.InputStream {
        val token = validAccessToken()
        val request = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            response.body().close()
            val message = when (response.statusCode()) {
                400 -> if (effort != ReasoningEffort.AUTOMATIC) "Requête ChatGPT refusée (400). Ce modèle peut ne pas prendre en charge ce niveau de réflexion ; choisis Automatique ou un niveau pris en charge." else "Requête ChatGPT refusée (400). Vérifie le modèle et la configuration."
                401, 403 -> "La connexion ChatGPT a expiré ou n’autorise pas ce modèle. Reconnecte le compte."
                429 -> "La limite d’usage ChatGPT est atteinte. Vérifie ton forfait et la limite de cette application."
                else -> "Le service ChatGPT a refusé la requête (${response.statusCode()})."
            }
            throw ChatGptPlanException(message)
        }
        return response.body()
    }

    private fun validAccessToken(): String = synchronized(lock) {
        val current = credentials ?: error("Connecte ton compte ChatGPT.")
        check(current.accessToken.isNotBlank()) { "Connecte ton compte ChatGPT." }
        if (Instant.now().epochSecond + 60 < current.expiresAt) return@synchronized current.accessToken
        check(current.refreshToken.isNotBlank()) { "Reconnecte ton compte ChatGPT." }
        val token = postToken(mapOf(
            "grant_type" to "refresh_token", "client_id" to current.clientId,
            "refresh_token" to current.refreshToken, "resource" to RESOURCE,
        ))
        val updated = current.copy(
            accessToken = token.string("access_token"),
            refreshToken = token["refresh_token"]?.jsonPrimitive?.contentOrNull ?: current.refreshToken,
            scope = token["scope"]?.jsonPrimitive?.contentOrNull ?: current.scope,
            expiresAt = Instant.now().epochSecond + token.long("expires_in"),
        )
        save(updated)
        updated.accessToken
    }

    private fun postToken(fields: Map<String, String>): JsonObject {
        val request = HttpRequest.newBuilder(URI("$AUTH_BASE/api/accounts/oauth/token"))
            .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form(fields))).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "La connexion ChatGPT a échoué (${response.statusCode()})." }
        return json.parseToJsonElement(response.body()).jsonObject
    }

    private fun verifyIdentity(idToken: String, clientId: String, nonce: String): Pair<String, String> {
        val jwt = SignedJWT.parse(idToken)
        check(jwt.header.algorithm == JWSAlgorithm.RS256) { "Signature ChatGPT non reconnue." }
        val key = JWKSet.parse(requestPublicKeys()).getKeyByKeyId(jwt.header.keyID) as? RSAKey
            ?: error("Clé de signature ChatGPT introuvable.")
        check(jwt.verify(RSASSAVerifier(key))) { "Signature ChatGPT invalide." }
        val claims = jwt.jwtClaimsSet
        check(claims.issuer == AUTH_BASE && clientId in claims.audience) { "Identité ChatGPT inattendue." }
        check(claims.expirationTime?.toInstant()?.isAfter(Instant.now().minusSeconds(5)) == true) { "Identité ChatGPT expirée." }
        check(claims.getStringClaim("nonce") == nonce) { "Identité ChatGPT non liée à cette connexion." }
        val subject = claims.subject?.takeIf { it.isNotBlank() } ?: error("Identité ChatGPT absente.")
        return subject to (claims.getStringClaim("email") ?: "Compte ChatGPT")
    }

    private fun requestPublicKeys(): String {
        return requestPublicDocument("$AUTH_BASE/.well-known/jwks.json")
    }

    private fun requestPublicDocument(url: String): String {
        val response = http.send(HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "Impossible d’obtenir les métadonnées ChatGPT." }
        return response.body()
    }

    private fun awaitCallback(listener: ServerSocket, expectedState: String): Map<String, String> {
        val deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos()
        while (System.nanoTime() < deadline) {
            val socket = listener.accept()
            try {
                socket.soTimeout = 5_000
                val reader = socket.getInputStream().bufferedReader()
                val requestLine = reader.readLine() ?: continue
                while (reader.readLine()?.isNotBlank() == true) Unit
                val target = requestLine.split(' ').getOrNull(1).orEmpty()
                if (!target.startsWith("/auth/callback?")) continue
                val fields = target.substringAfter('?').split('&').mapNotNull { part ->
                    val separator = part.indexOf('=')
                    if (separator < 0) null else decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
                }.toMap()
                val accepted = fields["state"] == expectedState
                val response = if (accepted) "<html><body>Revenez à Dreamsign Wizard pour terminer la connexion.</body></html>"
                    else "<html><body>Connexion refusée. Revenez à Dreamsign Wizard.</body></html>"
                val bytes = response.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().write(("HTTP/1.1 ${if (accepted) "200 OK" else "400 Bad Request"}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                socket.getOutputStream().write(bytes)
                return fields
            } finally { socket.close() }
        }
        error("La connexion ChatGPT a expiré.")
    }

    private fun readCredentials(): ChatGptCredentials? = try {
        if (Files.exists(file)) json.decodeFromString<ChatGptCredentials>(Files.readString(file)) else null
    } catch (_: Exception) { null }

    private fun save(value: ChatGptCredentials) = synchronized(lock) {
        Files.createDirectories(file.parent)
        val temp = Files.createTempFile(file.parent, "chatgpt-plan-", ".tmp")
        try {
            restrictToOwner(temp)
            Files.writeString(temp, json.encodeToString(value))
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            credentials = value
        } finally { Files.deleteIfExists(temp) }
    }

    private fun restrictToOwner(path: Path) {
        val view = Files.getFileAttributeView(path, AclFileAttributeView::class.java)
            ?: error("La protection des identifiants n’est pas disponible.")
        val owner = Files.getOwner(path)
        val entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
            .setPermissions(EnumSet.allOf(AclEntryPermission::class.java)).build()
        view.acl = listOf(entry)
    }

    private fun randomString(size: Int): String = ByteArray(size).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    private fun form(fields: Map<String, String>): String = fields.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, Charsets.UTF_8)}=${URLEncoder.encode(it.value, Charsets.UTF_8)}"
    }

    private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8)

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
    private fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.long

    companion object {
        private const val AUTH_BASE = "https://auth.openai.com"
        private const val RESOURCE = "https://api.openai.com/v1"
    }
}

internal fun buildChatGptBody(model: String, goal: String, history: List<StudioMessage>, acceptedPlan: StudioPlan? = null, reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC): String {
    val input = buildJsonArray {
        history.takeLast(12).forEach { turn ->
            add(buildJsonObject {
                put("role", if (turn.role == "assistant") "assistant" else "user")
                put("content", studioMessageForTutor(turn))
            })
        }
    }
    return buildJsonObject {
        put("model", model)
        reasoningEffort.wireValue?.let { effort -> put("reasoning", buildJsonObject { put("effort", effort) }) }
        put("instructions", "$TUTOR_SYSTEM_PROMPT${acceptedPlanContext(acceptedPlan)}\nObjectif d’apprentissage : $goal")
        put("input", input)
        put("store", false)
        put("stream", true)
    }.toString()
}

internal fun parseChatGptStream(stream: java.io.InputStream): String = stream.use { source ->
    val answer = StringBuilder()
    var completed = false
    source.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            if (!line.startsWith("data: ")) return@forEach
            val data = line.removePrefix("data: ")
            if (data == "[DONE]") return@forEach
            val event = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return@forEach
            when (event["type"]?.jsonPrimitive?.contentOrNull) {
                "response.output_text.delta" -> answer.append(event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty())
                "response.completed" -> completed = true
                "response.failed" -> {
                    val code = (((event["response"] as? JsonObject)?.get("error") as? JsonObject)?.get("code") as? JsonPrimitive)?.contentOrNull
                    throw ChatGptPlanException(if (code == "subscription_sharing_usage_limit_exceeded")
                        "La limite d’usage ChatGPT est atteinte. Vérifie ton forfait et la limite de cette application."
                        else "La réponse ChatGPT a échoué. Réessaie ou vérifie ta connexion.")
                }
                "response.incomplete", "error" -> throw ChatGptPlanException("La réponse ChatGPT a été interrompue. Réessaie.")
            }
            check(answer.length <= 20_000) { "Réponse trop longue." }
        }
    }
    check(completed && answer.isNotBlank()) { "La réponse ChatGPT est incomplète." }
    answer.toString()
}
