package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Deck
import com.example.ui.theme.ActiveGreenHighlight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.HeaderGreenSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.NeutralCardBackground
import com.example.ui.theme.NeutralCardBorder
import com.example.ui.theme.SemiDarkGreen
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SurfaceControl
import com.example.ui.viewmodel.FlashcardViewModel
import kotlinx.coroutines.launch

private val MODE_ITEMS = listOf(
    "A" to R.string.mode_a,
    "B" to R.string.mode_b,
    "random" to R.string.mode_mix
)

@Composable
fun FlashcardScreen(
    viewModel: FlashcardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showHelpDialog by remember { mutableStateOf(false) }

    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val activeDeck by viewModel.activeDeck.collectAsStateWithLifecycle()
    val activeCards by viewModel.activeCards.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val showingSide by viewModel.showingSide.collectAsStateWithLifecycle()
    val startMode by viewModel.startMode.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    LaunchedEffect(importError) {
        importError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearImportError()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
            coroutineScope.launch { drawerState.close() }
        }
    }

    val openFilePicker = remember(filePickerLauncher) {
        { filePickerLauncher.launch(arrayOf("*/*")) }
    }

    val cardPositionText by remember(currentIndex, activeCards) {
        derivedStateOf {
            if (activeCards.isNotEmpty()) "${currentIndex + 1} / ${activeCards.size}" else null
        }
    }

    val activeCardText by remember(activeCards, currentIndex, showingSide) {
        derivedStateOf {
            val card = activeCards.getOrNull(currentIndex)
            if (card != null && showingSide in card.sides.indices) card.sides[showingSide] else ""
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            FlashcardDrawerSheet(
                decks = decks,
                activeDeckId = activeDeck?.id,
                onLoadClick = openFilePicker,
                onSelectDeck = { deck ->
                    viewModel.selectDeck(deck)
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteDeck = viewModel::deleteDeck,
                onOpenHelp = {
                    coroutineScope.launch { drawerState.close() }
                    showHelpDialog = true
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                FlashcardTopBar(
                    activeDeckName = activeDeck?.name,
                    cardPositionText = cardPositionText,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                )
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeSelectorRow(
                    startMode = startMode,
                    onSelectMode = viewModel::setMode
                )

                FlashcardBox(
                    hasCards = activeCards.isNotEmpty(),
                    cardText = activeCardText,
                    onClick = {
                        if (activeCards.isEmpty()) openFilePicker() else viewModel.flipCard()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                )

                FlashcardNavRow(
                    hasCards = activeCards.isNotEmpty(),
                    onPrev = viewModel::prevCard,
                    onFlip = viewModel::flipCard,
                    onNext = viewModel::nextCard
                )
            }
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun FlashcardDrawerSheet(
    decks: List<Deck>,
    activeDeckId: String?,
    onLoadClick: () -> Unit,
    onSelectDeck: (Deck) -> Unit,
    onDeleteDeck: (String) -> Unit,
    onOpenHelp: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DarkBackground,
        modifier = Modifier.width(300.dp)
    ) {
        Surface(
            color = HeaderGreenSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = SemiDarkGreen.copy(alpha = 0.65f)
                )
            }
        }

        DrawerActionButton(
            iconRes = R.drawable.ic_folder_open,
            label = stringResource(R.string.action_load),
            testTag = "btn_load",
            onClick = onLoadClick
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemiDarkGreen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_library_books),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.loaded_decks_header),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (decks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = stringResource(R.string.empty_decks_message),
                    color = MutedText,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("file_list")
            ) {
                items(
                    items = decks,
                    key = { it.id },
                    contentType = { "deck_item" }
                ) { deck ->
                    val isActive = activeDeckId == deck.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) ActiveGreenHighlight else Color.Transparent)
                            .clickable { onSelectDeck(deck) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deck.name,
                            color = if (isActive) SoftMint else Color.White,
                            fontSize = 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDeleteDeck(deck.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete_deck),
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = SemiDarkGreen.copy(alpha = 0.65f)
        )
        DrawerActionButton(
            iconRes = R.drawable.ic_help_outline,
            label = stringResource(R.string.action_how_it_works),
            testTag = "btn_help",
            onClick = onOpenHelp
        )
    }
}

@Composable
private fun DrawerActionButton(
    iconRes: Int,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceControl),
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = SoftMint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FlashcardTopBar(
    activeDeckName: String?,
    cardPositionText: String?,
    onOpenDrawer: () -> Unit
) {
    Surface(
        color = HeaderGreenSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenDrawer)
                        .padding(vertical = 6.dp)
                        .testTag("header_open_drawer"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconBadge()
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeDeckName != null) {
                            Text(
                                text = activeDeckName,
                                color = SoftMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("active_file_name")
                            )
                        }
                    }
                }

                if (cardPositionText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SemiDarkGreen.copy(alpha = 0.28f))
                            .border(1.dp, SemiDarkGreen, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("card_counter")
                    ) {
                        Text(
                            text = cardPositionText,
                            color = SoftMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = SemiDarkGreen.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun AppIconBadge() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(SemiDarkGreen),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_fg_cards),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ModeSelectorRow(
    startMode: String,
    onSelectMode: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MODE_ITEMS.forEach { (mode, labelRes) ->
            val isSelected = startMode == mode
            Button(
                onClick = { onSelectMode(mode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) SemiDarkGreen else SurfaceControl
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .testTag("btn_mode_$mode")
            ) {
                Text(
                    text = stringResource(labelRes),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FlashcardBox(
    hasCards: Boolean,
    cardText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("flashcard_clickable"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralCardBackground),
        border = BorderStroke(2.dp, NeutralCardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasCards) cardText else stringResource(R.string.empty_card_prompt),
                color = Color.White,
                fontSize = if (hasCards) 28.sp else 22.sp,
                fontWeight = if (hasCards) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("flashcard_text")
            )
        }
    }
}

@Composable
private fun FlashcardNavRow(
    hasCards: Boolean,
    onPrev: () -> Unit,
    onFlip: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = if (hasCards) Color.White else Color.White.copy(alpha = 0.4f)

        Button(
            onClick = onPrev,
            enabled = hasCards,
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceControl,
                disabledContainerColor = SurfaceControl.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(0.35f)
                .height(72.dp)
                .testTag("btn_prev")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_previous),
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }

        Button(
            onClick = onFlip,
            enabled = hasCards,
            colors = ButtonDefaults.buttonColors(
                containerColor = SemiDarkGreen,
                disabledContainerColor = SurfaceControl.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(0.25f)
                .height(72.dp)
                .testTag("btn_flip")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.action_flip),
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }

        Button(
            onClick = onNext,
            enabled = hasCards,
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceControl,
                disabledContainerColor = SurfaceControl.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(0.35f)
                .height(72.dp)
                .testTag("btn_next")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.action_next),
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.help_dialog_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.help_usage_body),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .border(1.dp, SemiDarkGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.help_format_title),
                        color = SoftMint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.help_format_cols),
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = SemiDarkGreen.copy(alpha = 0.4f)
                    )
                    Text(
                        text = stringResource(R.string.help_format_example),
                        color = SoftMint.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.help_dialog_confirm),
                    color = SoftMint,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = HeaderGreenSurface,
        textContentColor = Color.White,
        titleContentColor = Color.White
    )
}
