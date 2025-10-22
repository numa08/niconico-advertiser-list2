package net.numa08.niconico_advertiser_list2.datasource

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HttpClientのシングルトンファクトリ
 */
object HttpClientFactory {
    private var _httpClient: HttpClient? = null

    val httpClient: HttpClient
        get() {
            if (_httpClient == null) {
                _httpClient =
                    HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(
                                Json {
                                    ignoreUnknownKeys = true
                                    isLenient = true
                                },
                            )
                        }
                    }
            }
            return _httpClient!!
        }

    fun close() {
        _httpClient?.close()
        _httpClient = null
    }
}
