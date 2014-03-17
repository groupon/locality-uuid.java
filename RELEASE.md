Release Process
===============

Do the following steps to create and push a release:

- Finish up and commit release changes.
- Edit the version in pom.xml.
- Add release notes about the changes in the new version to NOTES.md.
- Edit references to the current version in README.md.
- Commit these changes as 'prepare for vX.Y.Z release'.
- Add a tag on this commit like `git tag -a 'vX.Y.Z'`, add an appropriate message.
- Push the changes with `git push origin master && git push origin master --tags`.
- Deploy the changes with `mvn clean deploy` and enter the GPG passphrase. You may have to create an encryption key if none exists.
- Go to http://oss.sonatype.org.
- Go to Staging Profiles, and choose comgroupon.
- Click Close and write a message like 'Locality-UUID vX.Y.Z release', then confirm.
- Wait a minute and click Refresh, then Release, and confirm.
- The new version should now be released, but may take a few hours to show up in the repo and a few days to show up on the Maven central website.

