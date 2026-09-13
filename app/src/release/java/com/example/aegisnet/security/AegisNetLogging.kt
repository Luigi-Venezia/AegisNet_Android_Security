package com.example.aegisnet.security

import okhttp3.OkHttpClient


/* Release build:
   logging HTTP completamente disabilitato.
   La stessa extension esiste nel source set debug,
   dove aggiunge HttpLoggingInterceptor.
   In questo modo la libreria di logging non è necessaria
   nella release APK */

/* Funzione dichiarata con qualificatore di accesso internal,
   dunque accessibile in tutti i metodi e funzioni appartenenti
   allo stesso modulo, in questo caso app */
internal fun OkHttpClient.Builder.configureAegisNetLogging(): OkHttpClient.Builder= this