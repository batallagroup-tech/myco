package com.batallagroup.myco.domain.usecase

import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.crypto.CryptoManager
import com.batallagroup.myco.core.utils.PreferenceManager
import javax.inject.Inject

data class UserIdentity(
    val userId: String,
    val encPublicKey: String,
    val signPublicKey: String
)

class GenerateIdentityUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val preferenceManager: PreferenceManager
) {
    operator fun invoke(): UserIdentity {
        // Si ya existe identidad, devolverla
        val existingId = preferenceManager.getString(Constants.PREF_USER_ID)
        if (existingId != null) {
            return UserIdentity(
                userId = existingId,
                encPublicKey = preferenceManager.getString(Constants.PREF_ENC_PUBLIC_KEY) ?: "",
                signPublicKey = preferenceManager.getString(Constants.PREF_SIGN_PUBLIC_KEY) ?: ""
            )
        }

        // Generar nuevo par de claves de cifrado
        val (encPK, encSK) = cryptoManager.generateEncryptionKeyPair()
        // Generar nuevo par de claves de firma
        val (signPK, signSK) = cryptoManager.generateSigningKeyPair()

        val userId = cryptoManager.deriveUserId(encPK)

        // Guardar en almacenamiento seguro
        preferenceManager.putString(Constants.PREF_USER_ID, userId)
        preferenceManager.putString(Constants.PREF_ENC_PUBLIC_KEY, cryptoManager.toBase64(encPK))
        preferenceManager.putString(Constants.PREF_ENC_PRIVATE_KEY, cryptoManager.toBase64(encSK))
        preferenceManager.putString(Constants.PREF_SIGN_PUBLIC_KEY, cryptoManager.toBase64(signPK))
        preferenceManager.putString(Constants.PREF_SIGN_PRIVATE_KEY, cryptoManager.toBase64(signSK))

        return UserIdentity(
            userId = userId,
            encPublicKey = cryptoManager.toBase64(encPK),
            signPublicKey = cryptoManager.toBase64(signPK)
        )
    }
}
