package com.cochat.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cochat.app.data.model.Attachment
import com.cochat.app.ui.theme.AwayAmber
import com.cochat.app.ui.theme.IndigoLight
import com.cochat.app.ui.theme.OfflineGray
import com.cochat.app.ui.theme.OnlineGreen

fun statusColor(status: String?): Color = when (status) {
    "online" -> OnlineGreen
    "away" -> AwayAmber
    else -> OfflineGray
}

@Composable
fun Avatar(imageUrl: String?, name: String, status: String? = null, size: Int = 44) {
    Box(modifier = Modifier.size(size.dp)) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(IndigoLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(name), fontWeight = FontWeight.Bold, fontSize = (size * 0.36).sp)
            }
        }
        if (status != null) {
            Box(
                modifier = Modifier
                    .size((size * 0.3).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(statusColor(status)),
            )
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
fun MessageBubble(
    text: String,
    isOwn: Boolean,
    time: String,
    senderName: String? = null,
    attachment: Attachment? = null,
    attachmentType: String = "text",
    onOpenAttachment: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
    ) {
        if (senderName != null && !isOwn) {
            Text(senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(start = 6.dp, bottom = 2.dp))
        }
        Surface(
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = if (isOwn) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (attachment != null) {
                    FilePreview(attachment, attachmentType, onOpenAttachment)
                    if (text.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                if (text.isNotBlank()) {
                    Text(text, color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    time,
                    fontSize = 10.sp,
                    color = if (isOwn) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun FilePreview(attachment: Attachment, type: String, onOpen: (String) -> Unit) {
    when (type) {
        "image" -> AsyncImage(
            model = attachment.url,
            contentDescription = attachment.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .widthIn(max = 220.dp)
                .heightIn(max = 220.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { onOpen(attachment.url) },
        )
        else -> Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.05f))
                .clickable { onOpen(attachment.url) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(attachment.fileName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(formatBytes(attachment.size), fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
        }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

@Composable
fun LoadingState(text: String = "Loading…") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(text, color = Color.Gray)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}
