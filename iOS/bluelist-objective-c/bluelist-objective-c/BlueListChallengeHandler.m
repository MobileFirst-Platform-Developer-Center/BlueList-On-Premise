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


#import "BlueListChallengeHandler.h"
#import <IMFData/IMFData.h>

@implementation BlueListChallengeHandler

-(instancetype) init
{
    self = [super initWithRealm: CLOUDANT_SCOPE];
    return self;
}

-(BOOL) isCustomResponse:(WLResponse *)response
{
    if(response && [response getResponseJson]){
        NSDictionary *responseJSON = [response getResponseJson];
        if ([responseJSON objectForKey:@"authRequired"]) {
            NSNumber *authRequired = (NSNumber*) [responseJSON objectForKey:@"authRequired"];
            return [authRequired boolValue];
        }
    }
    return NO;
}

-(void) handleChallenge:(WLResponse *)response
{
    NSLog(@"Inside handleChallenge - silently loggin in");
    WLProcedureInvocationData *invocationData = [[WLProcedureInvocationData alloc] initWithAdapterName:@"CloudantAuthenticationAdapter" procedureName:@"submitAuthentication"];
    invocationData.parameters = @[@"james", @"42"];
    
    [self submitAdapterAuthentication: invocationData options: nil];
}

-(void) onSuccess:(WLResponse *)response
{
    [self submitSuccess: response];
}

-(void) onFailure:(WLFailResponse *)response
{
    [self submitFailure: response];
}


@end