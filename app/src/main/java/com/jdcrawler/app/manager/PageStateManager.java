package com.jdcrawler.app.manager;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.jdcrawler.app.util.NodeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面状态管理器
 * 负责识别当前页面类型和管理页面状态转换
 */
public class PageStateManager {
    
    private static final String TAG = "PageStateManager";
    
    /**
     * 页面类型枚举
     */
    public enum PageType {
        SHOP_HOME,      // 店铺首页
        PRODUCT_LIST,   // 商品列表页
        PRODUCT_DETAIL, // 商品详情页
        SEARCH_RESULT,  // 搜索结果页
        CATEGORY,       // 分类页面
        UNKNOWN         // 未知页面
    }
    
    private PageType currentPageType = PageType.UNKNOWN;
    private PageType previousPageType = PageType.UNKNOWN;
    private List<PageType> pageHistory = new ArrayList<>();
    
    /**
     * 检测当前页面类型
     */
    public PageType detectPageType(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return PageType.UNKNOWN;
        }
        
        previousPageType = currentPageType;
        
        try {
            // 获取页面文本内容用于分析
            String pageText = NodeUtils.getNodeText(rootNode).toLowerCase();
            Log.d(TAG, "页面关键文本: " + pageText.substring(0, Math.min(200, pageText.length())));
            
            // 1. 检测商品详情页 - 优先级最高
            if (isProductDetailPage(rootNode, pageText)) {
                currentPageType = PageType.PRODUCT_DETAIL;
            }
            // 2. 检测店铺首页
            else if (isShopHomePage(rootNode, pageText)) {
                currentPageType = PageType.SHOP_HOME;
            }
            // 3. 检测商品列表页
            else if (isProductListPage(rootNode, pageText)) {
                currentPageType = PageType.PRODUCT_LIST;
            }
            // 4. 检测搜索结果页
            else if (isSearchResultPage(rootNode, pageText)) {
                currentPageType = PageType.SEARCH_RESULT;
            }
            // 5. 检测分类页面
            else if (isCategoryPage(rootNode, pageText)) {
                currentPageType = PageType.CATEGORY;
            }
            // 6. 未知页面
            else {
                currentPageType = PageType.UNKNOWN;
            }
            
            // 记录页面历史
            if (currentPageType != previousPageType) {
                pageHistory.add(currentPageType);
                Log.d(TAG, "页面状态变化: " + previousPageType + " -> " + currentPageType);
                
                // 限制历史记录长度
                if (pageHistory.size() > 10) {
                    pageHistory.remove(0);
                }
            }
            
            return currentPageType;
            
        } catch (Exception e) {
            Log.e(TAG, "检测页面类型时发生错误", e);
            return PageType.UNKNOWN;
        }
    }
    
    /**
     * 检测是否为商品详情页
     */
    private boolean isProductDetailPage(AccessibilityNodeInfo rootNode, String pageText) {
        // 详情页特征关键词
        String[] detailKeywords = {
            "商品详情", "商品介绍", "规格参数", "用户评价", 
            "立即购买", "加入购物车", "商品评价", "产品参数",
            "choose", "buy now", "add to cart", "product detail"
        };
        
        // 检查关键词
        for (String keyword : detailKeywords) {
            if (pageText.contains(keyword.toLowerCase())) {
                Log.d(TAG, "发现详情页关键词: " + keyword);
                
                // 进一步验证：查找详情页特有的UI元素
                if (hasProductDetailElements(rootNode)) {
                    return true;
                }
            }
        }
        
        // 通过UI结构判断
        return hasProductDetailElements(rootNode);
    }
    
    /**
     * 检查是否有商品详情页特有元素
     */
    private boolean hasProductDetailElements(AccessibilityNodeInfo rootNode) {
        try {
            // 查找购买相关按钮
            List<AccessibilityNodeInfo> buyButtons = NodeUtils.findNodesByText(rootNode, 
                new String[]{"立即购买", "加入购物车", "现在购买", "buy now", "add to cart"});
            
            if (!buyButtons.isEmpty()) {
                Log.d(TAG, "找到购买按钮，确认为详情页");
                NodeUtils.recycleNodes(buyButtons);
                return true;
            }
            
            // 查找价格显示元素（详情页的价格格式通常更复杂）
            List<AccessibilityNodeInfo> priceNodes = NodeUtils.findNodesByResourceId(rootNode,
                new String[]{"price", "currentPrice", "jd_price"});
            
            if (!priceNodes.isEmpty()) {
                Log.d(TAG, "找到价格元素");
                NodeUtils.recycleNodes(priceNodes);
                
                // 同时查找规格选择器
                List<AccessibilityNodeInfo> specNodes = NodeUtils.findNodesByText(rootNode,
                    new String[]{"选择", "规格", "颜色", "尺寸", "版本"});
                
                if (!specNodes.isEmpty()) {
                    Log.d(TAG, "找到规格选择器，确认为详情页");
                    NodeUtils.recycleNodes(specNodes);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "检查详情页元素时发生错误", e);
            return false;
        }
    }
    
    /**
     * 检测是否为店铺首页
     */
    private boolean isShopHomePage(AccessibilityNodeInfo rootNode, String pageText) {
        String[] shopKeywords = {
            "店铺首页", "店铺", "关注店铺", "进入店铺", "shop", "store"
        };
        
        for (String keyword : shopKeywords) {
            if (pageText.contains(keyword.toLowerCase())) {
                Log.d(TAG, "发现店铺页关键词: " + keyword);
                return true;
            }
        }
        
        // 通过UI元素判断
        try {
            List<AccessibilityNodeInfo> shopElements = NodeUtils.findNodesByText(rootNode,
                new String[]{"关注", "收藏", "客服", "店铺评分"});
            
            if (!shopElements.isEmpty()) {
                NodeUtils.recycleNodes(shopElements);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "检查店铺页元素时发生错误", e);
        }
        
        return false;
    }
    
    /**
     * 检测是否为商品列表页
     */
    private boolean isProductListPage(AccessibilityNodeInfo rootNode, String pageText) {
        try {
            // 查找商品网格或列表容器
            List<AccessibilityNodeInfo> productNodes = NodeUtils.findProductNodes(rootNode);
            
            // 如果找到多个商品项，很可能是列表页
            if (productNodes.size() >= 2) {
                Log.d(TAG, "找到 " + productNodes.size() + " 个商品项，判断为列表页");
                NodeUtils.recycleNodes(productNodes);
                return true;
            }
            
            // 查找列表页特有的筛选和排序元素
            List<AccessibilityNodeInfo> filterNodes = NodeUtils.findNodesByText(rootNode,
                new String[]{"筛选", "排序", "价格", "销量", "filter", "sort"});
            
            if (!filterNodes.isEmpty()) {
                Log.d(TAG, "找到筛选排序元素，判断为列表页");
                NodeUtils.recycleNodes(filterNodes);
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "检查商品列表页时发生错误", e);
        }
        
        return false;
    }
    
    /**
     * 检测是否为搜索结果页
     */
    private boolean isSearchResultPage(AccessibilityNodeInfo rootNode, String pageText) {
        String[] searchKeywords = {
            "搜索结果", "search result", "找到", "共", "件商品"
        };
        
        for (String keyword : searchKeywords) {
            if (pageText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检测是否为分类页面
     */
    private boolean isCategoryPage(AccessibilityNodeInfo rootNode, String pageText) {
        String[] categoryKeywords = {
            "分类", "category", "全部分类", "商品分类"
        };
        
        for (String keyword : categoryKeywords) {
            if (pageText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    // ========== 状态查询方法 ==========
    
    public PageType getCurrentPageType() {
        return currentPageType;
    }
    
    public PageType getPreviousPageType() {
        return previousPageType;
    }
    
    public List<PageType> getPageHistory() {
        return new ArrayList<>(pageHistory);
    }
    
    public boolean isPageChanged() {
        return currentPageType != previousPageType;
    }
    
    public boolean canGoBack() {
        return pageHistory.size() > 1;
    }
    
    public PageType getExpectedPreviousPage() {
        if (pageHistory.size() >= 2) {
            return pageHistory.get(pageHistory.size() - 2);
        }
        return PageType.UNKNOWN;
    }
    
    /**
     * 重置页面状态
     */
    public void reset() {
        currentPageType = PageType.UNKNOWN;
        previousPageType = PageType.UNKNOWN;
        pageHistory.clear();
        Log.d(TAG, "页面状态已重置");
    }
    
    /**
     * 获取页面状态描述
     */
    public String getPageTypeDescription(PageType pageType) {
        switch (pageType) {
            case SHOP_HOME:
                return "店铺首页";
            case PRODUCT_LIST:
                return "商品列表";
            case PRODUCT_DETAIL:
                return "商品详情";
            case SEARCH_RESULT:
                return "搜索结果";
            case CATEGORY:
                return "分类页面";
            case UNKNOWN:
            default:
                return "未知页面";
        }
    }
}