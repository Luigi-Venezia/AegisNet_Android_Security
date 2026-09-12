package com.example.aegisnet.security

import com.example.aegisnet.model.ObservatoryResponse
import com.example.aegisnet.model.SecurityReport
import retrofit2.HttpException

/* Repository centrale per i dati di sicurezza.
   Nasconde al ViewModel la provenienza dei dati.
   Il ViewModel non conosce:
   Retrofit;
   OkHttp;
   endpoint HTTP;
   parsing della risposta;
   dettagli delle chiamate remote.
   In questo modo il Repository rappresenta
   l'astrazione tra ViewModel e datasource */
class SecurityRepository(
    private val api: AegisNetApi =
        AegisNetApiProvider.api,

    //Istanze delle classi UrlSecurityAnalyzer e PasswordSecuirityAnalyzer
    private val urlAnalyzer: UrlSecurityAnalyzer= UrlSecurityAnalyzer(),
    private val passwordAnalyzer: PasswordSecurityAnalyzer =
        PasswordSecurityAnalyzer()
) {

    //Password Security Checker, il controllo rimane completamente locale
    fun analyzePassword(
        password: String
    ): SecurityReport= passwordAnalyzer.analyze(password)


    /* URL Security Checker locale.
       Non esegue richieste HTTP */

    fun analyzeUrl(
        input: String
    ): SecurityReport= urlAnalyzer.analyze(input)

    //Restituisce l'hostname validato senza eseguire
    fun extractUrlHost(
        input: String
    ): String?= urlAnalyzer.extractHostname(input)

    /* Verifica online dell'hostname.
       È una suspend function perché l'operazione dipende
       dalla rete e viene chiamata dal ViewModel
       Il Repository interpreta la risposta HTTP e non
       espone dettagli interni di Retrofit alla UI */
    suspend fun checkUrlOnline(
        host: String
    ): ObservatoryResponse {

        val response =
            api.scanHost(host)


        //Un HTTP status fuori dalla famiglia 2xx non viene considerato un successo
        if (!response.isSuccessful) {
            throw HttpException(response)
        }


        //Una risposta 2xx senza body è comunque una risposta non valida
        val body =
            response.body()
                ?: throw IllegalStateException(
                    "Empty security-service response"
                )

        /* Tutti i dati ricevuti dalla rete sono considerati
           non trusted, verifichiamo quindi che i campi necessari
           siano effettivamente presenti */
        if (
            body.error != null ||
            body.grade.isNullOrBlank() ||
            body.score == null ||
            body.testsPassed == null ||
            body.testsFailed == null ||
            body.testsQuantity == null
        ) {
            throw IllegalStateException(
                "Invalid security-service response"
            )
        }

        return body
    }
}