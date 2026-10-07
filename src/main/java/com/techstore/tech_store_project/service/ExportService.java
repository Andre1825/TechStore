package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Movimiento;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    private static final byte[] BLUE   = {(byte) 13, (byte) 110, (byte) 253};
    private static final byte[] WHITE  = {(byte) 255, (byte) 255, (byte) 255};
    private static final byte[] STRIPE = {(byte) 243, (byte) 246, (byte) 255};
    private static final byte[] BORDER = {(byte) 220, (byte) 220, (byte) 220};
    private static final byte[] GREEN  = {(byte) 25, (byte) 135, (byte) 84};
    private static final byte[] RED    = {(byte) 220, (byte) 53, (byte) 69};

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;

    public ExportService(ProductoRepository productoRepository,
                         MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public void exportarProductos(HttpServletResponse response) throws IOException {
        List<Producto> productos = productoRepository.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Productos");
            sheet.setDefaultRowHeightInPoints(18);

            String[] headers = {"SKU", "Nombre", "Categoría", "Marca", "Precio (S/)", "Stock", "Stock Mín.", "Estado"};
            Row hRow = sheet.createRow(0);
            hRow.setHeightInPoints(24);
            XSSFCellStyle hdr = headerStyle(wb);
            for (int i = 0; i < headers.length; i++) cell(hRow, i, headers[i], hdr);

            int rowNum = 1;
            for (Producto p : productos) {
                boolean even = rowNum % 2 == 0;
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(18);
                cell(row, 0, p.getSku(),                                                    rowStyle(wb, even));
                cell(row, 1, p.getNombre(),                                                 rowStyle(wb, even));
                cell(row, 2, p.getCategoria().getNombre(),                                  rowStyle(wb, even));
                cell(row, 3, p.getMarca() != null ? p.getMarca().getNombre() : "—",         rowStyle(wb, even));
                cell(row, 4, p.getPrecio().doubleValue(),                                   priceStyle(wb, even));
                cell(row, 5, p.getStock(),                                                  centerStyle(wb, even));
                cell(row, 6, p.getStockMinimo() != null ? p.getStockMinimo() : 0,           centerStyle(wb, even));
                cell(row, 7, p.isActivo() ? "Activo" : "Inactivo",                         centerStyle(wb, even));
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
            sheet.createFreezePane(0, 1);
            sheet.setColumnWidth(0, 3800);
            sheet.setColumnWidth(1, 9000);
            sheet.setColumnWidth(2, 4500);
            sheet.setColumnWidth(3, 3800);
            sheet.setColumnWidth(4, 3800);
            sheet.setColumnWidth(5, 2800);
            sheet.setColumnWidth(6, 3200);
            sheet.setColumnWidth(7, 3200);

            wb.write(response.getOutputStream());
        }
    }

    public void exportarMovimientos(HttpServletResponse response) throws IOException {
        List<Movimiento> movimientos = movimientoRepository.findAllByOrderByFechaDesc();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Kardex");
            sheet.setDefaultRowHeightInPoints(18);

            String[] headers = {"Fecha", "Producto", "SKU", "Tipo", "Cantidad", "Stock Ant.", "Stock Final", "Doc. Ref.", "Usuario"};
            Row hRow = sheet.createRow(0);
            hRow.setHeightInPoints(24);
            XSSFCellStyle hdr = headerStyle(wb);
            for (int i = 0; i < headers.length; i++) cell(hRow, i, headers[i], hdr);

            int rowNum = 1;
            for (Movimiento m : movimientos) {
                boolean even = rowNum % 2 == 0;
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(18);

                byte[] tipoColor = "ENTRADA".equals(m.getTipo()) ? GREEN : RED;

                cell(row, 0, m.getFecha().toString().replace("T", " ").substring(0, 16), rowStyle(wb, even));
                cell(row, 1, m.getProducto().getNombre(),                                 rowStyle(wb, even));
                cell(row, 2, m.getProducto().getSku(),                                    rowStyle(wb, even));
                cell(row, 3, m.getTipo(),                                                 coloredTextStyle(wb, even, tipoColor));
                cell(row, 4, m.getCantidad(),                                             centerStyle(wb, even));
                cell(row, 5, m.getStockAnterior(),                                        centerStyle(wb, even));
                cell(row, 6, m.getStockResultante(),                                      centerStyle(wb, even));
                cell(row, 7, m.getDocumentoRef() != null ? m.getDocumentoRef() : "",     rowStyle(wb, even));
                cell(row, 8, m.getUsuarioNombre() != null ? m.getUsuarioNombre() : "",   rowStyle(wb, even));
            }

            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
            sheet.createFreezePane(0, 1);
            sheet.setColumnWidth(0, 4200);
            sheet.setColumnWidth(1, 9000);
            sheet.setColumnWidth(2, 3800);
            sheet.setColumnWidth(3, 3200);
            sheet.setColumnWidth(4, 2800);
            sheet.setColumnWidth(5, 3000);
            sheet.setColumnWidth(6, 3200);
            sheet.setColumnWidth(7, 3800);
            sheet.setColumnWidth(8, 3200);

            wb.write(response.getOutputStream());
        }
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(BLUE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(WHITE, null));
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s, BorderStyle.THIN, new XSSFColor(WHITE, null));
        return s;
    }

    private XSSFCellStyle rowStyle(XSSFWorkbook wb, boolean even) {
        XSSFCellStyle s = wb.createCellStyle();
        if (even) {
            s.setFillForegroundColor(new XSSFColor(STRIPE, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s, BorderStyle.THIN, new XSSFColor(BORDER, null));
        return s;
    }

    private XSSFCellStyle priceStyle(XSSFWorkbook wb, boolean even) {
        XSSFCellStyle s = rowStyle(wb, even);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private XSSFCellStyle centerStyle(XSSFWorkbook wb, boolean even) {
        XSSFCellStyle s = rowStyle(wb, even);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle coloredTextStyle(XSSFWorkbook wb, boolean even, byte[] color) {
        XSSFCellStyle s = rowStyle(wb, even);
        s.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(color, null));
        s.setFont(f);
        return s;
    }

    private void applyBorders(XSSFCellStyle s, BorderStyle bs, XSSFColor color) {
        s.setBorderTop(bs);    s.setTopBorderColor(color);
        s.setBorderBottom(bs); s.setBottomBorderColor(color);
        s.setBorderLeft(bs);   s.setLeftBorderColor(color);
        s.setBorderRight(bs);  s.setRightBorderColor(color);
    }

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void cell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private void cell(Row row, int col, int value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }
}
