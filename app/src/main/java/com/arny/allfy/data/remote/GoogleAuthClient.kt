package com.arny.allfy.data.remote

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException
import javax.inject.Inject

class GoogleAuthClient @Inject constructor(
    private val context: Context
) {
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    suspend fun signIn(): IntentSender? {
        try {
            val result = oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
            return result.pendingIntent.intentSender
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return null
        }
    }

    suspend fun getSignInResultFromIntent(intent: Intent): SignInResult {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            if (googleIdToken == null) {
                return SignInResult(
                    data = null,
                    errorMessage = "Google ID token is null"
                )
            }
            SignInResult(
                data = UserData(
                    userId = credential.id,
                    username = credential.displayName ?: "",
                    profilePictureUrl = credential.profilePictureUri?.toString(),
                    email = credential.id  // Using ID as email since we need it for Firebase
                ),
                errorMessage = null
            )
        } catch (e: Exception) {
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("535920978835-vf64ef426k8va6mudsejh35abttc4u3j.apps.googleusercontent.com")
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}

data class UserData(
    val userId: String,
    val username: String,
    val profilePictureUrl: String? = null,
    val email: String
)

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)
