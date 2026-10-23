package com.thusee.cake.ui.cake

import android.content.res.Configuration
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.thusee.cake.R
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.ui.theme.CakeTheme

@Composable
fun CakeRoute() {
    val viewModel: CakeViewModel = viewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected by viewModel.selectedCake.collectAsStateWithLifecycle()
    CakeScreen(
        state = state,
        selectedCake = selected,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onSelect = viewModel::selectCake,
        onDismiss = viewModel::dismissCake,
    )
}

@Composable
fun CakeScreen(
    state: CakeUiState,
    selectedCake: Cake?,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (Cake) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CakeContent(state = state,
        onRefresh = onRefresh,
        onRetry = onRefresh,
        onSelect = onSelect,
        modifier = modifier)
    selectedCake?.let { cake ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(cake.title) },
            text = { Text(cake.description.ifEmpty { stringResource(R.string.no_description) }) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CakeContent(
    state: CakeUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (Cake) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val entered = remember { mutableSetOf<String>() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onRefresh, enabled = state != CakeUiState.Loading &&
                        (state !is CakeUiState.Content || !state.isRefreshing)) {
                        Text(stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.fillMaxSize()
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text(
                        stringResource(R.string.collection_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.collection_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.collection_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                when (state) {
                    CakeUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                    is CakeUiState.Failure -> ErrorNotice(state.error, onRetry)
                    CakeUiState.Empty -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.empty_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(stringResource(R.string.empty_body))
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.refresh))
                        }
                    }

                    is CakeUiState.Content -> {
                        if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        state.refreshError?.let { ErrorNotice(it, onRetry, compact = true) }
                        val cakes = state.cakes
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(cakes.size, key = { cakes[it].id }, contentType = { "cake" }) { index ->
                                val cake = cakes[index]
                                var visible by remember(cake.id) {
                                    mutableStateOf(cake.id in entered)
                                }
                                LaunchedEffect(cake.id) {
                                    entered.add(cake.id)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible,
                                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) {
                                    Column {
                                        CakeRow(cake, { onSelect(cake) })
                                        if (index < cakes.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CakeRow(
    cake: Cake,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.show_description),
                onClick = onClick
            )
            .padding(
                horizontal = 24.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SubcomposeAsyncImage(
            model = cake.imageUrl, contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop,
            loading = { ImagePlaceholder() },
            error = { ImagePlaceholder() },
        )
        Column(Modifier.weight(1f)) {
            Text(
                cake.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.view_details), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ImagePlaceholder() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.image_placeholder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorNotice(
    error: CakeUiError,
    onRetry: () -> Unit,
    compact: Boolean = false
) {
    val message = when (error) {
        CakeUiError.Network -> R.string.error_network
        CakeUiError.Server -> R.string.error_server
        CakeUiError.InvalidData -> R.string.error_data
    }
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.fillMaxHeight())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!compact) Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

private val previewCakes = persistentListOf(
    Cake("Banana Cake", "A soft banana sponge.", ""),
    Cake("Carrot Cake", "Cream cheese icing and a little spice.", ""),
    Cake("Chocolate Cake", "Rich chocolate layers.", ""),
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", showBackground = true, fontScale = 1.5f)
@Composable
private fun CakeContentPreview() {
    CakeTheme {
        CakeContent(
            state = CakeUiState.Content(previewCakes),
            onRetry = {},
            onRefresh = {},
            onSelect = {}
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun CakeErrorPreview() {
    CakeTheme {
        CakeContent(
            state = CakeUiState.Failure(CakeUiError.Network),
            onRefresh = {},
            onRetry = {},
            onSelect = {}
        )
    }
}
