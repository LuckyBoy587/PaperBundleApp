package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.data.Task
import com.example.data.TaskDao
import com.example.data.SyncState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.core.content.edit

// Stores user details and family session info
data class UserSession(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val familyId: String? = null,
    val familyName: String? = null
)

data class FamilyMember(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String
)

object FirebaseSyncManager {
    private const val TAG = "PAPER_BUNDLE"
    private const val PREFS_NAME = "PaperBundleFirebasePrefs"
    private const val PREDEFINED_FAMILY_ID = "fam_baskaran_home"
    private const val PREDEFINED_FAMILY_NAME = "Baski Home"

    private var appContext: Context? = null

    var isFirebaseInitialized = false
        private set

    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession: MutableStateFlow<UserSession?> get() = _currentUserSession

    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: MutableStateFlow<List<FamilyMember>> get() = _familyMembers

    private var activeListener: ListenerRegistration? = null
    private var memberListener: ListenerRegistration? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // Check if Firebase is configured in BuildConfig
    private val isConfigValid: Boolean
        get() = try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val appId = BuildConfig.FIREBASE_APPLICATION_ID
            val projectId = BuildConfig.FIREBASE_PROJECT_ID

            apiKey.isNotBlank() && !apiKey.contains("YOUR_FIREBASE") &&
                    appId.isNotBlank() && !appId.contains("YOUR_FIREBASE") &&
                    projectId.isNotBlank() && !projectId.contains("YOUR_FIREBASE")
        } catch (_: Exception) {
            false
        }

    fun init(context: Context) {
        Log.d(TAG, "FirebaseSyncManager: init() called: isFirebaseInitialized=$isFirebaseInitialized")
        appContext = context.applicationContext
        if (isFirebaseInitialized) return

        try {
            // 1. Check if Firebase is already initialized automatically by the Google Services Plugin
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isFirebaseInitialized = true
                Log.d(TAG, "FirebaseSyncManager: Firebase has already been initialized automatically (via google-services.json).")
            } else {
                // 2. Try to initialize using the default options (which loads from google-services.json generated resources)
                FirebaseApp.initializeApp(context)
                isFirebaseInitialized = true
                Log.d(TAG, "FirebaseSyncManager: Firebase initialized successfully using default options (google-services.json).")
            }
        } catch (e: Exception) {
            Log.d(TAG, "FirebaseSyncManager: Default Firebase initialization failed or google-services.json missing: ${e.message}. Trying custom BuildConfig fallback.")
            // 3. Fallback to manually building options from BuildConfig if provided
            if (isConfigValid) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .build()

                    FirebaseApp.initializeApp(context, options)
                    isFirebaseInitialized = true
                    Log.d(TAG, "FirebaseSyncManager: Firebase initialized successfully with custom BuildConfig options.")
                } catch (ex: Exception) {
                    Log.e(TAG, "FirebaseSyncManager: Failed to initialize Firebase with custom BuildConfig options", ex)
                    isFirebaseInitialized = false
                }
            } else {
                Log.d(TAG, "FirebaseSyncManager: Firebase custom credentials missing or using placeholders. Starting in high-fidelity Sandbox Mode.")
                isFirebaseInitialized = false
            }
        }

        if (isFirebaseInitialized) {
            try {
                val db = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(100 * 1024 * 1024) // 100 MB Cache Size
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
                Log.d(TAG, "FirebaseSyncManager: Explicit persistent cache (100MB) configured for Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "FirebaseSyncManager: Error configuring Firestore settings", e)
            }
        }

        // Restore session from local SharedPreferences for fast startup (<2s)
        loadSavedSession(context)
    }

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkAvailable(): Boolean {
        val context = appContext ?: return false
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun loadSavedSession(context: Context) {
        Log.d(TAG, "FirebaseSyncManager: loadSavedSession() called")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString("uid", null)
        if (uid != null) {
            val name = prefs.getString("name", "User") ?: "User"
            val email = prefs.getString("email", "") ?: ""
            val photoUrl = prefs.getString("photoUrl", "") ?: ""
            val familyId = prefs.getString("familyId", null)
            val familyName = prefs.getString("familyName", null)

            Log.d(TAG, "FirebaseSyncManager: Saved session found: uid=$uid, name=$name, email=$email, familyId=$familyId, familyName=$familyName")

            val session = UserSession(
                uid = uid,
                name = name,
                email = email,
                photoUrl = photoUrl,
                familyId = familyId,
                familyName = familyName
            )
            _currentUserSession.value = session
            _familyMembers.value = listOf(FamilyMember(uid, name, email, photoUrl))
        } else {
            Log.d(TAG, "FirebaseSyncManager: No saved session found in SharedPreferences.")
        }
    }

    fun saveSession(context: Context, session: UserSession?) {
        _currentUserSession.value = session
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            if (session != null) {
                Log.d(
                    TAG,
                    "FirebaseSyncManager: saveSession() called - Saving session: uid=${session.uid}, name=${session.name}, familyId=${session.familyId}, familyName=${session.familyName}"
                )
                putString("uid", session.uid)
                putString("name", session.name)
                putString("email", session.email)
                putString("photoUrl", session.photoUrl)
                putString("familyId", session.familyId)
                putString("familyName", session.familyName)
                _familyMembers.value =
                    listOf(FamilyMember(session.uid, session.name, session.email, session.photoUrl))
            } else {
                Log.d(TAG, "FirebaseSyncManager: saveSession() called - Clearing session (logout)")
                clear()
                _familyMembers.value = emptyList()
                stopSyncing()
            }
        }
    }

    // Google Login - support real firebase flow & high-fidelity sandbox
    fun authenticateWithGoogle(
        context: Context,
        idToken: String?,
        profileChoiceName: String,
        profileChoiceEmail: String,
        profileChoicePhoto: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "FirebaseSyncManager: authenticateWithGoogle() called: profileChoiceName='$profileChoiceName', hasIdToken=${idToken != null}, isFirebaseInitialized=$isFirebaseInitialized")
        if (isFirebaseInitialized && idToken != null) {
            // Real Firebase authentication using Google Credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        Log.d(TAG, "FirebaseSyncManager: Real Firebase Google Auth successful: uid=${fbUser?.uid}, email=${fbUser?.email}")
                        val session = UserSession(
                            uid = fbUser?.uid ?: UUID.randomUUID().toString(),
                            name = fbUser?.displayName ?: profileChoiceName,
                            email = fbUser?.email ?: profileChoiceEmail,
                            photoUrl = fbUser?.photoUrl?.toString() ?: profileChoicePhoto,
                            familyId = PREDEFINED_FAMILY_ID,
                            familyName = PREDEFINED_FAMILY_NAME
                        )
                        
                        // Predefine the family and add user to it in Firestore
                        val db = FirebaseFirestore.getInstance()
                        val familyRef = db.collection("families").document(PREDEFINED_FAMILY_ID)
                        val familyData = hashMapOf(
                            "familyName" to PREDEFINED_FAMILY_NAME
                        )
                        val memberData = hashMapOf(
                            "uid" to session.uid,
                            "name" to session.name,
                            "email" to session.email,
                            "photoUrl" to session.photoUrl
                        )
                        
                        familyRef.set(familyData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d(TAG, "FirebaseSyncManager: Predefined family registry set/merged successfully.")
                                familyRef.collection("members").document(session.uid).set(memberData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "FirebaseSyncManager: Successfully registered user into predefined family.")
                                        saveSession(context, session)
                                        onComplete(true, null)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "FirebaseSyncManager: Failed to register user into predefined family: ${e.localizedMessage}", e)
                                        onComplete(false, "Failed to register family member: ${e.localizedMessage}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "FirebaseSyncManager: Predefined family registry setup failed: ${e.localizedMessage}", e)
                                onComplete(false, "Failed to initialize family workspace: ${e.localizedMessage}")
                            }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Firebase Sign-In failed"
                        Log.e(TAG, "FirebaseSyncManager: Real Firebase Google Auth failed: $errMsg", task.exception)
                        onComplete(false, errMsg)
                    }
                }
        } else {
            val errorMsg = if (!isFirebaseInitialized) {
                "Firebase is not initialized. Please verify your google-services.json file is present and properly formatted."
            } else {
                "Google Services Web Client ID is not configured (or missing from google-services.json). Sign-in cannot proceed."
            }
            Log.e(TAG, "FirebaseSyncManager: Real Firebase Google Auth failed: $errorMsg")
            onComplete(false, errorMsg)
        }
    }



    // Realtime synchronizer and observer of family members
    fun startSyncing(taskDao: TaskDao) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return

        Log.d(TAG, "FirebaseSyncManager: startSyncing() called for familyId=$familyId, isFirebaseInitialized=$isFirebaseInitialized")
        stopSyncing()

        // Real Firestore syncing begins

        if (isFirebaseInitialized) {
            val db = FirebaseFirestore.getInstance()

            memberListener = db.collection("families").document(familyId).collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "FirebaseSyncManager: Sync observation of members failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        Log.d(TAG, "FirebaseSyncManager: Member snapshot listener triggered: docCount=${snapshot.size()}")
                        val members = snapshot.documents.mapNotNull { doc ->
                            try {
                                val uid = doc.getString("uid") ?: doc.id
                                val name = doc.getString("name") ?: "Member"
                                val email = doc.getString("email") ?: ""
                                val photoUrl = doc.getString("photoUrl") ?: ""
                                FamilyMember(uid, name, email, photoUrl)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (members.isNotEmpty()) {
                            Log.d(TAG, "FirebaseSyncManager: Updating family members flow: size=${members.size}")
                            _familyMembers.value = members
                        }
                    }
                }
        }
    }

    fun stopSyncing() {
        Log.d(TAG, "FirebaseSyncManager: stopSyncing() called. activeListener exists=${activeListener != null}, memberListener exists=${memberListener != null}")
        activeListener?.remove()
        activeListener = null
        memberListener?.remove()
        memberListener = null
    }

    // Push local edits or newly created notes to Cloud Firestore
    fun pushTaskAdditionOrUpdate(task: Task) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return
        Log.d(TAG, "FirebaseSyncManager: pushTaskAdditionOrUpdate() called for taskId=${task.id}, title='${task.title}', isFirebaseInitialized=$isFirebaseInitialized")

        // Push task to firestore

        if (isFirebaseInitialized) {
            val isOnline = isNetworkAvailable()
            val initialSyncState = if (isOnline) SyncState.SYNCING else SyncState.PENDING_WRITE
            LocalSyncTracker.updateSyncState(task.id, initialSyncState)

            val db = FirebaseFirestore.getInstance()
            val taskMap = hashMapOf(
                "title" to task.title,
                "completed" to task.isCompleted,
                "profileOwner" to task.profileOwner,
                "createdBy" to task.createdByName,
                "createdByUid" to task.createdByUid,
                "completedBy" to task.completedByName,
                "completedByUid" to task.completedByUid,
                "createdAt" to task.createdAt,
                "completedAt" to task.completedAt
            )
            db.collection("families").document(familyId).collection("tasks").document(task.id)
                .set(taskMap)
                .addOnSuccessListener {
                    Log.d(TAG, "FirebaseSyncManager: pushTaskAdditionOrUpdate SUCCESS: Task saved successfully in cloud Firestore")
                    LocalSyncTracker.updateSyncState(task.id, SyncState.SYNCED)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FirebaseSyncManager: Error writing task doc to firestore", e)
                    LocalSyncTracker.updateSyncState(task.id, SyncState.ERROR)
                }
        }
    }

    // Permanently remove a paper note from Cloud Firestore
    fun pushTaskDeletion(taskId: String) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return
        Log.d(TAG, "FirebaseSyncManager: pushTaskDeletion() called for taskId=$taskId, isFirebaseInitialized=$isFirebaseInitialized")

        // Push task deletion to firestore

        if (isFirebaseInitialized) {
            val isOnline = isNetworkAvailable()
            val initialSyncState = if (isOnline) SyncState.SYNCING else SyncState.PENDING_WRITE
            LocalSyncTracker.updateSyncState(taskId, initialSyncState)

            val db = FirebaseFirestore.getInstance()
            db.collection("families").document(familyId).collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "FirebaseSyncManager: pushTaskDeletion SUCCESS: Task doc removed successfully from firestore")
                    LocalSyncTracker.clearSyncState(taskId)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FirebaseSyncManager: Error deleting task doc from firestore", e)
                    LocalSyncTracker.updateSyncState(taskId, SyncState.ERROR)
                }
        }
    }
}
