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

/* Activity dedicata alla guida educativa sulla sicurezza
   L'Activity gestisce soltanto la presentazione dei contenuti
   I dati statici sono definiti nel SecurityGuideRepository e
   rappresentati dal modello SecurityGuideItem
   In questo modo UI e contenuti restano separati */
class SecurityGuideActivity : AppCompatActivity() {

    private lateinit var binding: SecurityGuideActivityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= SecurityGuideActivityLayoutBinding.inflate(layoutInflater)

        setContentView(binding.root)

        renderGuide(SecurityGuideRepository.items)

        supportActionBar?.hide()

        setupListener()


        }

    /* Trasforma la List dei contenuti in card visualizzate nella UI
       La guida contiene pochi elementi statici, quindi una LinearLayout
       dentro ScrollView è sufficiente e non è necessario RecyclerView */
    private fun renderGuide(
        items: List<SecurityGuideItem>
    ) {
        binding.layoutGuideItems.removeAllViews()

        items.forEach { item ->
            binding.layoutGuideItems.addView(
                createGuideCard(item)
            )
        }
    }

    /* Crea una singola card della guida
       Non contiene logica di sicurezza: si occupa esclusivamente
       della trasformazione del modello in una View */
    private fun createGuideCard(
        item: SecurityGuideItem
    ): MaterialCardView {

        val card =
            MaterialCardView(this).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(16)
                    }

                setCardBackgroundColor(
                    getColor(R.color.aegis_dark_surface)
                )

                radius = dp(12).toFloat()
                cardElevation = dp(4).toFloat()
            }

        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        content.addView(
            TextView(this).apply {
                text = item.title
                setTextColor(
                    getColor(R.color.aegis_green)
                )
                textSize = 20f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }
        )

        content.addView(
            TextView(this).apply {
                text = item.explanation
                setTextColor(
                    getColor(R.color.aegis_white)
                )
                textSize = 15f
                gravity = Gravity.START

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(10)
                )
            }
        )

        /* la funzione map trasforma ogni consiglio aggiungendo il bullet
           joinToString costruisce il testo finale da visualizzare
           Sono operazioni sulle Collections di Kotlin e non
           dipendono da Android */
        val tipsText= item.tips.map { "• $it" }.joinToString("\n")

        content.addView(
            TextView(this).apply {
                text = tipsText
                setTextColor(
                    getColor(R.color.aegis_gray)
                )
                textSize = 14f
                setLineSpacing(0f, 1.15f)
            }
        )

        card.addView(content)

        return card
    }

    /* Conversione da dp a pixel
       I layout Android devono utilizzare unità indipendenti dalla densità,
       questo perchè gli schermi dei dispositivi possono avere densità di pixel
       differenti */
    private fun dp(value: Int): Int= (value * resources.displayMetrics.density).toInt()

    private fun setupListener(){
        //In questo caso l'oggetto viewBinding non viene sfruttato per usare btnBack
        val btnBack= findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener{
            finish() //L'activity attualmente presente in cima al BackStack viene scartata
        }
    }
}