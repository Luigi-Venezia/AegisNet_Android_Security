package com.example.aegisnet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.example.aegisnet.databinding.PasswordSecurityActivityLayoutBinding
import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import com.example.aegisnet.viewmodel.SecurityViewModel
import com.example.aegisnet.widget.AegisNetWidgetProvider

/* Schermata grafica del Password Security Checker.
   L'Activity si occupa esclusivamente di:
   leggere l'input;
   invocare il ViewModel;
   visualizzare il SecurityReport.
   La logica di valutazione è contenuta in
   PasswordSecurityAnalyzer */
class PasswordSecurityActivity : AppCompatActivity() {

    private lateinit var binding: PasswordSecurityActivityLayoutBinding

    private val securityViewModel: SecurityViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Visto che la schermata contiene un campo password,
          il FLAG_SECURE riduce il rischio che il contenuto della schermata
          venga catturato tramite screenshot o registrazione dello schermo */
        /*window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)*/


        binding = PasswordSecurityActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupListeners()
    }

  //Metodo che configura i listener dei button del layout
    private fun setupListeners() {

        //Se premuto la password inserita viene analizzata localmente
        binding.btnAnalyzePassword.setOnClickListener {

           /* Quando l'utente interagisce con btnAnalyzePassword,
              la password viene letta */
            val password= binding.etPassword.text.toString()

        /* Se il button viene premuto, ma nella editText non è stata inserita alcuna
           password, l'utente viene notificato con un messaggio */
            if(password.trim().isEmpty()) {

                binding.tvInputError.text= getString(R.string.password_empty_error)

                /* La view inizialmente impostata come non visibile tramite attributo,
                   viene resa visibile per visualizzare un testo */
                binding.tvInputError.visibility= View.VISIBLE
                return@setOnClickListener
            }

            /* La view torna ad essere non visibile se prima era stato eseguito il corpo
              dell'if precedente e quindi era stata resa visibile */
            if(binding.tvInputError.visibility != View.GONE){
                binding.tvInputError.visibility= View.GONE}

            /* ANALISI LOCALE:
              La password non viene:
              - inviata a Internet;
              - inserita in URL;
              - scritta in Logcat;
              - salvata in SharedPreferences;
              - salvata nel ViewModel;
              - salvata nel Repository.
              Viene usata soltanto come input temporaneo
              dell'analizzatore locale */

            //Contiene il SecurityReport creato dopo l'analisi della password
            val report = securityViewModel.analyzePassword(password)

            renderReport(report) //La DashBoard viene correttamente aggiornata con i nuovi dati

            securityViewModel.getSaveReport(report) //I dati del nuovo scan vengono salvati (senza password)

            //Viene creato un intent vuoto per passare il nuovo report all'activity sorgente tramite extras
            val newReportExtras= Intent()
            //Il report serializzato viene passato tramite extras
            newReportExtras.putExtra("NEW_REPORT_KEY",report)

            //I dati del nuovo report vengono inviati al sistema Android tramite intent
            setResult(RESULT_OK,newReportExtras)

            //Il widget viene correttamente aggiornato con le informazioni del nuovo scan
            AegisNetWidgetProvider.saveDashboardState(
                this,
                report)

            /* Dopo l'analisi eliminiamo immediatamente il valore
              dal campo EditText con la funzione clear(), evitando di lasciarlo inutilmente
              nella UI */
            binding.etPassword.text?.clear()
        }

        binding.btnBack.setOnClickListener {
            //L'activity termina la sua sessione
            finish() /* Gli viene fatta assumere la stessa funzione del back button della
            navigation bar in questo caso quando premuto viene scartata l'activity
            in cima al back stack android e si torna indietro */
        }
    }






     /* Visualizza esclusivamente informazioni derivate dalla password
        senza mostrare la password inserita */
     //Usato per aggiornare le textView con i parametri del SecurityReport
    private fun renderReport(
        report: SecurityReport
    ) {

        binding.tvScore.text = getString(
                R.string.password_score,
                report.score)

        binding.tvSecurityLevel.text = getSecurityLevelText(report.status)


        /* La lista dei controlli viene generata a partire dal
          SecurityReport prodotto dall'Analyzer */
        binding.layoutChecks.removeAllViews()

        report.checks.forEach { check ->

            val checkText =
                when (check.status) {

                    SecurityCheckStatus.PASSED ->
                        "✓ ${check.name}"

                    SecurityCheckStatus.WARNING ->
                        "⚠ ${check.name}"

                    SecurityCheckStatus.FAILED ->
                        "✗ ${check.name}"
                }

            val checkView =
                TextView(this).apply {

                    text = checkText

                    textSize = 16f

                    setTextColor(
                        getColor(
                            R.color.aegis_white
                        )
                    )

                    setPadding(
                        0,
                        10,
                        0,
                        10
                    )

                     /* La descrizione del controllo viene resa
                        disponibile all'accessibility senza contenere
                        la password originale */
                    contentDescription= check.description
                }

            binding.layoutChecks.addView(checkView)
        }

        binding.tvWarnings.text =
            getString(
                R.string.password_warnings,
                report.warnings
            )

        binding.tvWarnings.visibility =
            if (report.warnings > 0)
                View.VISIBLE
            else
                View.GONE

        binding.layoutResult.visibility =
            View.VISIBLE
    }


      //Trasforma l'enum SecurityLevel in testo da mostrare alla UI
    private fun getSecurityLevelText(
        level: SecurityLevel
    ): String {

        //Ritornerà una stringa che dipenderà dal parametro level
        return when (level) {

            /* Accesso alle risorse dichiarate nel file strigs.xml mediante la classe R
              generata in fase di build */
            //In questo caso sarà ritornata la risorsa convertendola in formato String
            SecurityLevel.STRONG -> getString(R.string.security_level_strong)

            SecurityLevel.MODERATE ->
                getString(
                    R.string.security_level_moderate
                )

            SecurityLevel.WEAK ->
                getString(
                    R.string.security_level_weak
                )

            SecurityLevel.CRITICAL ->
                getString(
                    R.string.security_level_critical
                )


            /* Questi valori appartengono alla Dashboard precedente */
            SecurityLevel.GOOD ->
                getString(
                    R.string.security_level_strong
                )

            SecurityLevel.WARNING ->
                getString(
                    R.string.security_level_weak
                )
        }
    }
}