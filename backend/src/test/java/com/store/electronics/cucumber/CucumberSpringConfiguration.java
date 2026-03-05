package com.store.electronics.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=c2VjcmV0S2V5Rm9yRWxlY3Ryb25pY3NTdG9yZUFwcGxpY2F0aW9u",
        "jwt.expiration=86400000"
})
public class CucumberSpringConfiguration {
}
