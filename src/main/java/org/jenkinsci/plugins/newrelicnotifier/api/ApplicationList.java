package org.jenkinsci.plugins.newrelicnotifier.api;

import java.util.List;

public class ApplicationList {
    
    private List<Application> applications;

    public ApplicationList(List<Application> applications) {
        this.applications = applications;
    }

    public List<Application> getApplications() {
        return applications;
    }
}
