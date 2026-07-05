package africa.prodesign.service;

import africa.prodesign.dto.BoqLineItem;
import africa.prodesign.dto.BoqResponse;
import africa.prodesign.entity.Project;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

@Service
public class BoqPdfExportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(26, 26, 46));
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, new Color(100, 100, 100));
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CATEGORY_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(4, 120, 87));
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(26, 26, 46));
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(26, 26, 46));
    private static final Font NOTE_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, new Color(120, 120, 120));
    private static final Color BRAND_GREEN = new Color(4, 120, 87);
    private static final Color LIGHT_GREEN = new Color(225, 245, 238);

    public byte[] export(Project project, BoqResponse boq) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Bill of Quantities — " + project.getName(), TITLE_FONT));
            String generatedDate = boq.generatedAt().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            Paragraph subtitle = new Paragraph("Currency: " + boq.currency() + "   ·   Generated: " + generatedDate, SUBTITLE_FONT);
            subtitle.setSpacingAfter(14);
            document.add(subtitle);

            PdfPTable table = new PdfPTable(new float[]{0.5f, 3.2f, 1f, 0.8f, 1.2f, 1.3f});
            table.setWidthPercentage(100);

            for (String h : new String[]{"S/N", "Description", "Qty", "Unit", "Rate", "Amount"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
                cell.setBackgroundColor(BRAND_GREEN);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int serial = 1;
            String currentCategory = null;
            for (BoqLineItem item : boq.lineItems()) {
                if (!item.category().equals(currentCategory)) {
                    currentCategory = item.category();
                    PdfPCell categoryCell = new PdfPCell(new Phrase(currentCategory, CATEGORY_FONT));
                    categoryCell.setColspan(6);
                    categoryCell.setBackgroundColor(LIGHT_GREEN);
                    categoryCell.setPadding(4);
                    table.addCell(categoryCell);
                }
                addCell(table, String.valueOf(serial++), Element.ALIGN_CENTER);
                addCell(table, item.description(), Element.ALIGN_LEFT);
                addCell(table, String.format("%.2f", item.quantity()), Element.ALIGN_CENTER);
                addCell(table, item.unit(), Element.ALIGN_CENTER);
                addCell(table, String.format("%,.2f", item.rate()), Element.ALIGN_RIGHT);
                addCell(table, String.format("%,.2f", item.amount()), Element.ALIGN_RIGHT);
            }

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL", TOTAL_FONT));
            totalLabelCell.setColspan(5);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setPadding(6);
            table.addCell(totalLabelCell);
            PdfPCell totalValueCell = new PdfPCell(new Phrase(String.format("%,.2f", boq.totalAmount()), TOTAL_FONT));
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValueCell.setPadding(6);
            table.addCell(totalValueCell);

            document.add(table);

            for (String note : boq.notes()) {
                Paragraph notePara = new Paragraph("Note: " + note, NOTE_FONT);
                notePara.setSpacingBefore(6);
                document.add(notePara);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate BOQ PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }
}