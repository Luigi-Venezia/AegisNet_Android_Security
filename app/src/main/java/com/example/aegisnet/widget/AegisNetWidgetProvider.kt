package com.example.aegisnet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.aegisnet.MainActivity
import com.example.aegisnet.R
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* Android Home Screen Widget di AegisNet.

   AppWidgetProvider è un componente Android specializzato nella gestione
   del ciclo di vita di un widget. Android richiama onUpdate() quando il
   sistema deve aggiornare le istanze del widget, ad esempio dopo la sua
   installazione e secondo l'intervallo definito in aegisnet_widget_info.xml */

   /* Il widget non esegue analisi di sicurezza proprie: visualizza solamente
      lo stato già prodotto dalla Dashboard. In questo modo non duplica
      PasswordSecurityAnalyzer, UrlSecurityAnalyzer o l'algoritmo di scoring */
class AegisNetWidgetProvider : AppWidgetProvider() {

    /* Android fornisce gli ID delle istanze del widget da aggiornare.
       RemoteViews rappresenta una versione limitata e serializzabile delle
       View che il launcher può mostrare in un'altra applicazione/processo.
       Per questo il widget non può usare direttamente le View dell'Activity */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
        }
    }

    companion object {

        private const val PREFS_NAME =
            "aegisnet_widget_state"

        private const val KEY_SCORE =
            "security_score"

        private const val KEY_STATUS =
            "security_status"

        private const val KEY_LAST_SCAN =
            "last_security_scan"

        private const val DEFAULT_SCORE = 82
        private const val DEFAULT_STATUS = "SECURE"

        /* Salva soltanto lo stato non sensibile della Dashboard necessario
           al widget e aggiorna immediatamente tutte le sue istanze
           Non vengono mai salvate password, token, credenziali o secret */

        fun saveDashboardState(
            context: Context,
            report: SecurityReport
        ) {
            //Viene ottenuto un oggetto che rappresenta il file SharedPreferences
            val shPrefsObject= context.getSharedPreferences(
                    PREFS_NAME, //Nome del file memorizzato nella cartella privata dell'app
                    Context.MODE_PRIVATE)

            val editor= shPrefsObject.edit()

                 editor.putInt(KEY_SCORE, report.score)
                 editor.putString(KEY_STATUS,widgetStatus(report.status))
                 editor.putLong(KEY_LAST_SCAN,System.currentTimeMillis())

            editor.apply() /* Le modifiche apportate al file SharedPreferences
                              vengono salvate in modo asincrono con apply() */

            updateAllWidgets(context)
        }

        //Recupera lo stato persistito e aggiorna una singola istanza
        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val preferences =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val score =
                preferences.getInt(
                    KEY_SCORE,
                    DEFAULT_SCORE
                )

            val status =
                preferences.getString(
                    KEY_STATUS,
                    DEFAULT_STATUS
                ) ?: DEFAULT_STATUS

            val lastScan =
                preferences.getLong(
                    KEY_LAST_SCAN,
                    0L
                )

            val lastScanText =
                if (lastScan > 0L) {
                    SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).format(Date(lastScan))
                } else {
                    context.getString(
                        R.string.widget_not_scanned
                    )
                }

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.aegisnet_widget_layout)

            views.setTextViewText(
                R.id.tvWidgetScore,
                context.getString(
                    R.string.widget_score,
                    score
                )
            )

            views.setTextViewText(
                R.id.tvWidgetStatus,
                status
            )

            views.setTextViewText(
                R.id.tvWidgetLastScan,
                context.getString(
                    R.string.widget_last_scan,
                    lastScanText
                )
            )

            /* Il PendingIntent contiene soltanto un Intent esplicito verso
               MainActivity. Non vengono inseriti password, token o altri dati
               sensibili negli extras dell'Intent.
               FLAG_IMMUTABLE impedisce a terzi di modificare il PendingIntent;
               FLAG_UPDATE_CURRENT mantiene aggiornato l'Intent esistente */

            val openAppIntent =
                Intent(context, MainActivity::class.java)

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(
                R.id.btnOpenAegisNet,
                pendingIntent
            )

            appWidgetManager.updateAppWidget(
                appWidgetId,
                views
            )
        }

        //Aggiorna tutte le istanze presenti sulla Home Screen
        private fun updateAllWidgets(
            context: Context
        ) {
            val manager= AppWidgetManager.getInstance(context)

            val componentName =
                ComponentName(
                    context,
                    AegisNetWidgetProvider::class.java
                )

            val ids= manager.getAppWidgetIds(componentName)

            ids.forEach { id ->
                updateWidget(
                    context,
                    manager,
                    id
                )
            }
        }

        /* La Dashboard usa più livelli interni, mentre il widget espone
           volutamente solo i tre stati richiesti dall'interfaccia esterna */
        private fun widgetStatus(
            level: SecurityLevel
        ): String =
            when (level) {
                SecurityLevel.GOOD,
                SecurityLevel.STRONG -> "SECURE"

                SecurityLevel.MODERATE,
                SecurityLevel.WARNING,
                SecurityLevel.WEAK -> "WARNING"

                SecurityLevel.CRITICAL -> "CRITICAL"
            }
    }
}
