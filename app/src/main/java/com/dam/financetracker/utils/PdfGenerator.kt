package com.dam.financetracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Importaciones requeridas para Rect y ViewGroup
import android.graphics.Rect
import android.view.ViewGroup


object PdfGenerator {

    /**
     * Genera un archivo PDF a partir de una vista, capturando su contenido completo.
     * @param context Contexto de la aplicación.
     * @param view La vista que se capturará (e.g., NestedScrollView, LinearLayout).
     * @param filenameBase Nombre base para el archivo PDF.
     */
    fun generatePdfFromView(context: Context, view: View, filenameBase: String) {
        val document = PdfDocument()
        val bitmap = viewToBitmap(view)

        // 1. Definir dimensiones de la página (tamaño A4 en puntos, 72 puntos por pulgada)
        val pageHeight = 1120
        val pageWidth = 792

        // 2. Crear las especificaciones de la página
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // 3. Redimensionar el bitmap para que quepa en el ancho de la página
        val scaleFactor = pageWidth.toFloat() / bitmap.width.toFloat()
        val scaledHeight = (bitmap.height.toFloat() * scaleFactor).toInt()

        // 4. Definir las áreas de origen y destino para el dibujo
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val destRect = Rect(0, 0, pageWidth, scaledHeight)

        // 5. Usar la sobrecarga drawBitmap(Bitmap, Rect, Rect, Paint)
        canvas.drawBitmap(bitmap, srcRect, destRect, null)

        document.finishPage(page)

        // 6. Generar el nombre y ruta del archivo
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${filenameBase}_$timeStamp.pdf"

        val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Para Android 10+ (API 29+), las apps usan MediaStore o Scoped Storage.
            // Usamos el directorio de Descargas externo a la app (para mejor compatibilidad con otras apps).
            // NOTA: Para API 30+, getExternalStoragePublicDirectory está obsoleto, se usa MediaStore o getExternalFilesDir
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
        } else {
            // Para dispositivos antiguos (API < 29)
            @Suppress("DEPRECATION")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
        }

        try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            Toast.makeText(context, "PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Convierte una vista (incluyendo su contenido desplazable si es ViewGroup) a un solo Bitmap.
     */
    private fun viewToBitmap(view: View): Bitmap {
        val totalHeight = if (view is ViewGroup && view.childCount > 0) {
            // Si es un contenedor con hijos (como NestedScrollView), toma la altura del primer hijo.
            view.getChildAt(0).height
        } else {
            // Si es una vista simple, toma su propia altura.
            view.height
        }

        val bitmap = Bitmap.createBitmap(view.width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(android.graphics.Color.WHITE)
        }
        // Traducir la vista al canvas para capturar su contenido completo
        view.draw(canvas)
        return bitmap
    }
}
