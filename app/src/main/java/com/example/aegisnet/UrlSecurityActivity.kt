package com.example.aegisnet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.aegisnet.databinding.UrlSecurityActivityLayoutBinding
import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import com.example.aegisnet.viewmodel.SecurityViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle

/* Interfaccia grafica dell'URL / Link Security Checker
   L'Activity legge l'input, delega l'analisi al ViewModel e visualizza
   esclusivamente il SecurityReport
   L'URL non viene aperto, eseguito né inviato in rete,
   evitando dunque possibili problematiche o attacchi in rete */
class UrlSecurityActivity : AppCompatActivity() {

    private lateinit var binding: UrlSecurityActivityLayoutBinding
    private val securityViewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= UrlSecurityActivityLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.hide()

        setupListeners()
        observeOnlineState()
    }

    //Configura i listener dei button della schermata
    private fun setupListeners() {

        //Si attiva quando l'utente preme su btnAnalyzeUrl per effettuare la verifica offline
        binding.btnAnalyzeUrl.setOnClickListener {

          //Viene prima verificato se la editText è vuota con isEmpty()
            if (binding.etUrl.text.trim().isEmpty()){

                binding.tvInputError.text= getString(R.string.url_empty_error)

                //tvInputError diventa visibile visualizzando il messaggio
                binding.tvInputError.visibility= View.VISIBLE

                return@setOnClickListener
            }

            //Se la textView utilizzata per rappresentare l'errore è visibile viene nascosta
            binding.tvInputError.visibility= View.GONE

            /* Analisi completamente locale.
              Nessuna richiesta HTTP viene eseguita */
            val report= securityViewModel.analyzeUrl(binding.etUrl.text.trim().toString())

            renderReport(report) //Il report viene passato al metodo usato per visualizzare i dati
        }

        //La pressione su btnBack comporta la stessa funzione del BackButton della navigation bar
        //si ritornerà all'activity precedentemente aperta
        binding.btnBack.setOnClickListener {
            finish() /* L'activity attuale presente nel BackStack Android viene scartata
                        mostrando quella precedente presente nello stack */
        }

        //Premendo su btnOnlineCheck la verifica dell'url viene fatta online
        binding.btnOnlineCheck.setOnClickListener {
            securityViewModel.checkUrlOnline(
                binding.etUrl.text.toString()) //L'input verrà passato al ViewModel
        }
    }

    //Visualizza il SecurityReport restituito dall'Analyzer
    private fun renderReport(
        report: SecurityReport) {

        binding.tvScore.text =
            getString(
                R.string.url_score,
                report.score)

        binding.tvRiskLevel.text= getRiskLevelText(report.status)

        /* Rimuove tutte le componenti View figlie contenute nella ViewGroup layoutChecks
           in questo modo le View possono essere nuovamente aggiornate con i dati del nuovo
           report senza sommarsi al precedente report */
        binding.layoutChecks.removeAllViews()

        //Ciclo che attraversa uno alla volta tutti i controlli di sicurezza della lista
        report.checks.forEach { check ->

            //Crea un elemento grafico di tipo TextView impostando le sue caratteristiche (testo,padding,size)
            val checkView= TextView(this).apply {

                //Il testo dipende dallo status dell'elemento corrente
                    text= when (check.status) {

                            SecurityCheckStatus.PASSED ->
                                "✓ ${check.name}"

                            SecurityCheckStatus.WARNING ->
                                "⚠ ${check.name}"

                            SecurityCheckStatus.FAILED ->
                                "✗ ${check.name}"
                        }

                    textSize = 16f //Valore di tipo Float

                    setTextColor(
                        getColor(
                            R.color.aegis_white)
                    )

                    setPadding(
                        0,
                        10,
                        0,
                        10
                    )

                    /* La descrizione viene resa disponibile
                       all'accessibility senza aggiungere l'URL
                       originale al risultato */
                    contentDescription =
                        check.description
                }

            binding.layoutChecks.addView(checkView)
        }

        binding.tvWarnings.text =
            getString(
                R.string.url_warnings,
                report.warnings)

        binding.tvWarnings.visibility =
            if (report.warnings > 0)
                View.VISIBLE //In questo caso la TextView diventa visibile
            else
                View.GONE //Non è ne visibile ma non occupa neanche spazio nel layout

        /* Spiegazione importante:
         HTTPS protegge la comunicazione mediante il protocollo TLS
         ma non dimostra che il sito sia affidabile */

        binding.tvHttpsExplanation.text =
            getString(
                R.string.https_trust_note)

        binding.layoutResult.visibility =
            View.VISIBLE
    }

    /* Il model SecurityLevel viene riutilizzato anche se la UI del
      URL Checker utilizza la nomenclatura richiesta:
      STRONG/GOOD -> LOW
      MODERATE -> MODERATE
      WEAK/WARNING -> HIGH
      CRITICAL -> CRITICAL */
    private fun getRiskLevelText(
        level: SecurityLevel
    ): String =
        when (level) {

            SecurityLevel.STRONG,
            SecurityLevel.GOOD ->
                getString(
                    R.string.url_risk_low
                )

            SecurityLevel.MODERATE ->
                getString(
                    R.string.url_risk_moderate
                )

            SecurityLevel.WEAK,
            SecurityLevel.WARNING ->
                getString(
                    R.string.url_risk_high
                )

            SecurityLevel.CRITICAL ->
                getString(
                    R.string.url_risk_critical
                )
        }


    //Metodo usato per metterci in ascolto dello StateFlow in modo sicuro ed efficiente
    private fun observeOnlineState() {
        //Sfruttiamo il lifecycle per vincolare il ciclo di vita della coroutine a quello dell'Activity
        lifecycleScope.launch {
            //repeatOnLifecycle blocca o riprende l'ascolto in base allo stato dell'Actvitiy (lifecycle aware)
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                securityViewModel.onlineUrlState.collect { state ->
                    when (state) {
                        //Gestiamo tutti i casi della sealed interface definita nel ViewModel
                        is SecurityViewModel.OnlineUrlState.Idle -> {
                            //Stato di quiete, la View rimane non visibile e non occupa spazio nel layout
                            binding.tvOnlineStatus.visibility = View.GONE
                        }
                        is SecurityViewModel.OnlineUrlState.Loading -> {
                            //Siamo in attesa del risultato e l'utente viene avvisato
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = "Verifica online in corso..."
                        }
                        is SecurityViewModel.OnlineUrlState.Success -> {
                            //Chiamata andata a buon fine, qui elaboriamo il risultato ricevuto
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            // Personalizza in base ai campi presenti in ObservatoryResponse
                            binding.tvOnlineStatus.text = "Verifica completata con successo."
                        }
                        is SecurityViewModel.OnlineUrlState.Error -> {
                            //Errore di rete o del server, si stampa il messaggio gestito nel VM
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = state.message
                        }
                    }
                }
            }
        }
    }
}