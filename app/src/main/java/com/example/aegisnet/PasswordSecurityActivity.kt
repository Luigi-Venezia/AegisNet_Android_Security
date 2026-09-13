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

//Gestisce l'interfaccia di analisi locale della Password
class PasswordSecurityActivity : AppCompatActivity() {
    private lateinit var binding: PasswordSecurityActivityLayoutBinding
    private val securityViewModel: SecurityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       //Si evitano possibili catture dello schermo
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE)

        binding = PasswordSecurityActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        setupListeners()
    }

    private fun setupListeners() {

        binding.btnAnalyzePassword.setOnClickListener {
            val password = binding.etPassword.text.toString()

            //Validazione dell'input
            if (password.trim().isEmpty()) {

                binding.tvInputError.text = getString(R.string.password_vuota_error)
                binding.tvInputError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (binding.tvInputError.visibility != View.GONE) {
                binding.tvInputError.visibility = View.GONE
            }

            // Genera report locale e aggiorna la UI
            val report = securityViewModel.analyzePassword(password)
            renderReport(report) //La DashBoard viene correttamente aggiornata con i nuovi dati

            securityViewModel.getSaveReport(report)

            //Passa i risultati generati alla MainActivity
            val newReportExtras = Intent()
            newReportExtras.putExtra("NEW_REPORT_KEY", report)
            setResult(RESULT_OK, newReportExtras)

            //Il widget viene aggiornato
            AegisNetWidgetProvider.saveDashboardState(this,report)

            //Si elimina il testo per sicurezza
            binding.etPassword.text?.clear()
        }
        binding.btnBack.setOnClickListener {
            finish() //L'activity termina la sua sessione}
        }
    }

    private fun renderReport(report: SecurityReport) {
        binding.tvScore.text = getString(R.string.password_punteggio,report.score)
        binding.tvSecurityLevel.text = getSecurityLevelText(report.status)
        binding.layoutChecks.removeAllViews()

        report.checks.forEach { check ->

            val checkText=when (check.status) {
                    SecurityCheckStatus.PASSED-> "✓ ${check.name}"
                    SecurityCheckStatus.WARNING->"⚠ ${check.name}"
                    SecurityCheckStatus.FAILED ->"✗ ${check.name}"}
            val checkView = TextView(this).apply {
                    text = checkText
                    textSize = 16f
                    setTextColor(
                        getColor(R.color.aegis_white))
                    setPadding(
                        0,10,0,10)
                    contentDescription= check.description}
            binding.layoutChecks.addView(checkView)
        }
        binding.tvWarnings.text= getString(R.string.password_warnings,report.warnings)
        binding.tvWarnings.visibility =
            if (report.warnings > 0){View.VISIBLE}
            else View.GONE
        binding.layoutResult.visibility= View.VISIBLE
    }

    private fun getSecurityLevelText(level: SecurityLevel): String{
        return when (level) {
            SecurityLevel.STRONG-> getString(R.string.security_level_forte)
            SecurityLevel.MODERATE-> getString(R.string.security_level_moderata)
            SecurityLevel.WEAK-> getString(R.string.security_level_debole)
            SecurityLevel.CRITICAL-> getString(R.string.security_level_critica)
            SecurityLevel.GOOD ->getString(R.string.security_level_forte)
            SecurityLevel.WARNING ->getString(R.string.security_level_debole)
        }
    }
}