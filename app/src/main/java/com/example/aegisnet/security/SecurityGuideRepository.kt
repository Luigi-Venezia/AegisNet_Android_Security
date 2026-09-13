package com.example.aegisnet.security

import com.example.aegisnet.model.SecurityGuideItem

//Repository locale contenente i dati della Security Guide
object SecurityGuideRepository {
    val items: List<SecurityGuideItem> = listOf(
        SecurityGuideItem(title = "Password Security",
            explanation = "Una password efficace deve essere difficile da indovinare e diversa per ogni servizio.",
            tips = listOf(
                "Preferisci password lunghe.",
                "Evita password ovvie come nomi, date o parole comuni.",
                "Non riutilizzare la stessa password su servizi diversi.",
                "Utilizza una combinazione di caratteri, numeri e simboli quando appropriato.")),
        SecurityGuideItem(
            title = "Phishing",
            explanation="Il phishing cerca di convincerti a fornire informazioni riservate attraverso messaggi o pagine contraffatte.",
            tips = listOf("Controlla con attenzione il mittente del messaggio.","Controlla l'URL prima di inserire dati personali.",
                "Diffida dei messaggi che creano urgenza o pressione.",
                "Non fornire credenziali a pagine sospette.")
        ),
        SecurityGuideItem(
            title = "Suspicious Links",
            explanation =
                "Un link può nascondere una destinazione diversa da quella che sembra indicare.",
            tips = listOf("Controlla il dominio principale dell'URL.",
                "Presta attenzione a URL molto lunghi o con parti anomale.",
                "Considera con cautela i link che utilizzano direttamente un indirizzo IP.",
                "Fai attenzione ai link abbreviati perché nascondono la destinazione finale.")
        ),
        SecurityGuideItem(title = "HTTPS / TLS",explanation= "HTTPS protegge la comunicazione tra dispositivo e server " +
                        "utilizzando TLS, contribuendo a proteggere i dati durante il transito.",
            tips = listOf(
                "Controlla che il sito utilizzi HTTPS quando trasmetti informazioni.",
                "Ricorda che HTTPS protegge la connessione, non certifica che il sito sia affidabile.",
                "Un sito di phishing può utilizzare HTTPS e avere comunque contenuti malevoli.")
        ),

        SecurityGuideItem(
            title = "Android Security",
            explanation = "Android applica diversi livelli di protezione, tra cui sandbox, permessi e isolamento dei processi.",
            tips = listOf("La sandbox limita l'accesso di un'app alle risorse di altre app.",
                "I permessi controllano l'accesso a risorse e funzionalità sensibili.",
                "I componenti Android devono essere esposti solo quando necessario.",
                "Applica il principio del least privilege: concedi solo ciò che serve.")
        ),
        SecurityGuideItem(
            title = "Security Best Practices", explanation = "La sicurezza quotidiana dipende da più comportamenti semplici applicati insieme.",
            tips = listOf("Mantieni aggiornati sistema operativo e applicazioni.", "Utilizza MFA quando disponibile.",
                "Proteggi le tue credenziali e non condividerle inutilmente.",
                "Fermati e verifica prima di aprire link o allegati sospetti.")))
}