package ceui.lisa.http;

import retrofit2.Call;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface SauceNaoApi {

    @Multipart
    @POST("/search.php")
    Call<Response<ResponseBody>> query(@Part MultipartBody.Part part);
}
