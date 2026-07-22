/*
 * Mojanq Launcher
 * Copyright (C) 2025 AltayHR and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.path

import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

const val URL_MCMOD: String = "https://www.mcmod.cn/"

// Mojanq Launcher project links
const val URL_PROJECT: String = "https://github.com/ThT0AltayHR/MojanqLauncher"
const val URL_STAR1XR: String = "https://github.com/ThT0AltayHR"
const val URL_PROJECT_INFO: String = "https://api.github.com/repos/ThT0AltayHR/MojanqLauncher/contents/v2"
const val URL_PROJECT_RELEASES_LATEST: String = "https://api.github.com/repos/ThT0AltayHR/MojanqLauncher/releases/latest"
const val URL_COMMUNITY: String = "https://github.com/ThT0AltayHR/MojanqLauncher/graphs/contributors"
const val URL_WEBLATE: String = "https://github.com/ThT0AltayHR/MojanqLauncher"
const val URL_SUPPORT: String = "https://github.com/ThT0AltayHR"

private const val TIME_OUT = 10000L

val URL_USER_AGENT: String
    get() = "Mojanq-Launcher/${BuildConfig.VERSION_NAME}"

private val CURSEFORGE_INTERCEPTOR = okhttp3.Interceptor { chain ->
    val original = chain.request()
    val request = original.newBuilder()
        .header("User-Agent", URL_USER_AGENT)
        .build()
    chain.proceed(request)
}

private val USER_AGENT_INTERCEPTOR = okhttp3.Interceptor { chain ->
    val original = chain.request()
    val url = original.url
    val apiKey = BuildKeys.CURSEFORGE_API_KEY
    val request = if (url.host.contains("curseforge.com") || url.host.contains("forgecdn.net")) {
        original.newBuilder()
            .header("x-api-key", apiKey)
            .build()
    } else {
        original
    }
    chain.proceed(request)
}

fun createRequestBuilder(url: String): Request.Builder {
    return createRequestBuilder(url, null)
}

fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
    val request = Request.Builder().url(url).header("User-Agent", URL_USER_AGENT)
    body?.let { request.post(it) }
    return request
}

fun createOkHttpClient(): OkHttpClient = createOkHttpClientBuilder().build()

fun createOkHttpClientBuilder(action: (OkHttpClient.Builder) -> Unit = { }): OkHttpClient.Builder {
    return OkHttpClient.Builder()
        .callTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
        .addInterceptor(CURSEFORGE_INTERCEPTOR)
        .addInterceptor(USER_AGENT_INTERCEPTOR)
        .apply(action)
}

val DOWNLOAD_OKHTTP_CLIENT: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(CURSEFORGE_INTERCEPTOR)
        .addInterceptor(USER_AGENT_INTERCEPTOR)
        .build()
}

val GLOBAL_JSON: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}

val GLOBAL_CLIENT: HttpClient by lazy {
    HttpClient(OkHttp) {
        engine {
            preconfigured = createOkHttpClientBuilder {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }.build()
        }
        install(ContentNegotiation) {
            json(GLOBAL_JSON)
        }
    }
}
