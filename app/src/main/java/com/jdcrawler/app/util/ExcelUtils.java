package com.jdcrawler.app.util;

import android.util.Log;

import com.jdcrawler.app.model.ProductInfo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel生成工具类
 * 使用Apache POI库生成包含商品信息的Excel文件
 */
public class ExcelUtils {
    
    private static final String TAG = "ExcelUtils";
    
    // Excel样式配置
    private static final String FONT_NAME = "Arial";
    private static final short HEADER_FONT_SIZE = 12;
    private static final short DATA_FONT_SIZE = 10;
    
    /**
     * 导出商品信息到Excel文件
     */
    public static boolean exportProductsToExcel(List<ProductInfo> products, String filePath) {
        if (products == null || products.isEmpty()) {
            Log.w(TAG, "没有商品数据需要导出");
            return false;
        }
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Log.d(TAG, "开始创建Excel文件，商品数量: " + products.size());
            
            // 创建基础信息工作表
            createBasicInfoSheet(workbook, products);
            
            // 创建规格参数工作表
            createSpecificationsSheet(workbook, products);
            
            // 创建统计分析工作表
            createAnalysisSheet(workbook, products);
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                Log.d(TAG, "Excel文件创建成功: " + filePath);
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "创建Excel文件时发生错误", e);
            return false;
        }
    }
    
    /**
     * 创建基础信息工作表
     */
    private static void createBasicInfoSheet(Workbook workbook, List<ProductInfo> products) {
        try {
            Sheet sheet = workbook.createSheet("商品基础信息");
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle priceStyle = createPriceStyle(workbook);
            
            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("京东商品信息爬取报告");
            titleCell.setCellStyle(createTitleStyle(workbook));
            
            // 合并标题单元格
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
            
            // 创建信息行
            Row infoRow = sheet.createRow(1);
            Cell infoCell = infoRow.createCell(0);
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            infoCell.setCellValue("生成时间: " + currentTime + " | 商品总数: " + products.size());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
            
            // 创建表头
            Row headerRow = sheet.createRow(3);
            String[] headers = {
                "序号", "商品标题", "价格", "店铺名称", "商品链接", 
                "销量", "评价数", "库存状态", "爬取时间"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            for (int i = 0; i < products.size(); i++) {
                ProductInfo product = products.get(i);
                Row dataRow = sheet.createRow(i + 4);
                
                // 序号
                dataRow.createCell(0).setCellValue(i + 1);
                
                // 商品标题
                Cell titleCell2 = dataRow.createCell(1);
                titleCell2.setCellValue(product.getTitle());
                titleCell2.setCellStyle(dataStyle);
                
                // 价格
                Cell priceCell = dataRow.createCell(2);
                priceCell.setCellValue(product.getFormattedPrice());
                priceCell.setCellStyle(priceStyle);
                
                // 店铺名称
                dataRow.createCell(3).setCellValue(product.getShopName());
                
                // 商品链接
                if (!product.getProductUrl().isEmpty()) {
                    Cell linkCell = dataRow.createCell(4);
                    linkCell.setCellValue(product.getProductUrl());
                    // 可以考虑添加超链接样式
                }
                
                // 销量
                dataRow.createCell(5).setCellValue(product.getSalesCount());
                
                // 评价数
                dataRow.createCell(6).setCellValue(product.getReviewCount());
                
                // 库存状态
                Cell stockCell = dataRow.createCell(7);
                stockCell.setCellValue(product.getStockStatus());
                if (product.isInStock()) {
                    stockCell.setCellStyle(createGreenStyle(workbook));
                } else {
                    stockCell.setCellStyle(createRedStyle(workbook));
                }
                
                // 爬取时间
                dataRow.createCell(8).setCellValue(product.getCrawlTime());
                
                // 设置数据样式
                for (int j = 0; j < 9; j++) {
                    Cell cell = dataRow.getCell(j);
                    if (cell != null && cell.getCellStyle() == null) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最大宽度限制
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }
            
            Log.d(TAG, "基础信息工作表创建完成");
            
        } catch (Exception e) {
            Log.e(TAG, "创建基础信息工作表时发生错误", e);
        }
    }
    
    /**
     * 创建规格参数工作表
     */
    private static void createSpecificationsSheet(Workbook workbook, List<ProductInfo> products) {
        try {
            Sheet sheet = workbook.createSheet("商品规格参数");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("商品标题");
            headerRow.createCell(2).setCellValue("参数名称");
            headerRow.createCell(3).setCellValue("参数值");
            headerRow.createCell(4).setCellValue("店铺名称");
            
            for (int i = 0; i < 5; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }
            
            int currentRow = 1;
            
            // 填充规格数据
            for (int i = 0; i < products.size(); i++) {
                ProductInfo product = products.get(i);
                Map<String, String> specs = product.getSpecifications();
                
                if (specs.isEmpty()) {
                    // 如果没有规格参数，创建一行说明
                    Row dataRow = sheet.createRow(currentRow++);
                    dataRow.createCell(0).setCellValue(i + 1);
                    dataRow.createCell(1).setCellValue(product.getShortDescription());
                    dataRow.createCell(2).setCellValue("无规格参数");
                    dataRow.createCell(3).setCellValue("-");
                    dataRow.createCell(4).setCellValue(product.getShopName());
                } else {
                    // 为每个规格参数创建一行
                    for (Map.Entry<String, String> spec : specs.entrySet()) {
                        Row dataRow = sheet.createRow(currentRow++);
                        dataRow.createCell(0).setCellValue(i + 1);
                        dataRow.createCell(1).setCellValue(product.getShortDescription());
                        dataRow.createCell(2).setCellValue(spec.getKey());
                        dataRow.createCell(3).setCellValue(spec.getValue());
                        dataRow.createCell(4).setCellValue(product.getShopName());
                        
                        // 设置样式
                        for (int j = 0; j < 5; j++) {
                            dataRow.getCell(j).setCellStyle(dataStyle);
                        }
                    }
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            Log.d(TAG, "规格参数工作表创建完成，数据行数: " + (currentRow - 1));
            
        } catch (Exception e) {
            Log.e(TAG, "创建规格参数工作表时发生错误", e);
        }
    }
    
    /**
     * 创建统计分析工作表
     */
    private static void createAnalysisSheet(Workbook workbook, List<ProductInfo> products) {
        try {
            Sheet sheet = workbook.createSheet("数据分析");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int currentRow = 0;
            
            // 基本统计信息
            Row titleRow = sheet.createRow(currentRow++);
            titleRow.createCell(0).setCellValue("数据统计分析");
            titleRow.getCell(0).setCellStyle(createTitleStyle(workbook));
            
            currentRow++; // 空行
            
            // 总体统计
            Row statRow1 = sheet.createRow(currentRow++);
            statRow1.createCell(0).setCellValue("商品总数");
            statRow1.createCell(1).setCellValue(products.size());
            
            // 计算价格统计
            double totalPrice = 0;
            double maxPrice = 0;
            double minPrice = Double.MAX_VALUE;
            int validPriceCount = 0;
            
            for (ProductInfo product : products) {
                double price = product.getPriceAsNumber();
                if (price > 0) {
                    totalPrice += price;
                    maxPrice = Math.max(maxPrice, price);
                    minPrice = Math.min(minPrice, price);
                    validPriceCount++;
                }
            }
            
            if (validPriceCount > 0) {
                Row statRow2 = sheet.createRow(currentRow++);
                statRow2.createCell(0).setCellValue("有效价格商品数");
                statRow2.createCell(1).setCellValue(validPriceCount);
                
                Row statRow3 = sheet.createRow(currentRow++);
                statRow3.createCell(0).setCellValue("平均价格");
                statRow3.createCell(1).setCellValue(String.format("¥%.2f", totalPrice / validPriceCount));
                
                Row statRow4 = sheet.createRow(currentRow++);
                statRow4.createCell(0).setCellValue("最高价格");
                statRow4.createCell(1).setCellValue(String.format("¥%.2f", maxPrice));
                
                Row statRow5 = sheet.createRow(currentRow++);
                statRow5.createCell(0).setCellValue("最低价格");
                statRow5.createCell(1).setCellValue(String.format("¥%.2f", minPrice));
            }
            
            // 店铺统计
            currentRow += 2; // 空行
            Row shopTitleRow = sheet.createRow(currentRow++);
            shopTitleRow.createCell(0).setCellValue("店铺分布统计");
            shopTitleRow.getCell(0).setCellStyle(headerStyle);
            
            // 统计各店铺商品数量
            Map<String, Integer> shopCounts = new java.util.HashMap<>();
            for (ProductInfo product : products) {
                String shopName = product.getShopName();
                shopCounts.put(shopName, shopCounts.getOrDefault(shopName, 0) + 1);
            }
            
            Row shopHeaderRow = sheet.createRow(currentRow++);
            shopHeaderRow.createCell(0).setCellValue("店铺名称");
            shopHeaderRow.createCell(1).setCellValue("商品数量");
            shopHeaderRow.getCell(0).setCellStyle(headerStyle);
            shopHeaderRow.getCell(1).setCellStyle(headerStyle);
            
            for (Map.Entry<String, Integer> entry : shopCounts.entrySet()) {
                Row shopRow = sheet.createRow(currentRow++);
                shopRow.createCell(0).setCellValue(entry.getKey());
                shopRow.createCell(1).setCellValue(entry.getValue());
                shopRow.getCell(0).setCellStyle(dataStyle);
                shopRow.getCell(1).setCellStyle(dataStyle);
            }
            
            // 库存状态统计
            currentRow += 2; // 空行
            Row stockTitleRow = sheet.createRow(currentRow++);
            stockTitleRow.createCell(0).setCellValue("库存状态统计");
            stockTitleRow.getCell(0).setCellStyle(headerStyle);
            
            Map<String, Integer> stockCounts = new java.util.HashMap<>();
            for (ProductInfo product : products) {
                String stockStatus = product.getStockStatus();
                stockCounts.put(stockStatus, stockCounts.getOrDefault(stockStatus, 0) + 1);
            }
            
            Row stockHeaderRow = sheet.createRow(currentRow++);
            stockHeaderRow.createCell(0).setCellValue("库存状态");
            stockHeaderRow.createCell(1).setCellValue("商品数量");
            stockHeaderRow.getCell(0).setCellStyle(headerStyle);
            stockHeaderRow.getCell(1).setCellStyle(headerStyle);
            
            for (Map.Entry<String, Integer> entry : stockCounts.entrySet()) {
                Row stockRow = sheet.createRow(currentRow++);
                stockRow.createCell(0).setCellValue(entry.getKey());
                stockRow.createCell(1).setCellValue(entry.getValue());
                stockRow.getCell(0).setCellStyle(dataStyle);
                stockRow.getCell(1).setCellStyle(dataStyle);
            }
            
            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            Log.d(TAG, "数据分析工作表创建完成");
            
        } catch (Exception e) {
            Log.e(TAG, "创建数据分析工作表时发生错误", e);
        }
    }
    
    // ========== 样式创建方法 ==========
    
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(HEADER_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);
        
        // 设置背景色
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 设置对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(DATA_FONT_SIZE);
        style.setFont(font);
        
        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 设置对齐
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    private static CellStyle createPriceStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
    
    private static CellStyle createGreenStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    private static CellStyle createRedStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}