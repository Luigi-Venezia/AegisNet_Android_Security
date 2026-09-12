package com.example.aegisnet.model

/* Modello immutabile di una sezione della Security Guide.
   La guida è contenuto statico: non richiede database, rete o Repository
   remoto. Ogni elemento contiene un titolo, una spiegazione e una lista
   di consigli pratici */
data class SecurityGuideItem(
    val title: String,
    val explanation: String,
    val tips: List<String>
)