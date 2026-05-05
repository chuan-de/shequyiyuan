'use client';

export type Rule = (value: string, values: Record<string, string>) => string | null;

export function validateForm(values: Record<string, string>, rules: Record<string, Rule[]>): Record<string, string> {
  const errors: Record<string, string> = {};
  Object.entries(rules).forEach(([field, fieldRules]) => {
    for (const rule of fieldRules) {
      const result = rule(values[field] ?? '', values);
      if (result) {
        errors[field] = result;
        break;
      }
    }
  });
  return errors;
}

export const required = (label: string): Rule => (value) => value.trim() ? null : `${label} is required`;
export const minLength = (label: string, length: number): Rule => (value) => value.length >= length ? null : `${label} must be at least ${length} characters`;
export const sameAs = (label: string, other: string): Rule => (value, values) => value === values[other] ? null : `${label} does not match`;
