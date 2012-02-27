LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := skia
LOCAL_CPP_EXTENSION := .cpp
LOCAL_SRC_FILES := \
	trunk/src/core/Sk64.cpp \
	trunk/src/core/SkAAClip.cpp \
	trunk/src/core/SkAdvancedTypefaceMetrics.cpp \
	trunk/src/core/SkAlphaRuns.cpp \
	trunk/src/core/SkBitmap.cpp \
	trunk/src/core/SkBitmapProcShader.cpp \
	trunk/src/core/SkBitmapProcState.cpp \
	trunk/src/core/SkBitmapProcState_matrixProcs.cpp \
	trunk/src/core/SkBitmapSampler.cpp \
	trunk/src/core/SkBitmap_scroll.cpp \
	trunk/src/core/SkBlitMask_D32.cpp \
	trunk/src/core/SkBlitRow_D16.cpp \
	trunk/src/core/SkBlitRow_D32.cpp \
	trunk/src/core/SkBlitRow_D4444.cpp \
	trunk/src/core/SkBlitter.cpp \
	trunk/src/core/SkBlitter_4444.cpp \
	trunk/src/core/SkBlitter_A1.cpp \
	trunk/src/core/SkBlitter_A8.cpp \
	trunk/src/core/SkBlitter_ARGB32.cpp \
	trunk/src/core/SkBlitter_RGB16.cpp \
	trunk/src/core/SkBlitter_Sprite.cpp \
	trunk/src/core/SkBuffer.cpp \
	trunk/src/core/SkCanvas.cpp \
	trunk/src/core/SkChunkAlloc.cpp \
	trunk/src/core/SkClampRange.cpp \
	trunk/src/core/SkClipStack.cpp \
	trunk/src/core/SkColor.cpp \
	trunk/src/core/SkColorFilter.cpp \
	trunk/src/core/SkColorTable.cpp \
	trunk/src/core/SkComposeShader.cpp \
	trunk/src/core/SkConcaveToTriangles.cpp \
	trunk/src/core/SkConfig8888.cpp \
	trunk/src/core/SkCordic.cpp \
	trunk/src/core/SkCubicClipper.cpp \
	trunk/src/core/SkData.cpp \
	trunk/src/core/SkDebug.cpp \
	trunk/src/core/SkDeque.cpp \
	trunk/src/core/SkDevice.cpp \
	trunk/src/core/SkDither.cpp \
	trunk/src/core/SkDraw.cpp \
	trunk/src/core/SkEdgeBuilder.cpp \
	trunk/src/core/SkEdgeClipper.cpp \
	trunk/src/core/SkEdge.cpp \
	trunk/src/core/SkFilterProc.cpp \
	trunk/src/core/SkFlattenable.cpp \
	trunk/src/core/SkFloat.cpp \
	trunk/src/core/SkFloatBits.cpp \
	trunk/src/core/SkFontHost.cpp \
	trunk/src/core/SkGeometry.cpp \
	trunk/src/core/SkGlyphCache.cpp \
	trunk/src/core/SkGraphics.cpp \
	trunk/src/core/SkLineClipper.cpp \
	trunk/src/core/SkMallocPixelRef.cpp \
	trunk/src/core/SkMask.cpp \
	trunk/src/core/SkMaskFilter.cpp \
	trunk/src/core/SkMath.cpp \
	trunk/src/core/SkMatrix.cpp \
	trunk/src/core/SkMetaData.cpp \
	trunk/src/core/SkMMapStream.cpp \
	trunk/src/core/SkPackBits.cpp \
	trunk/src/core/SkPaint.cpp \
	trunk/src/core/SkPath.cpp \
	trunk/src/core/SkPathEffect.cpp \
	trunk/src/core/SkPathHeap.cpp \
	trunk/src/core/SkPathMeasure.cpp \
	trunk/src/core/SkPicture.cpp \
	trunk/src/core/SkPictureFlat.cpp \
	trunk/src/core/SkPicturePlayback.cpp \
	trunk/src/core/SkPictureRecord.cpp \
	trunk/src/core/SkPixelRef.cpp \
	trunk/src/core/SkPoint.cpp \
	trunk/src/core/SkProcSpriteBlitter.cpp \
	trunk/src/core/SkPtrRecorder.cpp \
	trunk/src/core/SkQuadClipper.cpp \
	trunk/src/core/SkRasterClip.cpp \
	trunk/src/core/SkRasterizer.cpp \
	trunk/src/core/SkRect.cpp \
	trunk/src/core/SkRefDict.cpp \
	trunk/src/core/SkRegion.cpp \
	trunk/src/core/SkRegion_path.cpp \
	trunk/src/core/SkScalar.cpp \
	trunk/src/core/SkScalerContext.cpp \
	trunk/src/core/SkScan.cpp \
	trunk/src/core/SkScan_AntiPath.cpp \
	trunk/src/core/SkScan_Antihair.cpp \
	trunk/src/core/SkScan_Hairline.cpp \
	trunk/src/core/SkScan_Path.cpp \
	trunk/src/core/SkShader.cpp \
	trunk/src/core/SkShape.cpp \
	trunk/src/core/SkSpriteBlitter_ARGB32.cpp \
	trunk/src/core/SkSpriteBlitter_RGB16.cpp \
	trunk/src/core/SkStream.cpp \
	trunk/src/core/SkString.cpp \
	trunk/src/core/SkStroke.cpp \
	trunk/src/core/SkStrokerPriv.cpp \
	trunk/src/core/SkTSearch.cpp \
	trunk/src/core/SkTypeface.cpp \
	trunk/src/core/SkTypefaceCache.cpp \
	trunk/src/core/SkUnPreMultiply.cpp \
	trunk/src/core/SkUtils.cpp \
	trunk/src/core/SkWriter32.cpp \
	trunk/src/core/SkXfermode.cpp \
	trunk/src/ports/SkFontHost_sandbox_none.cpp \
	trunk/src/ports/SkGlobalInitialization_default.cpp \
	trunk/src/ports/SkFontHost_tables.cpp \
	trunk/src/ports/SkMemory_malloc.cpp \
	trunk/src/ports/SkOSFile_stdio.cpp \
	trunk/src/ports/SkTime_Unix.cpp \
	trunk/src/ports/SkXMLParser_empty.cpp \
	trunk/src/ports/SkDebug_android.cpp \
	trunk/src/ports/SkThread_pthread.cpp \
	trunk/src/ports/SkFontHost_android.cpp \
	trunk/src/ports/SkFontHost_gamma.cpp \
	trunk/src/ports/SkFontHost_FreeType.cpp \
	trunk/src/ports/FontHostConfiguration_android.cpp \
	trunk/src/opts/SkBitmapProcState_opts_none.cpp \
	trunk/src/opts/SkBlitRow_opts_none.cpp \
	trunk/src/opts/SkUtils_opts_none.cpp \
	trunk/src/effects/Sk1DPathEffect.cpp \
	trunk/src/effects/Sk2DPathEffect.cpp \
	trunk/src/effects/SkAvoidXfermode.cpp \
	trunk/src/effects/SkArithmeticMode.cpp \
	trunk/src/effects/SkBitmapCache.cpp \
	trunk/src/effects/SkBlurDrawLooper.cpp \
	trunk/src/effects/SkBlurMask.cpp \
	trunk/src/effects/SkBlurImageFilter.cpp \
	trunk/src/effects/SkBlurMaskFilter.cpp \
	trunk/src/effects/SkColorFilters.cpp \
	trunk/src/effects/SkColorMatrixFilter.cpp \
	trunk/src/effects/SkCornerPathEffect.cpp \
	trunk/src/effects/SkDashPathEffect.cpp \
	trunk/src/effects/SkDiscretePathEffect.cpp \
	trunk/src/effects/SkEffects.cpp \
	trunk/src/effects/SkEmbossMask.cpp \
	trunk/src/effects/SkEmbossMaskFilter.cpp \
	trunk/src/effects/SkGradientShader.cpp \
	trunk/src/effects/SkGroupShape.cpp \
	trunk/src/effects/SkKernel33MaskFilter.cpp \
	trunk/src/effects/SkLayerDrawLooper.cpp \
	trunk/src/effects/SkLayerRasterizer.cpp \
	trunk/src/effects/SkPaintFlagsDrawFilter.cpp \
	trunk/src/effects/SkPixelXorXfermode.cpp \
	trunk/src/effects/SkPorterDuff.cpp \
	trunk/src/effects/SkRectShape.cpp \
	trunk/src/effects/SkTableColorFilter.cpp \
	trunk/src/effects/SkTestImageFilters.cpp \
	trunk/src/effects/SkTransparentShader.cpp \
	trunk/src/images/bmpdecoderhelper.cpp \
	trunk/src/images/SkCreateRLEPixelRef.cpp \
	trunk/src/images/SkFDStream.cpp \
	trunk/src/images/SkFlipPixelRef.cpp \
	trunk/src/images/SkImageDecoder.cpp \
	trunk/src/images/SkImageDecoder_Factory.cpp \
	trunk/src/images/SkImageDecoder_libbmp.cpp \
	trunk/src/images/SkImageDecoder_libgif.cpp \
	trunk/src/images/SkImageDecoder_libico.cpp \
	trunk/src/images/SkImageDecoder_libpng.cpp \
	trunk/src/images/SkImageDecoder_wbmp.cpp \
	trunk/src/images/SkImageEncoder.cpp \
	trunk/src/images/SkImageEncoder_Factory.cpp \
	trunk/src/images/SkImageRef.cpp \
	trunk/src/images/SkImageRefPool.cpp \
	trunk/src/images/SkImageRef_GlobalPool.cpp \
	trunk/src/images/SkMovie.cpp \
	trunk/src/images/SkMovie_gif.cpp \
	trunk/src/images/SkPageFlipper.cpp \
	trunk/src/images/SkScaledBitmapSampler.cpp 
	
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/android/third_party/externals/freetype/include \
	$(LOCAL_PATH)/android/third_party/externals/expat/lib \
	$(LOCAL_PATH)/trunk/include/config \
	$(LOCAL_PATH)/trunk/include/core \
	$(LOCAL_PATH)/trunk/include/images \
	$(LOCAL_PATH)/trunk/include/effects \
	$(LOCAL_PATH)/trunk/include/ports \
	$(LOCAL_PATH)/trunk/include/opts \
	$(LOCAL_PATH)/trunk/include/xml \
	$(LOCAL_PATH)/trunk/src/core \
	$(LOCAL_PATH)/trunk/include/utils \
	$(LOCAL_PATH)/android/third_party/externals/gif \
	$(LOCAL_PATH)/android/third_party/externals/png
	
LOCAL_CFLAGS := \
	-DSK_SCALAR_IS_FLOAT \
	-DSK_CAN_USE_FLOAT \
	-DSK_BUILD_FOR_ANDROID \
	-DSK_BUILD_FOR_ANDROID_NDK \
	-DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=0 \
	-DSK_USE_POSIX_THREADS \
	-DSK_RELEASE \
	-DGR_RELEASE=1 \
	-DNDEBUG
	
LOCAL_CPPFLAGS := \
	-fno-rtti \
	-fno-exceptions

LOCAL_STATIC_LIBRARIES += libexpat_static libft2 libgif libpng

include $(BUILD_STATIC_LIBRARY)

# Fix some errors
BUILD_HOST_EXECUTABLE := $(LOCAL_PATH)/FakeHost.mk
BUILD_HOST_STATIC_LIBRARY := $(LOCAL_PATH)/FakeHost.mk 

# Import skia externals
$(call import-add-path,$(LOCAL_PATH)/android/third_party/externals)
$(call import-module,expat)
$(call import-module,freetype)
$(call import-module,gif)
$(call import-module,png)