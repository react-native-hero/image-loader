#import <React/RCTBridgeModule.h>

@interface RNTImageLoader : NSObject <RCTBridgeModule>

+ (void)init;

+ (void)loadImage:(NSString *)url onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete;

+ (void)setThumbnailImage:(UIImageView *)imageView url:(NSString *)url loading:(UIImage *)loading error:(UIImage *)error onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete;

+ (void)setHDImage:(UIImageView *)imageView url:(NSString *)url loading:(UIImage *)loading error:(UIImage *)error onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete;

+ (NSString *)getImageCachePath:(NSString *)url;

@end
