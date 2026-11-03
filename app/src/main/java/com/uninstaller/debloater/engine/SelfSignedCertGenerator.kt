package com.uninstaller.debloater.engine

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec

object SelfSignedCertGenerator {

    private const val KEY_FILE = "embedded_adb_key.pk8"
    private const val CERT_FILE = "embedded_adb_cert.der"

    fun loadOrGenerate(context: Context): Pair<PrivateKey, Certificate> {
        val keyFile = File(context.filesDir, KEY_FILE)
        val certFile = File(context.filesDir, CERT_FILE)

        if (keyFile.exists() && certFile.exists()) {
            runCatching {
                val keyBytes = keyFile.readBytes()
                val certBytes = certFile.readBytes()
                val kf = KeyFactory.getInstance("RSA")
                val privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(ByteArrayInputStream(certBytes))
                return privateKey to cert
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public

        val certDer = createSelfSignedX509Der(publicKey, privateKey)
        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(ByteArrayInputStream(certDer))

        runCatching {
            keyFile.writeBytes(privateKey.encoded)
            certFile.writeBytes(certDer)
        }

        return privateKey to cert
    }

    /**
     * Genera un certificato X.509 v3 self-signed in formato ASN.1 DER puro
     * firmato con SHA256withRSA, senza dipendenze esterne.
     */
    private fun createSelfSignedX509Der(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        // OID 1.2.840.113549.1.1.11 (sha256WithRSAEncryption) + NULL
        val sha256WithRsaAlgId = derSequence(
            byteArrayOf(
                0x06, 0x09,
                0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B,
                0x05, 0x00
            )
        )

        // Versione v3: [0] EXPLICIT INTEGER 2
        val version = byteArrayOf(0xA0.toByte(), 0x03, 0x02, 0x01, 0x02)
        // SerialNumber: INTEGER 1
        val serialNumber = byteArrayOf(0x02, 0x01, 0x01)

        // Name: CN=AppUninstallerPro (OID 2.5.4.3 = CommonName)
        val cnBytes = "AppUninstallerPro".toByteArray(StandardCharsets.UTF_8)
        val atv = derSequence(
            byteArrayOf(0x06, 0x03, 0x55, 0x04, 0x03) +
                derTag(0x0C, cnBytes)
        )
        val rdn = derTag(0x31, atv)
        val name = derSequence(rdn)

        // Validity: 2024-01-01 .. 2049-12-31
        val notBefore = derTag(0x17, "240101000000Z".toByteArray(StandardCharsets.US_ASCII))
        val notAfter = derTag(0x17, "491231235959Z".toByteArray(StandardCharsets.US_ASCII))
        val validity = derSequence(notBefore + notAfter)

        // SubjectPublicKeyInfo è già codificato in DER da publicKey.encoded
        val spki = publicKey.encoded

        val tbsCertificate = derSequence(
            version + serialNumber + sha256WithRsaAlgId + name + validity + name + spki
        )

        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(tbsCertificate)
        }.sign()

        // BIT STRING: 0x00 (zero unused bits) + signature
        val bitStringBody = ByteArray(signature.size + 1)
        bitStringBody[0] = 0x00
        System.arraycopy(signature, 0, bitStringBody, 1, signature.size)
        val signatureBitString = derTag(0x03, bitStringBody)

        return derSequence(tbsCertificate + sha256WithRsaAlgId + signatureBitString)
    }

    private fun derSequence(content: ByteArray): ByteArray = derTag(0x30, content)

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        writeDerLength(out, content.size)
        out.write(content)
        return out.toByteArray()
    }

    private fun writeDerLength(out: ByteArrayOutputStream, length: Int) {
        if (length < 128) {
            out.write(length)
        } else {
            val bytes = mutableListOf<Byte>()
            var temp = length
            while (temp > 0) {
                bytes.add(0, (temp and 0xFF).toByte())
                temp = temp ushr 8
            }
            out.write(0x80 or bytes.size)
            for (b in bytes) {
                out.write(b.toInt() and 0xFF)
            }
        }
    }
}
