package org.example.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.entity.BlockedUser;
import org.example.entity.User;
import org.example.repository.BlockedUserRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BlockedUserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    private User blocker;
    private User blocked;
    private User stranger;

    @BeforeEach
    void setUp() {
        blocker = userRepository.save(user("blocker@block.test", "blockerUser"));
        blocked = userRepository.save(user("blocked@block.test", "blockedUser"));
        stranger = userRepository.save(user("stranger@block.test", "strangerUser"));

        BlockedUser block = new BlockedUser();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        blockedUserRepository.save(block);
    }

    @Test
    void existsBlockBetween_queriedInBlockDirection_returnsTrue() {
        boolean exists = blockedUserRepository.existsBlockBetween(blocker.getId(), blocked.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsBlockBetween_queriedInReverseDirection_returnsTrue() {
        boolean exists = blockedUserRepository.existsBlockBetween(blocked.getId(), blocker.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsBlockBetween_unrelatedPair_returnsFalse() {
        boolean exists = blockedUserRepository.existsBlockBetween(blocker.getId(), stranger.getId());

        assertThat(exists).isFalse();
    }

    private User user(String email, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword("password123");
        user.setFirstName("Block");
        user.setLastName("Tester");
        return user;
    }
}
