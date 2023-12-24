package com.waze

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FetchApi {

    @GET("/fetchUser.php")
    fun fetch(@Query("userId") id : String) : Call<FetchData>

}