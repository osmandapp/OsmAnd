LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := skia2.2
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES := libskia2.2.so

include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := oskia
#NDK_MODULE_PATH := $(LOCAL_PATH)
#LOCAL_SRC_FILES := liboskia.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

#SKIA_FOLDER := $(LOCAL_PATH)/../../../skia/trunk
#SKIA_SRC := $(LOCAL_PATH)/../../../skia/trunk/src
#SKIA_FOLDER := $(LOCAL_PATH)/skia
ANDROID_FOLDER := /home/victor/projects/android/
SKIA_FOLDER := $(ANDROID_FOLDER)/external/skia
SKIA_SRC := skia

LOCAL_MODULE := osmand
LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni \
	$(SKIA_FOLDER)/include/core \
	$(SKIA_FOLDER)/include/utils \
	$(SKIA_FOLDER)/include/config \
	$(SKIA_FOLDER)/include/utils/android \
    $(SKIA_FOLDER)/src/core \
    $(ANDROID_FOLDER)/system/core/include \
    $(ANDROID_FOLDER)/frameworks/base/include

	
# 	skia/src/core/SkAdvancedTypefaceMetrics.cpp
#   skia/src/core/SkDrawing.cpp \

LOCAL_SRC_FILES := osmand/rendering.cpp
#	$(SKIA_SRC)/src/ports/SkDebug_stdio.cpp \
#	$(SKIA_SRC)/src/ports/SkFontHost_simple.cpp \
#	$(SKIA_SRC)/src/ports/SkFontHost_gamma.cpp \
#	$(SKIA_SRC)/src/ports/SkGlobals_global.cpp \
#	$(SKIA_SRC)/src/ports/SkOSFile_stdio.cpp \
#	$(SKIA_SRC)/src/opts/SkBlitRow_opts_none.cpp \
#	$(SKIA_SRC)/src/core/Sk64.cpp \
#	$(SKIA_SRC)/src/core/SkAAClip.cpp \
#	$(SKIA_SRC)/src/core/SkAlphaRuns.cpp \
#	$(SKIA_SRC)/src/core/SkBitmap.cpp \
#	$(SKIA_SRC)/src/core/SkBitmapProcShader.cpp \
#	$(SKIA_SRC)/src/core/SkBitmapProcState.cpp \
#	$(SKIA_SRC)/src/core/SkBitmapProcState_matrixProcs.cpp \
#    $(SKIA_SRC)/src/core/SkBitmapSampler.cpp \
#	$(SKIA_SRC)/src/core/SkBitmap_scroll.cpp \
#	$(SKIA_SRC)/src/core/SkBlitMask_D32.cpp \
#	$(SKIA_SRC)/src/core/SkBlitRow_D16.cpp \
#	$(SKIA_SRC)/src/core/SkBlitRow_D32.cpp \
#	$(SKIA_SRC)/src/core/SkBlitRow_D4444.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_4444.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_A1.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_A8.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_ARGB32.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_RGB16.cpp \
#	$(SKIA_SRC)/src/core/SkBlitter_Sprite.cpp \
#	$(SKIA_SRC)/src/core/SkBuffer.cpp \
#	$(SKIA_SRC)/src/core/SkCanvas.cpp \
#	$(SKIA_SRC)/src/core/SkChunkAlloc.cpp \
#	$(SKIA_SRC)/src/core/SkClampRange.cpp \
#	$(SKIA_SRC)/src/core/SkClipStack.cpp \
#	$(SKIA_SRC)/src/core/SkColor.cpp \
#	$(SKIA_SRC)/src/core/SkColorFilter.cpp \
#	$(SKIA_SRC)/src/core/SkColorTable.cpp \
#	$(SKIA_SRC)/src/core/SkComposeShader.cpp \
#	$(SKIA_SRC)/src/core/SkConcaveToTriangles.cpp \
#	$(SKIA_SRC)/src/core/SkCordic.cpp \
#	$(SKIA_SRC)/src/core/SkCubicClipper.cpp \
#	$(SKIA_SRC)/src/core/SkData.cpp \
#	$(SKIA_SRC)/src/core/SkDebug.cpp \
#	$(SKIA_SRC)/src/core/SkDeque.cpp \
#	$(SKIA_SRC)/src/core/SkDevice.cpp \
#	$(SKIA_SRC)/src/core/SkDither.cpp \
#	$(SKIA_SRC)/src/core/SkDraw.cpp \
#	$(SKIA_SRC)/src/core/SkEdge.cpp \
#	$(SKIA_SRC)/src/core/SkEdgeBuilder.cpp \
#	$(SKIA_SRC)/src/core/SkEdgeClipper.cpp \
#	$(SKIA_SRC)/src/core/SkFilterProc.cpp \
#	$(SKIA_SRC)/src/core/SkFlate.cpp \
#	$(SKIA_SRC)/src/core/SkFlattenable.cpp \
#	$(SKIA_SRC)/src/core/SkFloat.cpp \
#	$(SKIA_SRC)/src/core/SkFloatBits.cpp \
#	$(SKIA_SRC)/src/core/SkFontHost.cpp \
#	$(SKIA_SRC)/src/core/SkGeometry.cpp \
#	$(SKIA_SRC)/src/core/SkGlobals.cpp \
#	$(SKIA_SRC)/src/core/SkGlyphCache.cpp \
#	$(SKIA_SRC)/src/core/SkGraphics.cpp \
#	$(SKIA_SRC)/src/core/SkLineClipper.cpp \
#	$(SKIA_SRC)/src/core/SkMallocPixelRef.cpp \
#	$(SKIA_SRC)/src/core/SkMask.cpp \
#	$(SKIA_SRC)/src/core/SkMaskFilter.cpp \
#	$(SKIA_SRC)/src/core/SkMath.cpp \
#	$(SKIA_SRC)/src/core/SkMatrix.cpp \
#	$(SKIA_SRC)/src/core/SkMemory_stdlib.cpp \
#	$(SKIA_SRC)/src/core/SkMetaData.cpp \
#	$(SKIA_SRC)/src/core/SkMMapStream.cpp \
#	$(SKIA_SRC)/src/core/SkPackBits.cpp \
#	$(SKIA_SRC)/src/core/SkPaint.cpp \
#	$(SKIA_SRC)/src/core/SkPath.cpp \
#	$(SKIA_SRC)/src/core/SkPathEffect.cpp \
#	$(SKIA_SRC)/src/core/SkPathHeap.cpp \
#	$(SKIA_SRC)/src/core/SkPathMeasure.cpp \
#	$(SKIA_SRC)/src/core/SkPicture.cpp \
#	$(SKIA_SRC)/src/core/SkPictureFlat.cpp \
#	$(SKIA_SRC)/src/core/SkPicturePlayback.cpp \
#	$(SKIA_SRC)/src/core/SkPictureRecord.cpp \
#	$(SKIA_SRC)/src/core/SkPixelRef.cpp \
#	$(SKIA_SRC)/src/core/SkPoint.cpp \
#	$(SKIA_SRC)/src/core/SkProcSpriteBlitter.cpp \
#	$(SKIA_SRC)/src/core/SkPtrRecorder.cpp \
#	$(SKIA_SRC)/src/core/SkQuadClipper.cpp \
#	$(SKIA_SRC)/src/core/SkRasterClip.cpp \
#	$(SKIA_SRC)/src/core/SkRasterizer.cpp \
#	$(SKIA_SRC)/src/core/SkRect.cpp \
#	$(SKIA_SRC)/src/core/SkRefDict.cpp \
#	$(SKIA_SRC)/src/core/SkRegion.cpp \
#	$(SKIA_SRC)/src/core/SkRegion_path.cpp \
#	$(SKIA_SRC)/src/core/SkRegion_rects.cpp \
#	$(SKIA_SRC)/src/core/SkScalar.cpp \
#	$(SKIA_SRC)/src/core/SkScalerContext.cpp \
#	$(SKIA_SRC)/src/core/SkScan.cpp \
#	$(SKIA_SRC)/src/core/SkScan_Antihair.cpp \
#	$(SKIA_SRC)/src/core/SkScan_AntiPath.cpp \
#	$(SKIA_SRC)/src/core/SkScan_Hairline.cpp \
#	$(SKIA_SRC)/src/core/SkScan_Path.cpp \
#	$(SKIA_SRC)/src/core/SkShader.cpp \
#	$(SKIA_SRC)/src/core/SkShape.cpp \
#	$(SKIA_SRC)/src/core/SkSpriteBlitter_ARGB32.cpp \
#	$(SKIA_SRC)/src/core/SkSpriteBlitter_RGB16.cpp \
#	$(SKIA_SRC)/src/core/SkStream.cpp \
#	$(SKIA_SRC)/src/core/SkString.cpp \
#	$(SKIA_SRC)/src/core/SkStroke.cpp \
#	$(SKIA_SRC)/src/core/SkStrokerPriv.cpp \
#	$(SKIA_SRC)/src/core/SkTSearch.cpp \
#	$(SKIA_SRC)/src/core/SkTypeface.cpp \
#	$(SKIA_SRC)/src/core/SkTypefaceCache.cpp \
#	$(SKIA_SRC)/src/core/SkUnPreMultiply.cpp \
#	$(SKIA_SRC)/src/core/SkUtils.cpp \
#	$(SKIA_SRC)/src/core/SkWriter32.cpp \
#	$(SKIA_SRC)/src/core/SkXfermode.cpp 
	
	
LOCAL_CFLAGS := -Wall -g
LOCAL_LDLIBS := -ldl -llog -lcutils

#LOCAL_STATIC_LIBRARIES := oskia

LOCAL_SHARED_LIBRARIES := skia2.2
#    libcutils \
#    libutils \
#    libskia \
#    libandroid_runtime \
#    libGLESv2

# LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
