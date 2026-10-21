package com.example.adminwebjava.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExcelWorkbookBuilder {

    private final List<ExcelSheet> sheets = new ArrayList<>();

    public ExcelWorkbookBuilder addSheet(String name, List<List<Object>> rows) {
        String safeName = toSafeSheetName(name, sheets.size() + 1);
        List<List<Object>> copyRows = new ArrayList<>();
        if (rows != null) {
            for (List<Object> row : rows) {
                copyRows.add(row != null ? new ArrayList<>(row) : new ArrayList<>());
            }
        }
        sheets.add(new ExcelSheet(safeName, copyRows));
        return this;
    }

    public byte[] build() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            writeEntry(zos, "[Content_Types].xml", buildContentTypes());
            writeEntry(zos, "_rels/.rels", buildRootRelationships());
            writeEntry(zos, "docProps/app.xml", buildAppProperties());
            writeEntry(zos, "docProps/core.xml", buildCoreProperties());
            writeEntry(zos, "xl/workbook.xml", buildWorkbook());
            writeEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelationships());
            writeEntry(zos, "xl/styles.xml", buildStyles());

            for (int i = 0; i < sheets.size(); i++) {
                writeEntry(zos, "xl/worksheets/sheet" + (i + 1) + ".xml", buildWorksheet(sheets.get(i)));
            }
        }
        return baos.toByteArray();
    }

    private String buildContentTypes() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        builder.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        builder.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        builder.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        builder.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        builder.append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>");
        builder.append("<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>");

        for (int i = 0; i < sheets.size(); i++) {
            builder.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1)
                   .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }

        builder.append("</Types>");
        return builder.toString();
    }

    private static String buildRootRelationships() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
               "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
               "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
               "  <Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>\n" +
               "  <Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>\n" +
               "</Relationships>";
    }

    private String buildWorkbook() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        builder.append("<sheets>");

        for (int i = 0; i < sheets.size(); i++) {
            builder.append("<sheet name=\"").append(escape(sheets.get(i).name()))
                   .append("\" sheetId=\"").append(i + 1)
                   .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }

        builder.append("</sheets></workbook>");
        return builder.toString();
    }

    private String buildWorkbookRelationships() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");

        for (int i = 0; i < sheets.size(); i++) {
            builder.append("<Relationship Id=\"rId").append(i + 1)
                   .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                   .append(i + 1).append(".xml\"/>");
        }

        builder.append("<Relationship Id=\"rId").append(sheets.size() + 1)
               .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        builder.append("</Relationships>");
        return builder.toString();
    }

    private static String buildStyles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
               "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
               "  <fonts count=\"1\"><font><sz val=\"11\"/><color theme=\"1\"/><name val=\"Calibri\"/><family val=\"2\"/></font></fonts>\n" +
               "  <fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>\n" +
               "  <borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>\n" +
               "  <cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>\n" +
               "  <cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>\n" +
               "  <cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>\n" +
               "</styleSheet>";
    }

    private String buildWorksheet(ExcelSheet sheet) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        builder.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        builder.append("<sheetData>");

        for (int rowIndex = 0; rowIndex < sheet.rows().size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            List<Object> row = sheet.rows().get(rowIndex);
            builder.append("<row r=\"").append(rowNumber).append("\">");

            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                builder.append(buildCell(row.get(columnIndex), columnIndex, rowNumber));
            }

            builder.append("</row>");
        }

        builder.append("</sheetData></worksheet>");
        return builder.toString();
    }

    private static String buildCell(Object value, int columnIndex, int rowNumber) {
        String reference = getCellReference(columnIndex, rowNumber);
        if (value == null) {
            return "<c r=\"" + reference + "\"/>";
        }

        if (isNumeric(value)) {
            return "<c r=\"" + reference + "\"><v>" + value + "</v></c>";
        }

        String text;
        if (value instanceof LocalDateTime localDateTime) {
            text = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            text = value.toString();
        }

        return "<c r=\"" + reference + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" + escape(text) + "</t></is></c>";
    }

    private static boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    private static String buildAppProperties() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
               "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">\n" +
               "  <Application>VERSA AdminWeb</Application>\n" +
               "</Properties>";
    }

    private static String buildCoreProperties() {
        String created = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
               "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
               "  <dc:creator>VERSA AdminWeb</dc:creator>\n" +
               "  <cp:lastModifiedBy>VERSA AdminWeb</cp:lastModifiedBy>\n" +
               "  <dcterms:created xsi:type=\"dcterms:W3CDTF\">" + created + "</dcterms:created>\n" +
               "  <dcterms:modified xsi:type=\"dcterms:W3CDTF\">" + created + "</dcterms:modified>\n" +
               "</cp:coreProperties>";
    }

    private static void writeEntry(ZipOutputStream zos, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String getCellReference(int columnIndex, int rowNumber) {
        return getColumnName(columnIndex) + rowNumber;
    }

    private static String getColumnName(int columnIndex) {
        int dividend = columnIndex + 1;
        StringBuilder columnName = new StringBuilder();

        while (dividend > 0) {
            int modulo = (dividend - 1) % 26;
            columnName.insert(0, (char) ('A' + modulo));
            dividend = (dividend - modulo) / 26;
        }

        return columnName.toString();
    }

    private static String toSafeSheetName(String name, int index) {
        if (name == null || name.isBlank()) {
            return "Sheet " + index;
        }
        String cleaned = name.replaceAll("[\\\\/:?*\\[\\]]", " ").trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            cleaned = "Sheet " + index;
        }
        return cleaned.length() <= 31 ? cleaned : cleaned.substring(0, 31);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record ExcelSheet(String name, List<List<Object>> rows) {}
}