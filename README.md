# Kinemic SDK Android Samples

This repository contains some sample applications showcasing the usage of the Kinemic SDK to integrate gesture control into a android application.
For more information about the SDK and our solutions for gesture interaction visit us at: https://kinemic.com.

## Engine Application

This sample project instanciates the de.kinemic.sdk.Emgine in the Application class as a singleton object.
Activities can get this instance from the Application and register/unregister listeners in onResume()/onPause() lifecycle callbacks.

The Application disconnects connected sensors and releases ressources when the user leaves the app.

## Engine Service

This sample project uses a local bound Service to hold the de.kinemic.sdk.Engine instance. Activities bind to the service to get the engine instance.
With the service it is possible to hold the sensor connection when the app is in the background. The service can also be extended as a foreground service, to indicate sensor connection via a noification and an icon in the taskbar.

