package com.howlab.howlcolorlens

import android.graphics.*
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark

class FaceGraphic (overlay: GraphicOverlay?, var firebaseVisionFace : FirebaseVisionFace?, var lensBitmap : Bitmap) : GraphicOverlay.Graphic(overlay){
    val idPaint = Paint().apply {
        color = Color.WHITE
    }
    override fun draw(canvas: Canvas?) {
        var face = firebaseVisionFace ?: return

        drawBitmapOverLandmarkPosition(canvas, face,lensBitmap, FirebaseVisionFaceLandmark.RIGHT_EYE)
        drawBitmapOverLandmarkPosition(canvas ,face,lensBitmap, FirebaseVisionFaceLandmark.LEFT_EYE)
    }
    fun drawBitmapOverLandmarkPosition(canvas: Canvas?, face: FirebaseVisionFace, overlayBitmap: Bitmap, landmarkID : Int){
        val landmark = face.getLandmark(landmarkID)
        val point = landmark?.position
        if (point != null){
            val imageEdgeSizeBasedOnFaceSize = face.boundingBox.height() / 9f
            val left = translateX(point.x) - imageEdgeSizeBasedOnFaceSize
            val top = translateY(point.y) - imageEdgeSizeBasedOnFaceSize
            val right = translateX(point.x) + imageEdgeSizeBasedOnFaceSize
            val bottom = translateY(point.y) + imageEdgeSizeBasedOnFaceSize
            canvas?.drawBitmap(overlayBitmap,null, Rect(left.toInt(),top.toInt(),right.toInt(),bottom.toInt()),null)
        }
    }

}