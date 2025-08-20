package com.jdcrawler.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品信息模型类
 * 存储从京东商品详情页提取的完整商品信息
 */
public class ProductInfo {
    
    // 基础信息
    private String title;           // 商品标题
    private String sku;             // 商品SKU/编号
    private String shopName;        // 店铺名称
    private String productUrl;      // 商品链接
    private String crawlTime;       // 爬取时间
    
    // 价格信息
    private String price;           // 当前价格
    private String priceRange;      // 价格范围（多规格商品）
    private String originalPrice;   // 原价
    private String discount;        // 折扣信息
    
    // 销售信息
    private String salesCount;      // 销量
    private String reviewCount;     // 评价数
    private String stockStatus;     // 库存状态
    private String rating;          // 商品评分
    
    // 规格参数
    private Map<String, String> specifications; // 规格参数表
    private List<String> availableSpecs;        // 可选规格
    
    // 图片信息
    private String mainImageUrl;    // 主图URL
    private List<String> imageUrls; // 所有图片URL
    
    // 详细描述
    private String description;     // 商品描述
    private String category;        // 商品分类
    private String brand;           // 品牌
    
    // 附加信息
    private Map<String, String> additionalInfo; // 其他附加信息
    private boolean isSuccess;      // 是否成功提取
    private String errorMessage;    // 错误信息
    
    public ProductInfo() {
        this.specifications = new HashMap<>();
        this.availableSpecs = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.additionalInfo = new HashMap<>();
        this.isSuccess = true;
        this.price = "";
        this.title = "";
        this.sku = "";
        this.shopName = "";
        this.productUrl = "";
        this.crawlTime = "";
        this.priceRange = "";
        this.originalPrice = "";
        this.discount = "";
        this.salesCount = "0";
        this.reviewCount = "0";
        this.stockStatus = "未知";
        this.rating = "";
        this.mainImageUrl = "";
        this.description = "";
        this.category = "";
        this.brand = "";
        this.errorMessage = "";
    }
    
    // ========== Getter和Setter方法 ==========
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku != null ? sku : "";
    }
    
    public String getShopName() {
        return shopName;
    }
    
    public void setShopName(String shopName) {
        this.shopName = shopName != null ? shopName : "";
    }
    
    public String getProductUrl() {
        return productUrl;
    }
    
    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl != null ? productUrl : "";
    }
    
    public String getCrawlTime() {
        return crawlTime;
    }
    
    public void setCrawlTime(String crawlTime) {
        this.crawlTime = crawlTime != null ? crawlTime : "";
    }
    
    public String getPrice() {
        return price;
    }
    
    public void setPrice(String price) {
        this.price = price != null ? price : "";
    }
    
    public String getPriceRange() {
        return priceRange;
    }
    
    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange != null ? priceRange : "";
    }
    
    public String getOriginalPrice() {
        return originalPrice;
    }
    
    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice != null ? originalPrice : "";
    }
    
    public String getDiscount() {
        return discount;
    }
    
    public void setDiscount(String discount) {
        this.discount = discount != null ? discount : "";
    }
    
    public String getSalesCount() {
        return salesCount;
    }
    
    public void setSalesCount(String salesCount) {
        this.salesCount = salesCount != null ? salesCount : "0";
    }
    
    public String getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(String reviewCount) {
        this.reviewCount = reviewCount != null ? reviewCount : "0";
    }
    
    public String getStockStatus() {
        return stockStatus;
    }
    
    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus != null ? stockStatus : "未知";
    }
    
    public String getRating() {
        return rating;
    }
    
    public void setRating(String rating) {
        this.rating = rating != null ? rating : "";
    }
    
    public Map<String, String> getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(Map<String, String> specifications) {
        this.specifications = specifications != null ? specifications : new HashMap<>();
    }
    
    public void addSpecification(String key, String value) {
        if (key != null && value != null) {
            this.specifications.put(key, value);
        }
    }
    
    public List<String> getAvailableSpecs() {
        return availableSpecs;
    }
    
    public void setAvailableSpecs(List<String> availableSpecs) {
        this.availableSpecs = availableSpecs != null ? availableSpecs : new ArrayList<>();
    }
    
    public void addAvailableSpec(String spec) {
        if (spec != null && !this.availableSpecs.contains(spec)) {
            this.availableSpecs.add(spec);
        }
    }
    
    public String getMainImageUrl() {
        return mainImageUrl;
    }
    
    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl != null ? mainImageUrl : "";
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
    
    public void addImageUrl(String imageUrl) {
        if (imageUrl != null && !this.imageUrls.contains(imageUrl)) {
            this.imageUrls.add(imageUrl);
        }
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category != null ? category : "";
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand != null ? brand : "";
    }
    
    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(Map<String, String> additionalInfo) {
        this.additionalInfo = additionalInfo != null ? additionalInfo : new HashMap<>();
    }
    
    public void addAdditionalInfo(String key, String value) {
        if (key != null && value != null) {
            this.additionalInfo.put(key, value);
        }
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    public void setSuccess(boolean success) {
        this.isSuccess = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage != null ? errorMessage : "";
        if (!errorMessage.isEmpty()) {
            this.isSuccess = false;
        }
    }
    
    // ========== 实用方法 ==========
    
    /**
     * 获取格式化的价格
     */
    public String getFormattedPrice() {
        if (price.isEmpty()) {
            return "价格未知";
        }
        
        if (price.startsWith("¥") || price.startsWith("￥")) {
            return price;
        } else {
            return "¥" + price;
        }
    }
    
    /**
     * 获取规格参数的格式化字符串
     */
    public String getSpecificationsString() {
        if (specifications.isEmpty()) {
            return "无规格参数";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : specifications.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 检查商品信息是否完整
     */
    public boolean isInfoComplete() {
        return !title.isEmpty() && !price.isEmpty() && !shopName.isEmpty();
    }
    
    /**
     * 获取商品的简短描述
     */
    public String getShortDescription() {
        String desc = title;
        if (desc.length() > 50) {
            desc = desc.substring(0, 47) + "...";
        }
        return desc;
    }
    
    /**
     * 获取数值形式的价格（用于排序和计算）
     */
    public double getPriceAsNumber() {
        try {
            // 移除非数字字符，保留小数点
            String numericPrice = price.replaceAll("[^0-9.]", "");
            if (!numericPrice.isEmpty()) {
                return Double.parseDouble(numericPrice);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return 0.0;
    }
    
    /**
     * 获取数值形式的销量
     */
    public int getSalesCountAsNumber() {
        try {
            String numericSales = salesCount.replaceAll("[^0-9]", "");
            if (!numericSales.isEmpty()) {
                return Integer.parseInt(numericSales);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return 0;
    }
    
    /**
     * 获取数值形式的评价数
     */
    public int getReviewCountAsNumber() {
        try {
            String numericReviews = reviewCount.replaceAll("[^0-9]", "");
            if (!numericReviews.isEmpty()) {
                return Integer.parseInt(numericReviews);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return 0;
    }
    
    /**
     * 检查是否有库存
     */
    public boolean isInStock() {
        return "现货".equals(stockStatus) || "有库存".equals(stockStatus);
    }
    
    /**
     * 获取商品信息的摘要
     */
    public String getSummary() {
        return String.format("商品: %s | 价格: %s | 店铺: %s | 销量: %s | 库存: %s",
                getShortDescription(),
                getFormattedPrice(),
                shopName,
                salesCount,
                stockStatus
        );
    }
    
    @Override
    public String toString() {
        return "ProductInfo{" +
                "title='" + title + '\'' +
                ", sku='" + sku + '\'' +
                ", price='" + price + '\'' +
                ", shopName='" + shopName + '\'' +
                ", salesCount='" + salesCount + '\'' +
                ", stockStatus='" + stockStatus + '\'' +
                ", isSuccess=" + isSuccess +
                '}';
    }
    
    /**
     * 创建一个包含错误信息的ProductInfo实例
     */
    public static ProductInfo createErrorInstance(String errorMessage) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setSuccess(false);
        productInfo.setErrorMessage(errorMessage);
        productInfo.setTitle("数据提取失败");
        return productInfo;
    }
    
    /**
     * 验证商品信息的有效性
     */
    public boolean validate() {
        if (!isSuccess) {
            return false;
        }
        
        // 基本信息不能为空
        if (title.trim().isEmpty()) {
            setErrorMessage("商品标题为空");
            return false;
        }
        
        if (price.trim().isEmpty()) {
            setErrorMessage("商品价格为空");
            return false;
        }
        
        // 价格必须是有效数字
        if (getPriceAsNumber() <= 0) {
            setErrorMessage("商品价格无效");
            return false;
        }
        
        return true;
    }
}