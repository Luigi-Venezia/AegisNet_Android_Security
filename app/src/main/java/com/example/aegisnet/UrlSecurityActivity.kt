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

//Gestisce  l'interfaccia di verifica URL locale e remota
class UrlSecurityActivity : AppCompatActivity() {
    private lateinit var binding: UrlSecurityActivityLayoutBinding
    private val securityViewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= UrlSecurityActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        setupListeners()
        observe()
    }
    private fun setupListeners() {

        binding.btnAnalyzeUrl.setOnClickListener {
          //Validazione input non trusted
            if (binding.etUrl.text.trim().isEmpty()){
                binding.tvInputError.text= getString(R.string.url_editText_vuota)
                binding.tvInputError.visibility= View.VISIBLE; return@setOnClickListener}

            binding.tvInputError.visibility= View.GONE
            //L'analisi viene fatta in locale
            val report= securityViewModel.analyzeUrl(binding.etUrl.text.trim().toString())
            renderReport(report)
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnOnlineCheck.setOnClickListener {
            val input=binding.etUrl.text.toString()
            securityViewModel.checkUrlOnline(input.trim())}
    }
    private fun renderReport(report: SecurityReport){

        binding.tvScore.text= getString(R.string.url_punteggio,report.score)
        binding.tvRiskLevel.text= getRiskLevelText(report.status)
        //Reset della UI per nuovi risultati
        binding.layoutChecks.removeAllViews()

        report.checks.forEach{check->
            val checkView= TextView(this).apply {
                    text= when (check.status) {
                            SecurityCheckStatus.PASSED-> "✓ ${check.name}"
                            SecurityCheckStatus.WARNING -> "⚠ ${check.name}"
                            SecurityCheckStatus.FAILED ->"✗ ${check.name}"}
                    textSize = 16f
                    setTextColor(getColor(R.color.aegis_white))
                    setPadding(0,10,0,10)
                    contentDescription= check.description
                }
            binding.layoutChecks.addView(checkView)
        }

        binding.tvWarnings.text = getString(R.string.url_warnings,report.warnings)
        binding.tvWarnings.visibility=if (report.warnings > 0)View.VISIBLE
            else View.GONE
        binding.tvHttpsExplanation.text= getString(R.string.https_trust_note)
        binding.layoutResult.visibility= View.VISIBLE
    }

    private fun getRiskLevelText(level: SecurityLevel):String{
        val riskLevel:String= when (level) {
            SecurityLevel.STRONG,SecurityLevel.GOOD->getString(
                    R.string.url_rischio_basso
                )
            SecurityLevel.MODERATE->getString(
                    R.string.url_rischio_moderato
                )
            SecurityLevel.WEAK,SecurityLevel.WARNING ->getString(R.string.url_rischio_alto)
            SecurityLevel.CRITICAL->getString(R.string.url_rischio_critico)
        };return riskLevel
    }

    // Osserva il valore dello StateFlow
    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
                securityViewModel.onlineUrlState.collect { state ->
                    when (state) {
                        is SecurityViewModel.OnlineUrlState.Idle -> {
                            binding.tvOnlineStatus.visibility = View.GONE}
                        is SecurityViewModel.OnlineUrlState.Loading -> {
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = "Verifica online in corso..."
                        }
                        is SecurityViewModel.OnlineUrlState.Success -> {
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = "Verifica completata con successo." }
                        is SecurityViewModel.OnlineUrlState.Error -> {
                            binding.tvOnlineStatus.visibility = View.VISIBLE
                            binding.tvOnlineStatus.text = state.message}
                    }
                }
            }
        }
    }
}