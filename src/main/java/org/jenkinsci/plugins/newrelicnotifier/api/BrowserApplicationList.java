package org.jenkinsci.plugins.newrelicnotifier.api;

import java.util.List;

public class BrowserApplicationList {

    private List<Application> browser_applications;

    public BrowserApplicationList(List<Application> browserApplications) {
        this.browser_applications = browserApplications;
    }

    public List<Application> getBrowserApplications() {
        return browser_applications;
    }
}
