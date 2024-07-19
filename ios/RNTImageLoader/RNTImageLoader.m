#import "RNTImageLoader.h"
#import <React/RCTConvert.h>

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

+ (UIImage *)getBase64Image:(NSString *)base64 {
    NSData *imageData = [[NSData alloc]
                initWithBase64EncodedString:base64
                options:NSDataBase64DecodingIgnoreUnknownCharacters];

    return [UIImage imageWithData:imageData];
}

+ (NSString *)saveImage:(UIImage *)image dirName:(NSSearchPathDirectory) dirName fileName:(NSString *)fileName {

    NSArray *paths = NSSearchPathForDirectoriesInDomains(dirName, NSUserDomainMask, YES);
    NSString *filePath = [[paths objectAtIndex:0] stringByAppendingPathComponent:fileName];

    BOOL result = [UIImagePNGRepresentation(image) writeToFile:filePath atomically:YES];
    if (result == YES) {
        return filePath;
    }

    return @"";

}

RCT_EXPORT_METHOD(saveBase64Image:(NSDictionary*)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *base64 = [RCTConvert NSString:options[@"base64"]];
    NSString *name = [RCTConvert NSString:options[@"name"]];

    UIImage *image = [RNTImageLoader getBase64Image:base64];
    NSString *filePath = [RNTImageLoader saveImage:image dirName:NSDocumentDirectory fileName:name];

    resolve(@{
        @"path": filePath,
    });

}

RCT_EXPORT_METHOD(decodeImageQRCode:(NSDictionary*)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *path = [RCTConvert NSString:options[@"path"]];

    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    dispatch_async(queue, ^{
        UIImage *image = [UIImage imageWithContentsOfFile:path];
        if (image == nil) {
            reject(@"-1", @"image file is not found.", nil);
            return;
        }

        CIContext * context = [CIContext contextWithOptions:nil];
        NSDictionary * param = [NSDictionary dictionaryWithObject:CIDetectorAccuracyHigh forKey:CIDetectorAccuracy];
        CIDetector * detector = [CIDetector detectorOfType:CIDetectorTypeQRCode context:context options:param];
        NSArray *features = [detector featuresInImage:[CIImage imageWithCGImage:image.CGImage]];

        if (features.count > 0) {
            CIQRCodeFeature *feature = [features objectAtIndex:0];
            resolve(@{
                @"text": feature.messageString,
            });
        }
        else {
            resolve(@{
                @"text": @"",
            });
        }
    });

}

RCT_EXPORT_METHOD(compressImage:(NSDictionary*)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    NSString *path = [RCTConvert NSString:options[@"path"]];
    int size = [RCTConvert int:options[@"size"]];
    int width = [RCTConvert int:options[@"width"]];
    int height = [RCTConvert int:options[@"height"]];
    int maxSize = [RCTConvert int:options[@"maxSize"]];
    int maxWidth = [RCTConvert int:options[@"maxWidth"]];
    int maxHeight = [RCTConvert int:options[@"maxHeight"]];
    
    if (maxWidth == 0) {
        maxWidth = width;
    }
    if (maxHeight == 0) {
        maxHeight = height;
    }

    if (size <= maxSize && width <= maxWidth && height <= maxHeight) {
        resolve(@{
            @"path": path,
            @"size": @(size),
            @"width": @(width),
            @"height": @(height)
        });
        return;
    }

    UIImage *image = [UIImage imageWithContentsOfFile:path];
    if (image == nil) {
        reject(@"-1", @"image file is not found.", nil);
        return;
    }

    float ratio = (float)width / (float)height;
    BOOL decreaseWidth = width < height;

    NSString *outputDir = NSTemporaryDirectory();
    if (outputDir == nil) {
        outputDir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    }
    
    BOOL isPNGImage = NO;
    NSString *outputExtername = @".jpg";
    
    NSString *pathExtension = [NSURL fileURLWithPath:path].pathExtension;
    if ([[pathExtension uppercaseString] isEqual: @"PNG"]) {
        isPNGImage = YES;
        outputExtername = @".png";
    }

    NSString *uuid = [[NSUUID UUID] UUIDString];
    NSString *outputFile = [NSString stringWithFormat: @"%@%@%@", outputDir, uuid, outputExtername];

    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);

    dispatch_async(queue, ^{
        
        int outputSize = 0;
        int outputWidth = 0;
        int outputHeight = 0;
        if (maxWidth < maxHeight) {
            outputWidth = maxWidth;
            outputHeight = (int)((float)outputWidth / ratio);
        }
        else {
            outputHeight = maxHeight;
            outputWidth = (int)((float)outputHeight * ratio);
        }

        UIImage *outputImage = image;
        NSData *outputData = nil;

        BOOL success = NO;

        while (outputWidth > 0 && outputHeight > 0) {

            CGSize cgSize = CGSizeMake((CGFloat)outputWidth, (CGFloat)outputHeight);
            CGRect cgRect = CGRectMake(0, 0, (CGFloat)outputWidth, (CGFloat)outputHeight);
            UIGraphicsBeginImageContextWithOptions(cgSize, NO, 1);
            [image drawInRect:cgRect];
            outputImage = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();

            if (isPNGImage) {
                outputData = UIImagePNGRepresentation(outputImage);
            }
            else {
                outputData = UIImageJPEGRepresentation(outputImage, 0.8);
            }
            outputSize = (int)[outputData length];

            if (outputSize <= maxSize && outputWidth <= maxWidth && outputHeight <= maxHeight) {
                success = [outputData writeToFile:outputFile atomically:YES];
                break;
            }
            else {
                if (decreaseWidth) {
                    outputWidth -= [self getDecreaseOffset:outputWidth];
                    outputHeight = (int)((float)outputWidth / ratio);
                }
                else {
                    outputHeight -= [self getDecreaseOffset:outputHeight];
                    outputWidth = (int)((float)outputHeight * ratio);
                }
            }

        };

        dispatch_async(dispatch_get_main_queue(), ^{
            if (success) {
                resolve(@{
                          @"path": outputFile,
                          @"size": @(outputSize),
                          @"width": @(outputWidth),
                          @"height": @(outputHeight)
                          });
            }
            else {
                reject(@"-1", @"compress image failed.", nil);
            }
        });

    });

}

- (int)getDecreaseOffset:(int)size {

    if (size > 10000) {
        return 5000;
    }
    else if (size > 8000) {
        return 4000;
    }
    else if (size > 4000) {
        return 2000;
    }
    else if (size > 3000) {
        return 1000;
    }
    else if (size > 2000) {
        return 500;
    }
    else if (size > 1500) {
        return 300;
    }
    else if (size > 1000) {
        return 200;
    }
    else if (size > 500) {
        return 50;
    }
    else if (size > 300) {
        return 30;
    }

    return 10;

}

@end
