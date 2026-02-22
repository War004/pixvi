package com.cryptic.pixvi.notification

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cryptic.pixvi.database.notification.NotifType
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel
){
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationContent(
        listState = listState,
        uiState = uiState,
        uiEventFlow = viewModel.uiEvent,
        onClearAll = { /* TODO */ },
        onNotificationAction = { it->
            viewModel.notificationAction(it)
        }
    )
}

/**
 * Stateless notification content composable.
 * Takes UI state directly for testability and preview support.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationContent(
    listState: NotificationListState,
    uiState: NotificationUiState,
    uiEventFlow: Flow<NotificaitonUiEvent>,
    onClearAll: () -> Unit,
    onNotificationAction: (NotificationAction) -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiEventFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            uiEventFlow.collect { event ->
                when (event) {
                    is NotificaitonUiEvent.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    IconButton(
                        onClick = onClearAll
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear all eligible notification"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val notificationGroup = listOf(
                NotifType.DOWNLOAD,
                NotifType.SYSTEM,
                NotifType.AUTHOR,
                NotifType.PIXIV
            )

            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                notificationGroup.forEachIndexed { index, type ->
                    ToggleButton(
                        checked = uiState.notificationView == type,
                        onCheckedChange = {
                            onNotificationAction(NotificationAction.ChangeView(choice = type))
                        },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            notificationGroup.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(text = type.displayName)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ){
                AssistChip(
                    onClick = {},
                    label = { Text("Image") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Pdf") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Gif") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Html") }
                )
            }

            // Downloads LazyColumn
            LazyColumn(
                modifier = Modifier.padding(12.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ongoing downloads section
                if (listState.ongoingDownloadNotification.isNotEmpty()) {
                    item {
                        Text(
                            text = "In Progress",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = listState.ongoingDownloadNotification,
                        key = { "ongoing_${it.fileName}_${it.time}" }
                    ) { notification ->
                        if (notification.hasForcedToStopped || notification.errorCode != null) {
                            ErrorNotificationCard(
                                fileName = notification.fileName,
                                formattedData = notification.formattedTime,
                                time = notification.time,
                                totalItems = notification.totalItems,
                                totalSuccess = notification.totalSuccess,
                                onClear = { /* inactive */ },
                                onRetry = { /* inactive */ },
                                errorCode = notification.errorCode ?: 0,
                                errorMessage = notification.errorMessage ?: "Download cancelled"
                            )
                        } else {
                            val progress = if (notification.totalItems > 0) {
                                notification.totalSuccess.toFloat() / notification.totalItems
                            } else 0f

                            ProcessingNotificationCard(
                                fileName = notification.fileName,
                                formattedData = notification.formattedTime,
                                isWorking = true,
                                totalItem = notification.totalItems,
                                totalSuccess = notification.totalSuccess,
                                totalFailed = notification.totalFailed,
                                totalProgress = progress,
                                onPause = { /* inactive */ },
                                onCancel = { /* inactive */ },
                                onRetry = { /* inactive */ }
                            )
                        }
                    }
                }

                // Completed downloads section
                if (listState.completedDownloadNotification.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = listState.completedDownloadNotification,
                        key = { "completed_${it.databaseId}" }
                    ) { notification ->

                        //if pdf rendering
                        if(notification.needsPdfRendering == true){
                            //check the pdf necceasry details
                            //if()
                            PdfRenderingCard(
                                databaseId = notification.databaseId,
                                fileName = notification.fileName,
                                savedToFolder = notification.savedFolder,
                                formattedData = notification.formattedTime,
                                onNotificationActions = {
                                    onNotificationAction(it)
                                },
                                startIndex = notification.startIndex!!,
                                endIndex = notification.endIndex!!,
                                pdfSessionId = notification.pdfSessionId!!
                            )
                        }
                        else{
                            CompletedNotificationCard(
                                fileName = notification.fileName,
                                formattedData = notification.formattedTime,
                                fileDirectUri = notification.directFileUri,
                                fileSaveFolderPath = notification.savedFolder,
                                fileFormatType = notification.mediaType,
                                onClear = { /* inactive */ },
                                onFolderLongPress = { /* inactive */ },
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class NotificationType(val displayName: String){

    DOWNLOADS("Download"),SYSTEM("System"),AUTHOR("Author"),PIXIV("Pixiv")

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompletedNotificationCard(
    fileName: String,
    formattedData: String,
    fileDirectUri: String?,
    fileSaveFolderPath: String?,
    fileFormatType: Int,
    onClear: (Int) -> Unit,
    onFolderLongPress:(Uri) -> Unit,
){
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)
        ){
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                            fontWeight = FontWeight.Bold
                        )) {
                            append("$fileName • ")
                        }
                        withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(
                            fontWeight = FontWeight.Light,
                            baselineShift = BaselineShift(0.15f)
                        )) {
                            append(formattedData)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge // ensure consistent baseline
                )
                Text(
                    modifier = Modifier.combinedClickable(
                        onClick = {/*Do Nothing*/},

                        onLongClick = {onFolderLongPress}
                    ),
                    text = "Saved to ${fileSaveFolderPath ?:"Unknown Folder"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light
                )
            }
            //
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                ){
                    /*
                    Text(
                        text = "34/34/5678",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light
                    )*/
                }
                Row(){
                    //0=image,1=pdf,2=gif,3=html
                    IconButton(
                        onClick = {
                            if(!fileDirectUri.isNullOrEmpty()){
                                val fileUri = fileDirectUri.toUri()
                                val fileType = when(fileFormatType){
                                    3 -> "image/gif" //need to verfiy if the animation in pixiv is mp4 or gif
                                    0 -> "image/*"
                                    1 -> "application/pdf"
                                    2 -> "text/html"
                                    else ->{
                                        Toast.makeText(context, "Can not open the file",Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                }

                                //launch the intent
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, fileType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }

                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "No app can open this type of files.",Toast.LENGTH_SHORT).show()
                                }
                            }
                            else{
                                Toast.makeText(context, "Can't find the files",Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = IconButtonDefaults.largeRoundShape
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Default.OpenInNew, null)
                    }

                    IconButton(
                        onClick = {
                            onClear
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,null
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CardPreview(){
    PdfRenderingCard(
        fileName = "MyName.pdf",
        savedToFolder = "Downloads/Clock",
        formattedData = "12/03/2026",
        onNotificationActions = {},
        startIndex = 0,
        endIndex = 12,
        pdfSessionId = "afwef",
        databaseId = 34
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PdfRenderingCard(
    databaseId: Int,
    fileName: String,
    savedToFolder: String,
    startIndex: Int,
    endIndex: Int,
    pdfSessionId: String,
    formattedData: String,
    onNotificationActions: (NotificationAction) -> Unit
){
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)
        ){
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                            fontWeight = FontWeight.Bold
                        )) {
                            append("$fileName • ")
                        }
                        withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(
                            fontWeight = FontWeight.Light,
                            baselineShift = BaselineShift(0.15f)
                        )) {
                            append(formattedData)
                        }
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge // ensure consistent baseline
                )
                Text(
                    modifier = Modifier.combinedClickable(
                        onClick = {/*Do Nothing*/},

                        onLongClick = {
                            //Do something releated to folder
                        }
                    ),
                    text = "Saved to $savedToFolder",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light
                )
            }
            //
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                ){
                    /*
                    Text(
                        text = "34/34/5678",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light
                    )*/
                }
                Row(){

                    IconButton(
                        onClick = {
                            //launch the pdf page.
                        },
                        shape = IconButtonDefaults.largeRoundShape
                    ) {
                        Icon(imageVector = Icons.Outlined.Print, null)
                    }

                    IconButton(
                        onClick = {
                            onNotificationActions(
                                NotificationAction.SavePdf(
                                    databaseId = databaseId,
                                    startIndex = startIndex,
                                    endIndex = endIndex,
                                    postTitle = fileName,
                                    postSessionId = pdfSessionId,
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessingNotificationCard(
    fileName: String,
    formattedData: String,
    isWorking: Boolean,
    totalItem: Int,
    totalSuccess: Int,
    totalFailed: Int,
    totalProgress: Float,
    onPause: (Int) -> Unit,
    onCancel:(Int) -> Unit,
    onRetry:(Int)-> Unit
){
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()//.height(70.dp)//.padding(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)
        ){
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                            fontWeight = FontWeight.Bold
                        )) {
                            append("$fileName • ")
                        }
                        withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(
                            fontWeight = FontWeight.Light,
                            baselineShift = BaselineShift(0.15f)
                        )) {
                            append(formattedData)
                        }
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge // ensure consistent baseline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = if(isWorking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                    IconButton(
                        enabled = !isWorking,
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            null
                        )
                    }
                    IconButton(
                        enabled = !isWorking,
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            null
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = {
                        totalProgress
                    }
                )
            }
            //
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Downloading...",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(text = "S: $totalSuccess/$totalItem")
                Text(text = "E: $totalFailed/$totalItem")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ErrorNotificationCard(
    fileName: String,
    formattedData: String,
    time: Long,
    totalItems: Int,
    totalSuccess: Int,
    onClear: (Int) -> Unit,
    onRetry: () -> Unit,
    errorCode: Int,
    errorMessage: String
){
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()//.height(70.dp)//.padding(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min)
        ){

            Column(
                modifier = Modifier.fillMaxHeight().weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween
            ){
                Row(){
                    Icon(
                        imageVector = Icons.Default.Warning,
                        tint = Color.Red,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
                            )) {
                                append("$fileName • ")
                            }
                            withStyle(style = MaterialTheme.typography.bodySmall.toSpanStyle().copy(
                                fontWeight = FontWeight.Light,
                                baselineShift = BaselineShift(0.15f)
                            )) {
                                append(formattedData)
                            }
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge // ensure consistent baseline
                    )
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Code: $errorCode", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Message: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        enabled = true,
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            null
                        )
                    }
                }
            }
            //
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cancelled",
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.ClearAll, null
                    )
                }

                Text(text = "S: $totalSuccess/$totalItems")

            }
        }
    }
}
//requirement for download
/*
Filename, button to open, timestamp
 */
/*
sealed interface NotificationType{
    data object System: NotificationType
    data object Author: NotificationType
    data object Download: NotificationType
    /*
    sealed interface Download: NotificationType{
        data class Image(val fileName: String, val fileUri: Uri?)
        data class Manga(val fileName: String, val fileUri: Uri?, val processed: Boolean) //processed mean, if the manga(pages with more then one page is printed as pdf or not)
        data class Novel(val fileName: String, val fileUri: Uri?, val processed: Boolean)
    }*/
}*/