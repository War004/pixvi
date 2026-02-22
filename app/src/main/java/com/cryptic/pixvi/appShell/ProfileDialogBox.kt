package com.cryptic.pixvi.appShell

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.cryptic.pixvi.core.network.model.artwork.Request

@Composable
fun ProfileDialogBox(
    profileUrl: String?,
    userName: String, //acutal name
    userid: String, //@....
    notificationValue: Int?,
    onNotificationPress:() ->Unit,
    onDismissRequest: () -> Unit
    //dialogAction...
){
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ){
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(

                ){
                    if(profileUrl.isNullOrBlank() || profileUrl == "https://s.pximg.net/common/images/no_profile.png"){
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null
                        )
                    }
                    else{
                        AsyncImage(
                            model = profileUrl,
                            contentDescription = null,
                            modifier = Modifier.clip(RoundedCornerShape(50.dp)).size(38.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 7.dp))
                    //Another column
                    Column {
                        Text(text = userName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "@$userid", style = MaterialTheme.typography.bodyMedium)
                        //divider
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth())

                Row(
                    modifier = Modifier
                        .clickable(
                        onClick = {
                            onDismissRequest()
                            onNotificationPress()
                        }
                    )
                        .fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(imageVector = Icons.Default.Notifications, null)
                    Spacer(modifier = Modifier.padding(horizontal = 7.dp))
                    Text(
                        text = "Notification",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                //Manage account
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(imageVector = Icons.AutoMirrored.Default.OpenInNew, null)
                    Spacer(modifier = Modifier.padding(horizontal = 7.dp))
                    Text(
                        text = "Manage Your Pixiv Account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}