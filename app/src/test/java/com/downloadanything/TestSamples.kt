// 테스트 리소스의 실제 응답 샘플(JSON)을 읽는 도우미
package com.downloadanything

object TestSamples {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("samples/$name")) { "샘플 없음: $name" }.readText()
}
