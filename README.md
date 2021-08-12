# QuantConnect / IBAutomater

IBAutomater is an automation tool for IB Gateway v978 and above. It supports starting, stopping and automated logins for IB Gateway. This package is best used in conjunction with LEAN. LEAN handles the automatic login and portfolio construction once you start the IB Gateway.

## Installation

IBAutomater is available on [NuGet](https://www.nuget.org/packages/QuantConnect.IBAutomater/):

`PM> Install-Package QuantConnect.IBAutomater`

To get started with IBAutomater, first you will need to get the library itself. The easiest way to do this is to install the package into your project using  [NuGet](https://www.nuget.org/packages/QuantConnect.IBAutomater/). Using Visual Studio this can be done in two ways.

#### Using the package manager

In Visual Studio right click on your solution and select 'Manage NuGet Packages for solution...'. A screen will appear which initially shows the currently installed packages. At the top select 'Browse'. This will let you download the package from the NuGet server. In the search box type 'QuantConnect.IBAutomater'. The QuantConnect.IBAutomater package should come up in the results. After selecting the package you can then on the right hand side select in which projects in your solution the package should install. After you've selected all projects you wish to install and use QuantConnect.IBAutomater in click 'Install' and the package will be downloaded and added to your selected projects.

#### Using the package manager console

In Visual Studio in the top menu select 'Tools' -> 'NuGet Package Manager' -> 'Package Manager Console'. This should open up a command line interface. On top of the interface there is a dropdown menu where you can select the Default Project. This is the project that IBAutomater will be installed in. After selecting the correct project type Install-Package QuantConnect.IBAutomater in the command line interface. This should install the latest version of the package in your project.

After doing either of above steps you should now be ready to start using IBAutomater.

## Code example

``` C#
using QuantConnect.IBAutomater;

// Create a new instance of IBAutomater
_ibAutomater = new IBAutomater.IBAutomater(ibDirectory, ibVersion, userName, password, tradingMode, port, exportIbGatewayLogs);

// You can bind to event handlers to receive the output data.
_ibAutomater.OutputDataReceived += OnIbAutomaterOutputDataReceived;

// Gracefully handle errors
_ibAutomater.ErrorDataReceived += OnIbAutomaterErrorDataReceived;

// Get events once the IBGateway has exited.
_ibAutomater.Exited += OnIbAutomaterExited;

// Get events once the IBGateway has auto-restarted.
_ibAutomater.Restarted += OnIbAutomaterRestarted;

// Trigger the IBGateway to start and login with your configured parameters.
_ibAutomater.Start(false);

// Stop IBGateway with a simple command.
_ibAutomater.Stop();
```

## How it works

IBAutomater has been implemented as two components:

1. a Java component (.jar file)

    The IBAutomater.jar file is loaded as a Java agent into IB Gateway process with the following advantages:
    
    - remove the requirement of installing Java separately as IBGateway uses a bundled version of the Java runtime
    - avoid dependencies on IB Gateway jar libraries
    - allow us to handle auto-restarts requiring full authentication only weekly (instead of daily)

    More information on the Java Instrumentation API can be found [here](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)

    This component is responsible for interacting with the IB Gateway user interface, performing the following tasks:

    - entering login credentials
    - configuring IBGateway settings
    - dismissing message and confirmation windows
  
2. a C# component (.dll file), .NET assembly built for both NET Standard 2.0 and NET 5.0

    This component takes care of the following tasks:

    - starting and stopping IBGateway
    - notifying the client application of any error conditions
    - firing events such as Exited and Restarted

## How to build

To build the NuGet package we need to complete the following three steps in order:
1. Build the Java project
2. Build the C# solution
3. Upload the new package to NuGet

#### 1. Java build (with NetBeans)

- Download Apache NetBeans 12.x [here](https://netbeans.apache.org/)
- Install Apache NetBeans 12.1 or higher (currently bundled Java version: JDK 15)
- Open the project in /IBAutomater/java/IBAutomater
- Build the project: MainMenu -> Run -> Clean and Build Project
  - output: /IBAutomater/java/IBAutomater/dist/IBAutomater.jar

#### 2. C# build (with Visual Studio)

- Increment the version number in QuantConnect.IBAutomater.csproj
- Open the solution file: /IBAutomater/IBAutomater.sln
- Rebuild solution 
  - output: /IBAutomater/QuantConnect.IBAutomater/bin/Debug/QuantConnect.IBAutomater.2.0.xx.nupkg

#### 3. NuGet upload

- In the /IBAutomater/QuantConnect.IBAutomater/bin/Debug/ folder, 
  run the following commands:
```
  nuget setApiKey <api-key>
  nuget push QuantConnect.IBAutomater.2.0.xx.nupkg -Source https://api.nuget.org/v3/index.json
```


