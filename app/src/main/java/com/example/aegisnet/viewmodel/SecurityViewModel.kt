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

/* ViewModel principale di AegisNet
   Il codice View invoca soltanto metodi del ViewModel
   Il ViewModel coordina il Repository e possiede
   lo stato osservabile della verifica online
   Il Password Checker e il controllo locale degli URL
   restano completamente locali */
class SecurityViewModel(application: Application): AndroidViewModel(application){

    private val repository= SecurityRepository()//repository è un'istanza della classe SecurityRepository

    private val context= application.applicationContext

    private val sharedPrefsObject= context.getSharedPreferences("last_password_scan",
        Context.MODE_PRIVATE)

    //Report di default usato quando non ci sono dati di precedenti report memorizzati
    private var securityReport =
        SecurityReport(
            score = 0,
            status = SecurityLevel.CRITICAL,
            passedChecks = 0,
            warnings = 0)


    /* Stato della verifica remota
       La sealed interface permette alla UI di gestire
       in modo esaustivo:
       Idle
       Loading
       Success
       Error */
    sealed interface OnlineUrlState {

        data object Idle :
            OnlineUrlState

        data object Loading :
            OnlineUrlState

        data class Success(
            val result: ObservatoryResponse
        ) : OnlineUrlState

        data class Error(
            val message: String
        ) : OnlineUrlState
    }

    /* Backing property:
       MutableStateFlow è privato.
       La UI riceve solamente StateFlow read-only */
    private val _onlineUrlState =
        MutableStateFlow<OnlineUrlState>(
            OnlineUrlState.Idle)

    val onlineUrlState:
            StateFlow<OnlineUrlState> =
        _onlineUrlState.asStateFlow()

    /* Analisi password completamente locale
       La password non viene conservata dal ViewModel
       e non entra mai nel percorso Retrofit/OkHttp */
    fun analyzePassword(
        password: String
    ): SecurityReport = repository.analyzePassword(password)

    //Analisi URL locale permette il funzionamento anche senza connessione Internet
    fun analyzeUrl(
        input: String
    ): SecurityReport= repository.analyzeUrl(input)


    /* Verifica online secondaria dell'hostname
       viewModelScope utilizza il Main dispatcher
       withContext(Dispatchers.IO) sposta la parte
       di networking sul thread pool dedicato all'I/O
       Al termine withContext ritorna sul dispatcher
       originale, quindi l'aggiornamento di StateFlow
       avviene nuovamente sul Main Thread */
    fun checkUrlOnline(
        input: String
    ) {

        //Prima validiamo l'input localmente, se non è valido non parte nessuna richiesta
        val host= repository.extractUrlHost(input)

        if (host == null) {

            _onlineUrlState.value =
                OnlineUrlState.Error(
                    "Enter a valid HTTP or HTTPS URL before the online check.")

            return
        }

        /* viewModelScope collega la coroutine al ciclo di vita del ViewModel,
            la coroutine terminerà la sua esecuzione quando il viewModel sarà
            distrutto */

        viewModelScope.launch {

            _onlineUrlState.value =
                OnlineUrlState.Loading

            try {

                // Solo questa parte viene eseguita su Dispatchers.IO
                val result =
                    withContext(
                        Dispatchers.IO
                    ) {
                        repository.checkUrlOnline(host)
                    }

                //Siamo nuovamente sul Main dispatcher
                _onlineUrlState.value =
                    OnlineUrlState.Success(result)

            } catch (
                e: CancellationException
            ) {
                throw e

            } catch (
                _: IOException
            ) {


                //Nessuna rete, timeout DNS/socket, server non raggiungibile ecc
                _onlineUrlState.value =
                    OnlineUrlState.Error(
                        "Network unavailable. The local URL check is still available."
                    )

            } catch (
                e: HttpException
            ) {

                //Gestione esplicita degli errori HTTP
                val message =
                    when (e.code()) {

                        in 400..499 ->
                            "The remote security service rejected the request."

                        in 500..599 ->
                            "The remote security service is temporarily unavailable."

                        else ->
                            "The remote security service returned an HTTP error."
                    }

                _onlineUrlState.value =
                    OnlineUrlState.Error(message)

            } catch (
                _: IllegalStateException
            ) {

                //JSON/risposta non conforme a quella attesa
                _onlineUrlState.value =
                    OnlineUrlState.Error(
                        "The remote security service returned an invalid response."
                    )

            } catch (
                _: Exception
            ) {

                /* Eccezione inattesa:
                   Non mostriamo stack trace o messaggi
                   interni all'utente */

                _onlineUrlState.value =
                    OnlineUrlState.Error(
                        "The online security check could not be completed."
                    )
            }
        }
    }

    private fun saveReport(report: SecurityReport){

        //Vengono salvati i dati del report nelle SharedPreferences
        sharedPrefsObject.edit{
            putInt("security_score",report.score)
            putString("security_level",report.status.toString())
            putInt("security_warning",report.warnings)
            putInt("security_passedChecks",report.passedChecks)
        } //Con la funzione edit le modifiche vengono automaticamente apportate senza usare apply o commit
    }

    //readReportData è usato salvare il contenuto del file SharedPreferences in un'istanza SecurityReport
    //L'istanza sarà poi usata per aggiornare la DashBoard della MainActivity con i dati del vecchio report
    //Relativo all'analisi di una Password
    private fun readReportData(): SecurityReport{
        val securityReportObject= SecurityReport(
            sharedPrefsObject.getInt("security_score", securityReport.score),
            enumValueOf(sharedPrefsObject.getString("security_level", securityReport.status.toString()).toString()),
                sharedPrefsObject.getInt("security_passedChecks",securityReport.passedChecks),
                sharedPrefsObject.getInt("security_warning",securityReport.warnings))
        securityReport= securityReportObject

        return securityReportObject
    }

    fun getReadReportData()= readReportData() /* Metodo get che permette di ottenere il valore
                                                 ritornato da readReportData() */

    fun getSaveReport(report: SecurityReport)= saveReport(report)
}