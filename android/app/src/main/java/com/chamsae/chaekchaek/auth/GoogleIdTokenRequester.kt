package com.chamsae.chaekchaek.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.chamsae.chaekchaek.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun requestGoogleIdToken(context: Context): String {
  val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.google_web_client_id)).build()
  val response = CredentialManager.create(context).getCredential(
    context = context,
    request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
  )
  val credential = response.credential as? CustomCredential
  check(credential?.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    "Google ID 토큰을 받지 못했습니다."
  }
  return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
