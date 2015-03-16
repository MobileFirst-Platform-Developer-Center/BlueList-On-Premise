##Sample: mfp-bluelist-on-premises

The mfp-bluelist-on-premises samples are simple todo task list applications. In mfp-bluelist-on-premises, the tasks persist to a mobile backend, using the Cloudant NoSQL DB service.

This sample will leverage the technology available in the IBM MobileFirst Foundation to allow us to store data for this mobile application. This will be accomplished by utilizing the IBM MobileFirst Platform Cloudant Data Layer Local Edition. The Cloudant Data Layer Local Edition is an advanced NoSQL database that is capable of handling a wide variety of data types, such as JSON, full-text, and geospatial data. This sample shows how to integrate and utilize the proper SDKs and APIs into iOS and Android MobileFirst applications.

##Downloading the samples

You can clone the samples from IBM DevOps Services with the following command:

`
git clone https://hub.jazz.net/git/mobilecloud/mfp-bluelist-on-premises
`
##Before you run the sample:

- You will need to have an instance of [IBM MobileFirst Platform Cloudant Data Layer Local Edition](http://www-01.ibm.com/support/knowledgecenter/SSTPQH_1.0.0/com.ibm.cloudant.local.install.doc/topics/clinstall_cloudant_local_overview.html) configured and started.

- You will need the MobileFirst Platform Server instance installed and configured. One way to accomplish this is by [installing the MobileFirst CLI](http://ibm.biz/knowctr#SSHS8R_7.0.0/com.ibm.worklight.installconfig.doc/dev/t_wl_installing_cli.html)

- You will need a proxy to communicate with Cloudant and the Mobile First PLatform --> [Installing the MobileFirst Data Proxy Server](http://ibm.biz/knowctr#SSHS8R_7.0.0/com.ibm.worklight.installconfig.doc/install_config/t_installing_imf_datastore.html)

## Worklight Server Setup Using MobileFirst CLI

Navigate to the BlueList directory.
`
$> cd mfp-bluelist-on-premises/BlueList/
`

Complete the following calls in the BlueList directory:
- `$> mfp create-server`
- `$> mfp start`
- `$> mfp console`

At this point, a browser will open and you should see the Worklight console.  The default login credentials are user 'admin' with password 'admin'.  There should be 2 applications deployed and 1 adapter.

## Running the Android sample (mfp-bluelist-on-premises/android)

See the instructions in the android/README.md file.

## Running the iOS sample (mfp-bluelist-on-premises/iOS)

See the instructions in the iOS/README.md file.

## For More Info on How to Run the Samples and How They Work See the [Getting Started Module](https://developer.ibm.com/mobilefirstplatform/documentation/getting-started-7-0/advanced-topics/cloudant-nosql-db-api/)
