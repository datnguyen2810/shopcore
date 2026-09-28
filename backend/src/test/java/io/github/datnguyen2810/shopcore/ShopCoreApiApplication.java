package io.github.datnguyen2810.shopcore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShopCoreApiApplicationTests {

    @Test
    void contextLoads() {
    }
}