package com.turaf.architecture.runners;

import com.intuit.karate.junit5.Karate;

public class ReportTestRunner {
    
    @Karate.Test
    Karate testReportManagement() {
        return Karate.run("classpath:features/reports/report-management.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testReportGeneration() {
        return Karate.run("classpath:features/reports/report-generation.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testAllReports() {
        return Karate.run("classpath:features/reports")
            .relativeTo(getClass());
    }
}
