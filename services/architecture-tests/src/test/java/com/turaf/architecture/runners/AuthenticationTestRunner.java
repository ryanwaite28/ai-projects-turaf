package com.turaf.architecture.runners;

import com.intuit.karate.junit5.Karate;

public class AuthenticationTestRunner {
    
    @Karate.Test
    Karate testLogin() {
        return Karate.run("classpath:features/authentication/login.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testRegistration() {
        return Karate.run("classpath:features/authentication/registration.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testTokenRefresh() {
        return Karate.run("classpath:features/authentication/token-refresh.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testLogout() {
        return Karate.run("classpath:features/authentication/logout.feature")
            .relativeTo(getClass());
    }
}
