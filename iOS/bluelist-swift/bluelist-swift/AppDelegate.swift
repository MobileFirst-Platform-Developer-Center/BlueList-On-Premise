// Copyright 2014, 2015 IBM Corp. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//  Use this file to import your target's public headers that you would like to expose to Swift.
//

import UIKit

let IBM_SYNC_ENABLE = true

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    var testNavigationController: UINavigationController?
  
    

    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
    
        let wlClient = [WLClient.sharedInstance()]
        let challengeHandler = BlueListChallengeHandler();
        WLClient.sharedInstance().registerChallengeHandler(challengeHandler)
        // Override point for customization after application launch.
        
        //for debuging different authentication methods, this clears the backend oauth token, if a token is found then it's used
        //clearKeychain()
        
      
        
        
        return true
    }
    
    func applicationWillResignActive(application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    }
    
    func applicationDidEnterBackground(application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
        
        // Consider if these calls should be in applicationDidEnterBackground and/or other application lifecycle event.
        // Perhaps [IMFLogger send]; should only happen when the end-user presses a button to do so, for example.
        // CAUTION: the URL receiving the uploaded log and analytics payload is auth-protected, so these calls
        // should only be made after authentication, otherwise your end-user will receive a random auth prompt!
       // if isUserAuthenticated {
          //     }
        
    }
    
    func applicationWillEnterForeground(application: UIApplication) {
        // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    }
    
    func applicationDidBecomeActive(application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }
    
    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    
    func application(application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: NSError) {
          }
    
    func application(application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: NSData) {
 
    }
    
    func application(application: UIApplication, didReceiveRemoteNotification userInfo: [NSObject : AnyObject], fetchCompletionHandler completionHandler: (UIBackgroundFetchResult) -> Void) {
    }
    
    func application(application: UIApplication, handleActionWithIdentifier identifier: String?, forRemoteNotification userInfo: [NSObject : AnyObject], completionHandler: () -> Void) {    }
    
    // helper functions for debuging
    func deleteAllKeysForSecClass(secClass: CFTypeRef) {
        let dict = NSMutableDictionary()
        let kSecAttrAccessGroupSwift = NSString(format: kSecClass)
        dict.setObject(secClass, forKey: kSecAttrAccessGroupSwift)
        SecItemDelete(dict)
    }
    
    func clearKeychain () {
        deleteAllKeysForSecClass(kSecClassIdentity)
        deleteAllKeysForSecClass(kSecClassGenericPassword)
        deleteAllKeysForSecClass(kSecClassInternetPassword)
        deleteAllKeysForSecClass(kSecClassCertificate)
        deleteAllKeysForSecClass(kSecClassKey)
    }
      
    
}

