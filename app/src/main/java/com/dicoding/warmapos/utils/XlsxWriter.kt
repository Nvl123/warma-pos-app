package com.dicoding.warmapos.utils

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal XLSX writer using only Java standard library (ZIP + XML).
 * Compatible with Android API 24+ without any Apache POI dependency.
 */
object XlsxWriter {

    /**
     * Write rows (list of lists) to an XLSX file.
     * @param file destination File
     * @param rows first row is treated as the header
     */
    fun write(file: File, rows: List<List<String>>): Boolean {
        return try {
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zip ->
                    writeEntry(zip, "[Content_Types].xml", contentTypes())
                    writeEntry(zip, "_rels/.rels", rels())
                    writeEntry(zip, "xl/workbook.xml", workbook())
                    writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels())
                    writeEntry(zip, "xl/styles.xml", styles())
                    writeEntry(zip, "xl/worksheets/sheet1.xml", sheet(rows))
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
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Products" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
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

    private fun sheet(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>""")
        rows.forEachIndexed { rowIdx, row ->
            val isHeader = rowIdx == 0
            val style = if (isHeader) """ s="1"""" else ""
            sb.append("\n    <row r=\"${rowIdx + 1}\">")
            row.forEachIndexed { colIdx, value ->
                val colLetter = ('A' + colIdx).toString()
                val ref = "$colLetter${rowIdx + 1}"
                sb.append("""<c r="$ref" t="inlineStr"$style><is><t>${escape(value)}</t></is></c>""")
            }
            sb.append("</row>")
        }
        sb.append("""
  </sheetData>
</worksheet>""")
        return sb.toString()
    }
}
