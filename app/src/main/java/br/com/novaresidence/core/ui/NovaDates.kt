package br.com.novaresidence.core.ui

import br.com.novaresidence.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Datas das listas agrupadas por dia (histórico da central e mensagens), no fuso do aparelho.

// O locale padrão é o idioma do app (AppLanguageStore.wrap); lido a cada chamada porque a troca de idioma
// recria a Activity sem reiniciar o processo.
private val Clock = DateTimeFormatter.ofPattern("HH:mm")

/** "Hoje", "Ontem" ou "24 set": cabeçalho dos grupos por dia. */
fun Instant.dayLabel(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone), locale: Locale = Locale.getDefault()): UiText =
    when (val day = atZone(zone).toLocalDate()) {
        today -> uiText(R.string.date_today)
        today.minusDays(1) -> uiText(R.string.date_yesterday)
        else -> day.format(DateTimeFormatter.ofPattern("d MMM", locale)).replace(".", "").asUiText()
    }

fun Instant.clockTime(zone: ZoneId = ZoneId.systemDefault()): String = atZone(zone).format(Clock)

fun Long.clockTime(zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(this).clockTime(zone)

/** "Hoje, 10:30" (detalhe da mensagem, Figma 18). */
fun Instant.dayAndTime(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): UiText =
    uiText(R.string.date_day_time, dayLabel(zone, today), clockTime(zone))
