package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.ArtifactType
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.Notebook
import com.example.model.NotebookColorToken
import com.example.model.SourceType
import com.example.model.StudyArtifact
import com.example.model.StudySource
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun notebooksRef(uid: String) =
        db.collection("users").document(uid).collection("notebooks")

    private fun sourcesRef(uid: String, notebookId: String) =
        notebooksRef(uid).document(notebookId).collection("sources")

    private fun artifactsRef(uid: String, notebookId: String) =
        notebooksRef(uid).document(notebookId).collection("artifacts")

    private fun messagesRef(uid: String, notebookId: String) =
        notebooksRef(uid).document(notebookId).collection("messages")

    fun observeNotebooks(userId: String): Flow<List<Notebook>> = flow {
        val path = "users/$userId/notebooks"
        emitAll(
            notebooksRef(userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Notebook::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotebook(userId: String, notebookId: String): Flow<Notebook?> = flow {
        val path = "users/$userId/notebooks/$notebookId"
        emitAll(
            notebooksRef(userId).document(notebookId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            Notebook::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeSources(userId: String, notebookId: String): Flow<List<StudySource>> = flow {
        val path = "users/$userId/notebooks/$notebookId/sources"
        emitAll(
            sourcesRef(userId, notebookId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudySource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeArtifacts(userId: String, notebookId: String): Flow<List<StudyArtifact>> = flow {
        val path = "users/$userId/notebooks/$notebookId/artifacts"
        emitAll(
            artifactsRef(userId, notebookId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyArtifact::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, notebookId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/notebooks/$notebookId/messages"
        emitAll(
            messagesRef(userId, notebookId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createNotebook(
        title: String,
        subject: String,
        colorToken: NotebookColorToken
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Notebook title cannot be empty." }
            val cleanSubject = subject.trim().take(60)
            val docRef = notebooksRef(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "subject" to cleanSubject,
                "colorToken" to colorToken.key,
                "sourceCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNotebookMetadata(
        notebookId: String,
        title: String,
        subject: String,
        colorToken: NotebookColorToken
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId"
        return try {
            val uid = requireUserId()
            require(notebookId.isNotBlank())
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Notebook title cannot be empty." }
            val docRef = notebooksRef(uid).document(notebookId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "subject" to subject.trim().take(60),
                    "colorToken" to colorToken.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNotebookOverview(notebookId: String, overview: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId"
        return try {
            val uid = requireUserId()
            val docRef = notebooksRef(uid).document(notebookId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "summaryOverview" to overview.take(15000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNotebook(notebookId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId"
        return try {
            val uid = requireUserId()
            docPath = notebooksRef(uid).document(notebookId).path
            // Clean up subcollections before removing parent notebook
            val sourcesSnap = sourcesRef(uid, notebookId).get().await()
            val artifactsSnap = artifactsRef(uid, notebookId).get().await()
            val messagesSnap = messagesRef(uid, notebookId).get().await()
            val refs = sourcesSnap.documents.map { it.reference } +
                artifactsSnap.documents.map { it.reference } +
                messagesSnap.documents.map { it.reference } +
                notebooksRef(uid).document(notebookId)
            refs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSource(
        notebookId: String,
        title: String,
        sourceType: SourceType,
        mimeType: String,
        content: String,
        currentSourceCount: Long
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/sources"
        return try {
            val uid = requireUserId()
            require(notebookId.isNotBlank())
            val cleanTitle = title.trim().take(160).ifEmpty { "Untitled Source" }
            val cleanContent = content.trim().take(80000)
            require(cleanContent.isNotEmpty()) { "Source content cannot be empty." }
            val excerpt = cleanContent.replace(Regex("\\s+"), " ").take(380)
            val wordCount = cleanContent.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtMost(50000).toLong()

            val srcRef = sourcesRef(uid, notebookId).document()
            docPath = srcRef.path
            val nbRef = notebooksRef(uid).document(notebookId)

            val payload = mapOf(
                "userId" to uid,
                "notebookId" to notebookId,
                "title" to cleanTitle,
                "sourceType" to sourceType.key,
                "mimeType" to mimeType.take(120),
                "content" to cleanContent,
                "excerpt" to excerpt,
                "wordCount" to wordCount,
                "included" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.set(srcRef, payload)
            batch.update(
                nbRef,
                mapOf(
                    "sourceCount" to (currentSourceCount + 1L).coerceAtMost(200L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(srcRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSourceIncluded(
        notebookId: String,
        sourceId: String,
        included: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/sources/$sourceId"
        return try {
            val uid = requireUserId()
            val ref = sourcesRef(uid, notebookId).document(sourceId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "included" to included,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSourceContent(
        notebookId: String,
        sourceId: String,
        title: String,
        content: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/sources/$sourceId"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(160)
            val cleanContent = content.trim().take(80000)
            require(cleanTitle.isNotEmpty() && cleanContent.isNotEmpty()) {
                "Source title and content cannot be empty."
            }
            val excerpt = cleanContent.replace(Regex("\\s+"), " ").take(380)
            val wordCount = cleanContent.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtMost(50000).toLong()
            val ref = sourcesRef(uid, notebookId).document(sourceId)
            docPath = ref.path
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "excerpt" to excerpt,
                    "wordCount" to wordCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSource(
        notebookId: String,
        sourceId: String,
        currentSourceCount: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/sources/$sourceId"
        return try {
            val uid = requireUserId()
            val ref = sourcesRef(uid, notebookId).document(sourceId)
            docPath = ref.path
            val batch = db.batch()
            batch.delete(ref)
            batch.update(
                notebooksRef(uid).document(notebookId),
                mapOf(
                    "sourceCount" to (currentSourceCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveArtifact(
        notebookId: String,
        artifactType: ArtifactType,
        title: String,
        payloadJson: String,
        sourceTitles: List<String>
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/artifacts"
        return try {
            val uid = requireUserId()
            val ref = artifactsRef(uid, notebookId).document()
            docPath = ref.path
            val payload = mapOf(
                "userId" to uid,
                "notebookId" to notebookId,
                "artifactType" to artifactType.key,
                "title" to title.trim().take(160).ifEmpty { artifactType.label },
                "payloadJson" to payloadJson.take(100000),
                "sourceTitles" to sourceTitles.take(30),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteArtifact(notebookId: String, artifactId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/artifacts/$artifactId"
        return try {
            val uid = requireUserId()
            val ref = artifactsRef(uid, notebookId).document(artifactId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveChatMessage(
        notebookId: String,
        role: ChatRole,
        text: String,
        citations: List<String>
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/messages"
        return try {
            val uid = requireUserId()
            val ref = messagesRef(uid, notebookId).document()
            docPath = ref.path
            val payload = mapOf(
                "userId" to uid,
                "notebookId" to notebookId,
                "role" to role.key,
                "text" to text.trim().take(40000),
                "citations" to citations.take(20),
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(notebookId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notebooks/$notebookId/messages"
        return try {
            val uid = requireUserId()
            val snap = messagesRef(uid, notebookId).get().await()
            snap.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
