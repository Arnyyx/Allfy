package com.arny.allfy.data.remote

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthClient @Inject constructor(
    context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    fun buildSignInRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("535920978835-vf64ef426k8va6mudsejh35abttc4u3j.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    suspend fun signIn(activity: Activity): SignInResult = withContext(Dispatchers.Main) {
        try {
            val request = buildSignInRequest()
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )
            handleSignInResult(result)
        } catch (e: GetCredentialException) {
            SignInResult(
                data = null,
                googleIdToken = null,
                errorMessage = e.message ?: "Failed to sign in with Google"
            )
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): SignInResult {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    SignInResult(
                        data = UserData(
                            userId = googleIdTokenCredential.id,
                            username = googleIdTokenCredential.displayName ?: "",
                            profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                            email = googleIdTokenCredential.id
                        ),
                        googleIdToken = googleIdTokenCredential.idToken,
                        errorMessage = null
                    )
                } else {
                    SignInResult(
                        data = null,
                        googleIdToken = null,
                        errorMessage = "Unsupported credential type"
                    )
                }
            }
            else -> {
                SignInResult(
                    data = null,
                    googleIdToken = null,
                    errorMessage = "Unexpected credential type"
                )
            }
        }
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
    val googleIdToken: String?,
    val errorMessage: String?
)