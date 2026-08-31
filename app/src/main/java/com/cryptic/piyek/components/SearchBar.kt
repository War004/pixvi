package com.cryptic.piyek.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptic.piyek.R

@Composable
/*
    onSearch should have a method, that retakes the focus to false.
 */
fun SearchBar(
    textFieldState: TextFieldState,
    onSearch: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search or enter URL",
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    BasicTextField(
        state = textFieldState,
        scrollState = scrollState,
        modifier = modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = {
            onSearch()
            focusManager.clearFocus()
        },
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        painter = painterResource(R.drawable.filter_icon),
                        contentDescription = "Search filter"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Preview
@Composable
fun SearchBarPreview() {
    SearchBar(
        textFieldState = rememberTextFieldState(initialText = ""),
        onSearch = {},
        onFilterClick = {}
    )
}