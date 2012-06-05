// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef OSMAND_NACL_H_
#define OSMAND_NACL_H_

#include <pthread.h>
#include <map>
#include <vector>
#include "ppapi/cpp/graphics_2d.h"
#include "ppapi/cpp/image_data.h"
#include "ppapi/cpp/instance.h"
#include "ppapi/cpp/rect.h"
#include "ppapi/cpp/size.h"

namespace osmand {

// The Instance class.  One of these exists for each instance of your NaCl
// module on the web page.  The browser will ask the Module object to create
// a new Instance for each occurrence of the <embed> tag that has these
// attributes:
//     type="application/x-nacl"
//     nacl="pi_generator.nmf"
class PiGenerator : public pp::Instance {
 public:
  explicit PiGenerator(PP_Instance instance);
  virtual ~PiGenerator();

  // Start up the ComputePi() thread.
  virtual bool Init(uint32_t argc, const char* argn[], const char* argv[]);

  // Update the graphics context to the new size, and regenerate |pixel_buffer_|
  // to fit the new size as well.
  virtual void DidChangeView(const pp::Rect& position, const pp::Rect& clip);

  // Called by the browser to handle the postMessage() call in Javascript.
  // The message in this case is expected to contain the string 'paint', and
  // if so this invokes the Paint() function.  If |var_message| is not a string
  // type, or contains something other than 'paint', this method posts an
  // invalid value for Pi (-1.0) back to the browser.
  virtual void HandleMessage(const pp::Var& var_message);

  // Return a pointer to the pixels represented by |pixel_buffer_|.  When this
  // method returns, the underlying |pixel_buffer_| object is locked.  This
  // call must have a matching UnlockPixels() or various threading errors
  // (e.g. deadlock) will occur.
  uint32_t* LockPixels();
  // Release the image lock acquired by LockPixels().
  void UnlockPixels() const;

  // Flushes its contents of |pixel_buffer_| to the 2D graphics context.  The
  // ComputePi() thread fills in |pixel_buffer_| pixels as it computes Pi.
  // This method is called by HandleMessage when a message containing 'paint'
  // is received.  Echos the current value of pi as computed by the Monte Carlo
  // method by posting the value back to the browser.
  void Paint();

  bool quit() const {
    return quit_;
  }

  // |pi_| is computed in the ComputePi() thread.
  double pi() const {
    return pi_;
  }

  int width() const {
    return pixel_buffer_ ? pixel_buffer_->size().width() : 0;
  }
  int height() const {
    return pixel_buffer_ ? pixel_buffer_->size().height() : 0;
  }

  // Indicate whether a flush is pending.  This can only be called from the
  // main thread; it is not thread safe.
  bool flush_pending() const {
    return flush_pending_;
  }
  void set_flush_pending(bool flag) {
    flush_pending_ = flag;
  }

 private:
  // Create and initialize the 2D context used for drawing.
  void CreateContext(const pp::Size& size);
  // Destroy the 2D drawing context.
  void DestroyContext();
  // Push the pixels to the browser, then attempt to flush the 2D context.  If
  // there is a pending flush on the 2D context, then update the pixels only
  // and do not flush.
  void FlushPixelBuffer();

  bool IsContextValid() const {
    return graphics_2d_context_ != NULL;
  }

  mutable pthread_mutex_t pixel_buffer_mutex_;
  pp::Graphics2D* graphics_2d_context_;
  pp::ImageData* pixel_buffer_;
  bool flush_pending_;
  bool quit_;
  pthread_t compute_pi_thread_;
  double pi_;

  // ComputePi() estimates Pi using Monte Carlo method and it is executed by a
  // separate thread created in SetWindow(). ComputePi() puts kMaxPointCount
  // points inside the square whose length of each side is 1.0, and calculates
  // the ratio of the number of points put inside the inscribed quadrant divided
  // by the total number of random points to get Pi/4.
  static void* ComputePi(void* param);
};

}  // namespace osmand

#endif  // OSMAND_NACL_H
