package org.example.tools;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class VapidKeyGenerator {

    private VapidKeyGenerator() {
    }

    public static void main(String[] args) throws GeneralSecurityException {

        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String publicKey = encoder.encodeToString(
                Utils.encode((ECPublicKey) keyPair.getPublic()));
        String privateKey = encoder.encodeToString(
                Utils.encode((ECPrivateKey) keyPair.getPrivate()));

        System.out.println("push.vapid.public-key=" + publicKey);
        System.out.println("push.vapid.private-key=" + privateKey);
    }
}
