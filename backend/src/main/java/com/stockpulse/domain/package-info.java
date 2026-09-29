/**
 * Domain entities for the StockPulse pricing optimization system.
 * 
 * This package contains all the core business entities that represent the 
 * domain model of the application.
 * 
 * Entities:
 * - Product: Represents a product in inventory with pricing and stock information
 * - PricingSuggestion: Represents a pricing recommendation for a product
 * - ReorderSuggestion: Represents a reorder recommendation for a product
 * 
 * Enums:
 * - ProductCategory: Defines product categories (ELECTRONICS, APPAREL, HOME)
 * - ProductStatus: Defines product availability status (ACTIVE, PRICE_REVIEW_PENDING, OUT_OF_STOCK)
 * - ChangeDirection: Defines pricing change direction (INCREASE, DECREASE, HOLD)
 * - SuggestionStatus: Defines suggestion status (PENDING, ACCEPTED, REJECTED)
 * - TriggerReason: Defines reasons for triggering suggestions (INITIAL, INVENTORY_LOW, DEMAND_SPIKE, MANUAL)
 */
package com.stockpulse.domain;