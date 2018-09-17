# Kinemic SDK Android Samples

This repository contains some sample applications showcasing the usage of the Kinemic SDK to integrate gesture control into an android application.
For more information about the SDK and our solutions for gesture interaction visit us at: https://kinemic.com.

## Getting Started

After cloning the repository you will have to provide your credentials to [Kinemic's Artifactory](https://kinemic.jfrog.io) to access our SDK library artifacts.

To do so, edit the `gradle.properties` file and update the following lines with your credentials:

```
artifactory_user=your_kinemic_artifactory_user
artifactory_password=your_password_or_API_Key
```

If you do not yet have credentials for Kinemic's Artifactory, please contact our [support](mailto:support@kinemic.de).

## Engine Application

This sample project instanciates the de.kinemic.sdk.Emgine in the Application class as a singleton object. This is the recommended approach for most use-cases.
Activities can get this instance from the Application and register/unregister listeners in onResume()/onPause() lifecycle callbacks.

The Application disconnects connected sensors and releases ressources when the user leaves the app.

## Engine Service

This sample project uses a local bound Service to hold the de.kinemic.sdk.Engine instance. Activities bind to the service to get the engine instance.
With the service it is possible to hold the sensor connection when the app is in the background. The service can also be extended as a foreground service, to indicate sensor connection via a noification and an icon in the taskbar.
