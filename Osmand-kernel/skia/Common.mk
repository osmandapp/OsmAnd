OSMAND_SKIA_LOC := ./skia_library

OSMAND_SKIA_ABS := $(LOCAL_PATH)/skia_library

OSMAND_FREETYPE_ABS := $(LOCAL_PATH)/../freetype/freetype_library

OSMAND_PNG_ABS := $(LOCAL_PATH)/../png/png_library

OSMAND_GIF_ABS := $(LOCAL_PATH)/../gif/gif_library

OSMAND_EXPAT_ABS := $(LOCAL_PATH)/../expat/expat_library

OSMAND_JPEG_ABS := $(LOCAL_PATH)/../jpeg/jpeg_library

LOCAL_SRC_FILES := \
	$(OSMAND_SKIA_LOC)/src/core/Sk64.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkAAClip.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkAdvancedTypefaceMetrics.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkAlphaRuns.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmap.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmapProcShader.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmapProcState.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmapProcState_matrixProcs.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmapSampler.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBitmap_scroll.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitMask_D32.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitRow_D16.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitRow_D32.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitRow_D4444.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_4444.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_A1.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_A8.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_ARGB32.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_RGB16.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBlitter_Sprite.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkBuffer.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkCanvas.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkChunkAlloc.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkClampRange.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkClipStack.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkColor.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkColorFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkColorTable.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkComposeShader.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkConcaveToTriangles.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkConfig8888.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkCordic.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkCubicClipper.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkData.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDebug.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDeque.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDevice.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDeviceProfile.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDither.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkDraw.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkEdgeBuilder.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkEdgeClipper.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkEdge.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFilterProc.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFlattenable.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFloat.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFloatBits.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFontHost.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkGeometry.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkGlyphCache.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkGraphics.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkLineClipper.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMallocPixelRef.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMask.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMaskFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMath.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMatrix.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkMetaData.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPackBits.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPaint.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPath.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPathHeap.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPathMeasure.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPicture.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPictureFlat.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPicturePlayback.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPictureRecord.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPixelRef.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPoint.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkProcSpriteBlitter.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkPtrRecorder.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkQuadClipper.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRasterClip.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRasterizer.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRect.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRefDict.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRegion.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkRegion_path.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScalar.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScalerContext.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScan.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScan_AntiPath.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScan_Antihair.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScan_Hairline.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkScan_Path.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkShader.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkShape.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkSpriteBlitter_ARGB32.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkSpriteBlitter_RGB16.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkStream.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkString.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkStroke.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkStrokerPriv.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkTSearch.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkTypeface.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkTypefaceCache.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkUnPreMultiply.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkUtils.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkFlate.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkWriter32.cpp \
	$(OSMAND_SKIA_LOC)/src/core/SkXfermode.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/Sk1DPathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/Sk2DPathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkAvoidXfermode.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkArithmeticMode.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkBitmapCache.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkBlurDrawLooper.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkBlurImageFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkBlurMask.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkBlurMaskFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkColorFilters.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkColorMatrixFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkCornerPathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkDashPathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkDiscretePathEffect.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkEffects.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkEmbossMask.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkEmbossMaskFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkGradientShader.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkGroupShape.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkKernel33MaskFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkLayerDrawLooper.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkLayerRasterizer.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkMorphologyImageFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkPaintFlagsDrawFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkPixelXorXfermode.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkPorterDuff.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkRectShape.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkTableColorFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkTableMaskFilter.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkTestImageFilters.cpp \
	$(OSMAND_SKIA_LOC)/src/effects/SkTransparentShader.cpp \
	$(OSMAND_SKIA_LOC)/src/images/bmpdecoderhelper.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkBitmapRegionDecoder.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkCreateRLEPixelRef.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkFDStream.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkFlipPixelRef.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_Factory.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libbmp.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libpng.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageEncoder.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageEncoder_Factory.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageRef.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageRefPool.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkImageRef_GlobalPool.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkMovie.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkPageFlipper.cpp \
	$(OSMAND_SKIA_LOC)/src/images/SkScaledBitmapSampler.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkBase64.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkBoundaryPatch.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkCamera.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkColorMatrix.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkCubicInterval.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkCullPoints.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkDeferredCanvas.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkDumpCanvas.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkInterpolator.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkLayer.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkMatrix44.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkMeshUtils.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkNinePatch.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkNWayCanvas.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkOSFile.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkParse.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkParseColor.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkParsePath.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkProxyCanvas.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkSfntUtils.cpp \
	$(OSMAND_SKIA_LOC)/src/utils/SkUnitMappers.cpp 

#	$(OSMAND_SKIA_LOC)/src/images/SkMovie_gif.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libbmp.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libgif.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libico.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libjpeg.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_libpng.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkImageDecoder_wbmp.cpp \
#	$(OSMAND_SKIA_LOC)/src/images/SkJpegUtility.cpp \

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(OSMAND_SKIA_ABS)/src/core \
	$(OSMAND_SKIA_ABS)/include/core \
	$(OSMAND_SKIA_ABS)/include/config \
	$(OSMAND_SKIA_ABS)/include/effects \
	$(OSMAND_SKIA_ABS)/include/images \
	$(OSMAND_SKIA_ABS)/include/ports \
	$(OSMAND_SKIA_ABS)/include/utils \
	$(OSMAND_SKIA_ABS)/include/xml \
	$(OSMAND_FREETYPE_ABS)/include \
	$(OSMAND_PNG_ABS) \
	$(OSMAND_GIF_ABS) \
	$(OSMAND_EXPAT_ABS)/lib \
	$(OSMAND_JPEG_ABS) 