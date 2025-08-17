package com.ecommerce.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic application context test
 */
@Transactional
@Rollback
class AuthServiceApplicationTest extends BaseTestWithMysql {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }
}