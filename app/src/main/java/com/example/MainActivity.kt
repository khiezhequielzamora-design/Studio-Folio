package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.SourceExtractor
import com.example.data.StudyAiService
import com.example.data.StudyRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NotebookWorkspaceScreen
import com.example.ui.theme.StudyFolioTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.WorkspaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_LIBRARY = "library"
const val ROUTE_NOTEBOOK = "notebook/{notebookId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge display and render gated Compose UI
        enableEdgeToEdge()
        setContent {
            StudyFolioTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Scholar"
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIBRARY
    ) {
        composable(ROUTE_LIBRARY) {
            val libraryViewModel: LibraryViewModel = viewModel(
                key = "library_$currentUserId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        LibraryViewModel(
                            repository = StudyRepository(db),
                            currentUserId = currentUserId
                        )
                    }
                }
            )

            LibraryScreen(
                viewModel = libraryViewModel,
                userDisplayName = userDisplayName,
                onOpenNotebook = { notebookId ->
                    navController.navigate("notebook/$notebookId")
                },
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }

        composable(
            route = ROUTE_NOTEBOOK,
            arguments = listOf(navArgument("notebookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getString("notebookId").orEmpty()
            val workspaceViewModel: WorkspaceViewModel = viewModel(
                key = "workspace_${currentUserId}_$notebookId",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val aiService = StudyAiService()
                        val extractor = SourceExtractor(app.applicationContext, aiService)
                        WorkspaceViewModel(
                            repository = StudyRepository(db),
                            aiService = aiService,
                            sourceExtractor = extractor,
                            currentUserId = currentUserId,
                            notebookId = notebookId
                        )
                    }
                }
            )

            NotebookWorkspaceScreen(
                viewModel = workspaceViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
