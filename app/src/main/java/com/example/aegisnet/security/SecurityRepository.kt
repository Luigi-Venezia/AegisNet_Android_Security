package com.example.aegisnet.security

import com.example.aegisnet.model.ObservatoryResponse
import com.example.aegisnet.model.SecurityReport
import retrofit2.HttpException

//Gestisce l'accesso ai dati di sicurezza da sorgenti remote
class SecurityRepository(
    private val api: AegisNetApi=AegisNetApiProvider.api,
    private val urlAnalyzer: UrlSecurityAnalyzer= UrlSecurityAnalyzer(),
    private val passwordAnalyzer: PasswordSecurityAnalyzer=PasswordSecurityAnalyzer())
{
    fun analyzePassword(password:String):SecurityReport= passwordAnalyzer.analyze(password)
    fun analyzeUrl(input: String): SecurityReport= urlAnalyzer.analyze(input)
    fun extractUrlHost(input:String):String?{
        val hostname= urlAnalyzer.extractHostname(input)
        return hostname}
    suspend fun checkUrlOnline(host: String): ObservatoryResponse {
        val response=api.scanHost(host)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val body =response.body() ?:throw IllegalStateException("Empty security-service response")

        if (body.error != null || body.grade.isNullOrBlank() || body.score == null ||
            body.testsPassed == null || body.testsFailed == null || body.testsQuantity == null){
            throw IllegalStateException("Invalid security-service response")
        };return body
    }
}