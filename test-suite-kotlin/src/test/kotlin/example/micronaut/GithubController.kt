package example.micronaut

import io.micronaut.core.async.annotation.SingleResult
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.smallrye.mutiny.Uni

@Controller("/github")
class GithubController(private val githubApiClient: GithubApiClient) {

    @Get("/releases")
    @SingleResult
    fun fetchReleases(): Uni<List<GithubRelease>> {
        return githubApiClient.fetchReleases()
    }
}
