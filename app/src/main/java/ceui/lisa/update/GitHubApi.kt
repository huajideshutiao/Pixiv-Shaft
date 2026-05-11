package ceui.lisa.update

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {

    @GET("repos/{owner}/{repo}/releases/latest")
    fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<GitHubRelease>

    @GET("repos/{owner}/{repo}/releases")
    fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): Call<List<GitHubRelease>>

    companion object {
        const val BASE_URL = "https://api.github.com/"
        const val OWNER = "huajideshutiao"
        const val REPO = "Pixiv-Shaft"
    }
}
