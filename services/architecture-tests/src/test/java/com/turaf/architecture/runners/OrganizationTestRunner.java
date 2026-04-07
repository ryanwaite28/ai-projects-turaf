package com.turaf.architecture.runners;

import com.intuit.karate.junit5.Karate;

public class OrganizationTestRunner {
    
    @Karate.Test
    Karate testOrganizationCrud() {
        return Karate.run("classpath:features/organizations/create-organization.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testMemberManagement() {
        return Karate.run("classpath:features/organizations/manage-members.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testOrganizationWorkflows() {
        return Karate.run("classpath:features/organizations/organization-workflows.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testAllOrganizations() {
        return Karate.run("classpath:features/organizations")
            .relativeTo(getClass());
    }
}
