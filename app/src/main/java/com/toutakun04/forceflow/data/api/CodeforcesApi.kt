package com.toutakun04.forceflow.data.api

import com.toutakun04.forceflow.data.model.ApiResponse
import com.toutakun04.forceflow.data.model.Submission
import com.toutakun04.forceflow.data.model.UserInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface CodeforcesApi {
    @GET("user.status")
    suspend fun getUserSubmissions(
        @Query("handle") handle: String
    ): ApiResponse<List<Submission>>

    @GET("user.info")
    suspend fun getUserInfo(
        @Query("handles") handles: String
    ): ApiResponse<List<UserInfo>>
}
