package com.jdcrawler.app.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.jdcrawler.app.model.ProductInfo;
import com.jdcrawler.app.util.NodeUtils;
import com.jdcrawler.app.util.ExcelUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据收集器
 * 负责从商品详情页提取完整的商品信息并生成Excel报告
 */
public class DataCollector {
    
    private static final String TAG = "DataCollector";
    
    // 价格匹配模式
    private static final Pattern PRICE_PATTERN = Pattern.compile("¥?([0-9,]+\\.?[0-9]*)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9,]+)");
    
    // 商品信息关键词映射
    private static final Map<String, String> SPEC_KEYWORDS = new HashMap<>();
    
    static {
        // 初始化规格参数关键词映射
        SPEC_KEYWORDS.put("品牌", "brand");
        SPEC_KEYWORDS.put("型号", "model");
        SPEC_KEYWORDS.put("颜色", "color");
        SPEC_KEYWORDS.put("尺寸", "size");
        SPEC_KEYWORDS.put("材质", "material");
        SPEC_KEYWORDS.put("重量", "weight");
        SPEC_KEYWORDS.put("产地", "origin");
        SPEC_KEYWORDS.put("保修", "warranty");
        SPEC_KEYWORDS.put("规格", "specification");
        SPEC_KEYWORDS.put("包装", "package");
    }
    
    /**
     * 从商品详情页提取完整商品信息
     */
    public ProductInfo extractProductDetail(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            Log.w(TAG, "根节点为空，无法提取商品信息");
            return null;
        }
        
        try {
            Log.d(TAG, "开始提取商品详情信息");
            
            ProductInfo productInfo = new ProductInfo();
            
            // 提取基础信息
            extractBasicInfo(rootNode, productInfo);
            
            // 提取价格信息
            extractPriceInfo(rootNode, productInfo);
            
            // 提取规格参数
            extractSpecifications(rootNode, productInfo);
            
            // 提取销售信息
            extractSalesInfo(rootNode, productInfo);
            
            // 提取图片信息
            extractImageInfo(rootNode, productInfo);
            
            // 设置提取时间
            productInfo.setCrawlTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            Log.d(TAG, "商品信息提取完成: " + productInfo.getTitle());
            return productInfo;
            
        } catch (Exception e) {
            Log.e(TAG, "提取商品详情时发生错误", e);
            return null;
        }
    }
    
    /**
     * 提取基础商品信息
     */
    private void extractBasicInfo(AccessibilityNodeInfo rootNode, ProductInfo productInfo) {
        try {
            // 提取商品标题
            String title = extractProductTitle(rootNode);
            productInfo.setTitle(title);
            
            // 提取商品ID/SKU
            String sku = extractProductSku(rootNode);
            productInfo.setSku(sku);
            
            // 提取店铺信息
            String shopName = extractShopName(rootNode);
            productInfo.setShopName(shopName);
            
            // 生成商品链接（如果可能的话）
            String productUrl = generateProductUrl(sku);
            productInfo.setProductUrl(productUrl);
            
            Log.d(TAG, "基础信息提取完成 - 标题: " + title + ", SKU: " + sku + ", 店铺: " + shopName);
            
        } catch (Exception e) {
            Log.e(TAG, "提取基础信息时发生错误", e);
        }
    }
    
    /**
     * 提取商品标题
     */
    private String extractProductTitle(AccessibilityNodeInfo rootNode) {
        try {
            // 尝试多种方式查找标题
            String[] titleSelectors = {
                "商品名称", "商品标题", "title", "product_title", "goods_title"
            };
            
            // 方法1: 通过资源ID查找
            List<AccessibilityNodeInfo> titleNodes = NodeUtils.findNodesByResourceId(rootNode, titleSelectors);
            for (AccessibilityNodeInfo node : titleNodes) {
                String text = NodeUtils.getNodeText(node);
                if (!text.isEmpty() && text.length() > 10) { // 标题通常较长
                    NodeUtils.recycleNodes(titleNodes);
                    return text.trim();
                }
            }
            
            // 方法2: 通过页面结构分析
            String title = extractTitleByStructure(rootNode);
            if (!title.isEmpty()) {
                return title;
            }
            
            // 方法3: 查找最长的文本作为标题
            return findLongestText(rootNode);
            
        } catch (Exception e) {
            Log.e(TAG, "提取商品标题时发生错误", e);
            return "未知商品";
        }
    }
    
    /**
     * 通过页面结构分析提取标题
     */
    private String extractTitleByStructure(AccessibilityNodeInfo rootNode) {
        try {
            String pageText = NodeUtils.getNodeText(rootNode);
            String[] lines = pageText.split("\\n");
            
            // 标题通常在页面前几行，且长度适中
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 10 && line.length() < 200 && 
                    !line.contains("¥") && !line.contains("价格") && 
                    !line.contains("购买") && !line.contains("评价")) {
                    return line;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "结构分析提取标题时发生错误", e);
        }
        
        return "";
    }
    
    /**
     * 查找页面中最长的文本作为标题
     */
    private String findLongestText(AccessibilityNodeInfo rootNode) {
        try {
            String longestText = "";
            findLongestTextRecursive(rootNode, longestText);
            return longestText;
            
        } catch (Exception e) {
            Log.e(TAG, "查找最长文本时发生错误", e);
            return "商品标题获取失败";
        }
    }
    
    private String findLongestTextRecursive(AccessibilityNodeInfo node, String currentLongest) {
        if (node == null) return currentLongest;
        
        try {
            CharSequence nodeText = node.getText();
            if (nodeText != null && nodeText.length() > currentLongest.length() && 
                nodeText.length() > 10 && nodeText.length() < 200) {
                String text = nodeText.toString().trim();
                if (!text.contains("¥") && !text.contains("价格")) {
                    currentLongest = text;
                }
            }
            
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    currentLongest = findLongestTextRecursive(child, currentLongest);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "递归查找最长文本时发生错误", e);
        }
        
        return currentLongest;
    }
    
    /**
     * 提取商品SKU
     */
    private String extractProductSku(AccessibilityNodeInfo rootNode) {
        try {
            String pageText = NodeUtils.getNodeText(rootNode);
            
            // 查找商品编号模式
            Pattern skuPattern = Pattern.compile("商品编号[：:]*\\s*([0-9A-Za-z]+)");
            Matcher matcher = skuPattern.matcher(pageText);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 查找SKU模式
            Pattern skuPattern2 = Pattern.compile("SKU[：:]*\\s*([0-9A-Za-z]+)");
            matcher = skuPattern2.matcher(pageText);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 如果找不到，生成一个基于时间的临时ID
            return "JD_" + System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "提取SKU时发生错误", e);
            return "SKU_UNKNOWN";
        }
    }
    
    /**
     * 提取店铺名称
     */
    private String extractShopName(AccessibilityNodeInfo rootNode) {
        try {
            String[] shopKeywords = {"店铺", "商家", "卖家", "旗舰店", "专营店", "官方店"};
            
            List<AccessibilityNodeInfo> shopNodes = NodeUtils.findNodesByText(rootNode, shopKeywords);
            for (AccessibilityNodeInfo node : shopNodes) {
                String text = NodeUtils.getNodeText(node);
                // 查找包含店铺关键词的文本
                for (String keyword : shopKeywords) {
                    if (text.contains(keyword)) {
                        String shopName = extractShopNameFromText(text, keyword);
                        if (!shopName.isEmpty()) {
                            NodeUtils.recycleNodes(shopNodes);
                            return shopName;
                        }
                    }
                }
            }
            
            NodeUtils.recycleNodes(shopNodes);
            return "未知店铺";
            
        } catch (Exception e) {
            Log.e(TAG, "提取店铺名称时发生错误", e);
            return "店铺名称获取失败";
        }
    }
    
    /**
     * 从文本中提取店铺名称
     */
    private String extractShopNameFromText(String text, String keyword) {
        try {
            // 查找关键词前后的文本
            int keywordIndex = text.indexOf(keyword);
            if (keywordIndex >= 0) {
                // 通常店铺名称在关键词之前
                String beforeKeyword = text.substring(0, keywordIndex).trim();
                if (!beforeKeyword.isEmpty() && beforeKeyword.length() < 50) {
                    return beforeKeyword;
                }
                
                // 或者在关键词之后
                String afterKeyword = text.substring(keywordIndex + keyword.length()).trim();
                if (!afterKeyword.isEmpty() && afterKeyword.length() < 50) {
                    return afterKeyword;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "从文本提取店铺名称时发生错误", e);
        }
        
        return "";
    }
    
    /**
     * 提取价格信息
     */
    private void extractPriceInfo(AccessibilityNodeInfo rootNode, ProductInfo productInfo) {
        try {
            String pageText = NodeUtils.getNodeText(rootNode);
            
            // 查找价格相关的节点
            String[] priceKeywords = {"价格", "¥", "￥", "现价", "售价", "优惠价"};
            List<AccessibilityNodeInfo> priceNodes = NodeUtils.findNodesByText(rootNode, priceKeywords);
            
            List<String> allPrices = new ArrayList<>();
            
            // 从价格节点中提取价格
            for (AccessibilityNodeInfo node : priceNodes) {
                String nodeText = NodeUtils.getNodeText(node);
                String price = NodeUtils.extractPrice(nodeText);
                if (!price.isEmpty()) {
                    allPrices.add(price);
                }
            }
            
            // 从整个页面文本中提取价格
            Matcher matcher = PRICE_PATTERN.matcher(pageText);
            while (matcher.find()) {
                String price = matcher.group(1);
                if (!allPrices.contains(price)) {
                    allPrices.add(price);
                }
            }
            
            // 选择最合适的价格（通常是第一个找到的价格）
            if (!allPrices.isEmpty()) {
                productInfo.setPrice(allPrices.get(0));
                
                // 如果有多个价格，记录价格范围
                if (allPrices.size() > 1) {
                    productInfo.setPriceRange(String.join(" - ", allPrices));
                }
            } else {
                productInfo.setPrice("价格未知");
            }
            
            NodeUtils.recycleNodes(priceNodes);
            Log.d(TAG, "价格信息提取完成: " + productInfo.getPrice());
            
        } catch (Exception e) {
            Log.e(TAG, "提取价格信息时发生错误", e);
            productInfo.setPrice("价格提取失败");
        }
    }
    
    /**
     * 提取规格参数
     */
    private void extractSpecifications(AccessibilityNodeInfo rootNode, ProductInfo productInfo) {
        try {
            Map<String, String> specifications = new HashMap<>();
            String pageText = NodeUtils.getNodeText(rootNode);
            
            // 查找规格参数表
            extractSpecsFromStructure(rootNode, specifications);
            
            // 从页面文本中提取规格信息
            extractSpecsFromText(pageText, specifications);
            
            productInfo.setSpecifications(specifications);
            
            Log.d(TAG, "规格参数提取完成，共 " + specifications.size() + " 个参数");
            
        } catch (Exception e) {
            Log.e(TAG, "提取规格参数时发生错误", e);
        }
    }
    
    /**
     * 从结构中提取规格参数
     */
    private void extractSpecsFromStructure(AccessibilityNodeInfo rootNode, Map<String, String> specifications) {
        try {
            // 查找规格参数相关的节点
            String[] specKeywords = {"规格参数", "商品参数", "产品参数", "详细参数"};
            List<AccessibilityNodeInfo> specNodes = NodeUtils.findNodesByText(rootNode, specKeywords);
            
            for (AccessibilityNodeInfo node : specNodes) {
                extractSpecsFromNode(node, specifications);
            }
            
            NodeUtils.recycleNodes(specNodes);
            
        } catch (Exception e) {
            Log.e(TAG, "从结构提取规格参数时发生错误", e);
        }
    }
    
    /**
     * 从节点中提取规格参数
     */
    private void extractSpecsFromNode(AccessibilityNodeInfo node, Map<String, String> specifications) {
        if (node == null) return;
        
        try {
            String nodeText = NodeUtils.getNodeText(node);
            String[] lines = nodeText.split("\\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.contains(":") || line.contains("：")) {
                    String[] parts = line.split("[：:]", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (!key.isEmpty() && !value.isEmpty()) {
                            specifications.put(key, value);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "从节点提取规格时发生错误", e);
        }
    }
    
    /**
     * 从文本中提取规格参数
     */
    private void extractSpecsFromText(String pageText, Map<String, String> specifications) {
        try {
            // 使用正则表达式匹配 "键:值" 或 "键：值" 模式
            Pattern specPattern = Pattern.compile("([^\\n:：]{2,20})[：:]([^\\n:：]{1,50})");
            Matcher matcher = specPattern.matcher(pageText);
            
            while (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                
                // 过滤掉不相关的信息
                if (isValidSpecification(key, value)) {
                    specifications.put(key, value);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "从文本提取规格参数时发生错误", e);
        }
    }
    
    /**
     * 验证是否为有效的规格参数
     */
    private boolean isValidSpecification(String key, String value) {
        // 过滤掉不相关的键值对
        String[] invalidKeys = {"购买", "评价", "收藏", "分享", "客服", "返回", "确定", "取消"};
        String keyLower = key.toLowerCase();
        
        for (String invalid : invalidKeys) {
            if (keyLower.contains(invalid)) {
                return false;
            }
        }
        
        // 检查值是否合理
        return value.length() > 0 && value.length() < 100 && !value.contains("点击");
    }
    
    /**
     * 提取销售信息
     */
    private void extractSalesInfo(AccessibilityNodeInfo rootNode, ProductInfo productInfo) {
        try {
            String pageText = NodeUtils.getNodeText(rootNode);
            
            // 提取销量
            String salesCount = extractSalesCount(pageText);
            productInfo.setSalesCount(salesCount);
            
            // 提取评价数
            String reviewCount = extractReviewCount(pageText);
            productInfo.setReviewCount(reviewCount);
            
            // 提取库存状态
            String stockStatus = extractStockStatus(pageText);
            productInfo.setStockStatus(stockStatus);
            
            Log.d(TAG, "销售信息提取完成 - 销量: " + salesCount + ", 评价: " + reviewCount + ", 库存: " + stockStatus);
            
        } catch (Exception e) {
            Log.e(TAG, "提取销售信息时发生错误", e);
        }
    }
    
    /**
     * 提取销量
     */
    private String extractSalesCount(String pageText) {
        try {
            Pattern salesPattern = Pattern.compile("销量[：:]?\\s*([0-9,]+)|已售[：:]?\\s*([0-9,]+)|月销[：:]?\\s*([0-9,]+)");
            Matcher matcher = salesPattern.matcher(pageText);
            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return matcher.group(i);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取销量时发生错误", e);
        }
        return "0";
    }
    
    /**
     * 提取评价数
     */
    private String extractReviewCount(String pageText) {
        try {
            Pattern reviewPattern = Pattern.compile("评价[：:]?\\s*([0-9,]+)|评论[：:]?\\s*([0-9,]+)|([0-9,]+)\\s*条评价");
            Matcher matcher = reviewPattern.matcher(pageText);
            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        return matcher.group(i);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取评价数时发生错误", e);
        }
        return "0";
    }
    
    /**
     * 提取库存状态
     */
    private String extractStockStatus(String pageText) {
        try {
            if (pageText.contains("现货") || pageText.contains("有库存")) {
                return "现货";
            } else if (pageText.contains("缺货") || pageText.contains("无库存")) {
                return "缺货";
            } else if (pageText.contains("预售") || pageText.contains("预定")) {
                return "预售";
            } else {
                return "未知";
            }
        } catch (Exception e) {
            Log.e(TAG, "提取库存状态时发生错误", e);
            return "未知";
        }
    }
    
    /**
     * 提取图片信息
     */
    private void extractImageInfo(AccessibilityNodeInfo rootNode, ProductInfo productInfo) {
        try {
            // 这里可以扩展提取商品图片URL的逻辑
            // 由于AccessibilityService的限制，图片URL提取比较困难
            // 可以尝试查找ImageView节点的contentDescription
            
            List<String> imageUrls = new ArrayList<>();
            extractImageUrlsRecursive(rootNode, imageUrls);
            
            if (!imageUrls.isEmpty()) {
                productInfo.setMainImageUrl(imageUrls.get(0));
                productInfo.setImageUrls(imageUrls);
            }
            
            Log.d(TAG, "图片信息提取完成，找到 " + imageUrls.size() + " 个图片");
            
        } catch (Exception e) {
            Log.e(TAG, "提取图片信息时发生错误", e);
        }
    }
    
    /**
     * 递归提取图片URL
     */
    private void extractImageUrlsRecursive(AccessibilityNodeInfo node, List<String> imageUrls) {
        if (node == null) return;
        
        try {
            // 检查当前节点是否是ImageView
            if ("android.widget.ImageView".equals(node.getClassName())) {
                CharSequence contentDesc = node.getContentDescription();
                if (contentDesc != null && contentDesc.toString().startsWith("http")) {
                    imageUrls.add(contentDesc.toString());
                }
            }
            
            // 递归检查子节点
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    extractImageUrlsRecursive(child, imageUrls);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "递归提取图片URL时发生错误", e);
        }
    }
    
    /**
     * 生成商品链接
     */
    private String generateProductUrl(String sku) {
        try {
            if (sku != null && !sku.isEmpty() && !sku.startsWith("JD_")) {
                return "https://item.jd.com/" + sku + ".html";
            }
        } catch (Exception e) {
            Log.e(TAG, "生成商品链接时发生错误", e);
        }
        return "";
    }
    
    /**
     * 导出数据到Excel
     */
    public void exportToExcel(List<ProductInfo> products, Context context) {
        try {
            if (products == null || products.isEmpty()) {
                Log.w(TAG, "没有商品数据需要导出");
                return;
            }
            
            Log.d(TAG, "开始导出 " + products.size() + " 个商品到Excel");
            
            // 创建导出目录
            File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "JDCrawler");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "京东商品数据_" + timestamp + ".xlsx";
            File excelFile = new File(exportDir, fileName);
            
            // 使用ExcelUtils导出
            boolean success = ExcelUtils.exportProductsToExcel(products, excelFile.getAbsolutePath());
            
            if (success) {
                Log.d(TAG, "Excel文件导出成功: " + excelFile.getAbsolutePath());
            } else {
                Log.e(TAG, "Excel文件导出失败");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "导出Excel时发生错误", e);
        }
    }
}