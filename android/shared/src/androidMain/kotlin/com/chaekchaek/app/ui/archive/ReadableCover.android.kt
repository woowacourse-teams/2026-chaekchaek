package com.chaekchaek.app.ui.archive

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun ImageRequest.Builder.readableCover(): ImageRequest.Builder = allowHardware(false)
