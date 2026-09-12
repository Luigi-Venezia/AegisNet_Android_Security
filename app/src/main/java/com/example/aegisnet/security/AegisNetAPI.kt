package com.example.aegisnet.security

import com.example.aegisnet.model.ObservatoryResponse
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/* Contratto Retrofit dell'unica operazione di networking
   usata da AegisNet.
   Retrofit trasforma questa interfaccia annotata in una
   implementazione HTTP type-safe e, grazie a suspend,
   non richiede callback manuali */
interface AegisNetApi {

    /* Avvia una verifica HTTP Observatory sull'hostname indicato
       Il servizio remoto analizza la configurazione HTTP
       di sicurezza del sito */
    @POST("api/v2/scan")
    suspend fun scanHost(
        @Query("host") host: String
    ): Response<ObservatoryResponse>
}

//Singleton che costruisce una sola istanza condivisa di Retrofit + OkHttp
object AegisNetApiProvider {

    private const val BASE_URL =
        "https://observatory-api.mdn.mozilla.net/"

    private val okHttpClient =
        OkHttpClient.Builder()

            /* Timeout espliciti: un servizio remoto non deve
               poter mantenere indefinitamente una coroutine
               in attesa della risposta */

            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .configureAegisNetLogging()
            .build()

            /* L'implementazione di questa extension cambia
               automaticamente tra DEBUG e RELEASE:
               DEBUG  -> logging controllato
               RELEASE -> nessun logging */

    val api: AegisNetApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AegisNetApi::class.java)
    }
}