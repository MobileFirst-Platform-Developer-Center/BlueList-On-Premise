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