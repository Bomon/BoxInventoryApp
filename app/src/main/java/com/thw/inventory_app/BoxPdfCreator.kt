package com.thw.inventory_app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.pdf.draw.VerticalPositionMark
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class BoxPdfCreator {

    lateinit var loadingView: ConstraintLayout
    lateinit var doneView: ConstraintLayout
    lateinit var share_btn: MaterialButton
    lateinit var download_btn: MaterialButton
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var imageDoneView: ShapeableImageView

    fun showExportSheet(context: Context) {
        bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(R.layout.sheet_export_pdf)

        loadingView = bottomSheetDialog.findViewById<ConstraintLayout>(R.id.sheet_export_loading_container)!!
        doneView = bottomSheetDialog.findViewById<ConstraintLayout>(R.id.sheet_export_loading_done_container)!!
        share_btn = bottomSheetDialog.findViewById<MaterialButton>(R.id.sheet_export_share_btn)!!
        download_btn = bottomSheetDialog.findViewById<MaterialButton>(R.id.sheet_export_save_btn)!!
        imageDoneView = bottomSheetDialog.findViewById<ShapeableImageView>(R.id.loading_done)!!

        bottomSheetDialog.show()
    }

    fun createPdf(context: Context?, box_model: BoxModel, viewLifecycleOwner: LifecycleOwner) {
        if (::bottomSheetDialog.isInitialized){
            bottomSheetDialog.dismiss()
        }

        if (context != null) {
            showExportSheet(context)

            var file: File? = null
            try {

                val mHandler = Handler(Looper.getMainLooper())
                val runnable = Runnable {

                    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val pdfFileName = box_model.id + "_" + timeStamp
                    file = File(context.cacheDir, pdfFileName + ".pdf")
                    file!!.deleteOnExit()

                    val document = Document(RectangleReadOnly(842f,595f), 10f, 10f, 10f, 10f);

                    PdfWriter.getInstance(
                        document,
                        FileOutputStream(file)
                    )

                    buildPDFcontent(context, document, box_model)
                    document.close()

                    mHandler.post(Runnable {
                        Log.e("Error", "Handler Response")
                        loadingView.visibility = View.GONE
                        doneView.visibility = View.VISIBLE
                    })
                }

                val thread = Thread(runnable)
                thread.start()

                bottomSheetDialog.setOnDismissListener {
                    Log.e("Error", "File Dialog was dismissed")
                    mHandler.removeCallbacks(runnable)
                    thread.interrupt()
                    it.dismiss()
                    file?.delete()
                }

            } catch (de: DocumentException) {
                System.err.println(de.message)
            } catch (ioe: IOException) {
                System.err.println(ioe.message)
            }

            share_btn.setOnClickListener {
                if (file != null){
                    val intentShareFile = Intent(Intent.ACTION_SEND)

                    val uri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        file!!
                    )

                    // required to omit Permission Denial error
                    intentShareFile.clipData = ClipData(
                        "Generated PDF with Box description",
                        listOf("application/pdf").toTypedArray(),
                        ClipData.Item(uri)
                    )

                    intentShareFile.type = "application/pdf"
                    intentShareFile.putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )
                    intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    //intentShareFile.putExtra(
                    //    Intent.EXTRA_SUBJECT,
                    //    "Subject"
                    //)
                    //intentShareFile.putExtra(Intent.EXTRA_TEXT, "Text")

                    context.startActivity(Intent.createChooser(intentShareFile, "PDF Teilen"))
                }

            }

            download_btn.setOnClickListener {
                if (file != null) {

                    var fis = FileInputStream(file)

                    val baos = ByteArrayOutputStream(1024)
                    val buffer = ByteArray(1024)
                    var read = -1
                    while (fis.read(buffer).also { read = it } != -1) {
                        baos.write(buffer, 0, read)
                    }
                    baos.close()
                    fis.close()
                    val storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    var downloadFile = File(storageDir, box_model.id + ".pdf")
                    var fos = FileOutputStream(downloadFile)
                    baos.writeTo(fos)
                    fos.close()

                    val uri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        downloadFile
                    )


                    Toast.makeText(
                        context,
                        "PDF in Downloads gespeichert",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(uri, "application/pdf")
                    intent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        "PDF öffnen mit"
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    fun buildPDFcontent(context: Context, document: Document, box_model: BoxModel){
        document.setMargins(10f, 10f, 16f, 10f);
        // Create Header

        val headerPlaceholder = Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 1f))
        //addEmptyLine(headerPlaceholder, 1)
        val headerParagraph = Paragraph()
        val headerTable = PdfPTable(4)
        headerTable.widthPercentage = 95f
        val widths = floatArrayOf(8f, 72f, 10f, 10f)
        headerTable.setWidths(widths)

        val eagle: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_adler)
        val stream = ByteArrayOutputStream()
        if (eagle != null) {
            Log.e("ERROR", "Adding image")
            eagle.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val image: Image = Image.getInstance(stream.toByteArray())
            stream.close()
            image.scaleAbsolute(50f, 50f)
            val cell = PdfPCell(image)
            cell.border = Rectangle.NO_BORDER
            headerTable.addCell(cell)
        }

        val textTable = PdfPTable(1)
        val cell2 = PdfPCell(Phrase("Bundesanstalt Technisches Hilfswerk",
            FontFactory.getFont(FontFactory.TIMES_BOLD, 16f)))
        cell2.border = Rectangle.NO_BORDER
        textTable.addCell(cell2)

        val cell3 = PdfPCell(Phrase("Ortsverband Obernburg, FGr Öl (C)",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f)))
        cell3.border = Rectangle.NO_BORDER
        textTable.addCell(cell3)

        val cell1 = PdfPCell(textTable)
        cell1.border = Rectangle.NO_BORDER
        headerTable.addCell(cell1)

        val oel: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_oel)
        stream.reset()
        if (oel != null) {
            Log.e("ERROR", "Adding image")
            oel.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val image: Image = Image.getInstance(stream.toByteArray())
            stream.close()
            image.scaleAbsolute(50f, 50f)
            val cell = PdfPCell(image)
            cell.border = Rectangle.NO_BORDER
            headerTable.addCell(cell)
        }

        val thw: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_thw)
        stream.reset()
        if (thw != null) {
            Log.e("ERROR", "Adding image")
            thw.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val image: Image = Image.getInstance(stream.toByteArray())
            stream.close()
            image.scaleAbsolute(50f, 50f)
            val cell = PdfPCell(image)
            cell.border = Rectangle.NO_BORDER
            headerTable.addCell(cell)
        }

        headerParagraph.add(headerPlaceholder)
        headerParagraph.add(headerTable)

        val header = HeaderFooter(headerParagraph, false)
        header.setAlignment(Element.ALIGN_CENTER)
        document.setHeader(header)

        //footer
        val glue = Chunk(VerticalPositionMark())
        val timeStamp: String = SimpleDateFormat("dd.MM.yyyy").format(Date())
        val p = Paragraph(Phrase("           " + timeStamp,
            FontFactory.getFont(FontFactory.HELVETICA, 7f)))
        p.add(Chunk(glue))
        p.add(Phrase("Im Weidig 22 - 63785 Obernburg - Tel: (0 60 22) 64 95 10",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)))
        p.add(Chunk(glue))
        p.add(Phrase(box_model.id + " - " + box_model.name + "           ",
            FontFactory.getFont(FontFactory.HELVETICA, 7f)))
        p.spacingAfter = 10f


        val footerPlaceholder = Paragraph()
        addEmptyLine(footerPlaceholder, 1)
        p.add(footerPlaceholder)

        val footer = HeaderFooter(p, false)
        document.setFooter(footer)


        // Open document for actual content
        document.open()

        // Add title
        var titleContainer = Paragraph()

        val title = Paragraph()
        title.add(Phrase("Box ",
            FontFactory.getFont(FontFactory.HELVETICA, 36f)))
        title.add(Phrase(box_model.id,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36f)))
        title.add(Phrase(" - ",
            FontFactory.getFont(FontFactory.HELVETICA, 36f)))
        title.add(Phrase(box_model.name,
            FontFactory.getFont(FontFactory.HELVETICA, 36f)))

        createTextInTable(titleContainer, title, 70f, Rectangle.ALIGN_LEFT, 0f, 0f)
        document.add(titleContainer)

        // Create content table
        val contentTable = PdfPTable(4)
        val contentTableWidths = floatArrayOf(0.5f, 1.5f, 10.5f, 4.5f)
        contentTable.setWidths(contentTableWidths)
        contentTable.widthPercentage = 70f
        contentTable.horizontalAlignment = Rectangle.ALIGN_LEFT
        contentTable.setSpacingBefore(30f)

        val cellTitle0 = PdfPCell(Phrase(""))
        cellTitle0.border = Rectangle.NO_BORDER
        cellTitle0.horizontalAlignment = Rectangle.ALIGN_LEFT
        contentTable.addCell(cellTitle0)

        val cellTitle1 = PdfPCell(Phrase("Anz.",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)))
        cellTitle1.border = Rectangle.NO_BORDER
        cellTitle1.horizontalAlignment = Rectangle.ALIGN_LEFT
        cellTitle1.paddingBottom = 4f
        contentTable.addCell(cellTitle1)

        val cellTitle2 = PdfPCell(Phrase("Gegenstand",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)))
        cellTitle2.border = Rectangle.NO_BORDER
        cellTitle2.horizontalAlignment = Rectangle.ALIGN_LEFT
        cellTitle2.paddingBottom = 4f
        contentTable.addCell(cellTitle2)

        val cellTitle3 = PdfPCell(Phrase("InvNr.",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)))
        cellTitle3.border = Rectangle.NO_BORDER
        cellTitle3.horizontalAlignment = Rectangle.ALIGN_LEFT
        cellTitle3.paddingBottom = 4f
        contentTable.addCell(cellTitle3)

        val itemNames = Utils.getAllItemNames()

        for (contentItem in box_model.content) {
            val cellTitle0 = PdfPCell(Phrase(""))
            cellTitle0.border = Rectangle.NO_BORDER
            cellTitle0.horizontalAlignment = Rectangle.ALIGN_LEFT
            contentTable.addCell(cellTitle0)

            val cellContent1 = PdfPCell(Phrase(contentItem.amount,
                FontFactory.getFont(FontFactory.HELVETICA, 14f)))
            cellContent1.paddingLeft = 4f
            cellContent1.paddingRight = 4f
            cellContent1.paddingBottom = 4f
            contentTable.addCell(cellContent1)

            val cellContent2 = PdfPCell(Phrase(itemNames[contentItem.id],
                FontFactory.getFont(FontFactory.HELVETICA, 14f)))
            cellContent2.paddingLeft = 4f
            cellContent2.paddingRight = 4f
            cellContent2.paddingBottom = 4f
            contentTable.addCell(cellContent2)

            val cellContent3 = PdfPCell(Phrase(contentItem.invnum.split(";").joinToString("\n"),
                FontFactory.getFont(FontFactory.HELVETICA, 14f)))
            cellContent3.paddingLeft = 4f
            cellContent3.paddingRight = 4f
            cellContent3.paddingBottom = 4f
            contentTable.addCell(cellContent3)

        }

        var contentContainer = Paragraph()
        contentContainer.add(contentTable)
        document.add(contentTable)

        // Create QRCode
        try {
            var bmp: Bitmap? = encodeAsBitmap(box_model.qrcode, BarcodeFormat.QR_CODE, 512, 512);
            stream.reset()
            if (bmp != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, 80, stream)
                val image: Image = Image.getInstance(stream.toByteArray())
                stream.close()
                image.scaleAbsolute(175f, 175f)
                image.setAbsolutePosition(620f, 330f)
                document.add(image)
            }

        } catch (e: WriterException) {
            e.printStackTrace()
        }

        // Display Box Img
        if (box_model.image != "") {
            val boxImage: Image = Image.getInstance(Base64.decode(box_model.image, Base64.DEFAULT))
            boxImage.scaleAbsolute(220f, 165f)
            boxImage.setAbsolutePosition(605f, 150f)
            document.add(boxImage)
        }

    }

    private fun addEmptyLine(paragraph: Paragraph, number: Int) {
        for (i in 0 until number) {
            paragraph.add(Phrase("", FontFactory.getFont(FontFactory.HELVETICA, 1f)))
        }
    }

    fun encodeAsBitmap(
        contents: String,
        format: BarcodeFormat?,
        desiredWidth: Int,
        desiredHeight: Int
    ): Bitmap? {
        var hints: Hashtable<EncodeHintType?, Any?>? = null
        val encoding: String? = guessAppropriateEncoding(contents)
        if (encoding != null) {
            hints = Hashtable(2)
            hints[EncodeHintType.CHARACTER_SET] = encoding
        }
        val writer = MultiFormatWriter()
        val result = writer.encode(contents, format, desiredWidth, desiredHeight, hints)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        // All are 0, or black, by default
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun guessAppropriateEncoding(contents: String): String? {
        // Very crude at the moment
        for (i in 0 until contents.length) {
            if (contents[i].code > 0xFF) {
                return "UTF-8"
            }
        }
        return null
    }

    private fun createTextInTable(paragraph: Paragraph, content: Phrase, widthPercentage: Float, horizontalAlignment: Int, spacingBefore: Float, spacingAfter: Float){
        val table = PdfPTable(2)
        val contentTableWidths = floatArrayOf(2f, 98f)
        table.setWidths(contentTableWidths)

        table.widthPercentage = widthPercentage
        table.horizontalAlignment = horizontalAlignment
        table.setSpacingAfter(spacingAfter)
        table.setSpacingBefore(spacingBefore)

        val cellTitle0 = PdfPCell(Phrase(""))
        cellTitle0.border = Rectangle.NO_BORDER
        cellTitle0.horizontalAlignment = Rectangle.ALIGN_LEFT
        table.addCell(cellTitle0)

        val cell = PdfPCell(content)
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = Rectangle.ALIGN_LEFT
        //cell.setLeading(0f, 1.2f)
        table.addCell(cell)

        paragraph.add(table)
    }


}