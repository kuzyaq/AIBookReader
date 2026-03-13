package com.example.aibookreader.domain.service

import com.example.aibookreader.domain.model.ParsedEpubData

interface EpubParser {
    suspend fun parse(filePath: String): ParsedEpubData
}