# Kinemic SDK Android Samples

This repository contains a sample application showcasing the usage of the Kinemic SDK to integrate gesture control into an android application.
For more information about the SDK and our solutions for gesture interaction visit us at: https://kinemic.com.

## Engine Application

This sample project instanciates the de.kinemic.sdk.Engine using our custom Application class as a singleton object. This is the recommended approach for most use-cases.
Activities can get this instance from the Application and register/unregister listeners in onResume()/onPause() lifecycle callbacks.

The Application disconnects connected sensors and releases ressources when the user leaves the app.
