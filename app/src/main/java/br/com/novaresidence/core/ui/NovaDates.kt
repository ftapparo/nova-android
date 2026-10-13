package br.com.novaresidence.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Datas das listas agrupadas por dia (histórico da central e mensagens), no fuso do aparelho.

private val PtBr = Locale.forLanguageTag("pt-BR")
private val ShortDate = DateTimeFormatter.ofPattern("d MMM", PtBr)
private val Clock = DateTimeFormatter.ofPattern("HH:mm", PtBr)

/** "Hoje", "Ontem" ou "24 set": cabeçalho dos grupos por dia. */
fun Instant.dayLabel(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String =
    when (val day = atZone(zone).toLocalDate()) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> day.format(ShortDate).replace(".", "")
    }

fun Instant.clockTime(zone: ZoneId = ZoneId.systemDefault()): String = atZone(zone).format(Clock)

fun Long.clockTime(zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(this).clockTime(zone)

/** "Hoje, 10:30" (detalhe da mensagem, Figma 18). */
fun Instant.dayAndTime(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String =
    "${dayLabel(zone, today)}, ${clockTime(zone)}"
