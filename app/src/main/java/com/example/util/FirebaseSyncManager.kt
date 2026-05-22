package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.Task
import com.example.data.TaskDao
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Stores user details and family session info
data class UserSession(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val familyId: String? = null,
    val familyName: String? = null,
    val familyInviteCode: String? = null
)

data class FamilyMember(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String
)

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_NAME = "PaperBundleFirebasePrefs"

    var isFirebaseInitialized = false
        private set

    // Emulated multiplayer users when in SANDBOX mode
    val sandboxUsers = listOf(
        UserSession("user_mom", "Mom (Amma)", "mom@gmail.com", "https://api.dicebear.com/7.x/adventurer/svg?seed=Mom"),
        UserSession("user_dad", "Dad (Appa)", "dad@gmail.com", "https://api.dicebear.com/7.x/adventurer/svg?seed=Dad"),
        UserSession("user_son", "Son (Kowshik)", "kowshik@gmail.com", "https://api.dicebear.com/7.x/adventurer/svg?seed=Son")
    )

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
        } catch (e: Exception) {
            false
        }

    fun init(context: Context) {
        if (isFirebaseInitialized) return

        if (isConfigValid) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build()

                FirebaseApp.initializeApp(context, options)
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase initialized successfully in production mode.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase with custom options", e)
                isFirebaseInitialized = false
            }
        } else {
            Log.d(TAG, "Firebase credentials missing or using placeholders. Starting in high-fidelity Sandbox Mode.")
            isFirebaseInitialized = false
        }

        // Restore session from local SharedPreferences for fast startup (<2s)
        loadSavedSession(context)
    }

    private fun loadSavedSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString("uid", null)
        if (uid != null) {
            val name = prefs.getString("name", "User") ?: "User"
            val email = prefs.getString("email", "") ?: ""
            val photoUrl = prefs.getString("photoUrl", "") ?: ""
            val familyId = prefs.getString("familyId", null)
            val familyName = prefs.getString("familyName", null)
            val inviteCode = prefs.getString("familyInviteCode", null)

            val session = UserSession(
                uid = uid,
                name = name,
                email = email,
                photoUrl = photoUrl,
                familyId = familyId,
                familyName = familyName,
                familyInviteCode = inviteCode
            )
            _currentUserSession.value = session
            _familyMembers.value = listOf(FamilyMember(uid, name, email, photoUrl))
        }
    }

    fun saveSession(context: Context, session: UserSession?) {
        _currentUserSession.value = session
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        if (session != null) {
            edit.putString("uid", session.uid)
            edit.putString("name", session.name)
            edit.putString("email", session.email)
            edit.putString("photoUrl", session.photoUrl)
            edit.putString("familyId", session.familyId)
            edit.putString("familyName", session.familyName)
            edit.putString("familyInviteCode", session.familyInviteCode)
            _familyMembers.value = listOf(FamilyMember(session.uid, session.name, session.email, session.photoUrl))
        } else {
            edit.clear()
            _familyMembers.value = emptyList()
            stopSyncing()
        }
        edit.apply()
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
        if (isFirebaseInitialized && idToken != null) {
            // Real Firebase authentication using Google Credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        val session = UserSession(
                            uid = fbUser?.uid ?: UUID.randomUUID().toString(),
                            name = fbUser?.displayName ?: profileChoiceName,
                            email = fbUser?.email ?: profileChoiceEmail,
                            photoUrl = fbUser?.photoUrl?.toString() ?: profileChoicePhoto
                        )
                        saveSession(context, session)
                        onComplete(true, null)
                    } else {
                        onComplete(false, task.exception?.localizedMessage ?: "Firebase Sign-In failed")
                    }
                }
        } else {
            // High-fidelity Sandbox authenticate flow
            // Use selected chosen mock profile or generate a random uid
            val generatedUid = "sandbox_" + profileChoiceName.lowercase().replace(" ", "_")
            val session = UserSession(
                uid = generatedUid,
                name = profileChoiceName,
                email = profileChoiceEmail,
                photoUrl = profileChoicePhoto
            )
            saveSession(context, session)
            onComplete(true, null)
        }
    }

    // Create a new Family workspace
    fun createFamily(
        context: Context,
        familyName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val current = _currentUserSession.value ?: return onComplete(false, "No active user session")
        val randomDigits = (100..999).random()
        val suffix = familyName.take(4).uppercase().replace(" ", "")
        val code = "$suffix$randomDigits"
        val newFamilyId = "fam_${UUID.randomUUID().toString().take(8)}"

        if (isFirebaseInitialized) {
            val db = FirebaseFirestore.getInstance()
            val familyData = hashMapOf(
                "familyName" to familyName,
                "createdBy" to current.uid,
                "inviteCode" to code
            )
            db.collection("families").document(newFamilyId)
                .set(familyData)
                .addOnSuccessListener {
                    // Register current user as a family member
                    val memberData = hashMapOf(
                        "uid" to current.uid,
                        "name" to current.name,
                        "email" to current.email,
                        "photoUrl" to current.photoUrl
                    )
                    db.collection("families").document(newFamilyId).collection("members").document(current.uid)
                        .set(memberData)
                        .addOnSuccessListener {
                            val updatedSession = current.copy(
                                familyId = newFamilyId,
                                familyName = familyName,
                                familyInviteCode = code
                            )
                            saveSession(context, updatedSession)
                            onComplete(true, null)
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, "Failed to register family member: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Failed to create family registry: ${e.localizedMessage}")
                }
        } else {
            // Sandbox creation
            val updatedSession = current.copy(
                familyId = newFamilyId,
                familyName = familyName,
                familyInviteCode = code
            )
            saveSession(context, updatedSession)
            onComplete(true, null)
        }
    }

    // Join via Invite Code
    fun joinFamily(
        context: Context,
        inviteCode: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val current = _currentUserSession.value ?: return onComplete(false, "No active user session")
        val cleanCode = inviteCode.trim().uppercase()

        if (isFirebaseInitialized) {
            val db = FirebaseFirestore.getInstance()
            db.collection("families")
                .whereEqualTo("inviteCode", cleanCode)
                .get()
                .addOnSuccessListener { query ->
                    if (query == null || query.isEmpty) {
                        onComplete(false, "Invalid Invite Code! Please verify with your family member.")
                    } else {
                        val doc = query.documents.first()
                        val famId = doc.id
                        val famName = doc.getString("familyName") ?: "Family Board"
                        
                        // Register member in Firestore
                        val memberData = hashMapOf(
                            "uid" to current.uid,
                            "name" to current.name,
                            "email" to current.email,
                            "photoUrl" to current.photoUrl
                        )
                        db.collection("families").document(famId).collection("members").document(current.uid)
                            .set(memberData)
                            .addOnSuccessListener {
                                val updatedSession = current.copy(
                                    familyId = famId,
                                    familyName = famName,
                                    familyInviteCode = cleanCode
                                )
                                saveSession(context, updatedSession)
                                onComplete(true, null)
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, "Failed to register profile info: ${e.localizedMessage}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Error verifying invite code: ${e.localizedMessage}")
                }
        } else {
            // High fidelity sandbox verification
            // Support joining predefined codes for simulated family play!
            // "HOME482" standard
            val defaultName = if (cleanCode.startsWith("HOME")) "Baskaran Home" else "Family Shared Board"
            val targetFamId = "fam_sandbox_${cleanCode.lowercase()}"
            val updatedSession = current.copy(
                familyId = targetFamId,
                familyName = defaultName,
                familyInviteCode = cleanCode
            )
            saveSession(context, updatedSession)
            onComplete(true, null)
        }
    }

    // Realtime synchronizer and observer of tasks
    fun startSyncing(taskDao: TaskDao) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return

        stopSyncing()

        if (isFirebaseInitialized) {
            val db = FirebaseFirestore.getInstance()
            activeListener = db.collection("families").document(familyId).collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Sync observation failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val firestoreTasks = snapshot.documents.mapNotNull { doc ->
                            try {
                                val title = doc.getString("title") ?: ""
                                val isCompleted = doc.getBoolean("completed") ?: false
                                val profileOwner = doc.getString("profileOwner") ?: "GENERAL"
                                val createdBy = doc.getString("createdBy") ?: "Unknown"
                                val completedBy = doc.getString("completedBy")
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val completedAt = doc.getLong("completedAt")

                                Task(
                                    id = doc.id,
                                    title = title,
                                    isCompleted = isCompleted,
                                    profileOwner = profileOwner,
                                    createdAt = createdAt,
                                    completedAt = completedAt,
                                    createdByUid = doc.getString("createdByUid") ?: "",
                                    createdByName = createdBy,
                                    completedByUid = doc.getString("completedByUid"),
                                    completedByName = completedBy,
                                    familyId = familyId,
                                    firebaseSynced = true
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Mirror tasks into Room DB on background Thread
                        ioScope.launch {
                            // Find deleted tasks: tasks in local Room that belong to current family, but aren't in Firestore anymore
                            // To keep it simple, we replace/update
                            for (task in firestoreTasks) {
                                taskDao.insertTask(task)
                            }
                        }
                    }
                }

            memberListener = db.collection("families").document(familyId).collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Sync observation of members failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
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
                            _familyMembers.value = members
                        }
                    }
                }
        }
    }

    fun stopSyncing() {
        activeListener?.remove()
        activeListener = null
        memberListener?.remove()
        memberListener = null
    }

    // Push local edits or newly created notes to Cloud Firestore
    fun pushTaskAdditionOrUpdate(task: Task) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return

        if (isFirebaseInitialized) {
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
                    Log.d(TAG, "Task saved successfully in cloud Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing task doc to firestore", e)
                }
        }
    }

    // Permanently remove a paper note from Cloud Firestore
    fun pushTaskDeletion(taskId: String) {
        val session = _currentUserSession.value ?: return
        val familyId = session.familyId ?: return

        if (isFirebaseInitialized) {
            val db = FirebaseFirestore.getInstance()
            db.collection("families").document(familyId).collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Task doc removed successfully from firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting task doc from firestore", e)
                }
        }
    }
}
