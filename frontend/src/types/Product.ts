export interface Product {
  id: string;
  sku: string;
  name: string;
  category: ProductCategory;
  currentPrice: number;
  stockLevel: number;
  reorderThreshold: number;
  demandVelocity: number;
  status: ProductStatus;
}

export enum ProductCategory {
  ELECTRONICS = "ELECTRONICS",
  APPAREL = "APPAREL",
  HOME = "HOME"
}

export enum ProductStatus {
  ACTIVE = "ACTIVE",
  PRICE_REVIEW_PENDING = "PRICE_REVIEW_PENDING",
  OUT_OF_STOCK = "OUT_OF_STOCK"
}

export enum ChangeDirection {
  INCREASE = "INCREASE",
  DECREASE = "DECREASE",
  HOLD = "HOLD"
}

export enum SuggestionStatus {
  PENDING = "PENDING",
  ACCEPTED = "ACCEPTED",
  REJECTED = "REJECTED"
}

export enum TriggerReason {
  INITIAL = "INITIAL",
  INVENTORY_LOW = "INVENTORY_LOW",
  DEMAND_SPIKE = "DEMAND_SPIKE",
  MANUAL = "MANUAL"
}

export enum RedorderStatus {
  PENDING = "PENDING",
  ACCEPTED = "ACCEPTED",
  REJECTED = "REJECTED"
}