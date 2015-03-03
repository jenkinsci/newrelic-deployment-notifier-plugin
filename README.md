Jenkins New Relic Deployment Notifier Plugin
============================================

Jenkins plugin to notify New Relic about [deployments][].

Read more: [https://wiki.jenkins-ci.org/display/JENKINS/New+Relic+Deployment+Notifier+Plugin](https://wiki.jenkins-ci.org/display/JENKINS/New+Relic+Deployment+Notifier+Plugin)

Requirements
============

* An account at **[New Relic][]**
* API access enabled by creating an **[API key][]**
* **Jenkins 1.580.1** or newer

Usage
=====

Use the New Relic Deployment Notifier by adding it as a _Post Step_ in you Jenkins build job configuration.

1. In your Jenkins job configuration go to the *Post-build Actions* section, click on *Add post-build action* and select *New Relic Deployment Notifications*
2. Create an username/password credential for the API key. Enter the key as the password.
3. Select an application in the dropdown list.
4. Add any of the optional values: _description_, _revision_, _changelog_ or _user_

If you have configured everything correctly, Jenkins will notify you New Relic account of subsequent deployments.

It is possible to configure several applications to be notified.

Maintainers
===========

* Mads Mohr Christensen

License
-------

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

[deployments]: https://docs.newrelic.com/docs/apm/applications-menu/events/deployments-dashboard
[New Relic]: http://newrelic.com/
[API key]: https://docs.newrelic.com/docs/apm/apis/requirements/api-key