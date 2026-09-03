package org.example.configuration.push;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.function.Supplier;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushConfiguration {

    @Bean
    public Supplier<PushService> pushServiceSupplier(
            @Value("${push.vapid.public-key}") String publicKey,
            @Value("${push.vapid.private-key}") String privateKey,
            @Value("${push.vapid.subject}") String subject) {

        Security.addProvider(new BouncyCastleProvider());

        return () -> {
            try {
                return new PushService(publicKey, privateKey, subject);
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("Failed to initialize PushService", exception);
            }
        };
    }
}
