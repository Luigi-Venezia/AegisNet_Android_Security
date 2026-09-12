package com.example.aegisnet

//Classi importate nel file kotlin
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aegisnet.databinding.MainActivityLayoutBinding
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import com.example.aegisnet.viewmodel.SecurityViewModel
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts


//Creazione della classe MainActivity che eredita dalla classe AppCompactActivity
class MainActivity : AppCompatActivity() {

    /* Dichiarazione di una varibile che contiene l'oggetto della classe viewBinding
        del layout dell'activity, con lateinit la variabile verrà inizializzata
        solo successivamente */
    private lateinit var viewBindingObject: MainActivityLayoutBinding

    private val securityViewModel: SecurityViewModel by viewModels()

    // 2. REGISTRAZIONE DEL LAUNCHER COME PROPRIETÀ DELLA CLASSE
    // Questo oggetto si mette in ascolto del risultato restituito dall'Activity di destinazione
    val reportResult= registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Controlliamo se l'Activity di destinazione si è chiusa con successo (RESULT_OK)
        if (result.resultCode == RESULT_OK) {

            // Estraiamo l'oggetto serializzato utilizzando la chiave condivisa
            val newReport = result.data?.getSerializableExtra("NEW_REPORT_KEY") as? SecurityReport

            // Se il report è valido, aggiorniamo l'interfaccia grafica
            if (newReport != null) {
                renderReport(newReport)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) /* Viene invocato il metodo originale della classe padre
        quando si tratta di metodi che rappresentano
        un ciclo di vita dell'activity come onCreate() */

        /*Inizializzazione dell'oggetto viewBinding precedentemente dichiarato,
          il nostro oggetto conterrà riferimenti alle view del layout */
        viewBindingObject = MainActivityLayoutBinding.inflate(layoutInflater)

        //Indichiamo all'activity quale layout usare come sua interfaccia grafica
        //In questo caso è il layout dove sono contenute tutte le view
        setContentView(viewBindingObject.root)

        setupDashBoard() /* L'interfaccia viene aggiornata con il precedente report salvato nelle
                            SharedPreferences */


        supportActionBar?.hide() //L'action bar di sistema viene nascosta

        setupListener() //Invocazione del metodo per attendere l'interazione dell'utente con i button
    }

   //Metodo dove sono contenuti i listener in ascolto delle varie sottoclassi view button
    private fun setupListener(){

       //Listener per il button btnPasswordChecker
        viewBindingObject.btnPasswordChecker.setOnClickListener {
            /* Crea un intent esplicito specificando il context e il nome
               dell'activity
               l'Activity viene poi creata con startActivity */
            val intent= Intent(this, PasswordSecurityActivity::class.java)
            reportResult.launch(intent) /* Con startActivity l'activity specificata nell'intent
            viene creata e avviata da Android */
        }

        viewBindingObject.btnUrlChecker.setOnClickListener {
            startActivity(Intent(this,
                UrlSecurityActivity::class.java))
        }

        viewBindingObject.btnSecurityGuide.setOnClickListener {
           val intent= Intent(this,SecurityGuideActivity::class.java)
            startActivity(intent)
        }
    }

 //Aggiorna le textView a seconda del parametro report
    private fun renderReport(report: SecurityReport) {

        viewBindingObject.tvSecurityScore.text= report.score.toString()
        viewBindingObject.tvSecurityStatus.text = when(report.status) {
            SecurityLevel.GOOD,
            SecurityLevel.STRONG ->
                getString(
                    R.string.security_status_good
                ) //La textView tvSecurityStatus conterrà "Good" come text

            SecurityLevel.MODERATE,
            SecurityLevel.WARNING,
            SecurityLevel.WEAK ->
                getString(
                    R.string.security_status_warning
                )

            SecurityLevel.CRITICAL ->
                 getString(
                    R.string.security_status_critical
                )
        }

        viewBindingObject.tvPassedChecks.text= "${getString(R.string.passed_checks)}${report.passedChecks}"

        viewBindingObject.tvWarnings.text= "${getString(R.string.warnings)}${report.warnings}"
    }

    private fun setupDashBoard(){

        val lastReport= securityViewModel.getReadReportData() //Ottiene l'ultimo report salvato

        viewBindingObject.tvSecurityScore.text= lastReport.score.toString()
        viewBindingObject.tvWarnings.text= "${getString(R.string.warnings)}${lastReport.warnings}"
        viewBindingObject.tvSecurityStatus.text= lastReport.status.toString()
        viewBindingObject.tvPassedChecks.text= "${getString(R.string.passed_checks)}${lastReport.passedChecks}"
    }
}
