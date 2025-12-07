package com.dam.financetracker.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

        try {
            // USAR MediaStore API para Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    document.close()
                    Toast.makeText(context, "PDF guardado exitosamente en Downloads/$filename", Toast.LENGTH_LONG).show()
                } else {
                    document.close()
                    Toast.makeText(context, "Error al crear el archivo PDF", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Método legacy para Android 9 y anteriores
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)

                val fos = FileOutputStream(file)
                document.writeTo(fos)
                document.close()
                fos.close()

                Toast.makeText(context, "PDF guardado en Downloads/$filename", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            document.close()
            e.printStackTrace()
            Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Convierte una vista (incluyendo su contenido desplazable si es ViewGroup) a un solo Bitmap.
     */
    private fun viewToBitmap(view: View): Bitmap {
        // Forzar medición completa de la vista
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        val totalHeight = if (view is ViewGroup && view.childCount > 0) {
            // Si es un contenedor con hijos (como NestedScrollView), toma la altura medida del primer hijo.
            view.getChildAt(0).measuredHeight
        } else {
            // Si es una vista simple, toma su propia altura medida.
            view.measuredHeight
        }

        val bitmap = Bitmap.createBitmap(view.width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(android.graphics.Color.WHITE)
        }
        
        // Layoutear la vista con las medidas correctas
        view.layout(0, 0, view.width, totalHeight)
        
        // Traducir la vista al canvas para capturar su contenido completo
        view.draw(canvas)
        return bitmap
    }
}
