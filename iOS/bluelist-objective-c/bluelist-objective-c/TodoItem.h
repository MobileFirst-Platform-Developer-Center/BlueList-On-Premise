#import <CloudantToolkit/CloudantToolkit.h>

@interface TodoItem : NSObject <CDTDataObject>
@property NSString *name;
@property NSNumber *priority;
//Required by the IMFDataObject protocol
@property (strong, nonatomic, readwrite) CDTDataObjectMetadata *metadata;
@end