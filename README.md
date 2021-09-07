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

// Get events once the IB Gateway has exited.
_ibAutomater.Exited += OnIbAutomaterExited;

// Get events once the IB Gateway has auto-restarted.
_ibAutomater.Restarted += OnIbAutomaterRestarted;

// Trigger the IB Gateway to start and login with your configured parameters.
_ibAutomater.Start(false);

// Stop IB Gateway with a simple command.
_ibAutomater.Stop();
```

## How it works

IBAutomater has been implemented as two components:

1. a Java component (.jar file)

    The IBAutomater.jar file is loaded as a Java agent into the IB Gateway process with the following advantages:
    
    - remove the requirement of installing Java separately as IB Gateway uses a bundled version of the Java runtime
    - avoid dependencies on the IB Gateway jar libraries
    - allow us to handle auto-restarts requiring full authentication only weekly (instead of daily)

    More information on the Java Instrumentation API can be found [here](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)

    This component is responsible for interacting with the IB Gateway user interface, performing the following tasks:

    - entering login credentials
    - configuring IB Gateway settings
    - dismissing message and confirmation windows
  
2. a C# component (.dll file), .NET assembly built for both NET Standard 2.0 and NET 5.0

    This component takes care of the following tasks:

    - starting and stopping the IB Gateway
    - notifying the client application of any error conditions
    - firing events such as `Exited` and `Restarted`

## IB Gateway auto-restarts

As the IB Gateway requires a daily shutdown (or restart), 
IBAutomater always selects auto-restart instead of auto-logoff 
because it allows users with 2FA enabled to authenticate only once per week instead of each day.

IBAutomater handles the detection of auto-restarts and notifies the client application with events.
These are the two events that have to be handled by the client:
1. `Restarted` - The IB Gateway was auto-restarted with no authentication required (soft restart),
this happens every day at the configured auto-restart time.

> For now IBAutomater does not set the auto-restart time 
(the default time is 11:45 PM, in the system timezone), 
but this setting might be configurable in future versions. 

2. `Exited` - The IB Gateway was closed by IBAutomater (because full authentication is required or there was an error)
and the client application is responsible for 
restarting the IB Gateway via the `Start()` method.

###### Example auto-restart event handling code

``` C#

private void OnIbAutomaterExited(object sender, ExitedEventArgs e)
{
    // check if IB Gateway was closed because of an IBAutomater error
    var result = _ibAutomater.GetLastStartResult();
    CheckIbAutomaterError(result);

    if (!result.HasError)
    {
        // IB Gateway was closed by IBAutomater because the auto-restart token expired or it was closed manually (less likely)
        Console.WriteLine("OnIbAutomaterExited(): IB Gateway close detected, restarting IBAutomater in 10 seconds...");

        // Wait a few seconds for IB Gateway to shutdown
        Thread.Sleep(TimeSpan.FromSeconds(10));

        try
        {
            // Close the client API connection
            Disconnect();

            // Restart IB Gateway
            CheckIbAutomaterError(_ibAutomater.Start(false));

            // Open the client API connection
            Connect();
        }
        catch (Exception exception)
        {
            Console.WriteLine($"OnIbAutomaterExited(): IBAutomaterRestartError - {exception}");
        }
    }
}

private void OnIbAutomaterRestarted(object sender, EventArgs e)
{
    // check if IB Gateway was closed because of an IBAutomater error
    var result = _ibAutomater.GetLastStartResult();
    CheckIbAutomaterError(result);

    if (!result.HasError)
    {
        // IB Gateway was restarted automatically
        Console.WriteLine("OnIbAutomaterRestarted(): IB Gateway restart detected, reconnecting...");

        try
        {
            // Close the client API connection
            Disconnect();

            // Open the client API connection
            Connect();
        }
        catch (Exception exception)
        {
            Console.WriteLine($"OnIbAutomaterRestarted(): IBAutomaterAutoRestartError - {exception}");
        }
    }
}

private void CheckIbAutomaterError(StartResult result)
{
    if (result.HasError)
    {
        // notify the user that an IBAutomater error has occurred
        Console.WriteLine($"CheckIbAutomaterError(): {result.ErrorCode} - {result.ErrorMessage}");
    }
}
```

## Two-factor authentication (2FA)

The only 2FA method supported by IBAutomater is the IBKR mobile application with seamless authentication enabled.

The 2FA request will only be sent to the phone when logging in for the first time at startup and once per week after the weekly auto-restart.

When this method is selected, IB Gateway shows a 2FA popup window (after the user/password credentials have been validated)
and waits for the user to complete the authentication by entering the PIN on the phone.

If the PIN is correct, the popup window will automatically close and both IB Gateway and IBAutomater will proceed normally.

If the PIN is incorrect (or the user does not send a PIN within 3 minutes) the window will automatically close/reopen 
and IB Gateway will send another 2FA request to the phone.

After three failed attempts, IBAutomater will be terminated and 
show the following error message:
```
The two factor authentication request timed out. 
The request must be confirmed within 3 minutes.
```

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


