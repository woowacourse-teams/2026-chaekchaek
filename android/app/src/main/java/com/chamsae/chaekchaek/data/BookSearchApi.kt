package com.chamsae.chaekchaek.data

import com.chamsae.chaekchaek.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 알라딘(Aladin) Open API 도서 검색. GET, TTBKey는 [BuildConfig.ALADIN_TTB_KEY]로 주입된다
 * (키 자체는 로컬 전용 `local.properties`에만 존재, 커밋되지 않음).
 */
object BookSearchApi {
  private const val ENDPOINT = "https://www.aladin.co.kr/ttb/api/ItemSearch.aspx"

  suspend fun search(query: String): List<BookSearchResult> =
    withContext(Dispatchers.IO) {
      val url =
        buildString {
          append(ENDPOINT)
          append("?ttbkey=").append(BuildConfig.ALADIN_TTB_KEY)
          append("&Query=").append(URLEncoder.encode(query, "UTF-8"))
          append("&QueryType=Keyword")
          append("&SearchTarget=Book")
          append("&MaxResults=20")
          append("&start=1")
          append("&output=js")
          append("&Version=20131101")
        }
      try {
        fetch(url)
      } catch (e: IOException) {
        fetch(url)
      }
    }

  private fun fetch(url: String): List<BookSearchResult> {
    val connection = (URL(url).openConnection() as HttpURLConnection)
    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    try {
      val code = connection.responseCode
      val body =
        (if (code in 200..299) connection.inputStream else connection.errorStream)
          .bufferedReader(Charsets.UTF_8)
          .use { it.readText() }
      if (code !in 200..299) throw IOException("검색 실패: HTTP $code")
      return parseBookSearchResults(body)
    } finally {
      connection.disconnect()
    }
  }
}
