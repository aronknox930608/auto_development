package cc.unitmesh.devti.counit.client

import cc.unitmesh.devti.counit.model.PromptResult
import cc.unitmesh.devti.counit.model.Tool
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CoUnitApi {
    @GET("/api/agent/prompt/explain")
    fun explainQuery(@Query("q") q: String): Call<PromptResult>

    @POST("/api/agent/prompt/functions/matching")
    fun toolPrompter(@Query("q") q: String): Call<PromptResult>

    @GET("/api/agent/prompt/functions/list")
    fun functions(): Call<List<Tool>>
}
