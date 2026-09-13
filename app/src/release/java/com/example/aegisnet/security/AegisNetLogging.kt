package com.example.aegisnet.security
import okhttp3.OkHttpClient
/* Usato nella Release:
   logging HTTP completamente disabilitato */
internal fun OkHttpClient.Builder.configureAegisNetLogging(): OkHttpClient.Builder= this