## How to create a new version of the NuGet package

#### Create the NuGet package file

- Set the version number in the `QuantConnect.IBAutomater.nuspec` file
- Run the following command:
```
nuget pack QuantConnect.IBAutomater.nuspec
```

#### Set the NuGet API key

```
nuget setApiKey <my-api-key>
```

#### Upload the NuGet package

- Set the version number and run the following command:
```
nuget push QuantConnect.IBAutomater.1.0.1.nupkg -Source https://api.nuget.org/v3/index.json
```