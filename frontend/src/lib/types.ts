export type Money = { amount: string; currency: string };
export type Quantity = { value: string; unit: "GRAM" | "MILLILITER" | "EACH" };

export type Ingredient = { key: string; name: string; quantity: Quantity };
export type Meal = {
  slot: string;
  title: string;
  localizedTitle: string;
  description: string;
  minutes: number;
  servings: number;
  dietaryLabel: string;
  ingredients: Ingredient[];
  method: string[];
  reasons: string[];
};

export type ProductAlternative = {
  sku: string;
  brand: string;
  productName: string;
  packageSize: Quantity;
  packagePrice: Money;
  reason: string;
};

export type BasketLine = {
  id: string;
  ingredientKey: string;
  ingredientName: string;
  sku: string;
  brand: string;
  productName: string;
  required: Quantity;
  packageSize: Quantity;
  packageCount: number;
  leftover: Quantity;
  lineTotal: Money;
  selected: boolean;
  pantryStaple: boolean;
  estimatedWeight: boolean;
  availability: string;
  alternatives: ProductAlternative[];
};

export type Plan = {
  id: string;
  status: string;
  householdSize: number;
  dinnerCount: number;
  budget: Money;
  total: Money;
  remaining: Money;
  meals: Meal[];
  basket: {
    id: string;
    status: string;
    lines: BasketLine[];
    completeTotal: Money;
    alreadyAtHome: Money;
    toBuy: Money;
    selectedCount: number;
    cartReference?: string;
  };
  notices: string[];
  updatedAt: string;
  version: number;
};

export type PlanRequest = {
  householdSize: number;
  dinnerCount: number;
  budget: Money;
  maxCookingMinutes: number | null;
  priority: "BALANCED" | "LOWER_COST" | "QUICKER" | "PROTEIN_RICH";
  dietaryRules: string[];
  allergens: string[];
  excludedIngredients: string[];
  requirementsReviewed: boolean;
};
