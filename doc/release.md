# Packaging Releases

We release this software on
[Clojars](https://clojars.org/cloudpassage-lib).

## Getting started with releases

1. First, you will need a Clojars account. You can sign up for one on their
   [website](https://clojars.org/register).

2. You will also need a PGP key for signing the packages. (If you don't already
   have one, you will have to create one.) Use this key to [encrypt a copy of
   your Clojars
   credentials](https://github.com/technomancy/leiningen/blob/792750b7a1bdf0499081c72b197df41cee5ef648/doc/DEPLOY.md#gpg),
   stored at `~/.lein/credentials.clj.gpg`.

   This will allow you to sign copies of the release.

3. Ask a current maintainer to add you to the
   [cloudpassage-lib](https://clojars.org/cloudpassage-lib) group on Clojars.
   Otherwise, you will not have the authorization to upload releases.

4. Assuming you have an up-to-date copy of the cloudpassage-lib git repository
   checked out, you are now ready to prepare a release.

## Packaging and uploading a release

We use the `lein-release` plugin to release new versions of the library. Its
docs are a little sparse, so here are some instructions to help you prepare
releases.

1. When you are ready to release a new version of the library, you should let
   everyone else know by opening a pull request to update the change log
   (`CHANGELOG.md`). We have a **release** label you can apply to your pull
   request.

   By convention, the person who reviews and merges this PR will create the new
   release.

   You should consider whether the release is a *minor* or *major* release.
   Minor releases contain improvements or new API calls, and do not break or
   change existing functionality. Major releases make "breaking" changes such
   that consumers of the library would have to make changes their code to
   upgrade to the new version. A minor release increments the version number by
   0.0.1; a major release increments the version number by 0.1.0.

2. To package and upload the release, run `lein release :minor` at the root of
   the repository with the desired release commit checked out (usually at the
   HEAD of the master branch on the upstream repository).

   [**Note:** we encountered a bug with `lein release :major`. If you need to
   make a major release, first manually make a commit to change the version
   number to a SNAPSHOT of the desired major version number in the
   `project.clj` file, and then proceed with a minor release.]

   The `lein-release` plugin then completes the following actions on your
   behalf:

   - Changes the version from `X.Y.Z-SNAPSHOT` to `X.Y.Z` in the `project.clj`
     file and commits the change.
   - Creates a git tag `cloudpassage-lib-X.Y.Z`.
   - Signs and uploads the release to Clojars.
   - Changes the version to `X.Y.(Z+1)-SNAPSHOT` and commits the change.

4. You now need to push the new commits and tag to the master branch on the
   upstream repository:

   ```
   git push upstream HEAD --tags
   ```

   Congratulations! You've completed a release.
