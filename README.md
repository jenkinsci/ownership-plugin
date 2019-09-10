Ownership Plugin for Jenkins
================

[![Join the chat at https://gitter.im/jenkinsci/ownership-plugin](https://badges.gitter.im/jenkinsci/ownership-plugin.svg)](https://gitter.im/jenkinsci/ownership-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/ownership.svg)](https://plugins.jenkins.io/ownership)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/ownership-plugin.svg?label=changelog)](https://github.com/jenkinsci/ownership-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/ownership.svg?color=blue)](https://plugins.jenkins.io/ownership)

This plugin provides an ownership engine for [Jenkins](https://jenkins.io/) automation server. 

# Supported features

* Ownership management for jobs, folders, runs and nodes (Summary boxes, ListView columns, etc.)
* Injection of ownership variables into the build environment
* Easy MailTo links for "Contact owners" and "Contact admins" with pre-formatted messages
* [Ownership-Based Security](doc/OwnershipBasedSecurity.md) (Role Strategy, Job Restrictions, Authorize Project, etc.)
* Integration with Jenkins Pipeline
* Ownership Filter for List Views

# Documentation

* [Configuration](doc/Configuration.md)
* [Ownership-Based Security](doc/OwnershipBasedSecurity.md)
* [AbstractProject Integration](doc/AbstractProjectSupport.md)
* [Pipeline Integration](doc/PipelineIntegration.md)
* [Contributing](CONTRIBUTING.md)
* [Changelog](CHANGELOG.md)

# Examples

Below you can find several Ownership Plugin UI examples.

Ownership Info Summary box:

![Ownership Summary box](doc/images/summaryBox.png)

Ownership Column:

![Ownership Column](doc/images/ownerColumn.png)

# Plugin Integrations

Ownership Plugin provides integration with multiple Jenkins plugins:

* Role Strategy - see [Ownership-Based Security](doc/OwnershipBasedSecurity.md)
* Job Restrictions - see [Ownership-Based Security](doc/OwnershipBasedSecurity.md)
* Authorize Project - see [Ownership-Based Security](doc/OwnershipBasedSecurity.md)
* Token Macro - see [AbstractProject Integration](doc/AbstractProjectSupport.md)
* Pipeline - see [Pipeline Integration](doc/PipelineIntegration.md)

The plugin also contributes extension points, which can be used by other plugins.

# License

[MIT License](http://www.opensource.org/licenses/mit-license.php)

# Changelog

See [GitHub releases](https://github.com/jenkinsci/ownership-plugin/releases) for recent releases.
Changelog for 0.12.1 and older versions is available [here](CHANGELOG.md).
