Local SMTP
==========

Add-on for
<a href="https://www.playframework.com/documentation/1.4.x/home">Play
Framework 1.x</a> (this does not work with Play 2.x series!)

This addon is used to start a local smtp server when running in test/dev
mode that catches all outgoing email. It is based on on SubEthaSMTP.

Similar software:

- https://mailcatcher.me/\
Written in ruby it seems.\
- https://mailtrap.io/\
non-free, saas.\
- http://smtp4dev.codeplex.com/\
Windows only\
- http://quintanasoft.com/dumbster/\
Java\
- https://nilhcem.github.io/FakeSMTP/\
Java, also based on SubEthaSMTP

Features
--------

-   Quick to set up
-   Writes to the filesystem, no database needed
-   Minimal UI
-   Download EML files to test your html mails in email clients without
    sending any mails and without configuring your mail client.
-   Embedded. No external installation/configuration needed

Sample application
------------------

Not gonna happen

See the play documentation on sending mails\
https://www.playframework.com/documentation/1.4.x/emails

Getting started
---------------

The preferred way is to install this plugin locally in a subfolder of
your application and have it in version control as a submodule.

### Install locally

First download the add-on and unpack it to
`my-app/modules-local/localsmtp`.\
Then register it in `dependencies.yml`.

    # Application dependencies
    require:
        - play
        - localsmtp -> localsmtp 0.1

    repositories:
        - Local Modules:
            type:       local
            artifact:   ${application.path}/modules-local/[module]
            contains:
                - localsmtp

For these changes to take effect you must update the depencies on the
command line.

    play dependencies modules-local/localsmtp --sync
    play dependencies --sync
    play eclipsify

Configuration
-------------

### Mail server configuration

To activate the local smtp server all you have to do is specify
`localhost` as host, and use `localsmtp` as both username and password.
Chose a port over 1024 or you will run into permission problems on
linux.

    mail.smtp.host=localhost
    mail.smtp.user=localsmtp
    mail.smtp.pass=localsmtp
    mail.smtp.port=37536

Usage
-----

To view all collected emails visit

        http://localhost:9000/@emails

Tags
----
