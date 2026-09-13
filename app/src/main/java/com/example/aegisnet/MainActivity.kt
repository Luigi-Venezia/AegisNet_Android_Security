package com.example.aegisnet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aegisnet.databinding.MainActivityLayoutBinding
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import com.example.aegisnet.viewmodel.SecurityViewModel
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityLayoutBinding
    private val securityViewModel: SecurityViewModel by viewModels()

    val reportResult= registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){result ->
        if (result.resultCode==RESULT_OK) {
            val newReport = result.data?.getSerializableExtra("NEW_REPORT_KEY") as? SecurityReport
            if (newReport!=null)render(newReport)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding= MainActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupDashBoard()

        supportActionBar?.hide()
        listener()
    }
    private fun listener(){
        binding.btnPasswordChecker.setOnClickListener{
            val intent= Intent(this, PasswordSecurityActivity::class.java)
            reportResult.launch(intent)}

        binding.btnUrlChecker.setOnClickListener {
            startActivity(Intent(this,UrlSecurityActivity::class.java))
        }
        binding.btnSecurityGuide.setOnClickListener {
           val intent= Intent(this,SecurityGuideActivity::class.java)
            startActivity(intent)
        }}

    private fun render(report: SecurityReport){
        binding.tvSecurityScore.text= report.score.toString()
        binding.tvSecurityStatus.text= when(report.status) {
            SecurityLevel.GOOD,
            SecurityLevel.STRONG -> getString(
                    R.string.stato_sicurezza_buono
                )
            SecurityLevel.MODERATE,
            SecurityLevel.WARNING,
            SecurityLevel.WEAK->getString(R.string.stato_sicurezza_warning)
            SecurityLevel.CRITICAL ->getString(
                    R.string.stato_sicurezza_critico
                )
        }
        binding.tvPassedChecks.text= "${getString(R.string.controlli_superati_text)}${report.passedChecks}"
        binding.tvWarnings.text= "${getString(R.string.warnings)}${report.warnings}"
    }
    private fun setupDashBoard(){
        val lastReport= securityViewModel.getReadReportData()
        binding.tvSecurityScore.text= lastReport.score.toString()
        binding.tvWarnings.text= "${getString(R.string.warnings)}${lastReport.warnings}"
        binding.tvSecurityStatus.text= lastReport.status.toString()
        binding.tvPassedChecks.text= "${getString(R.string.controlli_superati_text)}${lastReport.passedChecks}"
    }
}
