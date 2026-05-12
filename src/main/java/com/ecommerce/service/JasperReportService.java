package com.ecommerce.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class JasperReportService {

    public byte[] generateExcelReport(String title, String[] headers, String[] fields, Collection<?> data) throws JRException {
        StringBuilder jrxml = new StringBuilder();
        jrxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        jrxml.append("<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" ");
        jrxml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        jrxml.append("xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" ");
        jrxml.append("name=\"DynamicReport\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"555\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\">\n");
        
        // Register fields
        for (String field : fields) {
            jrxml.append("  <field name=\"").append(field).append("\" class=\"java.lang.String\"/>\n");
        }

        // Title band
        jrxml.append("  <title>\n");
        jrxml.append("    <band height=\"60\">\n");
        jrxml.append("      <staticText>\n");
        jrxml.append("        <reportElement x=\"0\" y=\"0\" width=\"555\" height=\"35\"/>\n");
        jrxml.append("        <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n");
        jrxml.append("          <font size=\"16\" isBold=\"true\"/>\n");
        jrxml.append("        </textElement>\n");
        jrxml.append("        <text><![CDATA[").append(title).append("]]></text>\n");
        jrxml.append("      </staticText>\n");
        jrxml.append("    </band>\n");
        jrxml.append("  </title>\n");

        // Column Header band
        jrxml.append("  <columnHeader>\n");
        jrxml.append("    <band height=\"25\">\n");
        int colWidth = 555 / headers.length;
        for (int i = 0; i < headers.length; i++) {
            jrxml.append("      <staticText>\n");
            jrxml.append("        <reportElement x=\"").append(i * colWidth).append("\" y=\"0\" width=\"").append(colWidth).append("\" height=\"20\"/>\n");
            jrxml.append("        <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n");
            jrxml.append("          <font size=\"10\" isBold=\"true\"/>\n");
            jrxml.append("        </textElement>\n");
            jrxml.append("        <text><![CDATA[").append(headers[i]).append("]]></text>\n");
            jrxml.append("      </staticText>\n");
        }
        jrxml.append("    </band>\n");
        jrxml.append("  </columnHeader>\n");

        // Detail band
        jrxml.append("  <detail>\n");
        jrxml.append("    <band height=\"22\">\n");
        for (int i = 0; i < fields.length; i++) {
            jrxml.append("      <textField textAdjust=\"StretchHeight\">\n");
            jrxml.append("        <reportElement x=\"").append(i * colWidth).append("\" y=\"0\" width=\"").append(colWidth).append("\" height=\"18\"/>\n");
            jrxml.append("        <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n");
            jrxml.append("          <font size=\"9\"/>\n");
            jrxml.append("        </textElement>\n");
            jrxml.append("        <textFieldExpression><![CDATA[$F{").append(fields[i]).append("}]]></textFieldExpression>\n");
            jrxml.append("      </textField>\n");
        }
        jrxml.append("    </band>\n");
        jrxml.append("  </detail>\n");

        jrxml.append("</jasperReport>\n");

        InputStream stream = new ByteArrayInputStream(jrxml.toString().getBytes(StandardCharsets.UTF_8));
        JasperReport report = JasperCompileManager.compileReport(stream);
        
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        Map<String, Object> parameters = new HashMap<>();
        
        JasperPrint print = JasperFillManager.fillReport(report, parameters, dataSource);
        
        JRXlsxExporter exporter = new JRXlsxExporter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(false);
        configuration.setDetectCellType(true);
        configuration.setCollapseRowSpan(false);
        configuration.setRemoveEmptySpaceBetweenRows(true);
        configuration.setRemoveEmptySpaceBetweenColumns(true);
        exporter.setConfiguration(configuration);
        
        exporter.exportReport();
        return out.toByteArray();
    }
}
