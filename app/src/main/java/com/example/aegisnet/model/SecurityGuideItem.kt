package com.example.aegisnet.model

//Singolo elemento statico della Security Guide
data class SecurityGuideItem(
    val title: String,
    val explanation: String,
    val tips: List<String>)