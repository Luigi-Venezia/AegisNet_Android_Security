package com.example.aegisnet.security

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

//Configurazione del logging disponibile soltanto nella variante di build DEBUG
//Funzione con qualificatore di accesso internal sarà accessibile nello stesso modulo app
internal fun OkHttpClient.Builder
        .configureAegisNetLogging():
        OkHttpClient.Builder =

    addInterceptor(

        HttpLoggingInterceptor { message ->

            /* BASIC non registra il body.
               Inoltre oscuriamo l'hostname passato
               nella query per evitare di trasformare
               un input utente in un dato persistente
               di Logcat */
            val redactedMessage =
                message.replace(
                    Regex(
                        "([?&]host=)[^\\s&]+"
                    ),
                    "$1<redacted>"
                )

            Log.d(
                "AegisNetHttp",
                redactedMessage
            )

        }.apply {

            level =
                HttpLoggingInterceptor.Level.BASIC
        }
    )