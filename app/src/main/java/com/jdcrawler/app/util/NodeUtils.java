package com.jdcrawler.app.util;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AccessibilityNodeInfo 工具类
 * 提供各种节点查找、分析和操作的实用方法
 */
public class NodeUtils {
    
    private static final String TAG = "NodeUtils";
    
    // 价格匹配正则表达式
    private static final Pattern PRICE_PATTERN = Pattern.compile("¥?([0-9,]+\\.?[0-9]*)");
    
    // 商品节点常见的资源ID和类名
    private static final String[] PRODUCT_NODE_IDS = {
        "product_item", "goods_item", "item", "product", "goods",
        "list_item", "grid_item", "card", "cell"
    };
    
    private static final String[] PRODUCT_NODE_CLASSES = {
        "LinearLayout", "RelativeLayout", "FrameLayout", 
        "ConstraintLayout", "CardView"
    };
    
    /**
     * 在节点树中查找包含指定文本的节点
     */
    public static List<AccessibilityNodeInfo> findNodesByText(AccessibilityNodeInfo rootNode, String[] searchTexts) {
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        
        if (rootNode == null || searchTexts == null) {
            return foundNodes;
        }
        
        try {
            for (String searchText : searchTexts) {
                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(searchText);
                if (nodes != null && !nodes.isEmpty()) {
                    foundNodes.addAll(nodes);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "查找文本节点时发生错误", e);
        }
        
        return foundNodes;
    }
    
    /**
     * 根据资源ID查找节点
     */
    public static List<AccessibilityNodeInfo> findNodesByResourceId(AccessibilityNodeInfo rootNode, String[] resourceIds) {
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        
        if (rootNode == null || resourceIds == null) {
            return foundNodes;
        }
        
        try {
            for (String resourceId : resourceIds) {
                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(resourceId);
                if (nodes != null && !nodes.isEmpty()) {
                    foundNodes.addAll(nodes);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "查找资源ID节点时发生错误", e);
        }
        
        return foundNodes;
    }
    
    /**
     * 查找商品节点
     * 智能识别页面中的商品卡片或列表项
     */
    public static List<AccessibilityNodeInfo> findProductNodes(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> productNodes = new ArrayList<>();
        
        if (rootNode == null) {
            return productNodes;
        }
        
        try {
            // 方法1: 通过资源ID查找
            List<AccessibilityNodeInfo> idNodes = findNodesByResourceId(rootNode, PRODUCT_NODE_IDS);
            for (AccessibilityNodeInfo node : idNodes) {
                if (isValidProductNode(node)) {
                    productNodes.add(node);
                }
            }
            
            // 方法2: 如果通过ID没找到足够的节点，尝试结构分析
            if (productNodes.size() < 2) {
                productNodes.clear();
                recycleNodes(idNodes);
                
                List<AccessibilityNodeInfo> structuralNodes = findProductNodesByStructure(rootNode);
                productNodes.addAll(structuralNodes);
            }
            
            Log.d(TAG, "找到 " + productNodes.size() + " 个潜在商品节点");
            return productNodes;
            
        } catch (Exception e) {
            Log.e(TAG, "查找商品节点时发生错误", e);
            return productNodes;
        }
    }
    
    /**
     * 通过结构分析查找商品节点
     */
    private static List<AccessibilityNodeInfo> findProductNodesByStructure(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> productNodes = new ArrayList<>();
        
        try {
            // 查找包含价格和文本的容器节点
            findProductNodesRecursive(rootNode, productNodes);
            
            // 过滤和去重
            List<AccessibilityNodeInfo> filteredNodes = new ArrayList<>();
            for (AccessibilityNodeInfo node : productNodes) {
                if (isValidProductNode(node) && !isNodeInList(node, filteredNodes)) {
                    filteredNodes.add(node);
                }
            }
            
            return filteredNodes;
            
        } catch (Exception e) {
            Log.e(TAG, "结构分析查找商品节点时发生错误", e);
            return productNodes;
        }
    }
    
    /**
     * 递归查找商品节点
     */
    private static void findProductNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> productNodes) {
        if (node == null) return;
        
        try {
            // 检查当前节点是否是商品节点
            if (looksLikeProductNode(node)) {
                productNodes.add(node);
                return; // 找到商品节点就不再深入其子节点
            }
            
            // 递归检查子节点
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    findProductNodesRecursive(child, productNodes);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "递归查找时发生错误", e);
        }
    }
    
    /**
     * 判断节点是否看起来像商品节点
     */
    private static boolean looksLikeProductNode(AccessibilityNodeInfo node) {
        try {
            String nodeText = getNodeText(node);
            
            // 检查是否包含价格信息
            boolean hasPrice = PRICE_PATTERN.matcher(nodeText).find();
            
            // 检查是否包含商品相关关键词
            boolean hasProductKeywords = nodeText.contains("￥") || 
                                       nodeText.contains("元") ||
                                       nodeText.contains("价格") ||
                                       nodeText.contains("购买") ||
                                       nodeText.contains("¥");
            
            // 检查节点大小（商品节点通常有一定的显示区域）
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            boolean hasReasonableSize = bounds.width() > 100 && bounds.height() > 100;
            
            // 检查是否可点击
            boolean isClickable = node.isClickable() || hasClickableParent(node);
            
            return hasPrice && hasProductKeywords && hasReasonableSize && isClickable;
            
        } catch (Exception e) {
            Log.e(TAG, "判断商品节点时发生错误", e);
            return false;
        }
    }
    
    /**
     * 验证是否为有效的商品节点
     */
    private static boolean isValidProductNode(AccessibilityNodeInfo node) {
        try {
            if (node == null) return false;
            
            // 检查节点是否可见
            if (!node.isVisibleToUser()) return false;
            
            // 检查节点边界
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.width() <= 0 || bounds.height() <= 0) return false;
            
            // 检查是否包含文本内容
            String nodeText = getNodeText(node);
            if (nodeText.trim().isEmpty()) return false;
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "验证商品节点时发生错误", e);
            return false;
        }
    }
    
    /**
     * 检查节点是否在列表中
     */
    private static boolean isNodeInList(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> nodeList) {
        try {
            Rect nodeBounds = new Rect();
            node.getBoundsInScreen(nodeBounds);
            
            for (AccessibilityNodeInfo existingNode : nodeList) {
                Rect existingBounds = new Rect();
                existingNode.getBoundsInScreen(existingBounds);
                
                // 如果边界重叠度很高，认为是同一个节点
                if (boundsOverlapSignificantly(nodeBounds, existingBounds)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "检查节点重复时发生错误", e);
            return false;
        }
    }
    
    /**
     * 检查两个边界是否显著重叠
     */
    private static boolean boundsOverlapSignificantly(Rect bounds1, Rect bounds2) {
        if (!Rect.intersects(bounds1, bounds2)) {
            return false;
        }
        
        Rect intersection = new Rect();
        intersection.setIntersect(bounds1, bounds2);
        
        int intersectionArea = intersection.width() * intersection.height();
        int bounds1Area = bounds1.width() * bounds1.height();
        int bounds2Area = bounds2.width() * bounds2.height();
        
        // 如果交集面积占任一边界面积的80%以上，认为是显著重叠
        double overlapRatio1 = (double) intersectionArea / bounds1Area;
        double overlapRatio2 = (double) intersectionArea / bounds2Area;
        
        return overlapRatio1 > 0.8 || overlapRatio2 > 0.8;
    }
    
    /**
     * 检查节点或其父节点是否可点击
     */
    private static boolean hasClickableParent(AccessibilityNodeInfo node) {
        try {
            AccessibilityNodeInfo current = node.getParent();
            int depth = 0;
            
            while (current != null && depth < 5) { // 最多检查5层父节点
                if (current.isClickable()) {
                    return true;
                }
                AccessibilityNodeInfo parent = current.getParent();
                current.recycle();
                current = parent;
                depth++;
            }
            
            if (current != null) {
                current.recycle();
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "检查可点击父节点时发生错误", e);
            return false;
        }
    }
    
    /**
     * 查找可滚动的节点
     */
    public static AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo rootNode) {
        if (rootNode == null) return null;
        
        try {
            return findScrollableNodeRecursive(rootNode);
        } catch (Exception e) {
            Log.e(TAG, "查找可滚动节点时发生错误", e);
            return null;
        }
    }
    
    /**
     * 递归查找可滚动节点
     */
    private static AccessibilityNodeInfo findScrollableNodeRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        try {
            // 检查当前节点是否可滚动
            if (node.isScrollable()) {
                return node;
            }
            
            // 递归检查子节点
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo scrollableChild = findScrollableNodeRecursive(child);
                    if (scrollableChild != null) {
                        return scrollableChild;
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "递归查找可滚动节点时发生错误", e);
            return null;
        }
    }
    
    /**
     * 获取节点及其所有子节点的文本内容
     */
    public static String getNodeText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        
        StringBuilder textBuilder = new StringBuilder();
        
        try {
            // 获取当前节点的文本
            CharSequence nodeText = node.getText();
            if (nodeText != null && nodeText.length() > 0) {
                textBuilder.append(nodeText).append(" ");
            }
            
            // 获取内容描述
            CharSequence contentDesc = node.getContentDescription();
            if (contentDesc != null && contentDesc.length() > 0) {
                textBuilder.append(contentDesc).append(" ");
            }
            
            // 递归获取子节点文本
            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    String childText = getNodeText(child);
                    if (!childText.isEmpty()) {
                        textBuilder.append(childText).append(" ");
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "获取节点文本时发生错误", e);
        }
        
        return textBuilder.toString().trim();
    }
    
    /**
     * 从文本中提取价格
     */
    public static String extractPrice(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        try {
            Matcher matcher = PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1); // 返回数字部分
            }
        } catch (Exception e) {
            Log.e(TAG, "提取价格时发生错误", e);
        }
        
        return "";
    }
    
    /**
     * 回收节点列表
     */
    public static void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) return;
        
        try {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    node.recycle();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "回收节点时发生错误", e);
        }
    }
    
    /**
     * 安全地回收单个节点
     */
    public static void recycleNode(AccessibilityNodeInfo node) {
        try {
            if (node != null) {
                node.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "回收节点时发生错误", e);
        }
    }
    
    /**
     * 获取节点的可读描述（用于调试）
     */
    public static String getNodeDescription(AccessibilityNodeInfo node) {
        if (node == null) return "null";
        
        try {
            StringBuilder desc = new StringBuilder();
            desc.append("类名: ").append(node.getClassName());
            
            if (node.getText() != null) {
                desc.append(", 文本: ").append(node.getText());
            }
            
            if (node.getContentDescription() != null) {
                desc.append(", 描述: ").append(node.getContentDescription());
            }
            
            if (node.getViewIdResourceName() != null) {
                desc.append(", ID: ").append(node.getViewIdResourceName());
            }
            
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            desc.append(", 边界: ").append(bounds);
            
            desc.append(", 可点击: ").append(node.isClickable());
            desc.append(", 可滚动: ").append(node.isScrollable());
            
            return desc.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "获取节点描述时发生错误", e);
            return "错误: " + e.getMessage();
        }
    }
}