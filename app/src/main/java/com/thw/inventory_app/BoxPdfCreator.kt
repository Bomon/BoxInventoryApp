package com.thw.inventory_app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class BoxPdfCreator {

    lateinit var loadingView: ConstraintLayout
    lateinit var doneView: ConstraintLayout
    lateinit var share_btn: MaterialButton
    lateinit var download_btn: MaterialButton
    lateinit var bottomSheetDialog: BottomSheetDialog

    fun showExportSheet(context: Context) {
        bottomSheetDialog = BottomSheetDialog(context)
        bottomSheetDialog.setContentView(R.layout.sheet_export_pdf)

        loadingView = bottomSheetDialog.findViewById<ConstraintLayout>(R.id.sheet_export_loading_container)!!
        doneView = bottomSheetDialog.findViewById<ConstraintLayout>(R.id.sheet_export_loading_done_container)!!
        share_btn = bottomSheetDialog.findViewById<MaterialButton>(R.id.sheet_export_share_btn)!!
        download_btn = bottomSheetDialog.findViewById<MaterialButton>(R.id.sheet_export_save_btn)!!

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
        // Create Header
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

        headerParagraph.add(headerTable)

        val header = HeaderFooter(headerParagraph, false)
        header.setAlignment(Element.ALIGN_CENTER)
        document.setHeader(header)

        // Footer
        /*val footerParagraph = Phrase()

        val footerTable = PdfPTable(3)
        footerTable.widthPercentage = 95f
        val widthsFooter = floatArrayOf(20f, 60f, 20f)
        footerTable.setWidths(widthsFooter)

        val timeStamp: String = SimpleDateFormat("dd.MM.yyyy").format(Date())
        val cell_f_1 = PdfPCell(Phrase(timeStamp,
            FontFactory.getFont(FontFactory.HELVETICA, 7f)))
        cell_f_1.border = Rectangle.NO_BORDER
        cell_f_1.horizontalAlignment = Rectangle.ALIGN_LEFT
        footerTable.addCell(cell_f_1)

        val cell_f_2 = PdfPCell(Phrase("Im Weidig 22 - 63785 Obernburg - ☎ (0 60 22) 64 95 10",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)))
        cell_f_2.border = Rectangle.NO_BORDER
        cell_f_2.horizontalAlignment = Rectangle.ALIGN_CENTER
        footerTable.addCell(cell_f_2)

        val cell_f_3 = PdfPCell(Phrase(box_model.id + " - " + box_model.name,
            FontFactory.getFont(FontFactory.HELVETICA, 7f)))
        cell_f_3.border = Rectangle.NO_BORDER
        cell_f_3.horizontalAlignment = Rectangle.ALIGN_RIGHT
        footerTable.addCell(cell_f_3)

        footerParagraph.add(footerTable)
        val footer = HeaderFooter(footerParagraph, false)
        document.setFooter(footer)*/


        /*var footerCell: PdfPCell = PdfPCell()
        var phrase1 = Phrase(Phrase("Im Weidig 22 - 63785 Obernburg - ☎ (0 60 22) 64 95 10",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)))
        var phrase2 = Phrase(Phrase(box_model.id + " - " + box_model.name,
            FontFactory.getFont(FontFactory.HELVETICA, 7f)))
        var phrase3 = Phrase(box_model.id + " - " + box_model.name,
            FontFactory.getFont(FontFactory.HELVETICA, 7f))
        footerCell.addElement(phrase1)
        footerCell.addElement(phrase2)
        footerCell.addElement(phrase3)

        val footerParagraph = Paragraph()
        footerParagraph.add(footerCell)
        val footer = HeaderFooter(footerParagraph, false)
        document.setFooter(footer)*/


        // step 3: we open the document
        document.open()


        val pointColumnWidths = floatArrayOf(150f, 150f, 150f)
        val table = PdfPTable(pointColumnWidths)

        // Adding cells to the table

        // Adding cells to the table
        table.addCell("Name")
        table.addCell("Raju")
        table.addCell("Id")
        table.addCell("1001")
        table.addCell("Designation")
        table.addCell("Programmer")

        // Adding Table to document

        // Adding Table to document
        document.add(table)

        val titleTable = PdfPTable(1)
        //titleTable.widthPercentage = 75f
        //val titleWidths = floatArrayOf(3f, 72f)
        //titleTable.setWidths(titleWidths)
        titleTable.horizontalAlignment = Rectangle.ALIGN_LEFT
        titleTable.setSpacingBefore(20f)

        //val emptyCell = PdfPCell(Phrase(""))
        //emptyCell.border = Rectangle.NO_BORDER
        //titleTable.addCell(emptyCell)

        val titleCell1 = PdfPCell(Phrase("Box " + box_model.id + " - " + box_model.name,
            FontFactory.getFont(FontFactory.HELVETICA, 36f)))
        titleCell1.border = Rectangle.NO_BORDER
        titleTable.addCell(titleCell1)
        document.add(titleTable)


        //var title = Paragraph("Box " + box_model.id + " - " + box_model.name,
        //    FontFactory.getFont(FontFactory.HELVETICA, 36f))
        //document.add(title)
        //val jpg: Image = Image.getInstance(outputFile.toURL())
        //val jpg: Image = Image.getInstance(Utils.StringToBitMap(box_model.image))
        //document.add(jpg)


    }


}