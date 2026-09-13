package com.example.aegisnet.security

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

//Configurazione del logging disponibile soltanto nella variante di build DEBUG
internal fun OkHttpClient.Builder
        .configureAegisNetLogging():
        OkHttpClient.Builder =

    addInterceptor(

        HttpLoggingInterceptor{message-> val redactedMessage=message.replace(
                    Regex("([?&]host=)[^\\s&]+"),"$1<redacted>")
            Log.d("AegisNetHttp",redactedMessage)
        }
            .apply {
            level= HttpLoggingInterceptor.Level.BASIC
        }
    )