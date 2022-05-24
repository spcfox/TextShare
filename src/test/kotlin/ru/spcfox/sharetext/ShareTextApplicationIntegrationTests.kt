package ru.spcfox.sharetext

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import net.bytebuddy.utility.dispatcher.JavaDispatcher.Container
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.lifecycle.Startables
import ru.spcfox.sharetext.configuration.properties.JwtProperties
import ru.spcfox.sharetext.sharetext.model.TextExposure
import ru.spcfox.sharetext.sharetext.model.TextResponseEntity
import ru.spcfox.sharetext.sharetext.model.UserResponseEntity
import java.util.*
import java.util.stream.Stream

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@KotlinParameterizedTest
@TestPropertySource(properties = [
    "JWT_SECRET=your-secret-key-for-hs256-at-least-256-bits",
    "HASHIDS_SALT=salt-for-hash-ids",
])
class ShareTextApplicationIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    @Autowired
    private lateinit var jwtParser: JwtParser
    @Autowired
    private lateinit var jwtProperties: JwtProperties

    companion object {
        @get:Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:11").apply {
            withDatabaseName("test-db")
            withUsername("user")
            withPassword("pass")
        }
        init {
            Startables.deepStart(Stream.of(postgres)).join()
            System.setProperty("JDBC_URL", postgres.jdbcUrl)
            System.setProperty("JDBC_USER", postgres.username)
            System.setProperty("JDBC_PASS", postgres.password)
        }
    }

    @Test
    fun `создание аккаунта`() {
        val name = "John"

        val token: String = createAccount(name).readResult()
        val accountInfo: UserResponseEntity = accountInfo(token).readResult()

        assertAll(
            { assertEquals(name, accountInfo.name) }
        )
    }

    @Test
    fun `изменение аккаунта`() {
        val name1 = "John"
        val name2 = "James"

        val token: String = createAccount(name1).readResult()
        val accountInfo1: UserResponseEntity = accountInfo(token).readResult()
        val accountInfo2: UserResponseEntity = editAccount(token, name2).readResult()
        val accountInfo3: UserResponseEntity = accountInfo(token).readResult()

        assertAll(
            { assertEquals(name1, accountInfo1.name) },
            { assertEquals(name2, accountInfo2.name) },
            { assertEquals(name2, accountInfo3.name) },
            { assertEquals(accountInfo1.userId, accountInfo2.userId) },
            { assertEquals(accountInfo2.userId, accountInfo3.userId) },
        )
    }

    @Test
    fun `смена токена`() {
        val name = "John"

        val token1: String = createAccount(name).readResult()
        val accountInfo1: UserResponseEntity = accountInfo(token1).readResult()
        val token2: String = revokeToken(token1).readResult()
        val error = accountInfo(token1).readError()
        val accountInfo2: UserResponseEntity = accountInfo(token2).readResult()

        assertAll(
            { assertEquals(error, INVALID_TOKEN) },
            { assertNotEquals(token1, token2) },
            { assertEquals(accountInfo1.name, accountInfo2.name) },
            { assertEquals(accountInfo1.userId, accountInfo2.userId) }
        )
    }

    @Test
    fun `неверный токен`() {
        val name = "John"
        val invalidToken = "fvsd7un4v9mfj973m7r3f4sef834"

        val error1 = accountInfo(invalidToken).readError()
        val error2 = editAccount(invalidToken, name).readError()
        val error3 = revokeToken(invalidToken).readError()

        assertAll(
            { assertEquals(INVALID_TOKEN, error1) },
            { assertEquals(INVALID_TOKEN, error2) },
            { assertEquals(INVALID_TOKEN, error3) }
        )
    }

    @Test
    fun `неверно подписанный токен`() {
        val name1 = "John"
        val name2 = "James"

        val token: String = createAccount(name1).readResult()
        val body = jwtParser.parseClaimsJws(token).body
        val userId = body.subject.toLong()
        val salt = body["slt"]
        val invalidToken = Jwts.builder()
            .setClaims(mapOf(
                "sub" to userId,
                "slt" to salt))
            .signWith(Keys.hmacShaKeyFor("invalid-secret-for-jwt-tokens-at-least-256-bits".toByteArray()), jwtProperties.algorithm)
            .compact()

        val error1 = accountInfo(invalidToken).readError()
        val error2 = editAccount(invalidToken, name2).readError()
        val error3 = revokeToken(invalidToken).readError()

        assertAll(
            { assertEquals(INVALID_TOKEN, error1) },
            { assertEquals(INVALID_TOKEN, error2) },
            { assertEquals(INVALID_TOKEN, error3) }
        )
    }

    @ParameterizedTest
    @ValueSource(ints = [10])
    fun `создание и изменение 10 аккаунтов`(times: Int) {
        val name1 = "John"
        val name2 = "James"

        val tokens = mutableListOf<String>()
        val infoBefore = mutableListOf<UserResponseEntity>()
        val infoAfter = mutableListOf<UserResponseEntity>()

        for (i in 0 until times) {
            tokens.add(createAccount("$name1$i").readResult())
        }
        for (i in 0 until times) {
            infoBefore.add(accountInfo(tokens[i]).readResult())
        }
        for (i in 0 until times) {
            infoAfter.add(editAccount(tokens[i], "$name2$i").readResult())
        }

        assertAll(
            { assertEquals(times, tokens.distinct().size) },
            { assertEquals(times, infoBefore.map { it.userId }.distinct().size) },
            { assertEquals(times, infoAfter.map { it.userId }.distinct().size) },
            { assertTrue(infoBefore.zip(infoAfter).all { it.first.userId == it.second.userId }) },
            { assertTrue(infoBefore.withIndex().all { it.value.name == "$name1${it.index}" }) },
            { assertTrue(infoAfter.withIndex().all { it.value.name == "$name2${it.index}" }) },
        )
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    fun `создание аккаунта с некорректными именем`(invalidName: String) {
        val error: String = createAccount(invalidName).readError()

        assertEquals(error, INVALID_NAME)
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    fun `изменение имени на некорректное`(invalidName: String) {
        val name = "John"

        val token: String = createAccount(name).readResult()
        val error = editAccount(token, invalidName).readError()

        assertEquals(error, INVALID_NAME)
    }

    @ParameterizedTest
    @MethodSource("notPrivateExposures")
    fun `создание текста`(exposure: TextExposure?) {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId: String = createText(token, title, body, exposure).readResult()
        val text: TextResponseEntity = getText(textId).readResult()

        assertAll(
            { assertEquals(textId, text.textId) },
            { assertEquals(title, text.title) },
            { assertEquals(body, text.body) },
            { assertEquals(name, text.author) },
            { assertFalse(text.canEdit) },
            { assertEquals(text.editedAt, text.createdAt) }
        )
    }

    @ParameterizedTest
    @MethodSource("exposures")
    fun `получения текста с токеном`(exposure: TextExposure?) {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId: String = createText(token, title, body, exposure).readResult()
        val text: TextResponseEntity = getText(textId, token).readResult()

        assertAll(
            { assertEquals(textId, text.textId) },
            { assertEquals(title, text.title) },
            { assertEquals(body, text.body) },
            { assertEquals(name, text.author) },
            { assertTrue(text.canEdit) },
            { assertEquals(exposure ?: TextExposure.PUBLIC, text.exposure) },
            { assertEquals(text.editedAt, text.createdAt) }
        )
    }

    @Test
    fun `получения приватного текста без токена`() {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"
        val exposure = TextExposure.PRIVATE

        val token: String = createAccount(name).readResult()
        val textId: String = createText(token, title, body, exposure).readResult()
        val error = getText(textId).readError()

        assertEquals(PERMISSION_DENIED, error)
    }

    @ParameterizedTest
    @MethodSource("notPrivateExposures")
    fun `получения текста с другим токеном токеном`(exposure: TextExposure?) {
        val name1 = "John"
        val name2 = "James"
        val title = "Good title"
        val body = "Very interesting text"

        val token1: String = createAccount(name1).readResult()
        val token2: String = createAccount(name2).readResult()
        val textId: String = createText(token1, title, body, exposure).readResult()
        val text: TextResponseEntity = getText(textId, token2).readResult()

        assertAll(
            { assertEquals(textId, text.textId) },
            { assertEquals(title, text.title) },
            { assertEquals(body, text.body) },
            { assertEquals(name1, text.author) },
            { assertFalse(text.canEdit) },
            { assertEquals(exposure ?: TextExposure.PUBLIC, text.exposure) },
        )
    }

    @ParameterizedTest
    @MethodSource("twoExposures")
    fun `редактирование текста`(exposure1: TextExposure?, exposure2: TextExposure?) {
        val name = "John"
        val title1 = "Good title"
        val body1 = "Very interesting text"
        val title2 = "New beautiful title"
        val body2 = "Not interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title1, body1, exposure1).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1, title2, body2, exposure2).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(title1, text1.title) },
            { assertEquals(title2, text2.title) },
            { assertEquals(body1, text1.body) },
            { assertEquals(body2, text2.body) },
            { assertEquals(name, text1.author) },
            { assertEquals(name, text2.author) },
            { assertEquals(exposure1 ?: TextExposure.PUBLIC, text1.exposure) },
            { assertEquals(exposure2 ?: text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertTrue(text2.editedAt!! > text2.createdAt!!) }
        )
    }

    @ParameterizedTest
    @MethodSource("twoExposures")
    fun `изменение только прав доступа`(exposure1: TextExposure?, exposure2: TextExposure?) {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title, body, exposure1).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1, exposure = exposure2).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(text1.title, text2.title) },
            { assertEquals(text1.body, text2.body) },
            { assertEquals(text1.author, text2.author) },
            { assertEquals(exposure1 ?: TextExposure.PUBLIC, text1.exposure) },
            { assertEquals(exposure2 ?: text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertEquals(text1.exposure != text2.exposure, text2.editedAt!! > text1.createdAt!!) }
        )
    }

    @Test
    fun `изменение только заголовка`() {
        val name = "John"
        val title1 = "Good title"
        val body = "Very interesting text"
        val title2 = "New beautiful title"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title1, body).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1, title = title2).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(title1, text1.title) },
            { assertEquals(title2, text2.title) },
            { assertEquals(text1.body, text2.body) },
            { assertEquals(text1.author, text2.author) },
            { assertEquals(text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertTrue(text2.editedAt!! > text2.createdAt!!) }
        )
    }

    @Test
    fun `изменение только текста`() {
        val name = "John"
        val title = "Good title"
        val body1 = "Very interesting text"
        val body2 = "Not interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title, body1).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1, body = body2).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(text1.title, text2.title) },
            { assertEquals(body1, text1.body) },
            { assertEquals(body2, text2.body) },
            { assertEquals(text1.author, text2.author) },
            { assertEquals(text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertTrue(text2.editedAt!! > text2.createdAt!!) }
        )
    }

    @Test
    fun `edit с исходными параметрами`() {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title, body).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1, body = text1.body, title = text1.title, exposure = text1.exposure).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(text1.title, text2.title) },
            { assertEquals(text1.body, text2.body) },
            { assertEquals(text1.author, text2.author) },
            { assertEquals(text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.editedAt, text2.editedAt) },
        )
    }

    @Test
    fun `edit без аргументов`() {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title, body).readResult()
        val text1: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = editText(token, textId1).readResult()
        val text2: TextResponseEntity = getText(textId2, token).readResult()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(textId1, text1.textId) },
            { assertEquals(textId2, text2.textId) },
            { assertEquals(text1.title, text2.title) },
            { assertEquals(text1.body, text2.body) },
            { assertEquals(text1.author, text2.author) },
            { assertEquals(text1.exposure, text2.exposure) },
            { assertEquals(text1.createdAt, text1.editedAt) },
            { assertEquals(text1.createdAt, text2.createdAt) },
            { assertEquals(text1.editedAt, text2.editedAt) },
        )
    }

    @Test
    fun `удаление текста`() {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId1: String = createText(token, title, body).readResult()
        val text: TextResponseEntity = getText(textId1, token).readResult()
        val textId2: String = deleteText(token, textId1).readResult()
        val error1 = getText(textId1, token).readError()
        val error2 = editText(textId1, token, exposure = TextExposure.PRIVATE).readError()
        val error3 = deleteText(textId1, token).readError()

        assertAll(
            { assertEquals(textId1, textId2) },
            { assertEquals(TEXT_NOT_FOUND, error1) },
            { assertEquals(TEXT_NOT_FOUND, error2) },
            { assertEquals(TEXT_NOT_FOUND, error3) },
        )
    }

    @ParameterizedTest
    @CsvSource("0, 20")
    fun `список последних текстов`(page: Int, pageSize: Int) {
        val name1 = "John"
        val name2 = "James"
        val title = "Good title"
        val body = "Very interesting text"
        val random = Random(333)
        val texts = mutableListOf<TextResponseEntity>()
        val exposures =  TextExposure.values()

        val token1: String = createAccount(name1).readResult()
        val token2: String = createAccount(name2).readResult()
        for (i in 0 until 100) {
            val firstUser = random.nextBoolean()
            val token = if (firstUser) token1 else token2
            val exposure = exposures[random.nextInt(exposures.size)]
            val textId: String = createText(token, "$title$i", "$body$i", exposure).readResult()
            val text: TextResponseEntity = getText(textId, token).readResult()
            texts.add(text)
        }

        val lastTexts: List<Map<String, *>> = getTextList(page = page, pageSize = pageSize).readResult()
        val localLastTexts: List<TextResponseEntity> = texts.asReversed().asSequence()
            .filter { it.exposure == TextExposure.PUBLIC }
            .drop(page * pageSize).take(pageSize).toList()

        assertAll(
            { assertEquals(localLastTexts.size, lastTexts.size) },
            { assertTrue(lastTexts.zip(localLastTexts).all { it.first["textId"] == it.second.textId }) }
        )
    }

    @ParameterizedTest
    @CsvSource("0, 20")
    fun `список последних текстов пользователя`(page: Int, pageSize: Int) {
        val name1 = "John"
        val name2 = "James"
        val title = "Good title"
        val body = "Very interesting text"
        val random = Random(333)
        val texts1 = mutableListOf<TextResponseEntity>()
        val texts2 = mutableListOf<TextResponseEntity>()
        val exposures =  TextExposure.values()

        val token1: String = createAccount(name1).readResult()
        val token2: String = createAccount(name2).readResult()
        for (i in 0 until 100) {
            val firstUser = random.nextBoolean()
            val token = if (firstUser) token1 else token2
            val exposure = exposures[random.nextInt(exposures.size)]
            val textId: String = createText(token, "$title$i", "$body$i", exposure).readResult()
            val text: TextResponseEntity = getText(textId, token).readResult()
            when (firstUser) {
                true -> texts1.add(text)
                false -> texts2.add(text)
            }
        }

        val lastTexts1: List<Map<String, *>> = getUserTextList(token1, page = page, pageSize = pageSize).readResult()
        val lastTexts2: List<Map<String, *>> = getUserTextList(token2, page = page, pageSize = pageSize).readResult()
        val localLastTexts1: List<TextResponseEntity> = texts1.asReversed().asSequence()
            .drop(page * pageSize).take(pageSize).toList()
        val localLastTexts2: List<TextResponseEntity> = texts2.asReversed().asSequence()
            .drop(page * pageSize).take(pageSize).toList()

        assertAll(
            { assertEquals(localLastTexts1.size, lastTexts1.size) },
            { assertEquals(localLastTexts2.size, lastTexts2.size) },
            { assertTrue(lastTexts1.zip(localLastTexts1).all { it.first["textId"] == it.second.textId }) },
            { assertTrue(lastTexts2.zip(localLastTexts2).all { it.first["textId"] == it.second.textId }) },
        )
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    fun `создание текста с некорректным заголовком`(title: String) {
        val name = "John"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val error = createText(token, title, body).readError()

        assertEquals(INVALID_TEXT, error)
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    fun `создание текста с некорректным содержимым`(body: String) {
        val name = "John"
        val title = "Good title"

        val token: String = createAccount(name).readResult()
        val error = createText(token, title, body).readError()

        assertEquals(INVALID_TEXT, error)
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    fun `изменение заголовка на некорректный`(title2: String) {
        val name = "John"
        val title1 = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId: String = createText(token, title1, body).readResult()
        val error = editText(token, textId, title = title2).readError()

        assertEquals(INVALID_TEXT, error)
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    fun `изменение содержимого на некорректное`(body2: String) {
        val name = "John"
        val title = "Good title"
        val body1 = "Very interesting text"

        val token: String = createAccount(name).readResult()
        val textId: String = createText(token, title, body1).readResult()
        val error = editText(token, textId, body = body2).readError()

        assertEquals(INVALID_TEXT, error)
    }

    @Test
    fun `изменение автора`() {
        val name1 = "John"
        val name2 = "James"
        val title = "Good title"
        val body = "Very interesting text"

        val token: String = createAccount(name1).readResult()
        val textId: String = createText(token, title, body).readResult()
        val text1: TextResponseEntity = getText(textId).readResult()
        val accountInfo: UserResponseEntity = editAccount(token, name2).readResult()
        val text2: TextResponseEntity = getText(textId).readResult()

        assertAll(
            { assertEquals(text1.textId, text2.textId) },
            { assertEquals(text1.title, text2.title) },
            { assertEquals(text1.body, text2.body) },
            { assertEquals(text1.exposure, text2.exposure) },
            { assertEquals(name1, text1.author) },
            { assertEquals(name2, text2.author) },
        )
    }

    @ParameterizedTest
    @MethodSource("exposures")
    fun `редактирование по чужому токену`(exposure: TextExposure?) {
        val name1 = "John"
        val name2 = "James"
        val title1 = "Good title"
        val body = "Very interesting text"
        val title2 = "New beautiful title"

        val token1: String = createAccount(name1).readResult()
        val token2: String = createAccount(name2).readResult()
        val textId: String = createText(token1, title1, body, exposure).readResult()
        val error = editText(token2, textId, title = title2).readError()

        assertEquals(PERMISSION_DENIED, error)
    }

    @ParameterizedTest
    @MethodSource("exposures")
    fun `удаление по чужому токену`(exposure: TextExposure?) {
        val name1 = "John"
        val name2 = "James"
        val title1 = "Good title"
        val body = "Very interesting text"

        val token1: String = createAccount(name1).readResult()
        val token2: String = createAccount(name2).readResult()
        val textId: String = createText(token1, title1, body, exposure).readResult()
        val error = deleteText(token2, textId).readError()

        assertEquals(PERMISSION_DENIED, error)
    }

    @ParameterizedTest
    @MethodSource("exposures")
    fun `редактирование по старому токену`(exposure: TextExposure?) {
        val name = "John"
        val title1 = "Good title"
        val body = "Very interesting text"
        val title2 = "New beautiful title"

        val token1: String = createAccount(name).readResult()
        val textId: String = createText(token1, title1, body, exposure).readResult()
        val token2: String = revokeToken(token1).readResult()
        val error = editText(token1, textId, title = title2).readError()

        assertEquals(INVALID_TOKEN, error)
    }

    @ParameterizedTest
    @MethodSource("exposures")
    fun `удаление по старому токену`(exposure: TextExposure?) {
        val name = "John"
        val title = "Good title"
        val body = "Very interesting text"

        val token1: String = createAccount(name).readResult()
        val textId: String = createText(token1, title, body, exposure).readResult()
        val token2: String = revokeToken(token1).readResult()
        val error = deleteText(token1, textId).readError()

        assertEquals(INVALID_TOKEN, error)
    }

    private fun invalidNames() = listOf(
        Arguments.of("  \t  \n\r    "),
        Arguments.of('a'.toString().repeat(500))
    )

    private fun invalidBodies() = listOf(
        Arguments.of("  \t  \n\r    "),
        Arguments.of(('a'..'z').toString().repeat(10000))
    )

    private fun exposures() = listOf(
        Arguments.of(null),
        Arguments.of(TextExposure.PUBLIC),
        Arguments.of(TextExposure.UNLISTED),
        Arguments.of(TextExposure.PRIVATE)
    )

    private fun notPrivateExposures() = listOf(
        Arguments.of(null),
        Arguments.of(TextExposure.PUBLIC),
        Arguments.of(TextExposure.UNLISTED)
    )

    private fun twoExposures(): Iterable<Arguments> {
        val exposures = listOf(null, TextExposure.PUBLIC, TextExposure.UNLISTED, TextExposure.PRIVATE)
        return exposures.flatMap { e1 -> exposures.map { e2 -> Arguments.of(e1, e2) } }
    }

    private fun accountInfo(token: String) =
        mockMvc.perform(
            get("/account/info")
                .params(createParams("token" to token))
        ).readResponse()

    private fun createAccount(name: String) =
        mockMvc.perform(
            post("/account/create")
                .params(createParams("name" to name))
        ).readResponse()

    private fun editAccount(token: String, name: String) =
        mockMvc.perform(
            post("/account/edit")
                .params(createParams("token" to token, "name" to name))
        ).readResponse()

    private fun revokeToken(token: String) =
        mockMvc.perform(
            post("/account/revoke")
                .params(createParams("token" to token))
        ).readResponse()

    private fun getText(textId: String, token: String? = null) =
        mockMvc.perform(
            get("/text/$textId")
                .params(createParams("token" to token))
        ).readResponse()

    private fun getTextList(token: String? = null, page: Int? = null, pageSize: Int? = null) =
        mockMvc.perform(
            get("/text/list")
                .params(createParams("token" to token, "page" to page, "pageSize" to pageSize))
        ).readResponse()

    private fun getUserTextList(token: String, page: Int? = null, pageSize: Int? = null) =
        mockMvc.perform(
            get("/text/user-list")
                .params(createParams("token" to token, "page" to page, "pageSize" to pageSize))
        ).readResponse()

    private fun createText(token: String, title: String, body: String, exposure: TextExposure? = null) =
        mockMvc.perform(
            post("/text/create")
                .params(createParams("token" to token, "title" to title, "exposure" to exposure))
                .content(body)
        ).readResponse()

    private fun editText(token: String, textId: String, title: String? = null, body: String? = null, exposure: TextExposure? = null) =
        mockMvc.perform(
            post("/text/edit/$textId")
                .params(createParams("token" to token, "title" to title, "exposure" to exposure))
                .content(body ?: "")
        ).readResponse()

    private fun deleteText(token: String, textId: String) =
        mockMvc.perform(
            post("/text/delete/$textId")
                .params(createParams("token" to token))
        ).readResponse()

    private fun ResultActions.readResponse(): ObjectNode = this
        .andExpect(status().isOk)
        .andReturn().response.getContentAsString(Charsets.UTF_8)
        .let {objectMapper.readValue(it, ObjectNode::class.java) }

    private fun ObjectNode.isOk(): Boolean = this.get("ok").asBoolean()

    private inline fun <reified T> ObjectNode.readResult(): T = this.let {
        assertTrue(isOk())
        objectMapper.treeToValue(get("result"), T::class.java)
    }

    private fun ObjectNode.readError(): String = this.let {
        assertFalse(isOk())
        return get("error").asText()
    }

    private fun createParams(vararg params: Pair<String, Any?>) =
        LinkedMultiValueMap<String, String>().apply {
            params.forEach { (key, value) -> if (value != null) add(key, value.toString()) }
        }

    private val INVALID_TOKEN = "INVALID_TOKEN"
    private val INVALID_NAME = "INVALID_NAME"
    private val INVALID_TEXT = "INVALID_TEXT"
    private val PERMISSION_DENIED = "PERMISSION_DENIED"
    private val TEXT_NOT_FOUND = "TEXT_NOT_FOUND"
}
