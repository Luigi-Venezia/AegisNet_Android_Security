package com.example.aegisnet.viewmodel
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.aegisnet.model.ObservatoryResponse
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import com.example.aegisnet.security.SecurityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.edit

//Coordina i dati e gestisce lo stato delle verifiche
class SecurityViewModel(application: Application): AndroidViewModel(application){
    private val repository= SecurityRepository()
    private val context= application.applicationContext
    private val sharedPrefs= context.getSharedPreferences("last_password_scan",Context.MODE_PRIVATE)

    /*Report di default */
    private var securityReport=SecurityReport(0,SecurityLevel.CRITICAL,0,0)
    //Stati della verifica
    sealed interface OnlineUrlState {
        data object Idle:OnlineUrlState
        data object Loading :OnlineUrlState
        data class Success(val result: ObservatoryResponse): OnlineUrlState
        data class Error(val  message: String): OnlineUrlState
    }
    private val _onlineUrlState= MutableStateFlow<OnlineUrlState>(OnlineUrlState.Idle)
    val onlineUrlState:StateFlow<OnlineUrlState> = _onlineUrlState.asStateFlow()
    fun analyzePassword(password: String):SecurityReport =repository.analyzePassword(password)
    fun analyzeUrl(input: String): SecurityReport= repository.analyzeUrl(input)

    //Esegue il controllo online
    fun checkUrlOnline(input: String){
        val host= repository.extractUrlHost(input)
        if (host == null) {
            _onlineUrlState.value= OnlineUrlState.Error("Enter a valid HTTP or HTTPS URL before the online check.")
            return}

        viewModelScope.launch{
            _onlineUrlState.value= OnlineUrlState.Loading
            try { val result = withContext(Dispatchers.IO)
            {
                repository.checkUrlOnline(host)
                    }
                _onlineUrlState.value= OnlineUrlState.Success(result)
            } catch(e: CancellationException){
                throw e
            }
            catch (_: IOException){
                _onlineUrlState.value=OnlineUrlState.Error("Network unavailable. The local URL check is still available.")
            }catch(e: HttpException){

                val message= when (e.code()){
                        in 400..499->"The remote security service rejected the request."
                        in 500..599 ->"The remote security service is temporarily unavailable."
                        else -> "The remote security service returned an HTTP error."}
                _onlineUrlState.value= OnlineUrlState.Error(message)


            } catch(_: IllegalStateException){
                _onlineUrlState.value =
                    OnlineUrlState.Error("The remote security service returned an invalid response.")

            } catch (_: Exception){
                _onlineUrlState.value =OnlineUrlState.Error("The online security check could not be " +
                        "completed.")
            }
        }
    }

    /*Il nuovo report viene salvato in locale
     */
    private fun saveReport(report: SecurityReport){
        sharedPrefs.edit{
            putInt("security_score",report.score)
            putString("security_level",report.status.toString())
            putInt("security_warning",report.warnings)
            putInt("security_passedChecks",report.passedChecks)}
    }

    //Il report in locale viene letto
    private fun readReportData(): SecurityReport{
        val securityReportObject= SecurityReport(sharedPrefs.getInt("security_score", securityReport.score),
            enumValueOf(sharedPrefs.getString("security_level", securityReport.status.toString()).toString()),
                sharedPrefs.getInt("security_passedChecks",securityReport.passedChecks),
                sharedPrefs.getInt("security_warning",securityReport.warnings))
        securityReport= securityReportObject
        return securityReportObject
    }
    fun getReadReportData()= readReportData()
    fun getSaveReport(report: SecurityReport)= saveReport(report)
}