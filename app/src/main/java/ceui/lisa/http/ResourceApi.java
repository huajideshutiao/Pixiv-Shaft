package ceui.lisa.http;

import retrofit2.Call;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ResourceApi {
    String JSDELIVR_BASE_URL = "https://cdn.jsdelivr.net/";
    String JSDELIVR_PROJECT_MASTER_PATH = "gh/huajideshutiao/Pixiv-Shaft@master/";

    @GET("gh/huajideshutiao/Pixiv-Shaft@master/app/src/main/assets/comment.filter.rule.txt")
    Call<ResponseBody> getCommentFilterRule();

    @GET(JSDELIVR_PROJECT_MASTER_PATH + "{path}")
    Call<ResponseBody> getByPath(@Path("path") String path);
}
