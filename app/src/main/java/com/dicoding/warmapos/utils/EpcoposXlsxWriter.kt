package com.dicoding.warmapos.utils

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Custom XLSX writer for EPCOPOS format using only Java standard library.
 * Matches the structure of template_epcopos_by_warmapos.xlsx exactly.
 */
object EpcoposXlsxWriter {

    private class SharedStrings {
        private val stringMap = mutableMapOf<String, Int>()
        private val stringList = mutableListOf<String>()

        fun getIndex(value: String): Int {
            return stringMap.getOrPut(value) {
                val index = stringList.size
                stringList.add(value)
                index
            }
        }

        fun generateXml(): String {
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("\n<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"${stringList.size}\" uniqueCount=\"${stringList.size}\">")
            stringList.forEach { str ->
                sb.append("<si><t xml:space=\"preserve\">${escape(str)}</t></si>")
            }
            sb.append("</sst>")
            return sb.toString()
        }
    }

    /**
     * Write products to an EPCOPOS XLSX file.
     * @param file destination File
     * @param products list of product data
     */
    fun write(file: File, products: List<com.dicoding.warmapos.data.model.Product>): Boolean {
        return try {
            val sharedStrings = SharedStrings()

            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zip ->
                    val sheet1Xml = sheet1(products, sharedStrings)
                    val sheet2Xml = emptySheet(listOf("parent_sku", "name", "sku", "barcode", "sales_price", "product_cost", "stock", "warning_limit", "unit", "unlimited", "image"), sharedStrings)
                    val sheet3Xml = emptySheet(listOf("parent_sku", "minimum_qty", "sales_price"), sharedStrings)
                    val sheet4Xml = emptySheet(listOf("parent_sku", "title", "name", "sales_price", "product_cost"), sharedStrings)

                    writeEntry(zip, "[Content_Types].xml", contentTypes())
                    writeEntry(zip, "_rels/.rels", rels())
                    writeEntry(zip, "xl/workbook.xml", workbook())
                    writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels())
                    writeEntry(zip, "xl/styles.xml", styles())
                    writeEntry(zip, "xl/sharedStrings.xml", sharedStrings.generateXml())
                    writeEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml)
                    writeEntry(zip, "xl/worksheets/sheet2.xml", sheet2Xml)
                    writeEntry(zip, "xl/worksheets/sheet3.xml", sheet3Xml)
                    writeEntry(zip, "xl/worksheets/sheet4.xml", sheet4Xml)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="product" sheetId="1" r:id="rId1"/>
    <sheet name="product_variant" sheetId="2" r:id="rId2"/>
    <sheet name="product_wholesale" sheetId="3" r:id="rId3"/>
    <sheet name="product_option" sheetId="4" r:id="rId4"/>
  </sheets>
</workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts><font><sz val="11"/><name val="Calibri"/></font>
         <font><sz val="11"/><b/><name val="Calibri"/></font></fonts>
  <fills><fill><patternFill patternType="none"/></fill>
         <fill><patternFill patternType="gray125"/></fill></fills>
  <borders><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""

    private fun sheet1(products: List<com.dicoding.warmapos.data.model.Product>, sharedStrings: SharedStrings): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>""")
        
        // Exact original EPCOPOS Sheet 1 Headers (12 columns)
        val headers = listOf(
            "name", "sku", "barcode", "category", "sales_price", 
            "product_cost", "stock", "warning_limit", "unit", 
            "featured", "unlimited", "image"
        )
        
        // Header Row
        sb.append("\n    <row r=\"1\">")
        headers.forEachIndexed { colIdx, header ->
            val colLetter = ('A' + colIdx).toString()
            val ref = "${colLetter}1"
            val sId = sharedStrings.getIndex(header)
            sb.append("""<c r="$ref" t="s" s="1"><v>$sId</v></c>""")
        }
        sb.append("</row>")

        // Data Rows
        products.forEachIndexed { index, p ->
            val rowIdx = index + 2 // 1-based, plus header
            sb.append("\n    <row r=\"$rowIdx\">")
            
            val rowData = listOf(
                p.name,             // A: name
                p.sku,              // B: sku
                "",                 // C: barcode -> empty
                p.category,         // D: category
                p.price.toString(), // E: sales_price
                "0",                // F: product_cost -> 0
                "0",                // G: stock -> 0
                "0",                // H: warning_limit -> 0
                p.unit,             // I: unit
                "no",               // J: featured -> no
                "yes",              // K: unlimited -> yes
                ""                  // L: image -> empty
            )

            rowData.forEachIndexed { colIdx, value ->
                val isNumeric = colIdx in 4..7
                val colLetter = ('A' + colIdx).toString()
                val ref = "$colLetter$rowIdx"
                
                if (isNumeric) {
                    val numValue = if (value.isEmpty()) "0" else escape(value)
                    sb.append("""<c r="$ref"><v>$numValue</v></c>""")
                } else {
                    if (value.isNotEmpty()) {
                        val sId = sharedStrings.getIndex(value)
                        sb.append("""<c r="$ref" t="s"><v>$sId</v></c>""")
                    } else {
                        sb.append("""<c r="$ref"/>""") // Empty cell completely compatible
                    }
                }
            }
            sb.append("</row>")
        }
        
        sb.append("""
  </sheetData>
</worksheet>""")
        return sb.toString()
    }

    private fun emptySheet(headers: List<String>, sharedStrings: SharedStrings): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">""")
        headers.forEachIndexed { colIdx, header ->
            val colLetter = ('A' + colIdx).toString()
            val ref = "${colLetter}1"
            val sId = sharedStrings.getIndex(header)
            sb.append("""<c r="$ref" t="s" s="1"><v>$sId</v></c>""")
        }
        sb.append("""</row>
  </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
