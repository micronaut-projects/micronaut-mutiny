/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.ResourceLoader
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class GithubControllerTest {
    val MICRONAUT_RELEASE =
            Pattern.compile("Micronaut (Core |Framework )?v?\\d+.\\d+.\\d+( (RC|M)\\d)?")

    @Test
    fun verifyGithubReleasesCanBeFetched() {
        val config = mapOf("micronaut.codec.json.additional-types" to listOf("application/vnd.github.v3+json"),
            "spec.name" to "GithubControllerTest")
        ApplicationContext.run(EmbeddedServer::class.java, config).use { github ->
            ApplicationContext.run(EmbeddedServer::class.java,
                mapOf("micronaut.http.services.github.url" to "http://localhost:${github.port}")
            ).use { embeddedServer ->
                embeddedServer.applicationContext
                    .createBean(HttpClient::class.java, embeddedServer.url)
                    .use { httpClient ->
                        val client = httpClient.toBlocking()
                        assertReleases(client, "/github/releases")
                    }
            }
        }
    }

    fun assertReleases(client: BlockingHttpClient, path: String) {
        val request : HttpRequest<Any> = HttpRequest.GET(path)
        val rsp = client.exchange(request,
            Argument.listOf(GithubRelease::class.java))
        assertEquals(HttpStatus.OK, rsp.status)
        val releases = rsp.body()
        assertNotNull(releases)
        assertReleases(releases!!.toList())
    }

    fun assertReleases(releases: List<GithubRelease>) {
        assertNotNull(releases)
        Assertions.assertTrue(releases
            .map { it.name }
            .all { MICRONAUT_RELEASE.matcher(it).find() })
    }

    @Requires(property = "spec.name", value = "GithubControllerTest")
    @Controller
    class GithubReleases(val resourceLoader : ResourceLoader) {
        @Produces("application/vnd.github.v3+json")
        @Get("/repos/micronaut-projects/micronaut-core/releases")
        fun coreReleases() : String {
            return resourceLoader.getResource("releases.json").orElseThrow().readText()
        }
    }

}
