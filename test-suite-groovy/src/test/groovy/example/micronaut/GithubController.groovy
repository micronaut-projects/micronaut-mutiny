package example.micronaut

import io.micronaut.core.async.annotation.SingleResult
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.smallrye.mutiny.Uni
import org.reactivestreams.Publisher

@Controller("/github")
class GithubController {
    private final GithubApiClient githubApiClient

    GithubController(GithubApiClient githubApiClient) {
        this.githubApiClient = githubApiClient
    }

    @Get("/releases")
    @SingleResult
    Uni<List<GithubRelease>> fetchReleases() {
        githubApiClient.fetchReleases()
    }
}
