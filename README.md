Jenkins New Relic Deployment Notifier Plugin
============================================

Jenkins plugin to notify New Relic about [deployments][].

## Requirements

* An account at **[New Relic][]**
* API access enabled by creating an **[API key][]**
* **Jenkins 1.580.1** or newer

## Usage

Use the New Relic Deployment Notifier by adding it as a *Post Step* in
you Jenkins build job configuration.

1.  In your Jenkins job configuration go to the **Post-build Actions**
    section, click on **Add post-build action** and select **New Relic
    Deployment Notifications**
    ![](docs/images/postbuild.png)
2.  Create a username/password credential for the API key. Enter the
    key as the password.
    ![](docs/images/credential.png)
3.  Select an application in the dropdown list.
    ![](docs/images/validcredential.png)
4.  Add any of the optional values: *description*, *revision*,
    *changelog* or *user*
    ![](docs/images/optional.png)

If you have configured everything correctly, Jenkins will notify you New
Relic account of subsequent deployments.

It is possible to configure several applications to be notified.
![](docs/images/addnotification.png)

### Getting user as an environment variable

Install the [Build User Vars
Plugin](https://wiki.jenkins.io/display/JENKINS/Build+User+Vars+Plugin)
and use any of the supported environment variables.
![](docs/images/user.png)

## Maintainers

* Jonathan Gordon
* Justin Lewis
* Mads Mohr Christensen

## Changelog

[Changelog](CHANGELOG.md)

## Development

### Project Set Up

For IntelliJ IDEA users, the following steps are recommended:

1. Install the Jenkins Development Support Plugin.
2. Open the project directory in IntelliJ IDEA and import as a Maven project. 
3. Go to **File** -\> **Project Structure** -\> **Project** and set the
    **Project SDK** to the JDK you want to use. Right now we're using JDK 8.

### Running Locally
1. Create a Maven Run Configuration with "hpi:run" as the Run command.
2. Start the configuration in Run or Debug mode.
3. The plugin will be available at
    [http://localhost:8080/jenkins/](http://localhost:8080/jenkins/). 
4. Create a new Freestyle Project and name it whatever you want.
5. Configure the job and click "Add post-build action" and select "New Relic Deployment Notifications".
6. Add User API Keys as Global Credentials with a Kind of "Username with password" and add the API key as the password. It's helpful to label the credential with the New Relic account id and the region (US or EU).
7. Configure all the fields for the job and save.
8. Click "Build Now" to run the job and check its status. If it succeeds you should be able to then see the deployment in New Relic under the selected application's Change Tracking tab.

## License

	(The MIT License)

	Copyright (c) 2015, Mads Mohr Christensen

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	'Software'), to deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be
	included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
	EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[deployments]: https://docs.newrelic.com/docs/change-tracking/change-tracking-view-analyze/
[New Relic]: http://newrelic.com/
[API key]: https://docs.newrelic.com/docs/apis/intro-apis/new-relic-api-keys/
