## How to create a new version of the NuGet package

- Set the version number in the `QuantConnect.IBAutomater.csproj` file and merge it to master
- Run the [Publish NuGet](../.github/workflows/publish.yml) workflow from the Actions tab, which builds the package and pushes it to nuget.org with `build_push_nuget.sh`

nuget.org rejects versions that already exist, so the workflow fails if the version was not changed.

#### nuget.org authentication

The workflow uses [Trusted Publishing](https://learn.microsoft.com/nuget/nuget-org/trusted-publishing), so no API key is stored in the repository. It needs:

- A trusted publishing policy on nuget.org, created by an owner of the package, with repository owner `QuantConnect`, repository `IBAutomater` and workflow file `publish.yml`
- A `NUGET_USER` repository secret holding that owner's nuget.org profile name (not the email address)

#### Build the package locally

```
./build_push_nuget.sh
```

Builds `java/IBAutomater/dist/IBAutomater.jar` with Ant, then packs `artifacts/QuantConnect.IBAutomater.<version>.nupkg` with the version in the csproj. The package is only pushed to nuget.org when the `NUGET_API_KEY` environment variable is set.

It requires a JDK, Ant and the .NET 10 SDK. On Windows, run it from Git Bash; the JDK and Ant bundled with NetBeans are used when none are on the PATH.
