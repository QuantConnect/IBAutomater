# QuantConnect / IBAutomater

IBAutomater is an automation tool for IB-Gateway 974 and above. It supports starting, stopping and automated logins for IB Gateway.

The built package is hosted on NuGet and can be installed to your packet manager by: `Install-Package QuantConnect.IBAutomater`.

On installation to your solution, the IBAutomator has one key class to instantiate:

```
// Create a new instance of IBAutomater
_ibAutomater = new IBAutomater.IBAutomater(twsDirectory, ibVersion, userName, password, tradingMode, port);

// You can bind to event handlers to receive the output data.
_ibAutomater.OutputDataReceived += OnIbAutomaterOutputDataReceived;

//Gracefully handle errors
_ibAutomater.ErrorDataReceived += OnIbAutomaterErrorDataReceived;

// Get events once the IBGateway has exited.
_ibAutomater.Exited += OnIbAutomaterExited;

//Trigger the IBGateway to start and login with your configured parameters.
_ibAutomater.Start(false);

// Stop IBGateway with a simple command.
_ibAutomater.Stop();
```
