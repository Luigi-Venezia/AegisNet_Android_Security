package com.example.aegisnet

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.aegisnet.databinding.SecurityGuideActivityLayoutBinding
import com.example.aegisnet.model.SecurityGuideItem
import com.example.aegisnet.security.SecurityGuideRepository
import com.google.android.material.card.MaterialCardView
import android.widget.Button

//Gestisce la guida educativa sulla sicurezza
class SecurityGuideActivity : AppCompatActivity() {
    private lateinit var binding: SecurityGuideActivityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= SecurityGuideActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        render(SecurityGuideRepository.items)
        supportActionBar?.hide()
        setupListener()}

    // Inserisce nella ScrollView le card dei contenuti statici
    private fun render(items: List<SecurityGuideItem>){
        binding.layoutGuideItems.removeAllViews()
        items.forEach { item -> binding.layoutGuideItems.addView(createGuideCard(item))
        }}

    //Genera a runtime la singola card
    private fun createGuideCard(item: SecurityGuideItem): MaterialCardView {

        val card= MaterialCardView(this).apply {
            layoutParams= LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT).apply{
                        bottomMargin = dp(16)}

                setCardBackgroundColor(getColor(R.color.aegis_dark_surface))
                radius= dp(12).toFloat()
                cardElevation = dp(4).toFloat()
            }

        val content= LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18),dp(18),dp(18),dp(18))
            }

        content.addView(TextView(this).apply{
                text = item.title
                setTextColor(getColor(R.color.aegis_green))
                textSize = 20f
                setTypeface(typeface,android.graphics.Typeface.BOLD)})

        content.addView(TextView(this).apply {text = item.explanation
                setTextColor(getColor(R.color.aegis_white))
                textSize = 15f
                gravity = Gravity.START

                setPadding(0,dp(10),0,dp(10))})

        // Formatta i consigli come lista
        val tipsText= item.tips.map { "• $it" }.joinToString("\n")
        content.addView(TextView(this).apply {
            text = tipsText
                setTextColor(getColor(R.color.aegis_gray))
                textSize = 14f
                setLineSpacing(0f, 1.15f)})
        card.addView(content)
        return card}

    //Converte i dp in pixel
    private fun dp(value: Int): Int= (value * resources.displayMetrics.density).toInt()
    private fun setupListener(){
        //In questo caso l'oggetto viewBinding non viene sfruttato per usare btnBack
        val btnBack= findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener{
            finish()}
    }
}