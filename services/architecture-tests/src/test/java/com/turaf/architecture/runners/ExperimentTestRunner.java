package com.turaf.architecture.runners;

import com.intuit.karate.junit5.Karate;

public class ExperimentTestRunner {
    
    @Karate.Test
    Karate testExperimentLifecycle() {
        return Karate.run("classpath:features/experiments/experiment-lifecycle.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testProblemManagement() {
        return Karate.run("classpath:features/experiments/problem-management.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testHypothesisManagement() {
        return Karate.run("classpath:features/experiments/hypothesis-management.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testMetricsRecording() {
        return Karate.run("classpath:features/experiments/metrics-recording.feature")
            .relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testAllExperiments() {
        return Karate.run("classpath:features/experiments")
            .relativeTo(getClass());
    }
}
