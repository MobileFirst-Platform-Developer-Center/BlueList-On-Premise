// Copyright 2014 IBM Corp. All Rights Reserved.
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

#import <UIKit/UIKit.h>
#import <CloudantToolkit/CloudantToolkit.h>
#import <IMFData/IMFData.h>
#import "DDASLLogger.h"
#import "DDTTYLogger.h"
#import "DDFileLogger.h"
#import <CloudantSync.h>
#import "BlueListChallengeHandler.h"


#define IBM_SYNC_ENABLE YES

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (nonatomic, retain) CDTStore* datastore;
@property (nonatomic, retain) CDTStore* remotedatastore;
@property (nonatomic, retain) CDTPullReplication *pull;
@property (nonatomic, retain) CDTPushReplication *push;
@property BOOL ready;

@end
