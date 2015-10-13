##Sample: mfp-bluelist-on-premises

The mfp-bluelist-on-premises samples are simple todo task list applications. In mfp-bluelist-on-premises, the tasks persist to a mobile backend, using the Cloudant NoSQL DB service.

This sample will leverage the technology available in the IBM MobileFirst Foundation to allow us to store data for this mobile application. This will be accomplished by utilizing the IBM MobileFirst Platform Cloudant Data Layer Local Edition. The Cloudant Data Layer Local Edition is an advanced NoSQL database that is capable of handling a wide variety of data types, such as JSON, full-text, and geospatial data. This sample shows how to integrate and utilize the proper SDKs and APIs into iOS and Android MobileFirst applications.

##Downloading the samples

You can clone the samples from IBM DevOps Services with the following command:

`
git clone https://github.com/MobileFirst-Platform-Developer-Center/BlueList-On-Premise
`
##Before you run the sample:

- You will need to have an instance of [IBM MobileFirst Platform Cloudant Data Layer Local Edition](http://www-01.ibm.com/support/knowledgecenter/SSTPQH_1.0.0/com.ibm.cloudant.local.install.doc/topics/clinstall_cloudant_local_overview.html) configured and started.

- You will need the MobileFirst Platform Server instance installed and configured. One way to accomplish this is by [installing the MobileFirst CLI](http://ibm.biz/knowctr#SSHS8R_7.0.0/com.ibm.worklight.installconfig.doc/dev/t_wl_installing_cli.html)

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

## Running the Android sample (BlueList-On-Premise/android)

See the instructions in the android/README.md file.

## Running the iOS sample (BlueList-On-Premise/iOS)

See the instructions in the iOS/README.md file.

## For More Info on How to Run the Samples and How They Work See the [Getting Started Module](https://developer.ibm.com/mobilefirstplatform/documentation/getting-started-7-1/foundation/data/working-with-cloudant-nosql-db-api/)

## License
This package contains sample code provided in source code form. The samples are licensed under the under the Apache License, Version 2.0 (the "License").  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 and may also view the license in the license.txt file within this package.  Also see the notices.txt file within this package for additional notices.

