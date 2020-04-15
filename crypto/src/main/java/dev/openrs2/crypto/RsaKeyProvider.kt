package dev.openrs2.crypto

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Provider

class RsaKeyProvider : Provider<RSAPrivateCrtKeyParameters> {
    override fun get(): RSAPrivateCrtKeyParameters {
        return if (Files.exists(PRIVATE_PATH)) {
            Rsa.readPrivateKey(PRIVATE_PATH)
        } else {
            val (public, private) = Rsa.generateKeyPair(Rsa.CLIENT_KEY_LENGTH)
            Rsa.writePublicKey(PUBLIC_PATH, public)
            Rsa.writePrivateKey(PRIVATE_PATH, private)
            private
        }
    }

    companion object {
        private val PUBLIC_PATH = Paths.get("etc/public.key")
        private val PRIVATE_PATH = Paths.get("etc/private.key")
    }
}