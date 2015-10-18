/**
* Copyright 2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#import "CloudantHttpInterceptor.h"
#import <IBMMobileFirstPlatformFoundation/IBMMobileFirstPlatformFoundation.h>

#define COOKIE_HEADER @"Cookie"

@interface CloudantHttpInterceptor ()

@property (readwrite) NSString *sessionCookie;
@property (readwrite) NSURL *refreshSessionCookieUrl;

@end

@implementation CloudantHttpInterceptor

-(instancetype)initWithSessionCookie:(NSString *)sessionCookie refreshUrl:(NSURL *)refreshSessionCookieUrl
{
    self = [super self];
    if(self){
        _sessionCookie = nil;//sessionCookie;
        _refreshSessionCookieUrl = refreshSessionCookieUrl;
    }
    return self;
}

-(CDTHTTPInterceptorContext*)interceptRequestInContext:(CDTHTTPInterceptorContext *)context
{
    if(![context.request valueForHTTPHeaderField:COOKIE_HEADER]){
        if(!self.sessionCookie){
            dispatch_semaphore_t sessionCookieSemaphore = dispatch_semaphore_create(0);

            [self obtainSessionCookie:^(NSError *error) {
                [context.request addValue:self.sessionCookie forHTTPHeaderField:COOKIE_HEADER];
                dispatch_semaphore_signal(sessionCookieSemaphore);
            }];

            dispatch_semaphore_wait(sessionCookieSemaphore, DISPATCH_TIME_FOREVER);
        }else{
            [context.request addValue:self.sessionCookie forHTTPHeaderField:COOKIE_HEADER];
        }
    }
    return context;
}

-(CDTHTTPInterceptorContext*)interceptResponseInContext:(CDTHTTPInterceptorContext *)context
{
    if(context.response.statusCode == 401 || context.response.statusCode == 403){
        dispatch_semaphore_t sessionCookieSemaphore = dispatch_semaphore_create(0);

        [self obtainSessionCookie:^(NSError *error) {
            [context.request addValue:self.sessionCookie forHTTPHeaderField:COOKIE_HEADER];
            context.shouldRetry = YES;
            dispatch_semaphore_signal(sessionCookieSemaphore);
        }];

        dispatch_semaphore_wait(sessionCookieSemaphore, DISPATCH_TIME_FOREVER);
    }
    return context;
}

-(void) obtainSessionCookie: (void(^)(NSError *error)) completionHandler
{

    [[WLAuthorizationManager sharedInstance]obtainAuthorizationHeaderForScope:@"cloudant" completionHandler:^(WLResponse *response, NSError *error) {
        if(error){
            completionHandler(error);
        }else{
            WLResourceRequest *request = [WLResourceRequest requestWithURL:self.refreshSessionCookieUrl method:@"POST" timeout:60];
            [request sendWithCompletionHandler:^(WLResponse *response, NSError *error) {
                if(error){
                    completionHandler(error);
                    return;
                }

                NSInteger httpStatus = ((NSHTTPURLResponse*)response).statusCode;
                if(httpStatus != 200){
                    completionHandler([NSError errorWithDomain:@"BlueList" code:42 userInfo:@{NSLocalizedDescriptionKey : [NSString stringWithFormat:@"Invalid HTTP Status %ld.  Check NodeJS application on Bluemix", httpStatus]}]);
                    return;
                }

                NSData *data = response.responseData;
                if(data){
                    NSError *jsonError = nil;
                    NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData: data options:0 error: &jsonError];
                    if(!jsonError && jsonObject){
                        NSString *sessionCookie = jsonObject[@"sessionCookie"];
                        self.sessionCookie = sessionCookie;
                        completionHandler(nil);
                    }else{
                        completionHandler(jsonError);
                    }
                }else{
                    completionHandler([NSError errorWithDomain:@"BlueList" code:42 userInfo:@{NSLocalizedDescriptionKey : @"No JSON data returned from enroll call.  Check NodeJS application on Bluemix"}]);
                }
            }];
        }
    }];
}

@end
