package com.example.aegisnet.security
import com.example.aegisnet.model.ObservatoryResponse
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

//Interfaccia per le API
interface AegisNetApi {
    // Richiede l'analisi di sicurezza dell'hostname al servizio remoto
    @POST("api/v2/scan")
    suspend fun scanHost(@Query("host") host: String):Response<ObservatoryResponse>
}
object AegisNetApiProvider {

    private const val BASE_URL="https://observatory-api.mdn.mozilla.net/"
    private val okHttpClient= OkHttpClient.Builder()
        //Timeout di sicurezza per evitare blocchi indefiniti
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS).configureAegisNetLogging().build()
    val api: AegisNetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AegisNetApi::class.java)}
}