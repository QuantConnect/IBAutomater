## How to create a new version of the NuGet package

Packages are built and pushed to nuget.org by the [Publish NuGet](../.github/workflows/publish.yml) workflow.

- Create a new GitHub release whose tag is the package version, e.g. `2.0.93` (a leading `v` is ignored)
- Publishing the release runs the workflow, which builds the package and pushes it to nuget.org with `build_push_nuget.sh`

Running the workflow manually only builds the package and attaches it to the run, so it can be used to check the build without publishing.

#### nuget.org authentication

The workflow uses [Trusted Publishing](https://learn.microsoft.com/nuget/nuget-org/trusted-publishing), so no API key is stored in the repository. It needs:

- A trusted publishing policy on nuget.org, created by an owner of the package, with repository owner `QuantConnect`, repository `IBAutomater` and workflow file `publish.yml`
- A `NUGET_USER` repository secret holding that owner's nuget.org profile name (not the email address)

#### Build the package locally

```
./build_push_nuget.sh [version]
```

Builds `java/IBAutomater/dist/IBAutomater.jar` with Ant, then packs `artifacts/QuantConnect.IBAutomater.<version>.nupkg`. Without a version, the `<Version>` in `QuantConnect.IBAutomater.csproj` is used. The package is only pushed to nuget.org when the `NUGET_API_KEY` environment variable is set.

It requires a JDK, Ant and the .NET 10 SDK. On Windows, run it from Git Bash; the JDK and Ant bundled with NetBeans are used when none are on the PATH.
