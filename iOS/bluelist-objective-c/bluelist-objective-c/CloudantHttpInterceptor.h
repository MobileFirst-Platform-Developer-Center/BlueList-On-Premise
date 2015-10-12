//
//  CloudantHttpInterceptor.h
//  bluelist-objective-c
//
//  Created by Kevan Holdaway on 10/9/15.
//  Copyright © 2015 IBM. All rights reserved.
//

#import <Foundation/Foundation.h>

#import <CloudantSync.h>

@interface CloudantHttpInterceptor : NSObject<CDTHTTPInterceptor>

@property (readonly) NSString *sessionCookie;

-(instancetype)initWithSessionCookie:(NSString*) sessionCookie refreshUrl:(NSURL*)refreshSessionCookieUrl;

@end
