#import "RNTImageLoader.h"

#import <SDWebImage/UIImageView+WebCache.h>
#import <SDWebImage/SDImageCache.h>
#import <SDWebImage/SDImageCodersManager.h>
#import <SDWebImageFLPlugin/SDWebImageFLPlugin.h>
#import <SDWebImageWebPCoder/SDImageWebPCoder.h>

@implementation RNTImageLoader

RCT_EXPORT_MODULE(RNTImageLoader);

+ (void)init {

    // https://github.com/SDWebImage/SDWebImage/wiki/Common-Problems
    
    // 1 Week
    SDImageCache.sharedImageCache.config.maxDiskAge = 3600 * 24 * 7;
    
    // 20 images (1024 * 1024 pixels)
    SDImageCache.sharedImageCache.config.maxMemoryCost = 1024 * 1024 * 4 * 20;
    
    // Disable weak cache, may see blank when return from background because memory cache is purged under pressure
    SDImageCache.sharedImageCache.config.shouldUseWeakMemoryCache = NO;
    
    // Use mmap for disk cache query
    SDImageCache.sharedImageCache.config.diskCacheReadingOptions = NSDataReadingMappedIfSafe;
    
    SDWebImageManager.sharedManager.optionsProcessor = [SDWebImageOptionsProcessor optionsProcessorWithBlock:^SDWebImageOptionsResult * _Nullable(NSURL * _Nullable url, SDWebImageOptions options, SDWebImageContext * _Nullable context) {
         // Disable Force Decoding in global, may reduce the frame rate
         options |= SDWebImageAvoidDecodeImage;
         return [[SDWebImageOptionsResult alloc] initWithOptions:options context:context];
    }];
    
    SDImageWebPCoder *webpCoder = [SDImageWebPCoder sharedCoder];
    [[SDImageCodersManager sharedManager] addCoder:webpCoder];
    
}

+ (void)loadImage:(NSString *)url onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete {
    
    NSURL *URL = [RNTImageLoader createImageURL:url];
    
    SDWebImageManager *manager = [SDWebImageManager sharedManager];
    
    [manager loadImageWithURL:URL
        options:0
        progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
            if (expectedSize > 0) {
                onProgress(receivedSize, expectedSize);
            }
        }
        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
            onComplete(image);
        }
    ];
    
}

+ (void)setThumbnailImage:(UIImageView *)imageView url:(NSString *)url loading:(UIImage *)loading error:(UIImage *)error onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete {
    
    NSURL *URL = [RNTImageLoader createImageURL:url];
   
    UIImage *failure = error;
    
    [imageView sd_setImageWithURL:URL
        placeholderImage:loading
        options:0
        progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
            if (expectedSize > 0) {
                onProgress(receivedSize, expectedSize);
            }
        }
        completed:^(UIImage * _Nullable image, NSError * _Nullable error, SDImageCacheType cacheType, NSURL * _Nullable imageURL) {
            if (image == nil && failure != nil) {
                imageView.image = failure;
            }
            onComplete(image);
        }
    ];
    
}

+ (void)setHDImage:(UIImageView *)imageView url:(NSString *)url loading:(UIImage *)loading error:(UIImage *)error onProgress:(void (^)(NSInteger, NSInteger))onProgress onComplete:(void (^)(UIImage*))onComplete {
    
    NSURL *URL = [RNTImageLoader createImageURL:url];
    
    UIImage *failure = error;
    
    [imageView sd_setImageWithURL:URL
        placeholderImage:loading
        options:0
        progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
            if (expectedSize > 0) {
                onProgress(receivedSize, expectedSize);
            }
        }
        completed:^(UIImage * _Nullable image, NSError * _Nullable error, SDImageCacheType cacheType, NSURL * _Nullable imageURL) {
            if (image == nil && failure != nil) {
                imageView.image = failure;
            }
            onComplete(image);
        }
    ];
    
}

+ (NSString *)getImageCachePath:(NSString *)url {
    NSString *path = [SDImageCache.sharedImageCache cachePathForKey:url];
    if (path != nil && path.length > 0) {
        return path;
    }
    return @"";
}

+ (NSURL *)createImageURL:(NSString *)url {
    NSURL *URL;
    if ([url hasPrefix:@"/"]) {
        URL = [NSURL fileURLWithPath:url];
    }
    else {
        URL = [NSURL URLWithString:url];
    }
    return URL;
}

@end
