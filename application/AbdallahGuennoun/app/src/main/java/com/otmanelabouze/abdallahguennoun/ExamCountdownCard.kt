package com.otmanelabouze.abdallahguennoun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun ExamCountdownCard(lang:String,level:String?) {
    // Do not compose a panel or run a clock for common-core or unloaded profiles.
    if(level!="1BAC" && level!="2BAC")return
    val now by produceState(Instant.now(),level) {
        while(true) {
            value=Instant.now()
            delay(60_000-Math.floorMod(value.toEpochMilli(),60_000L))
        }
    }
    val date=examDate(level,examYearAt(now))
    val title=if(level=="1BAC")appText(lang,"العد التنازلي للامتحان الجهوي","Compte à rebours — examen régional","Regional exam countdown")
        else appText(lang,"العد التنازلي للامتحان الوطني","Compte à rebours — examen national","National exam countdown")
    CampusPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,fontWeight=FontWeight.Bold)
            if(date==null) {
                Text(appText(lang,"موعد هذا الموسم غير متوفر بعد.","La date de cette année n’est pas encore disponible.","This school year’s date is not available yet."),style=MaterialTheme.typography.bodySmall)
            } else {
                val remaining=examRemaining(date,now)
                Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.forLanguageTag(lang))),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)
                if(remaining.started) {
                    Text(appText(lang,"حلّ موعد الدورة العادية.","La session normale a commencé.","The regular session date has arrived."),color=MaterialTheme.colorScheme.primary)
                } else {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        listOf(remaining.days to appText(lang,"أيام","Jours","Days"),remaining.hours to appText(lang,"ساعات","Heures","Hours"),remaining.minutes to appText(lang,"دقائق","Minutes","Minutes")).forEach {(number,label)->
                            Surface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f)) {
                                Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(number.toString().padStart(2,'0'),fontSize=27.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                                    Text(label,fontSize=11.sp)
                                }
                            }
                        }
                    }
                    Text(appText(lang,"حتى بداية يوم الامتحان، بتوقيت المغرب — الدورة العادية.","Jusqu’au début du jour de l’examen, heure du Maroc — session normale.","Until the start of exam day in Morocco — regular session."),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
