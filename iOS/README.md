##Sample: mfp-bluelist-on-premises

The mfp-bluelist-on-premises iOS sample is a simple todo task list applications. In mfp-bluelist-on-premises for iOS, the tasks persist to a mobile backend, using the Cloudant NoSQL DB service.

This sample will leverage the technology available in the IBM MobileFirst Foundation to allow us to store data for this mobile application. This will be accomplished by utilizing the IBM MobileFirst Platform Cloudant Data Layer Local Edition. The Cloudant Data Layer Local Edition is an advanced NoSQL database that is capable of handling a wide variety of data types, such as JSON, full-text, and geospatial data. This sample shows how to integrate and utilize the proper SDKs and APIs into an iOS application.

##Downloading the samples

You can clone the samples from IBM DevOps Services with the following command:

`
git clone https://github.com/MobileFirst-Platform-Developer-Center/BlueList-On-Premise
`
##Before you run the sample:

- You will need to have an instance of [IBM MobileFirst Platform Cloudant Data Layer Local Edition](http://www-01.ibm.com/support/knowledgecenter/SSTPQH_1.0.0/com.ibm.cloudant.local.install.doc/topics/clinstall_cloudant_local_overview.html) configured and started.

- You will need the MobileFirst Platform Server instance installed and configured. One way to accomplish this is by [installing the MobileFirst CLI](http://ibm.biz/knowctr#SSHS8R_7.0.0/com.ibm.worklight.installconfig.doc/dev/t_wl_installing_cli.html)

- You will need a proxy to communicate with Cloudant and the Mobile First PLatform --> [Installing the MobileFirst Data Proxy Server](http://ibm.biz/knowctr#SSHS8R_7.0.0/com.ibm.worklight.installconfig.doc/install_config/t_installing_imf_datastore.html)

## Worklight Server Setup Using MobileFirst CLI

Navigate to the BlueListProxy directory.
`
$> cd BlueList-On-Premise/BlueListProxy/
`

You will need to open BlueListProxy/server/conf/worklight.properties and configure the following properties:
  - CloudantProtocol=https
  - CloudantHost=<enter cloudant host here>
  - CloudantPort=443
  - CloudantUsername=<enter cloudant user here>
  - CloudantPassword=<enter cloudant password here>

Edit the server/conf/worklight.properties file.  Change the CloudantProtocol, CloudantHost, CloudantPort, CloudantUsername, and CloudantPassword values to match your IBM MobileFirst Platform Cloudant Data Layer Local Edition access information.

Complete the following calls in the BlueListProxy directory:
- `$> mfp create-server`
- `$> mfp start`
- `$> mfp console`

At this point, a browser will open and you should see the Worklight console.  The default login credentials are user 'admin' with password 'admin'.  There should be 2 applications deployed and 2 adapters.

##Obtaining and configuring the required SDKs
Make sure you have correctly installed and set up CocoaPods. If you have not done so, complete the following:

Install CocoaPods with the following command in Terminal:

`$ sudo gem install cocoapods`

Setup CocoaPods using the following terminal command:

`$ pod setup`

The next step is to download and install the required dependencies for this project using the Podfile that has been provided. Navigate to the Xcode project directory in terminal (/BlueList-On-Premise/iOS/bluelist-objective-c/ or /BlueList-On-Premise/iOS/bluelist-swift/ in the sample provided) and run the following command:

`$ pod install`

After the dependencies have been installed open the bluelist-objective-c.xcworkspace or bluelist-swift.xcworkspace. When using CocoaPods you must use the .xcworkspace instead of the .xcodeproject file because of how the dependencies are configured.

NOTE: For the Swift sample you must run and compile the application on the latest version of Xcode. For iOS9 some changes may be required to your application/server. Please see the following release document for more information:

[Support for iOS9] (https://developer.ibm.com/mobilefirstplatform/2015/09/09/mobilefirst-platform-support-for-ios-9/)

Once the project is open please check and update the following items:

- In the bluelist.plist file set the cloudantProxyUrl to your MobileFirst Data Proxy Server location.
- In the worklight.plist file make sure to check and update the values to match the MobileFirst Platform Server you have deployed.

Now we can run the sample on the simulators provided by Xcode or a supported iOS device.

## For More Info on How to Run this Sample and How It Works See the [Getting Started Module](https://developer.ibm.com/mobilefirstplatform/documentation/getting-started-7-1/foundation/data/working-with-cloudant-nosql-db-api/ios/)
