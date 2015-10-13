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

#import "TableViewController.h"
#import "AppDelegate.h"
#import <CloudantSync.h>
#import <CloudantSyncEncryption.h>
#import <IBMMobileFirstPlatformFoundation/IBMMobileFirstPlatformFoundation.h>
#import "CloudantHttpInterceptor.h"

#define DATATYPE_FIELD @"@datatype"
#define DATATYPE_VALUE @"TodoItem"

#define NAME_FIELD @"name"
#define PRIORITY_FIELD @"priority"

@interface TableViewController () <UITextFieldDelegate, CDTReplicatorDelegate,UIAlertViewDelegate>

@property UIImage* highImage;
@property UIImage* mediumImage;
@property UIImage* lowImage;
@property (strong, nonatomic) IBOutlet UISegmentedControl *segmentFilter;

@property (strong, nonatomic) IBOutlet UIBarButtonItem *settingsButton;
- (IBAction)filterTable:(UISegmentedControl *)sender;

// Items in list
@property NSMutableArray *itemList;
@property NSMutableArray* filteredListItems;

// Cloud sync properties
@property CDTDatastoreManager *datastoreManager;
@property CDTDatastore *datastore;

@property CDTReplicatorFactory *replicatorFactory;

@property CDTPullReplication *pullReplication;
@property CDTReplicator *pullReplicator;

@property CDTPushReplication *pushReplication;
@property CDTReplicator *pushReplicator;

@property BOOL doingPullReplication;

@property NSString *dbName;
@property NSURL *remotedatastoreurl;
@property id<CDTHTTPInterceptor> cloudantHttpInterceptor;

@property NSString *bluelistProxyAdapterURL;

@end

@implementation TableViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.highImage = [UIImage imageNamed:@"PriorityHigh.png"];
    self.mediumImage = [UIImage imageNamed:@"PriorityMedium.png"];
    self.lowImage = [UIImage imageNamed:@"PriorityLow.png"];
    
    
    self.itemList = [[NSMutableArray alloc]init];
    self.filteredListItems = [[NSMutableArray alloc]init];
    
    [self setupIMFDatabase];
    
    // Setting up the refresh control
    self.settingsButton.enabled = false;
    self.refreshControl = [[UIRefreshControl alloc]init];
    [self.refreshControl addTarget:self action:@selector(handleRefreshAction) forControlEvents:UIControlEventValueChanged];
    
    [self.refreshControl beginRefreshing];
    
    //logger
    NSLog(@"this is a info test log in ListTableViewController:viewDidLoad");
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Data Management

- (void) setupIMFDatabase {
    //Read the bluelist.plist
    NSString *configurationPath = [[NSBundle mainBundle]pathForResource:@"bluelist" ofType:@"plist"];
    NSDictionary *configuration = [NSDictionary dictionaryWithContentsOfFile:configurationPath];
    NSString *encryptionPassword = configuration[@"encryptionPassword"];
    self.bluelistProxyAdapterURL = configuration[@"bluelistProxyAdapterURL"];
    
    [self obtainUser:^(NSString *userId, NSError *error) {
        NSMutableString *errorMsg = [[NSMutableString alloc] init];
        if(error || !userId){
            [errorMsg appendString:@"Error obtaining Authentication Header.\nCheck to see if Authentication settings in the Info.plist match exactly to the ones in MCA, or check the applicationId and applicationRoute in bluelist.plist\n\n"];
            if (error != nil && error.userInfo != nil) {
                [errorMsg appendString:error.userInfo.description];
            }
            NSLog(@"%@", errorMsg);
            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:errorMsg delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
            [alert show];
        }else {
            NSLog(@"Authenticated user with id %@",userId);
            [self enrollUserWithCompletionHandler:^(NSString *dbname, NSError *error) {
                BOOL encryptionEnabled = NO;
                if(error){
                    dispatch_sync(dispatch_get_main_queue(), ^{
                        NSString *errorMsg = [NSString stringWithFormat:@"Enroll failed to create remote cloudant database for %@.  Error: %@", userId, error];
                        NSLog(@"%@", errorMsg);

                        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:errorMsg delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
                        [alert show];
                    });
                }else{
                    if(!encryptionPassword || [encryptionPassword isEqualToString:@""]){
                        encryptionEnabled = NO;
                    }
                    else{
                        encryptionEnabled = YES;
                        self.dbName = [self.dbName stringByAppendingString:@"secure"];
                    }
                    
                    //create CDTEncryptionKeyProvider
                    id<CDTEncryptionKeyProvider> keyProvider=nil;
                    NSError *error = nil;
                    
                    
                    // Create CDTDatastoreManager
                    NSFileManager *fileManager= [NSFileManager defaultManager];
                    NSURL *documentsDir = [[fileManager URLsForDirectory:NSDocumentDirectory
                                                               inDomains:NSUserDomainMask] lastObject];
                    NSURL *storeURL = [documentsDir URLByAppendingPathComponent: @"bluelistdir"];
                    
                    BOOL isDir;
                    BOOL exists = [fileManager fileExistsAtPath:[storeURL path] isDirectory:&isDir];
                    
                    if (exists && !isDir) {
                        if (error) {
                            NSLog(@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@.", documentsDir);
                            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:[NSString stringWithFormat:@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@", documentsDir ] delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
                            [alert show];
                        }
                        return;
                    }
                    
                    if (!exists) {
                        [fileManager createDirectoryAtURL:storeURL withIntermediateDirectories:YES attributes:nil error:&error];
                        if(error){
                            NSLog(@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@.", documentsDir);
                            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:[NSString stringWithFormat:@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@", documentsDir ] delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
                            [alert show];
                            return;
                        }
                    }
                    
                    self.datastoreManager = [[CDTDatastoreManager alloc] initWithDirectory:[storeURL path] error:&error];
                    if(error){
                        NSLog(@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@.", documentsDir);
                        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:[NSString stringWithFormat:@"DBCreationFailure: Could not create CDTDatastoreManager with directory %@", documentsDir ] delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
                        [alert show];
                        return;
                    }
                    
                    //create a local data store. Encrypt the local store if the setting is enabled
                    if(encryptionEnabled){
                        //Initalize the key provider
                        keyProvider = [CDTEncryptionKeychainProvider providerWithPassword:encryptionPassword forIdentifier:@"bluelist"];
                        NSLog(@"Attempting to create an ecrypted local data store");
                        //Initialize an encrypted local store
                        self.datastore = [self.datastoreManager datastoreNamed:self.dbName withEncryptionKeyProvider:keyProvider error:&error];
                    }else{
                        self.datastore = [self.datastoreManager datastoreNamed:self.dbName error:&error];
                    }
                    
                    // Setup required indexes for Query
                    [self.datastore ensureIndexed:@[DATATYPE_FIELD] withName:@"datatypeindex"];
                    
                    if (error) {
                        NSLog(@"DBCreationFailure: Could not create DB with name %@.", self.dbName);
                        if(encryptionEnabled){
                            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:@"Could not create an encrypted local store with credentials provided. Check the encryptionPassword in the bluelist.plist file." delegate:self cancelButtonTitle:@"Okay" otherButtonTitles:nil, nil];
                            [alert show];
                        }
                    }
                    else{
                        NSLog(@"Local data store created successfully");
                    }
                    
                    
                    
                    //create a replication push and pull objects using the local datastore and remote datastore url
                    self.replicatorFactory = [[CDTReplicatorFactory alloc]initWithDatastoreManager:self.datastoreManager];
                    
                    self.pullReplication = [CDTPullReplication replicationWithSource:self.remotedatastoreurl target:self.datastore];
                    [self.pullReplication addInterceptor:self.cloudantHttpInterceptor];
                    
                    self.pushReplication = [CDTPushReplication replicationWithSource:self.datastore target:self.remotedatastoreurl];
                    [self.pushReplication addInterceptor:self.cloudantHttpInterceptor];
                    
                    [self pullItems];
                }
            }];
        }
    }];
}

- (void) obtainUser: (void(^) (NSString *user, NSError *error)) completionHandler
{
    WLAuthorizationManager *authManager = [WLAuthorizationManager sharedInstance];
    [authManager obtainAuthorizationHeaderForScope:@"cloudant" completionHandler:^(WLResponse *response, NSError *error) {
        if (error != nil) {
            completionHandler(nil, error);
        } else {
            //lets make sure we have an user id before transitioning
            if (authManager.userIdentity != nil) {
                NSString *userId = [authManager.userIdentity valueForKey:@"id"];
                completionHandler(userId, nil);
            } else {
                completionHandler(nil, [NSError errorWithDomain:@"Bluelist" code:42 userInfo:@{NSLocalizedDescriptionKey : @"Valid Authentication Header, but userIdentity not found. You have to configure one of the methods available in Advanced Mobile Service on Bluemix, such as Facebook, Google, or Custom "}]);
            }
        }
    }];
}

-(void) enrollUserWithCompletionHandler: (void(^) (NSString*dbname, NSError *error)) completionHandler
{
    NSString *enrollUrlString = [NSString stringWithFormat:@"%@/enroll", self.bluelistProxyAdapterURL];
    NSURL *enrollUrl = [NSURL URLWithString:enrollUrlString];
    
    WLResourceRequest *request = [WLResourceRequest requestWithURL:enrollUrl method:@"PUT" timeout:60];
    [request sendWithCompletionHandler:^(WLResponse *response, NSError *error) {
        if(error){
            completionHandler(nil, error);
            return;
        }
        
        NSInteger httpStatus = response.status;
        if(httpStatus != 200){
            dispatch_sync(dispatch_get_main_queue(), ^{
                completionHandler(nil ,[NSError errorWithDomain:@"BlueList" code:42 userInfo:@{NSLocalizedDescriptionKey : [NSString stringWithFormat:@"Invalid HTTP Status %ld.  Check NodeJS application on Bluemix", httpStatus]}]);
            });
            return;
        }
        
        NSData *data = response.responseData;
        if(data){
            NSError *jsonError = nil;
            NSDictionary *jsonObject = [NSJSONSerialization JSONObjectWithData: data options:0 error: &jsonError];
            if(!jsonError && jsonObject){
                NSDictionary *cloudantAccess = jsonObject[@"cloudant_access"];
                NSString *cloudantHost = cloudantAccess[@"host"];
                NSString *cloudantPort = cloudantAccess[@"port"];
                NSString *cloudantProtocol = cloudantAccess[@"protocol"];
                self.dbName = jsonObject[@"database"];
                NSString *sessionCookie = jsonObject[@"sessionCookie"];
                
                
                self.remotedatastoreurl = [NSURL URLWithString: [NSString stringWithFormat:@"%@://%@:%@/%@", cloudantProtocol, cloudantHost, cloudantPort, self.dbName]];
                
                NSString *refreshUrlString = [NSString stringWithFormat:@"%@/sessioncookie", self.bluelistProxyAdapterURL];
                self.cloudantHttpInterceptor = [[CloudantHttpInterceptor alloc]initWithSessionCookie:sessionCookie refreshUrl:[NSURL URLWithString:refreshUrlString]];
                
                completionHandler(self.dbName, nil);
            }else{
                completionHandler(nil, jsonError);
            }
        }else{
            completionHandler(nil, [NSError errorWithDomain:@"BlueList" code:42 userInfo:@{NSLocalizedDescriptionKey : @"No JSON data returned from enroll call.  Check NodeJS application on Bluemix"}]);
        }
    }];
}

- (void)listItems: (void(^)(void)) cb
{
    NSLog(@"listItems called");
    
    CDTQResultSet *resultSet = [self.datastore find:@{DATATYPE_FIELD : DATATYPE_VALUE}];
    NSMutableArray *results = [NSMutableArray array];
    
    [resultSet enumerateObjectsUsingBlock:^(CDTDocumentRevision *rev, NSUInteger idx, BOOL *stop) {
        if(!rev.deleted)
            [results addObject:rev];
    }];
    
    self.itemList = results;
    [self reloadLocalTableData];
    
    if (cb) {
        cb();
    }
}

- (void) createItem: (CDTMutableDocumentRevision*) item
{
    //save will perform a create because the item object does not exist yet in the DB.
    NSError *error = nil;
    [self.datastore createDocumentFromRevision:item error:&error];
    
    if (error) {
        NSLog(@"createItem failed with error: %@", error);
    } else {
        [self listItems:nil];
    }
}
- (void) updateItem: (CDTMutableDocumentRevision*) item
{
    //save will perform a create because the CDTDocumentRevision already exists.
    NSError *error = nil;
    [self.datastore updateDocumentFromRevision:item error:&error];
    
    if (error) {
        NSLog(@"update failed with error: %@", error);
    } else {
        [self listItems:nil];
    }
}
-(void) deleteItem: (CDTDocumentRevision*) item
{
    NSError *error = nil;
    [self.datastore deleteDocumentFromRevision:item error:&error];
    
    if (error != nil) {
        NSLog(@"deleteItem failed with error: %@", error);
    } else {
        [self listItems:nil];
    }
}


#pragma mark - Cloud Sync
// Replicate from the remote to local datastore
- (void)pullItems
{
    NSError *error = nil;
    self.pullReplicator = [self.replicatorFactory oneWay:self.pullReplication error:&error];
    if(error != nil){
        NSLog(@"Error creating oneWay pullReplicator %@", error);
    }
    
    self.pullReplicator.delegate = self;
    self.doingPullReplication = YES;
    self.refreshControl.attributedTitle = [[NSAttributedString alloc]initWithString:@"Pulling Items from Cloudant"];
    
    error = nil;
    NSLog(@"Replicating data with NoSQL Database on the cloud");
    [self.pullReplicator startWithError:&error];
    if(error != nil){
        NSLog(@"error starting pull replicator: %@", error);
    }
    
}
// Replicate data & logs from the local to remote
- (void)pushItems
{
    NSError *error = nil;
    self.pushReplicator = [self.replicatorFactory oneWay:self.pushReplication error:&error];
    if(error != nil){
        NSLog(@"error creating one way push replicator: %@", error);
    }
    
    self.pushReplicator.delegate = self;
    
    self.doingPullReplication = NO;
    self.refreshControl.attributedTitle = [[NSAttributedString alloc]initWithString:@"Pushing Items from Cloudant"];
    
    error = nil;
    [self.pushReplicator startWithError:&error];
    if(error != nil){
        NSLog(@"error starting push replicator: %@", error);
    }
}
/**
 * Called when the replicator changes state.
 */
-(void) replicatorDidChangeState:(CDTReplicator*)replicator
{
    NSLog(@"replicatorDidChangeState %@",[CDTReplicator stringForReplicatorState:replicator.state]);
}

/**
 * Called whenever the replicator changes progress
 */
-(void) replicatorDidChangeProgress:(CDTReplicator*)replicator
{
    NSLog(@"replicatorDidChangeProgress %@",[CDTReplicator stringForReplicatorState:replicator.state]);
}

/**
 * Called when a state transition to COMPLETE or STOPPED is
 * completed.
 */
- (void)replicatorDidComplete:(CDTReplicator*)replicator
{
    NSLog(@"replicatorDidComplete %@",[CDTReplicator stringForReplicatorState:replicator.state]);
    if(self.doingPullReplication){
        //done doing pull, lets start push
        [self pushItems];
    } else {
        //doing push, push is done read items from local data store and end the refresh UI
        [self listItems:^{
            self.refreshControl.attributedTitle = [[NSAttributedString alloc]initWithString:@"  "];
            NSLog(@"Done refreshing table after replication");
            [self.refreshControl performSelectorOnMainThread:@selector(endRefreshing) withObject:nil waitUntilDone:YES];
            self.settingsButton.enabled = true;
        }];
    }
    
}

/**
 * Called when a state transition to ERROR is completed.
 */
- (void)replicatorDidError:(CDTReplicator*)replicator info:(NSError*)info
{
    self.refreshControl.attributedTitle = [[NSAttributedString alloc]initWithString:@"Error replicating with Cloudant"];
    NSLog(@"replicatorDidError %@",replicator.error);
    [self listItems:^{
        [self.refreshControl performSelectorOnMainThread:@selector(endRefreshing) withObject:nil waitUntilDone:YES];
    }];
    
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 2;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if (section == 0) {
        return self.filteredListItems.count;
    } else {
        return 1;
    }
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell;
    if( indexPath.section == 0){
        cell = [tableView dequeueReusableCellWithIdentifier:@"ItemCell" forIndexPath:indexPath];
        CDTDocumentRevision *item = (CDTDocumentRevision*)self.filteredListItems[indexPath.row];
        for (UIView *view in [cell.contentView subviews]) {
            if([view isKindOfClass:[UITextField class]]){
                ((UITextField*)view).text = item.body[NAME_FIELD];
                ((UITextField*)view).tag = indexPath.row;
            }
        }
        cell.imageView.image = [self getPriorityImageForPriority:[item.body[PRIORITY_FIELD] integerValue]];
        cell.contentView.tag = 0;
    } else {
        cell = [tableView dequeueReusableCellWithIdentifier:@"AddCell" forIndexPath:indexPath];
        //later use to check if it's add textField
        cell.contentView.tag = 1;
    }
    return cell;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return indexPath.section == 0;
}

// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [self deleteItem:self.filteredListItems[indexPath.row]];
        [self.filteredListItems removeObjectAtIndex:indexPath.row];
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    }
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
    if (indexPath.section == 0) {
        [self changePriorityForCell:[self.tableView cellForRowAtIndexPath:indexPath]];
    }
}

-(void) changePriorityForCell:(UITableViewCell *) cell{
    NSInteger selectedPriority;
    NSInteger newPriority = 0;
    NSIndexPath *indexPath = [self.tableView indexPathForCell:cell];
    
    CDTMutableDocumentRevision *item = [self.filteredListItems[indexPath.row] mutableCopy];
    selectedPriority = [item.body[PRIORITY_FIELD] integerValue];
    newPriority = [self getNextPriority:selectedPriority];
    item.body[PRIORITY_FIELD] = [NSNumber numberWithInteger:newPriority];
    cell.imageView.image = [self getPriorityImageForPriority:newPriority];
    [self updateItem:item];
}

-(NSInteger) getNextPriority:(NSInteger) currentPriority{
    NSInteger newPriority;
    switch (currentPriority) {
        case 2:
            newPriority = 0;
            break;
        case 1:
            newPriority = 2;
            break;
        default:
            newPriority = 1;
            break;
    }
    return newPriority;
}

-(UIImage*) getPriorityImageForPriority:(NSInteger)priority{
    
    UIImage* resultImage;
    
    switch (priority) {
        case 2:
            resultImage = self.highImage;
            break;
        case 1:
            resultImage = self.mediumImage;
            break;
            
        default:
            resultImage = self.lowImage;
            break;
    }
    return resultImage;
}

-(NSInteger) getPriorityForString:(NSString *) priorityString{
    NSInteger priority;
    if([priorityString isEqualToString:@"High"]){
        priority = 2;
    } else if ([priorityString isEqualToString:@"Medium"]){
        priority = 1;
    } else {
        priority = 0;
    }
    return priority;
}


-(void) filterContentForPriority:(NSString *)scope {
    NSInteger priority = [self getPriorityForString:scope];
    
    if(priority == 1 || priority == 2){
        //filter base on priority
        NSIndexSet *matchSet = [self.itemList indexesOfObjectsPassingTest:^BOOL(id obj, NSUInteger idx, BOOL *stop) {
            return [((CDTDocumentRevision *)obj).body[PRIORITY_FIELD] integerValue] == priority;
        }];
        [self.filteredListItems removeAllObjects];
        self.filteredListItems = [NSMutableArray arrayWithArray:[self.itemList objectsAtIndexes:matchSet]];
    } else {
        //don't filter select all
        [self.filteredListItems removeAllObjects];
        self.filteredListItems = [NSMutableArray arrayWithArray:self.itemList];
    }
}

- (IBAction)filterTable:(UISegmentedControl *)sender {
    [self reloadLocalTableData];
}

- (BOOL) textFieldShouldReturn:(UITextField *)textField{
    [self handleTextFields:textField];
    return YES;
}

- (void) handleTextFields:(UITextField *)textField{
    if (textField.superview.tag == 1 && textField.text.length > 0) {
        [self addItemFromtextField:textField];
    } else {
        [self updateItemFromtextField:textField];
    }
    [textField resignFirstResponder];
}

- (void) updateItemFromtextField:(UITextField *)textField{
    UITableViewCell *cell = (UITableViewCell *)textField.superview.superview;
    NSIndexPath *indexPath = [self.tableView indexPathForCell:cell];
    CDTMutableDocumentRevision *item = [self.filteredListItems[indexPath.row] mutableCopy];
    item.body[NAME_FIELD] = textField.text;
    [self updateItem:item];
}

- (void) addItemFromtextField:(UITextField *)textField{
    NSInteger priority = [self getPriorityForString:[self.segmentFilter titleForSegmentAtIndex:self.segmentFilter.selectedSegmentIndex]];
    NSString *name = textField.text;
    CDTMutableDocumentRevision *item = [CDTMutableDocumentRevision revision];
    item.body = @{DATATYPE_FIELD : DATATYPE_VALUE, NAME_FIELD : name, PRIORITY_FIELD : [NSNumber numberWithInteger:priority]};
    [self createItem:item];
    textField.text = @"";
}

-(void) reloadLocalTableData
{
    [self filterContentForPriority:[self.segmentFilter titleForSegmentAtIndex:self.segmentFilter.selectedSegmentIndex]];
    [self.filteredListItems sortUsingComparator:^NSComparisonResult(CDTDocumentRevision* item1, CDTDocumentRevision* item2) {
        return [item1.body[NAME_FIELD] caseInsensitiveCompare:item2.body[NAME_FIELD]];
    }];
    
    [self.tableView performSelectorOnMainThread:@selector(reloadData) withObject:nil waitUntilDone:NO];
}

-(void) handleRefreshAction
{
    if(IBM_SYNC_ENABLE){
        [self pullItems];
    } else {
        [self listItems:^{
            [self.refreshControl performSelectorOnMainThread:@selector(endRefreshing) withObject:nil waitUntilDone:NO];
        }];
    }
}

-(void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    [NSException raise:@"ApplicationFailure" format:@"Fatal error.  Terminating Bluelist."];
}
@end
