package com.aegis.vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aegis.vault.core.ServiceLocator
import com.aegis.vault.data.model.ItemType
import com.aegis.vault.ui.components.AegisFab
import com.aegis.vault.ui.components.AegisTabBar
import com.aegis.vault.ui.components.TabItem
import com.aegis.vault.ui.icons.AegisIcon
import com.aegis.vault.ui.screens.AboutScreen
import com.aegis.vault.ui.screens.AutoLockScreen
import com.aegis.vault.ui.screens.CardDetailScreen
import com.aegis.vault.ui.screens.CategoryScreen
import com.aegis.vault.ui.screens.ChangeMasterPasswordScreen
import com.aegis.vault.ui.screens.CreateMasterPasswordScreen
import com.aegis.vault.ui.screens.ExportImportScreen
import com.aegis.vault.ui.screens.GeneratorScreen
import com.aegis.vault.ui.screens.HelpScreen
import com.aegis.vault.ui.screens.InfoSheetScreen
import com.aegis.vault.ui.screens.ItemEditorScreen
import com.aegis.vault.ui.screens.LoginDetailScreen
import com.aegis.vault.ui.screens.NoteDetailScreen
import com.aegis.vault.ui.screens.OnboardingScreen
import com.aegis.vault.ui.screens.SearchScreen
import com.aegis.vault.ui.screens.SecurityScreen
import com.aegis.vault.ui.screens.SettingsScreen
import com.aegis.vault.ui.screens.UnlockScreen
import com.aegis.vault.ui.screens.VaultScreen

/** All navigation routes of the app. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val CREATE_MASTER = "createMaster"
    const val UNLOCK = "unlock"

    const val TAB_VAULT = "tab/vault"
    const val TAB_GENERATOR = "tab/generator"
    const val TAB_SECURITY = "tab/security"
    const val TAB_SETTINGS = "tab/settings"

    const val CATEGORY = "category/{itemType}"
    fun category(type: ItemType) = "category/${type.name}"

    const val LOGIN_DETAIL = "login/{id}"
    fun loginDetail(id: String) = "login/$id"

    const val CARD_DETAIL = "card/{id}"
    fun cardDetail(id: String) = "card/$id"

    const val NOTE_DETAIL = "note/{id}"
    fun noteDetail(id: String) = "note/$id"

    const val EDITOR = "editor?itemType={itemType}&id={id}"
    fun editor(type: ItemType, id: String = "") = "editor?itemType=${type.name}&id=$id"

    const val SEARCH = "search"

    const val ABOUT = "about"
    const val HELP = "help"
    const val CHANGE_MASTER = "changeMaster"
    const val AUTOLOCK = "autolock"
    const val IMPORT_EXPORT = "importExport"
    const val INFO_TWO_FACTOR = "info/twoFactor"
    const val INFO_SHARED = "info/shared"
    const val INFO_STORAGE = "info/storage"
}

private val TABS = listOf(
    TabItem("Passwords", AegisIcon.KeyRound),
    TabItem("Generator", AegisIcon.RefreshCw),
    TabItem("Security", AegisIcon.ShieldCheck),
    TabItem("Settings", AegisIcon.Settings),
)

private val TAB_ROUTES = listOf(
    Routes.TAB_VAULT,
    Routes.TAB_GENERATOR,
    Routes.TAB_SECURITY,
    Routes.TAB_SETTINGS,
)

@Composable
fun AegisNavHost() {
    val context = LocalContext.current
    val locator = remember(context) { ServiceLocator.from(context) }
    val navController = rememberNavController()

    val startDestination = remember {
        when {
            !locator.masterPasswordManager.hasMasterPassword -> Routes.ONBOARDING
            locator.sessionHolder.isUnlocked -> Routes.TAB_VAULT
            else -> Routes.UNLOCK
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // The mockups keep the tab bar on every screen inside the vault (tabs, category
    // lists, detail screens, search) and hide it only on the pre-vault auth screens.
    val authRoutes = listOf(Routes.ONBOARDING, Routes.CREATE_MASTER, Routes.UNLOCK)
    val showTabBar = currentRoute != null && currentRoute !in authRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onCreateMasterPassword = { navController.navigate(Routes.CREATE_MASTER) },
                    onUnlockExisting = { navController.navigate(Routes.UNLOCK) },
                )
            }

            composable(Routes.CREATE_MASTER) {
                CreateMasterPasswordScreen(
                    locator = locator,
                    onReady = {
                        navController.navigate(Routes.TAB_VAULT) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.UNLOCK) {
                UnlockScreen(
                    locator = locator,
                    onUnlocked = {
                        navController.navigate(Routes.TAB_VAULT) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    itemCount = locator.vaultRepository.runCatching { stats().total }.getOrDefault(0),
                )
            }

            composable(Routes.TAB_VAULT) {
                VaultScreen(
                    locator = locator,
                    onOpenCategory = { type -> navController.navigate(Routes.category(type)) },
                    onOpenLogin = { id -> navController.navigate(Routes.loginDetail(id)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onNewItem = { navController.navigate(Routes.editor(ItemType.Login)) },
                )
            }

            composable(Routes.TAB_GENERATOR) {
                GeneratorScreen(locator = locator)
            }

            composable(Routes.TAB_SECURITY) {
                SecurityScreen(
                    locator = locator,
                    onOpenLogin = { id -> navController.navigate(Routes.loginDetail(id)) },
                )
            }

            composable(Routes.TAB_SETTINGS) {
                SettingsScreen(
                    locator = locator,
                    onLockNow = {
                        locator.sessionHolder.lock()
                        locator.closeVault()
                        navController.navigate(Routes.UNLOCK) {
                            popUpTo(Routes.TAB_VAULT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAbout = { navController.navigate(Routes.ABOUT) },
                    onHelp = { navController.navigate(Routes.HELP) },
                    onChangeMasterPassword = { navController.navigate(Routes.CHANGE_MASTER) },
                    onAutoLock = { navController.navigate(Routes.AUTOLOCK) },
                    onTwoFactorInfo = { navController.navigate(Routes.INFO_TWO_FACTOR) },
                    onSharedItemsInfo = { navController.navigate(Routes.INFO_SHARED) },
                    onStorageInfo = { navController.navigate(Routes.INFO_STORAGE) },
                    onImportExport = { navController.navigate(Routes.IMPORT_EXPORT) },
                )
            }

            composable(Routes.CATEGORY) { entry ->
                val type = ItemType.fromRoute(entry.arguments?.getString("itemType").orEmpty())
                CategoryScreen(
                    locator = locator,
                    itemType = type,
                    onOpenLogin = { id -> navController.navigate(Routes.loginDetail(id)) },
                    onOpenCard = { id -> navController.navigate(Routes.cardDetail(id)) },
                    onOpenNote = { id -> navController.navigate(Routes.noteDetail(id)) },
                    onOpenIdentity = { id -> navController.navigate(Routes.editor(ItemType.Identity, id)) },
                    onNewItem = { navController.navigate(Routes.editor(type)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.LOGIN_DETAIL) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LoginDetailScreen(
                    locator = locator,
                    loginId = id,
                    onEdit = { navController.navigate(Routes.editor(ItemType.Login, id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.CARD_DETAIL) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                CardDetailScreen(
                    locator = locator,
                    cardId = id,
                    onEdit = { navController.navigate(Routes.editor(ItemType.Card, id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.NOTE_DETAIL) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                NoteDetailScreen(
                    locator = locator,
                    noteId = id,
                    onEdit = { navController.navigate(Routes.editor(ItemType.Note, id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.EDITOR) { entry ->
                val type = ItemType.fromRoute(entry.arguments?.getString("itemType").orEmpty())
                val id = entry.arguments?.getString("id").orEmpty()
                ItemEditorScreen(
                    locator = locator,
                    itemType = type,
                    itemId = id,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    locator = locator,
                    onOpenLogin = { id -> navController.navigate(Routes.loginDetail(id)) },
                    onOpenCard = { id -> navController.navigate(Routes.cardDetail(id)) },
                    onOpenNote = { id -> navController.navigate(Routes.noteDetail(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.IMPORT_EXPORT) {
                ExportImportScreen(
                    locator = locator,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.HELP) { HelpScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.CHANGE_MASTER) {
                ChangeMasterPasswordScreen(
                    locator = locator,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.AUTOLOCK) {
                AutoLockScreen(
                    locator = locator,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.INFO_TWO_FACTOR) {
                InfoSheetScreen(
                    title = "Two-factor auth",
                    body = "Aegis stores TOTP-style second factors per login. Aegis is offline by design, " +
                        "so it does not act as an authenticator app on its own — it keeps the " +
                        "two-factor status of each login next to its credentials, and the Security tab " +
                        "reports which logins still lack one.",
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.INFO_SHARED) {
                InfoSheetScreen(
                    title = "Shared items",
                    body = "Because nothing leaves this device, there is nothing to share into a cloud. " +
                        "Item sharing happens as encrypted exports you send yourself — see " +
                        "Import & export in Settings.",
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.INFO_STORAGE) {
                InfoSheetScreen(
                    title = "Storage",
                    body = "The vault is a single SQLCipher-encrypted database in Aegis' private storage. " +
                        "Backups are written by you from Import & export — Android never uploads it.",
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Bottom chrome: the tab bar on tab routes, plus the floating add button
        // (the mockups' stray purple dock FAB, resolved to the app gradient).
        if (showTabBar) {
            AegisTabBar(
                tabs = TABS,
                selectedIndex = currentRoute.toTabIndex(),
                onSelect = { index -> navController.navigateTab(index) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }

        if (currentRoute == Routes.TAB_VAULT) {
            AegisFab(
                onClick = { navController.navigate(Routes.editor(ItemType.Login)) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
                contentDescription = "Add item",
            )
        }
    }
}

private fun String?.toTabIndex(): Int = when (this) {
    Routes.TAB_VAULT -> 0
    Routes.TAB_GENERATOR -> 1
    Routes.TAB_SECURITY -> 2
    Routes.TAB_SETTINGS -> 3
    else -> 0
}

private fun NavHostController.navigateTab(index: Int) {
    val route = TAB_ROUTES.getOrElse(index) { Routes.TAB_VAULT }
    navigate(route) {
        popUpTo(Routes.TAB_VAULT) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
