# This file is based on external/skia/Android.mk from Android sources

LOCAL_PATH := $(call my-dir)

ifeq ($(SKIA_LOC),)
  SKIA_LOC := .
endif
ifeq ($(SKIA_ABS),)
  SKIA_ABS := $(LOCAL_PATH)
endif

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

# need a flag to tell the C side when we're on devices with large memory
# budgets (i.e. larger than the low-end devices that initially shipped)
ifeq ($(ARCH_ARM_HAVE_VFP),true)
    LOCAL_CFLAGS += -DANDROID_LARGE_MEMORY_DEVICE
endif

ifneq ($(ARCH_ARM_HAVE_VFP),true)
	LOCAL_CFLAGS += -DSK_SOFTWARE_FLOAT
endif

ifeq ($(ARCH_ARM_HAVE_NEON),true)
	LOCAL_CFLAGS += -D__ARM_HAVE_NEON
endif

LOCAL_MODULE := skia
LOCAL_SRC_FILES:= \
	$(SKIA_LOC)/trunk/src/core/Sk64.cpp \
	$(SKIA_LOC)/trunk/src/core/SkAAClip.cpp \
	$(SKIA_LOC)/trunk/src/core/SkAdvancedTypefaceMetrics.cpp \
	$(SKIA_LOC)/trunk/src/core/SkAlphaRuns.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmap.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmapProcShader.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmapProcState.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmapProcState_matrixProcs.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmapSampler.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBitmap_scroll.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitMask_D32.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitRow_D16.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitRow_D32.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitRow_D4444.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_4444.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_A1.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_A8.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_ARGB32.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_RGB16.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBlitter_Sprite.cpp \
	$(SKIA_LOC)/trunk/src/core/SkBuffer.cpp \
	$(SKIA_LOC)/trunk/src/core/SkCanvas.cpp \
	$(SKIA_LOC)/trunk/src/core/SkChunkAlloc.cpp \
	$(SKIA_LOC)/trunk/src/core/SkClampRange.cpp \
	$(SKIA_LOC)/trunk/src/core/SkClipStack.cpp \
	$(SKIA_LOC)/trunk/src/core/SkColor.cpp \
	$(SKIA_LOC)/trunk/src/core/SkColorFilter.cpp \
	$(SKIA_LOC)/trunk/src/core/SkColorTable.cpp \
	$(SKIA_LOC)/trunk/src/core/SkConfig8888.cpp \
	$(SKIA_LOC)/trunk/src/core/SkComposeShader.cpp \
	$(SKIA_LOC)/trunk/src/core/SkConcaveToTriangles.cpp \
	$(SKIA_LOC)/trunk/src/core/SkCordic.cpp \
	$(SKIA_LOC)/trunk/src/core/SkCubicClipper.cpp \
	$(SKIA_LOC)/trunk/src/core/SkData.cpp \
	$(SKIA_LOC)/trunk/src/core/SkDebug.cpp \
	$(SKIA_LOC)/trunk/src/core/SkDeque.cpp \
	$(SKIA_LOC)/trunk/src/core/SkDevice.cpp \
	$(SKIA_LOC)/trunk/src/core/SkDither.cpp \
	$(SKIA_LOC)/trunk/src/core/SkDraw.cpp \
	$(SKIA_LOC)/trunk/src/core/SkEdgeBuilder.cpp \
	$(SKIA_LOC)/trunk/src/core/SkEdgeClipper.cpp \
	$(SKIA_LOC)/trunk/src/core/SkEdge.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFilterProc.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFlattenable.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFloat.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFloatBits.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFontHost.cpp \
	$(SKIA_LOC)/trunk/src/core/SkGeometry.cpp \
	$(SKIA_LOC)/trunk/src/core/SkGlyphCache.cpp \
	$(SKIA_LOC)/trunk/src/core/SkGraphics.cpp \
	$(SKIA_LOC)/trunk/src/core/SkLineClipper.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMallocPixelRef.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMask.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMaskFilter.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMath.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMatrix.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMetaData.cpp \
	$(SKIA_LOC)/trunk/src/core/SkMMapStream.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPackBits.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPaint.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPath.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPathEffect.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPathHeap.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPathMeasure.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPicture.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPictureFlat.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPicturePlayback.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPictureRecord.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPixelRef.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPoint.cpp \
	$(SKIA_LOC)/trunk/src/core/SkProcSpriteBlitter.cpp \
	$(SKIA_LOC)/trunk/src/core/SkPtrRecorder.cpp \
	$(SKIA_LOC)/trunk/src/core/SkQuadClipper.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRasterClip.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRasterizer.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRect.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRefDict.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRegion.cpp \
	$(SKIA_LOC)/trunk/src/core/SkRegion_path.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScalar.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScalerContext.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScan.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScan_AntiPath.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScan_Antihair.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScan_Hairline.cpp \
	$(SKIA_LOC)/trunk/src/core/SkScan_Path.cpp \
	$(SKIA_LOC)/trunk/src/core/SkShader.cpp \
	$(SKIA_LOC)/trunk/src/core/SkShape.cpp \
	$(SKIA_LOC)/trunk/src/core/SkSpriteBlitter_ARGB32.cpp \
	$(SKIA_LOC)/trunk/src/core/SkSpriteBlitter_RGB16.cpp \
	$(SKIA_LOC)/trunk/src/core/SkStream.cpp \
	$(SKIA_LOC)/trunk/src/core/SkString.cpp \
	$(SKIA_LOC)/trunk/src/core/SkStroke.cpp \
	$(SKIA_LOC)/trunk/src/core/SkStrokerPriv.cpp \
	$(SKIA_LOC)/trunk/src/core/SkTSearch.cpp \
	$(SKIA_LOC)/trunk/src/core/SkTypeface.cpp \
	$(SKIA_LOC)/trunk/src/core/SkTypefaceCache.cpp \
	$(SKIA_LOC)/trunk/src/core/SkUnPreMultiply.cpp \
	$(SKIA_LOC)/trunk/src/core/SkUtils.cpp \
	$(SKIA_LOC)/trunk/src/core/SkFlate.cpp \
	$(SKIA_LOC)/trunk/src/core/SkWriter32.cpp \
	$(SKIA_LOC)/trunk/src/core/SkXfermode.cpp \
	$(SKIA_LOC)/trunk/src/effects/Sk1DPathEffect.cpp \
	$(SKIA_LOC)/trunk/src/effects/Sk2DPathEffect.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkAvoidXfermode.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkArithmeticMode.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkBitmapCache.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkBlurDrawLooper.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkBlurImageFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkBlurMask.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkBlurMaskFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkColorFilters.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkColorMatrixFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkCornerPathEffect.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkDashPathEffect.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkDiscretePathEffect.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkEffects.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkEmbossMask.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkEmbossMaskFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkGradientShader.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkGroupShape.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkKernel33MaskFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkLayerDrawLooper.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkLayerRasterizer.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkPaintFlagsDrawFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkPixelXorXfermode.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkPorterDuff.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkRectShape.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkTableColorFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkTableMaskFilter.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkTestImageFilters.cpp \
	$(SKIA_LOC)/trunk/src/effects/SkTransparentShader.cpp \
	$(SKIA_LOC)/trunk/src/images/bmpdecoderhelper.cpp \
	$(SKIA_LOC)/trunk/src/images/SkCreateRLEPixelRef.cpp \
	$(SKIA_LOC)/trunk/src/images/SkFDStream.cpp \
	$(SKIA_LOC)/trunk/src/images/SkFlipPixelRef.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_Factory.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_libbmp.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_libgif.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_libico.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_libpng.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageDecoder_wbmp.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageEncoder.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageEncoder_Factory.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageRef.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageRefPool.cpp \
	$(SKIA_LOC)/trunk/src/images/SkImageRef_GlobalPool.cpp \
	$(SKIA_LOC)/trunk/src/images/SkMovie.cpp \
	$(SKIA_LOC)/trunk/src/images/SkMovie_gif.cpp \
	$(SKIA_LOC)/trunk/src/images/SkPageFlipper.cpp \
	$(SKIA_LOC)/trunk/src/images/SkScaledBitmapSampler.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkDebug_android.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkGlobalInitialization_default.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkFontHost_FreeType.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkFontHost_sandbox_none.cpp	\
	$(SKIA_LOC)/trunk/src/ports/SkFontHost_android.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkFontHost_gamma.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkFontHost_tables.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkMemory_malloc.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkOSFile_stdio.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkTime_Unix.cpp \
	$(SKIA_LOC)/trunk/src/ports/SkThread_pthread.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkBoundaryPatch.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkCamera.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkColorMatrix.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkCubicInterval.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkCullPoints.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkDumpCanvas.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkInterpolator.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkLayer.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkMatrix44.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkMeshUtils.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkNinePatch.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkNWayCanvas.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkOSFile.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkParse.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkParseColor.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkParsePath.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkProxyCanvas.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkSfntUtils.cpp \
	$(SKIA_LOC)/trunk/src/utils/SkUnitMappers.cpp

# This file is replacement of $(SKIA_LOC)/trunk/src/ports/FontHostConfiguration_android.cpp 
LOCAL_SRC_FILES += \
	FontHostConfiguration_android.cpp
LOCAL_C_INCLUDES += \
	$(SKIA_ABS)/trunk/src/ports
	
ifeq ($(TARGET_ARCH),arm)

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_SRC_FILES += \
	$(SKIA_LOC)/trunk/src/opts/memset16_neon.S \
	$(SKIA_LOC)/trunk/src/opts/memset32_neon.S
endif

LOCAL_SRC_FILES += \
	$(SKIA_LOC)/trunk/src/opts/opts_check_arm.cpp \
	$(SKIA_LOC)/trunk/src/opts/memset.arm.S \
	$(SKIA_LOC)/trunk/src/opts/SkBitmapProcState_opts_arm.cpp \
	$(SKIA_LOC)/trunk/src/opts/SkBlitRow_opts_arm.cpp
		
else

LOCAL_SRC_FILES += \
	$(SKIA_LOC)/trunk/src/opts/SkBlitRow_opts_none.cpp \
	$(SKIA_LOC)/trunk/src/opts/SkBitmapProcState_opts_none.cpp \
	$(SKIA_LOC)/trunk/src/opts/SkUtils_opts_none.cpp
		
endif

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libjpeg \
	libutils \
	libz

LOCAL_STATIC_LIBRARIES := \
	libft2 \
	libpng \
	libgif \
	libexpat_static

LOCAL_C_INCLUDES += \
	$(SKIA_ABS)/trunk/src/core \
	$(SKIA_ABS)/trunk/include/core \
	$(SKIA_ABS)/trunk/include/config \
	$(SKIA_ABS)/trunk/include/effects \
	$(SKIA_ABS)/trunk/include/images \
	$(SKIA_ABS)/trunk/include/utils \
	$(SKIA_ABS)/trunk/include/xml \
	$(SKIA_ABS)/android/third_party/externals/freetype/include \
	$(SKIA_ABS)/android/third_party/externals/png \
	$(SKIA_ABS)/android/third_party/externals/gif \
	$(SKIA_ABS)/android/third_party/externals/expat/lib

ifeq ($(NO_FALLBACK_FONT),true)
	LOCAL_CFLAGS += -DNO_FALLBACK_FONT
endif

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
	
LOCAL_LDLIBS += -lz -llog

include $(BUILD_STATIC_LIBRARY)

#############################################################
# Build the skia gpu (ganesh) library
#

# include $(CLEAR_VARS)

# LOCAL_ARM_MODE := arm

# ifneq ($(ARCH_ARM_HAVE_VFP),true)
	# LOCAL_CFLAGS += -DSK_SOFTWARE_FLOAT
# endif

# ifeq ($(ARCH_ARM_HAVE_NEON),true)
	# LOCAL_CFLAGS += -DGR_ANDROID_BUILD=1
# endif

# LOCAL_SRC_FILES:= \
	# $(SKIA_LOC)/trunk/src/gpu/GrPrintf_skia.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGLContext.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGpuCanvas.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGpuDevice.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGr.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGrFontScaler.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkGrTexturePixelRef.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/SkNullGLContext.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/android/SkNativeGLContext_android.cpp

# LOCAL_SRC_FILES += \
	# $(SKIA_LOC)/trunk/src/gpu/GrAAHairLinePathRenderer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrAddPathRenderers_aahairline.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrAllocPool.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrAtlas.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrBufferAllocPool.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrClip.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrContext.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrDefaultPathRenderer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrDrawTarget.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLCreateNullInterface.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLDefaultInterface_native.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLIndexBuffer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLInterface.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLProgram.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLRenderTarget.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLSL.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLStencilBuffer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLTexture.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLUtil.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGLVertexBuffer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGpu.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGpuFactory.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGpuGL.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrGpuGLShaders.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrInOrderDrawBuffer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrMatrix.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrMemory.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrPathRendererChain.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrPathRenderer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrPathUtils.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrRectanizer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrRenderTarget.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrResource.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrResourceCache.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrStencil.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrStencilBuffer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrTesselatedPathRenderer.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrTextContext.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrTextStrike.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/GrTexture.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/gr_unittests.cpp \
	# $(SKIA_LOC)/trunk/src/gpu/android/GrGLCreateNativeInterface_android.cpp
  
# LOCAL_STATIC_LIBRARIES := libskiatess
# LOCAL_SHARED_LIBRARIES := \
	# libcutils \
	# libutils \
	# libskia \
	# libEGL \
	# libGLESv2

# LOCAL_C_INCLUDES += \
	# $(SKIA_ABS)/trunk/include/core \
	# $(SKIA_ABS)/trunk/include/config \
	# $(SKIA_ABS)/trunk/include/gpu \
	# $(SKIA_ABS)/trunk/src/core \
	# $(SKIA_ABS)/trunk/src/gpu \
	# $(SKIA_ABS)/trunk/third_party/glu

# LOCAL_MODULE := libskiagpu
# LOCAL_MODULE_TAGS := optional

# include $(BUILD_STATIC_LIBRARY)

# #############################################################
# # Build the skia gpu (ganesh) library
# #

# include $(CLEAR_VARS)

# LOCAL_ARM_MODE := arm

# LOCAL_SRC_FILES := \
	# third_party/glu/libtess/dict.c \
	# third_party/glu/libtess/geom.c \
	# third_party/glu/libtess/memalloc.c \
	# third_party/glu/libtess/mesh.c \
	# third_party/glu/libtess/normal.c \
	# third_party/glu/libtess/priorityq.c \
	# third_party/glu/libtess/render.c \
	# third_party/glu/libtess/sweep.c \
	# third_party/glu/libtess/tess.c \
	# third_party/glu/libtess/tessmono.c

# LOCAL_SHARED_LIBRARIES := \
	# libcutils \
	# libutils \
	# libEGL \
	# libGLESv2

# LOCAL_C_INCLUDES += \
  # $(LOCAL_PATH)/third_party/glu \
  # $(LOCAL_PATH)/third_party/glu/libtess \
  # frameworks/base/opengl/include

# LOCAL_LDLIBS += -lpthread

# LOCAL_MODULE:= libskiatess
# LOCAL_MODULE_TAGS := optional

# include $(BUILD_STATIC_LIBRARY)

# Fix some errors
BUILD_HOST_EXECUTABLE := $(LOCAL_PATH)/FakeHost.mk
BUILD_HOST_STATIC_LIBRARY := $(LOCAL_PATH)/FakeHost.mk 

# Import skia externals
$(call import-add-path,$(SKIA_ABS)/android/third_party/externals)
$(call import-module,expat)
$(call import-module,freetype)
$(call import-module,gif)
$(call import-module,png)